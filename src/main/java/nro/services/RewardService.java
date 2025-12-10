package nro.services;

import nro.ahwuocdz.ActivationSetData;
import nro.attr.Attribute;
import nro.consts.ConstAttribute;
import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.consts.ConstMob;
import nro.consts.ItemClothesData;
import nro.event.Event;
import nro.lib.RandomCollection;
import nro.models.item.ItemLuckyRound;
import nro.models.item.ItemOptionLuckyRound;
import nro.models.item.ItemReward;
import nro.models.mob.MobReward;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.server.Manager;
import nro.server.ServerLog;
import nro.server.ServerManager;
import nro.server.ServerNotify;
import nro.services.func.CombineServiceNew;
import nro.utils.TimeUtil;
import nro.utils.Util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 💖 ahwuocdz 💖
 * 
 */
public class RewardService {

    private static RewardService i;

    private static final int MAX_SKH_DROP_PER_HOUR = 10;
    private static int skhDropCount = 0;
    private static long lastResetSkhDropTime = System.currentTimeMillis();
    private static final int MAX_DO_THAN_DROP_PER_DAY = 10;

    private RewardService() {
    }

    // Reset counter nếu đã qua 1 giờ
    private static void checkResetSkhDropCount() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetSkhDropTime >= 3600000) { // 1 giờ = 3600000ms
            skhDropCount = 0;
            lastResetSkhDropTime = currentTime;
        }
    }

    // Kiểm tra có thể drop SKH không
    private static boolean canDropSKH() {
        checkResetSkhDropCount();
        return skhDropCount < MAX_SKH_DROP_PER_HOUR;
    }

    private static int getDoThanDropCountToday() {
        String today = TimeUtil.getTimeNow("yyyy-MM-dd"); // Lấy ngày hôm nay theo giờ VN
        try (java.sql.Connection conn = nro.jdbc.DBService.gI().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM do_than_drop_log WHERE DATE(CONVERT_TZ(drop_time, '+00:00', '+07:00')) = ?")) {
            ps.setString(1, today);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Kiểm tra có thể drop đồ thần không
    private static boolean canDropDoThan() {
        return getDoThanDropCountToday() < MAX_DO_THAN_DROP_PER_DAY;
    }

    // Lưu log drop đồ thần vào DB
    private static void logDoThanDrop(String playerName, long playerId, short itemId, String itemName) {
        try (java.sql.Connection conn = nro.jdbc.DBService.gI().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO do_than_drop_log (player_id, player_name, item_id, item_name, drop_time) VALUES (?, ?, ?, ?, NOW())")) {
            ps.setLong(1, playerId);
            ps.setString(2, playerName);
            ps.setShort(3, itemId);
            ps.setString(4, itemName);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static RewardService gI() {
        if (i == null) {
            i = new RewardService();
        }
        return i;
    }

    private MobReward getMobReward(Mob mob) {
        for (MobReward mobReward : Manager.MOB_REWARDS) {
            if (mobReward.tempId == mob.tempId) {
                return mobReward;
            }
        }
        return null;
    }

    // Danh sách map được phép drop Set Kích Hoạt
    private static final int[] MAP_DROP_SKH = { 1, 2, 3, 8, 9, 11, 15, 16, 17 };

    private boolean isMapDropSKH(int mapId) {
        for (int id : MAP_DROP_SKH) {
            if (mapId == id) {
                return true;
            }
        }
        return false;
    }

    // trả về list item quái die
    public List<ItemMap> getRewardItems(Player player, Mob mob, int x, int yEnd) {
        int mapid = player.zone.map.mapId;
        List<ItemMap> list = new ArrayList<>();
        MobReward mobReward = getMobReward(mob);
        if (mobReward != null) {
            int itemSize = mobReward.itemRewards.size();
            int goldSize = mobReward.goldRewards.size();
            int cskbSize = mobReward.capsuleKyBi.size();
            int foodSize = mobReward.foods.size();
            int biKiepSize = mobReward.biKieps.size();
            if (itemSize > 0) {
                ItemReward ir = mobReward.itemRewards.get(Util.nextInt(0, itemSize - 1));
                boolean inMap = false;
                if (ir.mapId[0] == -1) {
                    inMap = true;
                } else {
                    for (int i = 0; i < ir.mapId.length; i++) {
                        if (mob.zone.map.mapId == ir.mapId[i]) {
                            inMap = true;
                            break;
                        }
                    }
                }
                if (inMap) {
                    if (ir.forAllGender || ItemService.gI().getTemplate(ir.tempId).gender == player.gender
                            || ItemService.gI().getTemplate(ir.tempId).gender > 2) {

                        // up SKH - chỉ drop ở map 1, 2, 3 - giới hạn 10 món/giờ
                        if (isMapDropSKH(mapid) && Util.isTrueDrop(1, 50, player) || (player.getSession() != null && player.isAdmin() ) ) {
                            ItemClothesData.ClothesSet clothes = ItemClothesData.getClothes(player.gender, 1);
                            if (clothes != null) {
                                int[] clothesItems = {clothes.getAo(), clothes.getQuan(), clothes.getGang(), clothes.getGiay(), clothes.getRada()};
                                int randomIndex = Util.nextInt(clothesItems.length);
                                int selectedItemId = clothesItems[randomIndex];
                                
                                ItemMap itemMap = new ItemMap(mob.zone, selectedItemId, 1, x, yEnd, player.id);
                                initBaseOptionClothes(itemMap.itemTemplate.id, itemMap.itemTemplate.type,
                                        itemMap.options);
                                initActivationOption(player.gender, 4, itemMap.options);
                                list.add(itemMap);
                                ServerNotify.gI().notify(player.name + " vừa nhặt được " + itemMap.itemTemplate.name
                                        + " Set Kích Hoạt");
                            }
                        } else if (Util.isTrueDrop(ir.ratio, ir.typeRatio, player)) {
                            ItemMap itemMap = new ItemMap(mob.zone, ir.tempId, 1, x, yEnd, player.id);
                            // init option
                            switch (itemMap.itemTemplate.type) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                    initBaseOptionClothes(itemMap.itemTemplate.id, itemMap.itemTemplate.type,
                                            itemMap.options);
                                    initStarOption(itemMap, new RatioStar[] { new RatioStar((byte) 1, 20, 100),
                                            new RatioStar((byte) 2, 10, 100), new RatioStar((byte) 3, 5, 100),
                                            new RatioStar((byte) 4, 3, 200), new RatioStar((byte) 5, 2, 200),
                                            new RatioStar((byte) 6, 1, 200), new RatioStar((byte) 7, 1, 300), });
                                    initDepositOption(itemMap);
                                    break;
                                case 30:
                                    initBaseOptionSaoPhaLe(itemMap);
                                    break;
                            }
                            initNotTradeOption(itemMap);
                            initExpiryDateOption(itemMap);
                            initEventOption(itemMap);

                            // end init option
                            if (itemMap.itemTemplate.id >= 555 && itemMap.itemTemplate.id <= 567
                                    || itemMap.itemTemplate.id == 2009) {
                                ServerNotify.gI().notify(player.name + " vừa nhặt được " + itemMap.itemTemplate.name
                                        + " tại " + mob.zone.map.mapName + " khu vực " + mob.zone.zoneId);
                                ServerLog.logItemDrop(player.name, itemMap.itemTemplate.name);
                            }
                            list.add(itemMap);
                        }

                    }
                }
                if (cskbSize > 0) {
                    if (player.itemTime.isUseMayDo) {
                        ItemReward cskb = mobReward.capsuleKyBi.get(Util.nextInt(0, cskbSize - 1));
                        if (Util.isTrueDrop(cskb.ratio, cskb.typeRatio, player)) {
                            ItemMap itemMap = new ItemMap(mob.zone, cskb.tempId, 1, x, yEnd, player.id);
                            list.add(itemMap);
                        }
                    }
                }
                if (foodSize > 0) {
                    if (player.setClothes.godClothes) {
                        ItemReward food = mobReward.foods.get(Util.nextInt(0, foodSize - 1));
                        if (Util.isTrueDrop(food.ratio, food.typeRatio, player)) {
                            ItemMap itemMap = new ItemMap(mob.zone, food.tempId, 1, x, yEnd, player.id);
                            list.add(itemMap);
                        }
                    }
                }
                if (biKiepSize > 0) {
                    if (player.cFlag > 0) {
                        ItemReward biKiep = mobReward.biKieps.get(Util.nextInt(0, biKiepSize - 1));
                        if (Util.isTrueDrop(biKiep.ratio, biKiep.typeRatio, player)) {
                            ItemMap itemMap = new ItemMap(mob.zone, biKiep.tempId, 1, x, yEnd, player.id);
                            list.add(itemMap);
                        }
                    }
                }
                if (goldSize > 0 && biKiepSize <= 0 && foodSize <= 0 && cskbSize <= 0) {
                    ItemReward gr = mobReward.goldRewards.get(Util.nextInt(0, goldSize - 1));
                    if (Util.isTrueDrop(gr.ratio, gr.typeRatio, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone, gr.tempId, 1, x, yEnd, player.id);
                        initQuantityGold(itemMap);
                        list.add(itemMap);
                    }
                }
                if (mob.tempId == ConstMob.HIRUDEGARN) {
                    RandomCollection<Integer> rd = new RandomCollection<>();
                    rd.add(1, 1066);
                    rd.add(20, 861);
                    rd.add(5, 15);
                    rd.add(10, 17);
                    if (Util.isTrueDrop(10, 100, player)) {
                        ItemMap trungMabu = new ItemMap(mob.zone, (short) 568, 1, x, yEnd, player.id);
                        list.add(trungMabu);
                    }

                    for (int i = 0; i < 3; i++) {
                        int itemID = rd.next();
                        ItemMap itemMap = new ItemMap(mob.zone, itemID, 1, x + Util.nextInt(-50, 50), yEnd, player.id);
                        list.add(itemMap);
                    }
                    for (int i = 0; i < 10; i++) {
                        ItemReward gr = mobReward.goldRewards.get(Util.nextInt(0, goldSize - 1));
                        if (Util.isTrueDrop(gr.ratio, gr.typeRatio, player)) {
                            ItemMap itemMap = new ItemMap(mob.zone, gr.tempId, 1, x + Util.nextInt(-50, 50), yEnd,
                                    player.id);
                            initQuantityGold(itemMap);
                            list.add(itemMap);
                        }
                    }
                }

                if (mapid >= 169 && mapid <= 171) {
                    if (Util.isTrueDrop(1, 10, player)) {
                        Item HONG_NGOC = ItemService.gI().createNewItem((short) ConstItem.HONG_NGOC);
                        InventoryService.gI().addItemBag(player, HONG_NGOC, 1);
                        InventoryService.gI().sendItemBags(player);
                    }
                    if (Util.isTrueDrop(1, 10, player)) {
                        Item THOI_VANG = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG);
                        InventoryService.gI().addItemBag(player, THOI_VANG, 1);
                        InventoryService.gI().sendItemBags(player);
                    }
                }
                if (MapService.gI().isMapCold(mapid) && Util.isTrueDrop(1, 1000, player) && canDropDoThan()) {
                    int randomIndex = Util.nextInt(0, ConstItem.SET_DO_THAN.length - 1);
                    ItemMap itemMapCold = new ItemMap(mob.zone, ConstItem.SET_DO_THAN[randomIndex], 1, x, yEnd,
                            player.id);
                    initBaseOptionClothes(itemMapCold.itemTemplate.id, itemMapCold.itemTemplate.type,
                            itemMapCold.options);
                    initStarOption(itemMapCold, new RatioStar[] {
                            new RatioStar((byte) 1, 1, 2),
                            new RatioStar((byte) 2, 1, 3),
                            new RatioStar((byte) 3, 1, 4),
                            new RatioStar((byte) 4, 1, 5),
                            new RatioStar((byte) 5, 1, 6),
                            new RatioStar((byte) 6, 1, 7),
                            new RatioStar((byte) 7, 1, 8)
                    });
                    list.add(itemMapCold);
                    logDoThanDrop(player.name, player.id, itemMapCold.itemTemplate.id, itemMapCold.itemTemplate.name);
                    ServerNotify.gI().notify(player.name + " vừa nhặt được " + itemMapCold.itemTemplate.name
                            + " Đồ Thần");
                }
                if (MapService.gI().isMapNguHanhSon(mapid)) {
                    if (Util.isTrueDrop(1, 500, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone, ConstItem.QUA_HONG_DAO, 1, x, yEnd, player.id);
                        itemMap.options.add(new ItemOption(74, 0));
                        list.add(itemMap);
                    }
                    if (Util.isTrueDrop(1, 1000, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone, ConstItem.CHU_AN, 1, x, yEnd, player.id);
                        itemMap.options.add(new ItemOption(74, 0));
                        list.add(itemMap);
                    }
                } else if (MapService.gI().isMapDoanhTrai(mapid)) {
                    if (Util.isTrueDrop(1, 30, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone, ConstItem.CHU_KHAI, 1, x, yEnd, player.id);
                        itemMap.options.add(new ItemOption(74, 0));
                        list.add(itemMap);
                    }
                } else if (MapService.gI().isMapBanDoKhoBau(mapid)) {
                    if (Util.isTrueDrop(1, 30, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone, ConstItem.CHU_PHONG, 1, x, yEnd, player.id);
                        itemMap.options.add(new ItemOption(74, 0));
                        list.add(itemMap);
                    }
                }
                if (mapid >= 165 && mapid <= 167) {
                    ItemMap itemMap = new ItemMap(mob.zone,
                            Util.nextInt(CombineServiceNew.KHAM_DA_CUONG_HOA_ITEM_SD_ID,
                                    CombineServiceNew.KHAM_DA_CUONG_HOA_ITEM_KI_ID),
                            1,
                            x, yEnd, player.id);
                    list.add(itemMap);
                }
                if (mapid >= 160 && mapid <= 163) {
                    if (Util.isTrueDrop(1, 1000, player)) {
                        ItemMap itemMap = new ItemMap(mob.zone,
                                Util.nextInt(ConstItem.MANH_AO, ConstItem.MANH_GANG_TAY), 1,
                                x, yEnd, player.id);
                        list.add(itemMap);
                    }
                }
                if (Event.isEvent()) {
                    Event.getInstance().dropItem(player, mob, list, x, yEnd);
                }
                if (mapid == 153) {// map bang
                    int numMenber = player.zone.getPlayersSameClan(player.clan.id).size();
                    if (numMenber >= 2) {
                        if (Util.isTrueDrop(1, 500, player)) {
                            player.clanMember.memberPoint++;
                            Service.getInstance().sendThongBao(player, "Bạn nhận được capsule bang hội");
                        }
                    }
                }

                if (mapid >= 168 && mapid <= 171 && Util.isTrueDrop(1, 100, player)) {
                    list.add(new ItemMap(mob.zone, 2150, 1, x, yEnd, player.id));
                }
                // BDKB
                if (mapid >= 135 && mapid <= 138 && Util.isTrueDrop(10, 100, player)) {
                    int soluong = 1;
                    list.add(new ItemMap(mob.zone, 934, soluong, x, yEnd, player.id));
                }
            }
        }
        return list;

    }

    private void initQuantityGold(ItemMap item) {
        switch (item.itemTemplate.id) {
            case 76:
                item.quantity = Util.nextInt(1000, 5000);
                break;
            case 188:
                item.quantity = Util.nextInt(5000, 10000);
                break;
            case 189:
                item.quantity = Util.nextInt(10000, 20000);
                break;
            case 190:
                item.quantity = Util.nextInt(20000, 30000);
                break;
        }
        Attribute at = ServerManager.gI().getAttributeManager().find(ConstAttribute.VANG);
        if (at != null && !at.isExpired()) {
            item.quantity += item.quantity * at.getValue() / 100;
        }
    }

    // chỉ số cơ bản: hp, ki, hồi phục, sđ, crit
    public void initBaseOptionClothes(int tempId, int type, List<ItemOption> list) {
        int[][] option_param = { { -1, -1 }, { -1, -1 }, { -1, -1 }, { -1, -1 }, { -1, -1 } };
        switch (type) {
            case 0: // áo
                option_param[0][0] = 47; // giáp
                switch (tempId) {
                    case 0:
                        option_param[0][1] = 2;
                        break;
                    case 33:
                        option_param[0][1] = 4;
                        break;
                    case 3:
                        option_param[0][1] = 8;
                        break;
                    case 34:
                        option_param[0][1] = 16;
                        break;
                    case 136:
                        option_param[0][1] = 24;
                        break;
                    case 137:
                        option_param[0][1] = 40;
                        break;
                    case 138:
                        option_param[0][1] = 60;
                        break;
                    case 139:
                        option_param[0][1] = 90;
                        break;
                    case 230:
                        option_param[0][1] = 200;
                        break;
                    case 231:
                        option_param[0][1] = 250;
                        break;
                    case 232:
                        option_param[0][1] = 300;
                        break;
                    case 233:
                        option_param[0][1] = 400;
                        break;
                    case 1:
                        option_param[0][1] = 2;
                        break;
                    case 41:
                        option_param[0][1] = 4;
                        break;
                    case 4:
                        option_param[0][1] = 8;
                        break;
                    case 42:
                        option_param[0][1] = 16;
                        break;
                    case 152:
                        option_param[0][1] = 24;
                        break;
                    case 153:
                        option_param[0][1] = 40;
                        break;
                    case 154:
                        option_param[0][1] = 60;
                        break;
                    case 155:
                        option_param[0][1] = 90;
                        break;
                    case 234:
                        option_param[0][1] = 200;
                        break;
                    case 235:
                        option_param[0][1] = 250;
                        break;
                    case 236:
                        option_param[0][1] = 300;
                        break;
                    case 237:
                        option_param[0][1] = 400;
                        break;
                    case 2:
                        option_param[0][1] = 3;
                        break;
                    case 49:
                        option_param[0][1] = 5;
                        break;
                    case 5:
                        option_param[0][1] = 10;
                        break;
                    case 50:
                        option_param[0][1] = 20;
                        break;
                    case 168:
                        option_param[0][1] = 30;
                        break;
                    case 169:
                        option_param[0][1] = 50;
                        break;
                    case 170:
                        option_param[0][1] = 70;
                        break;
                    case 171:
                        option_param[0][1] = 100;
                        break;
                    case 238:
                        option_param[0][1] = 230;
                        break;
                    case 239:
                        option_param[0][1] = 280;
                        break;
                    case 240:
                        option_param[0][1] = 330;
                        break;
                    case 241:
                        option_param[0][1] = 450;
                        break;
                    case 555: // áo thần trái đất
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 800;
                        option_param[2][1] = 15;
                        break;
                    case 557: // áo thần namếc
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 800;
                        option_param[2][1] = 15;
                        break;
                    case 559: // áo thần xayda
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 800;
                        option_param[2][1] = 15;
                        break;
                }
                break;
            case 1: // quần
                option_param[0][0] = 6; // hp
                option_param[1][0] = 27; // hp hồi/30s
                switch (tempId) {
                    case 6:
                        option_param[0][1] = 30;
                        break;
                    case 35:
                        option_param[0][1] = 150;
                        option_param[1][1] = 12;
                        break;
                    case 9:
                        option_param[0][1] = 300;
                        option_param[1][1] = 40;
                        break;
                    case 36:
                        option_param[0][1] = 600;
                        option_param[1][1] = 120;
                        break;
                    case 140:
                        option_param[0][1] = 1400;
                        option_param[1][1] = 280;
                        break;
                    case 141:
                        option_param[0][1] = 3000;
                        option_param[1][1] = 600;
                        break;
                    case 142:
                        option_param[0][1] = 6000;
                        option_param[1][1] = 1200;
                        break;
                    case 143:
                        option_param[0][1] = 10000;
                        option_param[1][1] = 2000;
                        break;
                    case 242:
                        option_param[0][1] = 14000;
                        option_param[1][1] = 2500;
                        break;
                    case 243:
                        option_param[0][1] = 18000;
                        option_param[1][1] = 3000;
                        break;
                    case 244:
                        option_param[0][1] = 22000;
                        option_param[1][1] = 3500;
                        break;
                    case 245:
                        option_param[0][1] = 26000;
                        option_param[1][1] = 4000;
                        break;
                    case 7:
                        option_param[0][1] = 20;
                        break;
                    case 43:
                        option_param[0][1] = 25;
                        option_param[1][1] = 10;
                        break;
                    case 10:
                        option_param[0][1] = 120;
                        option_param[1][1] = 28;
                        break;
                    case 44:
                        option_param[0][1] = 250;
                        option_param[1][1] = 100;
                        break;
                    case 156:
                        option_param[0][1] = 600;
                        option_param[1][1] = 240;
                        break;
                    case 157:
                        option_param[0][1] = 1200;
                        option_param[1][1] = 480;
                        break;
                    case 158:
                        option_param[0][1] = 2400;
                        option_param[1][1] = 960;
                        break;
                    case 159:
                        option_param[0][1] = 4800;
                        option_param[1][1] = 1800;
                        break;
                    case 246:
                        option_param[0][1] = 13000;
                        option_param[1][1] = 2200;
                        break;
                    case 247:
                        option_param[0][1] = 17000;
                        option_param[1][1] = 2700;
                        break;
                    case 248:
                        option_param[0][1] = 21000;
                        option_param[1][1] = 3200;
                        break;
                    case 249:
                        option_param[0][1] = 25000;
                        option_param[1][1] = 3700;
                        break;
                    case 8:
                        option_param[0][1] = 20;
                        break;
                    case 51:
                        option_param[0][1] = 20;
                        option_param[1][1] = 8;
                        break;
                    case 11:
                        option_param[0][1] = 100;
                        option_param[1][1] = 20;
                        break;
                    case 52:
                        option_param[0][1] = 200;
                        option_param[1][1] = 80;
                        break;
                    case 172:
                        option_param[0][1] = 500;
                        option_param[1][1] = 200;
                        break;
                    case 173:
                        option_param[0][1] = 1000;
                        option_param[1][1] = 400;
                        break;
                    case 174:
                        option_param[0][1] = 2000;
                        option_param[1][1] = 800;
                        break;
                    case 175:
                        option_param[0][1] = 4000;
                        option_param[1][1] = 1600;
                        break;
                    case 250:
                        option_param[0][1] = 12000;
                        option_param[1][1] = 2100;
                        break;
                    case 251:
                        option_param[0][1] = 16000;
                        option_param[1][1] = 2600;
                        break;
                    case 252:
                        option_param[0][1] = 20000;
                        option_param[1][1] = 3100;
                        break;
                    case 253:
                        option_param[0][1] = 24000;
                        option_param[1][1] = 3600;
                        break;
                    case 556: // quần thần trái đất
                        option_param[0][0] = 22; // hp
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 52;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 15;
                        break;
                    case 558: // quần thần namếc
                        option_param[0][0] = 22; // hp
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 50;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 15;
                        break;
                    case 560: // quần thần xayda
                        option_param[0][0] = 22; // hp
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 48;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 15;
                        break;
                }
                break;
            case 2: // găng
                option_param[0][0] = 0; // sđ
                switch (tempId) {
                    case 21:
                        option_param[0][1] = 4;
                        break;
                    case 24:
                        option_param[0][1] = 7;
                        break;
                    case 37:
                        option_param[0][1] = 14;
                        break;
                    case 38:
                        option_param[0][1] = 28;
                        break;
                    case 144:
                        option_param[0][1] = 55;
                        break;
                    case 145:
                        option_param[0][1] = 110;
                        break;
                    case 146:
                        option_param[0][1] = 220;
                        break;
                    case 147:
                        option_param[0][1] = 530;
                        break;
                    case 254:
                        option_param[0][1] = 680;
                        break;
                    case 255:
                        option_param[0][1] = 1000;
                        break;
                    case 256:
                        option_param[0][1] = 1500;
                        break;
                    case 257:
                        option_param[0][1] = 2200;
                        break;
                    case 22:
                        option_param[0][1] = 3;
                        break;
                    case 46:
                        option_param[0][1] = 6;
                        break;
                    case 25:
                        option_param[0][1] = 12;
                        break;
                    case 45:
                        option_param[0][1] = 24;
                        break;
                    case 160:
                        option_param[0][1] = 50;
                        break;
                    case 161:
                        option_param[0][1] = 100;
                        break;
                    case 162:
                        option_param[0][1] = 200;
                        break;
                    case 163:
                        option_param[0][1] = 500;
                        break;
                    case 258:
                        option_param[0][1] = 630;
                        break;
                    case 259:
                        option_param[0][1] = 950;
                        break;
                    case 260:
                        option_param[0][1] = 1450;
                        break;
                    case 261:
                        option_param[0][1] = 2150;
                        break;
                    case 23:
                        option_param[0][1] = 5;
                        break;
                    case 53:
                        option_param[0][1] = 8;
                        break;
                    case 26:
                        option_param[0][1] = 16;
                        break;
                    case 54:
                        option_param[0][1] = 32;
                        break;
                    case 176:
                        option_param[0][1] = 60;
                        break;
                    case 177:
                        option_param[0][1] = 120;
                        break;
                    case 178:
                        option_param[0][1] = 240;
                        break;
                    case 179:
                        option_param[0][1] = 560;
                        break;
                    case 262:
                        option_param[0][1] = 700;
                        break;
                    case 263:
                        option_param[0][1] = 1050;
                        break;
                    case 264:
                        option_param[0][1] = 1550;
                        break;
                    case 265:
                        option_param[0][1] = 2250;
                        break;
                    case 562: // găng thần trái đất
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 3700;
                        option_param[2][1] = 17;
                        break;
                    case 564: // găng thần namếc
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 3500;
                        option_param[2][1] = 17;
                        break;
                    case 566: // găng thần xayda
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 3800;
                        option_param[2][1] = 17;
                        break;
                }
                break;
            case 3: // giày
                option_param[0][0] = 7; // ki
                option_param[1][0] = 28; // ki hồi /30s
                switch (tempId) {
                    case 27:
                        option_param[0][1] = 10;
                        break;
                    case 30:
                        option_param[0][1] = 25;
                        option_param[1][1] = 5;
                        break;
                    case 39:
                        option_param[0][1] = 120;
                        option_param[1][1] = 24;
                        break;
                    case 40:
                        option_param[0][1] = 250;
                        option_param[1][1] = 50;
                        break;
                    case 148:
                        option_param[0][1] = 500;
                        option_param[1][1] = 100;
                        break;
                    case 149:
                        option_param[0][1] = 1200;
                        option_param[1][1] = 240;
                        break;
                    case 150:
                        option_param[0][1] = 2400;
                        option_param[1][1] = 480;
                        break;
                    case 151:
                        option_param[0][1] = 5000;
                        option_param[1][1] = 1000;
                        break;
                    case 266:
                        option_param[0][1] = 9000;
                        option_param[1][1] = 1500;
                        break;
                    case 267:
                        option_param[0][1] = 14000;
                        option_param[1][1] = 2000;
                        break;
                    case 268:
                        option_param[0][1] = 19000;
                        option_param[1][1] = 2500;
                        break;
                    case 269:
                        option_param[0][1] = 24000;
                        option_param[1][1] = 3000;
                        break;
                    case 28:
                        option_param[0][1] = 15;
                        break;
                    case 47:
                        option_param[0][1] = 30;
                        option_param[1][1] = 6;
                        break;
                    case 31:
                        option_param[0][1] = 150;
                        option_param[1][1] = 30;
                        break;
                    case 48:
                        option_param[0][1] = 300;
                        option_param[1][1] = 60;
                        break;
                    case 164:
                        option_param[0][1] = 600;
                        option_param[1][1] = 120;
                        break;
                    case 165:
                        option_param[0][1] = 1500;
                        option_param[1][1] = 300;
                        break;
                    case 166:
                        option_param[0][1] = 3000;
                        option_param[1][1] = 600;
                        break;
                    case 167:
                        option_param[0][1] = 6000;
                        option_param[1][1] = 1200;
                        break;
                    case 270:
                        option_param[0][1] = 10000;
                        option_param[1][1] = 1700;
                        break;
                    case 271:
                        option_param[0][1] = 15000;
                        option_param[1][1] = 2200;
                        break;
                    case 272:
                        option_param[0][1] = 20000;
                        option_param[1][1] = 2700;
                        break;
                    case 273:
                        option_param[0][1] = 25000;
                        option_param[1][1] = 3200;
                        break;
                    case 29:
                        option_param[0][1] = 10;
                        break;
                    case 55:
                        option_param[0][1] = 20;
                        option_param[1][1] = 4;
                        break;
                    case 32:
                        option_param[0][1] = 100;
                        option_param[1][1] = 20;
                        break;
                    case 56:
                        option_param[0][1] = 200;
                        option_param[1][1] = 40;
                        break;
                    case 180:
                        option_param[0][1] = 400;
                        option_param[1][1] = 80;
                        break;
                    case 181:
                        option_param[0][1] = 1000;
                        option_param[1][1] = 200;
                        break;
                    case 182:
                        option_param[0][1] = 2000;
                        option_param[1][1] = 400;
                        break;
                    case 183:
                        option_param[0][1] = 4000;
                        option_param[1][1] = 800;
                        break;
                    case 274:
                        option_param[0][1] = 8000;
                        option_param[1][1] = 1300;
                        break;
                    case 275:
                        option_param[0][1] = 13000;
                        option_param[1][1] = 1800;
                        break;
                    case 276:
                        option_param[0][1] = 18000;
                        option_param[1][1] = 2300;
                        break;
                    case 277:
                        option_param[0][1] = 23000;
                        option_param[1][1] = 2800;
                        break;
                    case 563: // giày thần trái đất
                        option_param[0][0] = 23;
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 48;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 14;
                        break;
                    case 565: // giày thần namếc
                        option_param[0][0] = 23;
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 48;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 14;
                        break;
                    case 567: // giày thần xayda
                        option_param[0][0] = 23;
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 46;
                        option_param[1][1] = 10000;
                        option_param[2][1] = 14;
                        break;
                }
                break;
            case 4: // rada
                option_param[0][0] = 14; // crit
                switch (tempId) {
                    case 12:
                        option_param[0][1] = 1;
                        break;
                    case 57:
                        option_param[0][1] = 2;
                        break;
                    case 58:
                        option_param[0][1] = 3;
                        break;
                    case 59:
                        option_param[0][1] = 4;
                        break;
                    case 184:
                        option_param[0][1] = 5;
                        break;
                    case 185:
                        option_param[0][1] = 6;
                        break;
                    case 186:
                        option_param[0][1] = 7;
                        break;
                    case 187:
                        option_param[0][1] = 8;
                        break;
                    case 278:
                        option_param[0][1] = 9;
                        break;
                    case 279:
                        option_param[0][1] = 10;
                        break;
                    case 280:
                        option_param[0][1] = 11;
                        break;
                    case 281:
                        option_param[0][1] = 12;
                        break;
                    case 561: // nhẫn thần linh
                        option_param[2][0] = 21; // yêu cầu sức mạnh

                        option_param[0][1] = 15;
                        option_param[2][1] = 18;
                        break;
                }
                break;
        }

        for (int i = 0; i < option_param.length; i++) {
            if (option_param[i][0] != -1 && option_param[i][1] != -1) {
                list.add(new ItemOption(option_param[i][0], (option_param[i][1]
                        + Util.nextInt(-(option_param[i][1] * 10 / 100), option_param[i][1] * 10 / 100))));
            }
        }
    }

    private void initBaseOptionSaoPhaLe(ItemMap item) {
        int optionId = -1;
        switch (item.itemTemplate.id) {
            case 441: // hút máu
                optionId = 95;
                break;
            case 442: // hút ki
                optionId = 96;
                break;
            case 443: // phản sát thương
                optionId = 97;
                break;
            case 444:
                break;
            case 445:
                break;
            case 446: // vàng
                optionId = 100;
                break;
            case 447: // tnsm
                optionId = 101;
                break;
        }
        item.options.add(new ItemOption(optionId, 5));
    }

    public void initBaseOptionSaoPhaLe(Item item) {
        int optionId = -1;
        int param = 5;
        switch (item.template.id) {
            case 441: // hút máu
                optionId = 95;
                break;
            case 442: // hút ki
                optionId = 96;
                break;
            case 443: // phản sát thương
                optionId = 97;
                break;
            case 444:
                param = 3;
                optionId = 98;
                break;
            case 445:
                param = 3;
                optionId = 99;
                break;
            case 446: // vàng
                optionId = 100;
                break;
            case 447: // tnsm
                optionId = 101;
                break;
        }
        if (optionId != -1) {
            item.itemOptions.add(new ItemOption(optionId, param));
        }
    }

    // sao pha lê
    public void initStarOption(ItemMap item, RatioStar[] ratioStars) {
        RatioStar ratioStar = ratioStars[Util.nextInt(0, ratioStars.length - 1)];
        if (Util.isTrue(ratioStar.ratio, ratioStar.typeRatio)) {
            item.options.add(new ItemOption(107, ratioStar.numStar));
        }
    }

    public void initStarOption(Item item, RatioStar[] ratioStars) {
        RatioStar ratioStar = ratioStars[Util.nextInt(0, ratioStars.length - 1)];
        if (Util.isTrue(ratioStar.ratio, ratioStar.typeRatio)) {
            item.itemOptions.add(new ItemOption(107, ratioStar.numStar));
        }
    }

    // vật phẩm sự kiện
    private void initEventOption(ItemMap item) {
        switch (item.itemTemplate.id) {
            case 2013:
                item.options.add(new ItemOption(74, 0));
                break;
            case 2014:
                item.options.add(new ItemOption(74, 0));
                break;
            case 2015:
                item.options.add(new ItemOption(74, 0));
                break;
        }
    }

    // hạn sử dụng
    private void initExpiryDateOption(ItemMap item) {

    }

    // vật phẩm không thể giao dịch
    private void initNotTradeOption(ItemMap item) {
        switch (item.itemTemplate.id) {
            case 2009:
                item.options.add(new ItemOption(30, 0));
                break;

        }
    }

    // vật phẩm ký gửi
    private void initDepositOption(ItemMap item) {

    }

    // ==================== SET KÍCH HOẠT ====================

    // Tỉ lệ random set theo chủng tộc
    private static final int TRAI_DAT_RARE_CHANCE = 80; // 80% ra set hiếm (Thiên Xin Hàng/Kirin)
    private static final int NAMEK_RARE_CHANCE = 80; // 80% ra Pikkoro Daimao
    private static final int XAYDA_RARE_CHANCE = 80; // 80% ra set hiếm (Cadic/Nappa)

    // Index set trong ActivationSetData
    // Trái Đất: 0=Songoku, 1=Thiên Xin Hàng, 2=Kirin
    // Namek: 0=Ốc Tiêu, 1=Pikkoro Daimao, 2=Picolo
    // Xayda: 0=Kakarot, 1=Cadic, 2=Nappa

    /**
     * Khởi tạo option set kích hoạt cho item
     * 
     * @param gender Chủng tộc (0=Trái Đất, 1=Namek, 2=Xayda)
     * @param type   Loại item (chỉ áp dụng cho type <= 4)
     * @param list   Danh sách option để thêm vào
     */
    public void initActivationOption(int gender, int type, List<ItemOption> list) {
        if (type > 4) {
            return;
        }

        ActivationSetData setData = randomActivationSet(gender);
        if (setData == null) {
            return;
        }

        list.add(new ItemOption(setData.getOptionId(), 1)); // Tên set
        list.add(new ItemOption(setData.getEffectId(), 100)); // Hiệu ứng set
        list.add(new ItemOption(30, 0)); // Không thể giao dịch
        list.add(new ItemOption(73, 0)); // Khóa item
    }

    /**
     * Random set kích hoạt theo chủng tộc với tỉ lệ riêng
     */
    private ActivationSetData randomActivationSet(int gender) {
        return switch (gender) {
            case ActivationSetData.GENDER_TRAI_DAT -> randomTraiDatSet();
            case ActivationSetData.GENDER_NAMEK -> randomNamekSet();
            case ActivationSetData.GENDER_XAYDA -> randomXaydaSet();
            default -> null;
        };
    }

    /**
     * Trái Đất: 80% ra Thiên Xin Hàng hoặc Kirin, 20% ra Songoku
     */
    private ActivationSetData randomTraiDatSet() {
        if (Util.isTrue(TRAI_DAT_RARE_CHANCE, 100)) {
            // 80%: Random Thiên Xin Hàng (1) hoặc Kirin (2)
            return ActivationSetData.get(ActivationSetData.GENDER_TRAI_DAT, Util.nextInt(1, 2));
        } else {
            // 20%: Songoku (0)
            return ActivationSetData.get(ActivationSetData.GENDER_TRAI_DAT, 0);
        }
    }

    /**
     * Namek: 80% ra Pikkoro Daimao, 20% ra random cả 3
     */
    private ActivationSetData randomNamekSet() {
        if (Util.isTrue(NAMEK_RARE_CHANCE, 100)) {
            // 80%: Pikkoro Daimao (1)
            return ActivationSetData.get(ActivationSetData.GENDER_NAMEK, Util.nextInt(0, 1));
        } else {
            // 20%: Random Ốc Tiêu (0), Pikkoro Daimao (1), Picolo (2)
            return ActivationSetData.get(ActivationSetData.GENDER_NAMEK, 2);
        }
    }

    /**
     * Xayda: 90% ra Cadic hoặc Nappa, 10% ra Cadic
     */
    private ActivationSetData randomXaydaSet() {
        if (Util.isTrue(XAYDA_RARE_CHANCE, 100)) {
            // 90%: Random Cadic (1) hoặc Nappa (2)
            return ActivationSetData.get(ActivationSetData.GENDER_XAYDA, Util.nextInt(1, 2));
        } else {
            // 10%: Cadic (1)
            return ActivationSetData.get(ActivationSetData.GENDER_XAYDA, 0);
        }
    }

    private byte getMaxStarOfItemReward(ItemMap itemMap) {
        switch (itemMap.itemTemplate.id) {
            case 232:
            case 233:
            case 244:
            case 245:
            case 256:
            case 257:
            case 268:
            case 269:
            case 280:
            case 281:
            case 236:
            case 237:
            case 248:
            case 249:
            case 260:
            case 261:
            case 272:
            case 273:
            case 240:
            case 241:
            case 252:
            case 253:
            case 264:
            case 265:
            case 276:
            case 277:
                // đồ thần
            case 555:
            case 556:
            case 562:
            case 563:
            case 557:
            case 558:
            case 564:
            case 565:
            case 559:
            case 560:
            case 566:
            case 567:
            case 561:
                return 7;
            default:
                return 3;
        }
    }

    // --------------------------------------------------------------------------
    // Item reward lucky round
    public List<Item> getListItemLuckyRound(Player player, int num) {
        List<Item> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            ItemLuckyRound item = Manager.LUCKY_ROUND_REWARDS.next();
            if (item != null && (item.temp.gender == player.gender || item.temp.gender > 2)) {
                Item it = ItemService.gI().createNewItem(item.temp.id);
                for (ItemOptionLuckyRound io : item.itemOptions) {
                    int param = 0;
                    if (io.param2 != -1) {
                        param = Util.nextInt(io.param1, io.param2);
                    } else {
                        param = io.param1;
                    }
                    it.itemOptions.add(new ItemOption(io.itemOption.optionTemplate.id, param));
                }
                list.add(it);
            } else {
                Item it = ItemService.gI().createNewItem((short) 189, Util.nextInt(5, 50) * 1000);
                list.add(it);
            }
        }
        return list;
    }

    public static class RatioStar {

        public byte numStar;
        public int ratio;
        public int typeRatio;

        public RatioStar(byte numStar, int ratio, int typeRatio) {
            this.numStar = numStar;
            this.ratio = ratio;
            this.typeRatio = typeRatio;
        }
    }

    // public void rewardFirstTimeLoginPerDay(Player player) {
    // if (Util.compareDay(Date.from(Instant.now()), player.firstTimeLogin)) {
    // Item item = ItemService.gI().createNewItem((short) 649);
    // item.quantity = 1;
    // item.itemOptions.add(new ItemOption(74, 0));
    // item.itemOptions.add(new ItemOption(30, 0));
    // InventoryService.gI().addItemBag(player, item, 0);
    // Service.getInstance().sendThongBao(player,
    // "Quà đăng nhập hàng ngày: \nBạn nhận được " + item.template.name + " số lượng
    // : " + item.quantity);
    // player.firstTimeLogin = Date.from(Instant.now());
    // }
    // }
}
