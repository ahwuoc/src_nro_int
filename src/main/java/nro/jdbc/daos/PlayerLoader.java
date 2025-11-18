package nro.jdbc.daos;

import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTime;
import nro.models.player.*;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.skill.Skill;
import nro.models.npc.specialnpc.MagicTree;
import nro.models.npc.specialnpc.MabuEgg;
import nro.models.task.TaskMain;
import nro.models.task.Achivement;
import nro.models.task.AchivementTemplate;
import nro.card.Card;
import nro.card.CollectionBook;
import nro.services.ItemService;
import nro.utils.SkillUtil;
import nro.consts.ConstMap;
import nro.consts.ConstPlayer;
import nro.manager.AchiveManager;
import nro.manager.PetFollowManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to load and parse player data
 * Extracts reusable methods from GodGK.loadPlayer()
 */
public class PlayerLoader {

    private final JSONValue jv = new JSONValue();

    // ==================== HELPER METHODS ====================

    /**
     * Parse JSON array from string
     */
    protected JSONArray parseArray(String json) {
        return (JSONArray) JSONValue.parse(json);
    }

    /**
     * Parse JSON object from string
     */
    protected JSONObject parseObject(String json) {
        return (JSONObject) jv.parse(json);
    }

    /**
     * Get integer from JSON array at index
     */
    protected int getInt(JSONArray arr, int index) {
        return Integer.parseInt(arr.get(index).toString());
    }

    /**
     * Get long from JSON array at index
     */
    protected long getLong(JSONArray arr, int index) {
        return Long.parseLong(arr.get(index).toString());
    }

    /**
     * Get boolean from JSON array at index (1 = true, 0 = false)
     */
    protected boolean getBool(JSONArray arr, int index) {
        return getInt(arr, index) == 1;
    }

    /**
     * Get byte from JSON array at index
     */
    protected byte getByte(JSONArray arr, int index) {
        return Byte.parseByte(arr.get(index).toString());
    }

    /**
     * Get short from JSON array at index
     */
    protected short getShort(JSONArray arr, int index) {
        return Short.parseShort(arr.get(index).toString());
    }
    
    // ==================== JSON OBJECT HELPERS (for Rust JSON) ====================
    
    /**
     * Get integer from JSON object by key
     */
    protected int getInt(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }
    
    /**
     * Get long from JSON object by key
     */
    protected long getLong(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val == null) return 0L;
        return Long.parseLong(val.toString());
    }
    
    /**
     * Get byte from JSON object by key
     */
    protected byte getByte(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val == null) return 0;
        return Byte.parseByte(val.toString());
    }
    
    /**
     * Get short from JSON object by key
     */
    protected short getShort(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val == null) return 0;
        return Short.parseShort(val.toString());
    }
    
    /**
     * Get boolean from JSON object by key
     */
    protected boolean getBool(JSONObject obj, String key) {
        Object val = obj.get(key);
        if (val == null) return false;
        return Boolean.parseBoolean(val.toString());
    }
    
    /**
     * Get string from JSON object by key
     */
    protected String getString(JSONObject obj, String key) {
        Object val = obj.get(key);
        return val != null ? val.toString() : "";
    }

    // ==================== REUSABLE ITEM PARSING ====================

    /**
     * Parse a list of items from JSON string
     * REUSABLE for: items_body, items_bag, items_box, items_box_lucky_round
     */
    public List<Item> parseItems(String json) {
        List<Item> items = new ArrayList<>();
        
        try {
            JSONArray dataArray = parseArray(json);
            
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject itemData = (JSONObject) dataArray.get(i);
                Item item = parseItem(itemData);
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return items;
    }

    /**
     * Parse a single item from JSON object
     * Handles: temp_id, quantity, options, create_time, expiry check
     */
    private Item parseItem(JSONObject data) {
        try {
            short tempId = Short.parseShort(String.valueOf(data.get("temp_id")));
            
            // Empty item
            if (tempId == -1) {
                return ItemService.gI().createItemNull();
            }
            
            // Create item
            int quantity = Integer.parseInt(String.valueOf(data.get("quantity")));
            Item item = ItemService.gI().createNewItem(tempId, quantity);
            
            // Add item options
            JSONArray options = (JSONArray) data.get("option");
            if (options != null) {
                for (int j = 0; j < options.size(); j++) {
                    JSONArray opt = (JSONArray) options.get(j);
                    int optId = Integer.parseInt(String.valueOf(opt.get(0)));
                    int param = Integer.parseInt(String.valueOf(opt.get(1)));
                    item.itemOptions.add(new ItemOption(optId, param));
                }
            }
            
            // Check expiry
            item.createTime = Long.parseLong(String.valueOf(data.get("create_time")));
            if (ItemService.gI().isOutOfDateTime(item)) {
                return ItemService.gI().createItemNull();
            }
            
            return item;
            
        } catch (Exception e) {
            e.printStackTrace();
            return ItemService.gI().createItemNull();
        }
    }

    /**
     * Parse items for lucky round (no expiry check)
     */
    public List<Item> parseItemsLuckyRound(String json) {
        List<Item> items = new ArrayList<>();
        
        try {
            JSONArray dataArray = parseArray(json);
            
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject itemData = (JSONObject) dataArray.get(i);
                Item item = parseItemLuckyRound(itemData);
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return items;
    }

    /**
     * Parse lucky round item (no create_time, no expiry check)
     */
    private Item parseItemLuckyRound(JSONObject data) {
        try {
            short tempId = Short.parseShort(String.valueOf(data.get("temp_id")));
            
            if (tempId == -1) {
                return ItemService.gI().createItemNull();
            }
            
            int quantity = Integer.parseInt(String.valueOf(data.get("quantity")));
            Item item = ItemService.gI().createNewItem(tempId, quantity);
            
            // Add options
            JSONArray options = (JSONArray) data.get("option");
            if (options != null) {
                for (int j = 0; j < options.size(); j++) {
                    JSONArray opt = (JSONArray) options.get(j);
                    int optId = Integer.parseInt(String.valueOf(opt.get(0)));
                    int param = Integer.parseInt(String.valueOf(opt.get(1)));
                    item.itemOptions.add(new ItemOption(optId, param));
                }
            }
            
            return item;
            
        } catch (Exception e) {
            e.printStackTrace();
            return ItemService.gI().createItemNull();
        }
    }

    // ==================== ITEMS LOADING METHODS ====================

    /**
     * Load items_body
     */
    public void loadItemsBody(Player player, ResultSet rs) throws Exception {
        List<Item> items = parseItems(rs.getString("items_body"));
        player.inventory.itemsBody.addAll(items);
        
        // Ensure 13 slots (fix bug where only 12 slots exist)
        if (player.inventory.itemsBody.size() == 12) {
            player.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
    }

    /**
     * Load items_bag
     */
    public void loadItemsBag(Player player, ResultSet rs) {
        try {
            List<Item> items = parseItems(rs.getString("items_bag"));
            player.inventory.itemsBag.addAll(items);
        } catch (Exception e) {
            System.out.println("Lỗi hành trang hành trang người chơi");
            e.printStackTrace();
        }
    }

    /**
     * Load items_box
     */
    public void loadItemsBox(Player player, ResultSet rs) throws Exception {
        List<Item> items = parseItems(rs.getString("items_box"));
        player.inventory.itemsBox.addAll(items);
    }

    /**
     * Load items_box_lucky_round
     */
    public void loadItemsBoxLuckyRound(Player player, ResultSet rs) throws Exception {
        List<Item> items = parseItemsLuckyRound(rs.getString("items_box_lucky_round"));
        player.inventory.itemsBoxCrackBall.addAll(items);
    }

    /**
     * Load all items (body + bag + box + lucky round)
     */
    public void loadAllItems(Player player, ResultSet rs) throws Exception {
        loadItemsBody(player, rs);
        loadItemsBag(player, rs);
        loadItemsBox(player, rs);
        loadItemsBoxLuckyRound(player, rs);
    }

    // ==================== BASIC INFO ====================

    /**
     * Load basic player info
     */
    public void loadBasicInfo(Player player, ResultSet rs) throws Exception {
        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
        player.haveTennisSpaceShip = rs.getBoolean("have_tennis_space_ship");
    }

    // ==================== CLAN ====================

    /**
     * Load clan data and attach player to clan
     */
    public void loadClanData(Player player, ResultSet rs, int serverId) throws Exception {
        int clanId = rs.getInt("clan_id_sv" + serverId);
        if (clanId == -1) {
            return;
        }
        
        Clan clan = nro.services.ClanService.gI().getClanById(clanId);
        if (clan == null) {
            return;
        }
        
        attachPlayerToClan(player, clan);
    }

    /**
     * Attach player to clan
     */
    private void attachPlayerToClan(Player player, Clan clan) {
        for (ClanMember cm : clan.getMembers()) {
            if (cm.id == player.id) {
                clan.addMemberOnline(player);
                player.clan = clan;
                player.clanMember = cm;
                player.setBuff(clan.getBuff());
                break;
            }
        }
    }

    // ==================== EVENT DATA ====================

    /**
     * Load event data
     */
    public void loadEventData(Player player, ResultSet rs) throws Exception {
        player.event.setEventPoint(rs.getInt("event_point"));
        loadTetEventData(player, rs);
        loadKillWhisData(player, rs);
        loadMaBaoVeData(player, rs);
        loadEventRewards(player, rs);
    }

    /**
     * Load Tet event data
     */
    private void loadTetEventData(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("sk_tet"));
        
        player.event.setTimeCookTetCake(getInt(data, 0));
        player.event.setTimeCookChungCake(getInt(data, 1));
        player.event.setCookingTetCake(getBool(data, 2));
        player.event.setCookingChungCake(getBool(data, 3));
        player.event.setReceivedLuckyMoney(getBool(data, 4));
    }

    /**
     * Load Kill Whis data
     */
    private void loadKillWhisData(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("killWhis"));
        player.lastTimeSwapWhis = getLong(data, 0);
        player.lastTimeKillWhis = getLong(data, 1);
        player.levelKillWhis = getInt(data, 2);
        
        player.levelKillWhisDone = rs.getInt("levelKillWhis");
        player.timeKillWhis = rs.getLong("timeKillWhis");
    }

    /**
     * Load Ma Bao Ve data
     */
    private void loadMaBaoVeData(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("MaBaoVe"));
        player.isUseMaBaoVe = getBool(data, 0);
        player.MaBaoVe = getInt(data, 1);
    }

    /**
     * Load event rewards
     */
    private void loadEventRewards(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("checkNhanQua"));
        player.event.luotNhanNgocMienPhi = getInt(data, 0);
        player.event.luotNhanBuaMienPhi = getInt(data, 1);
        
        player.event.setMocNapDaNhan(rs.getInt("moc_nap"));
    }

    // ==================== INVENTORY ====================

    /**
     * Load inventory (gold, gem, ruby)
     */
    public void loadInventory(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_inventory"));
        
        player.inventory.gold = getLong(data, 0);
        player.inventory.gem = getInt(data, 1);
        player.inventory.ruby = getInt(data, 2);
        
        if (data.size() >= 4) {
            player.inventory.goldLimit = getLong(data, 3);
        }
    }

    // ==================== TITLES (DANH HIEU) ====================

    /**
     * Load all titles (5 titles)
     */
    public void loadTitles(Player player, ResultSet rs) throws Exception {
        loadTitle(player, rs, "dhtime", 1);
        loadTitle(player, rs, "dhtime2", 2);
        loadTitle(player, rs, "dhtime3", 3);
        loadTitle(player, rs, "dhtime4", 4);
        loadTitle(player, rs, "dhtime5", 5);
    }

    /**
     * Load single title
     */
    private void loadTitle(Player player, ResultSet rs, String column, int index) throws Exception {
        JSONArray data = parseArray(rs.getString(column));
        
        switch(index) {
            case 1:
                player.isTitleUse = getBool(data, 0);
                player.lastTimeTitle1 = getLong(data, 1);
                player.IdDanhHieu_1 = getInt(data, 2);
                player.ChiSoHP_1 = getInt(data, 3);
                player.ChiSoKI_1 = getInt(data, 4);
                player.ChiSoSD_1 = getInt(data, 5);
                break;
            case 2:
                player.isTitleUse2 = getBool(data, 0);
                player.lastTimeTitle2 = getLong(data, 1);
                player.IdDanhHieu_2 = getInt(data, 2);
                player.ChiSoHP_2 = getInt(data, 3);
                player.ChiSoKI_2 = getInt(data, 4);
                player.ChiSoSD_2 = getInt(data, 5);
                break;
            case 3:
                player.isTitleUse3 = getBool(data, 0);
                player.lastTimeTitle3 = getLong(data, 1);
                player.IdDanhHieu_3 = getInt(data, 2);
                player.ChiSoHP_3 = getInt(data, 3);
                player.ChiSoKI_3 = getInt(data, 4);
                player.ChiSoSD_3 = getInt(data, 5);
                break;
            case 4:
                player.isTitleUse4 = getBool(data, 0);
                player.lastTimeTitle4 = getLong(data, 1);
                player.IdDanhHieu_4 = getInt(data, 2);
                player.ChiSoHP_4 = getInt(data, 3);
                player.ChiSoKI_4 = getInt(data, 4);
                player.ChiSoSD_4 = getInt(data, 5);
                break;
            case 5:
                player.isTitleUse5 = getBool(data, 0);
                player.lastTimeTitle5 = getLong(data, 1);
                player.IdDanhHieu_5 = getInt(data, 2);
                player.ChiSoHP_5 = getInt(data, 3);
                player.ChiSoKI_5 = getInt(data, 4);
                player.ChiSoSD_5 = getInt(data, 5);
                break;
        }
    }

    // ==================== LOCATION ====================

    /**
     * Load location with map validation
     */
    public void loadLocation(Player player, ResultSet rs) {
        try {
            JSONArray data = parseArray(rs.getString("data_location"));
            
            player.location.x = getInt(data, 0);
            player.location.y = getInt(data, 1);
            int mapId = getInt(data, 2);
            
            // Validate and fix invalid map
            mapId = getValidMapId(player, mapId);
            
            // Join zone
            player.zone = nro.services.MapService.gI().getMapCanJoin(player, mapId);
            
        } catch (Exception e) {
            e.printStackTrace();
            setDefaultLocation(player);
        }
    }

    /**
     * Get valid map ID (fix special/invalid maps)
     */
    private int getValidMapId(Player player, int mapId) {
        if (isInvalidSpecialMap(mapId)) {
            player.location.x = 300;
            player.location.y = 336;
            return getTownMapId(player.gender);
        }
        
        if (nro.services.MapService.gI().isMapKhiGas(mapId)) {
            player.location.x = 106;
            player.location.y = 228;
            return 5;
        }
        
        return mapId;
    }

    /**
     * Check if map is invalid special map
     */
    private boolean isInvalidSpecialMap(int mapId) {
        return nro.services.MapService.gI().isMapDoanhTrai(mapId)
            || nro.services.MapService.gI().isMapBlackBallWar(mapId)
            || nro.services.MapService.gI().isMapBanDoKhoBau(mapId)
            || mapId == 126
            || mapId == ConstMap.CON_DUONG_RAN_DOC
            || mapId == ConstMap.CON_DUONG_RAN_DOC_142
            || mapId == ConstMap.CON_DUONG_RAN_DOC_143
            || mapId == ConstMap.HOANG_MAC;
    }

    /**
     * Get town map ID based on gender
     */
    private int getTownMapId(byte gender) {
        return gender + 21;
    }

    /**
     * Set default safe location
     */
    private void setDefaultLocation(Player player) {
        int mapId = getTownMapId(player.gender);
        player.location.x = 300;
        player.location.y = 336;
        player.zone = nro.services.MapService.gI().getMapCanJoin(player, mapId);
    }

    // ==================== PLAYER POINTS ====================

    /**
     * Load player points/stats
     */
    public int[] loadPlayerPoints(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_point"));
        
        int plMp = getInt(data, 1);
        int plHp = getInt(data, 6);
        
        player.nPoint.mpg = getInt(data, 2);
        player.nPoint.critg = getByte(data, 3);
        player.nPoint.limitPower = getByte(data, 4);
        player.nPoint.stamina = getShort(data, 5);
        player.nPoint.defg = getInt(data, 7);
        player.nPoint.tiemNang = getLong(data, 8);
        player.nPoint.maxStamina = getShort(data, 9);
        player.nPoint.dameg = getInt(data, 10);
        player.nPoint.power = getLong(data, 11);
        player.nPoint.hpg = getInt(data, 12);
        
        return new int[]{plHp, plMp};
    }

    // ==================== FRIENDS & ENEMIES ====================

    /**
     * Load friends and enemies
     */
    public void loadFriendsAndEnemies(Player player, ResultSet rs) throws Exception {
        loadFriends(player, rs);
        loadEnemies(player, rs);
    }

    /**
     * Load friends list
     */
    private void loadFriends(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("friends"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObject = (JSONObject) dataArray.get(i);
            Friend friend = new Friend();
            friend.id = getInt(dataObject, "id");
            friend.name = String.valueOf(dataObject.get("name"));
            friend.head = getShort(dataObject, "head");
            friend.body = getShort(dataObject, "body");
            friend.leg = getShort(dataObject, "leg");
            friend.bag = getByte(dataObject, "bag");
            friend.power = getLong(dataObject, "power");
            player.friends.add(friend);
        }
    }

    /**
     * Load enemies list
     */
    private void loadEnemies(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("enemies"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObject = (JSONObject) dataArray.get(i);
            Enemy enemy = new Enemy();
            enemy.id = getInt(dataObject, "id");
            enemy.name = String.valueOf(dataObject.get("name"));
            enemy.head = getShort(dataObject, "head");
            enemy.body = getShort(dataObject, "body");
            enemy.leg = getShort(dataObject, "leg");
            enemy.bag = getByte(dataObject, "bag");
            enemy.power = getLong(dataObject, "power");
            player.enemies.add(enemy);
        }
    }

    // ==================== SKILLS ====================

    /**
     * Load player skills
     */
    public void loadSkills(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("skills"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONArray skillTemp = (JSONArray) parseArray(String.valueOf(dataArray.get(i)));
            int tempId = getInt(skillTemp, 0);
            byte point = getByte(skillTemp, 2);
            
            Skill skill = null;
            if (point != 0) {
                skill = SkillUtil.createSkill(tempId, point);
            } else {
                skill = SkillUtil.createSkillLevel0(tempId);
            }
            
            skill.lastTimeUseThisSkill = getLong(skillTemp, 1);
            player.playerSkill.skills.add(skill);
        }
    }

    /**
     * Load skill shortcuts
     */
    public void loadSkillShortcuts(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("skills_shortcut"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            player.playerSkill.skillShortCut[i] = Byte.parseByte(String.valueOf(dataArray.get(i)));
        }
        
        // Select default skill
        for (int i : player.playerSkill.skillShortCut) {
            if (player.playerSkill.getSkillbyId(i) != null
                    && player.playerSkill.getSkillbyId(i).damage > 0) {
                player.playerSkill.skillSelect = player.playerSkill.getSkillbyId(i);
                break;
            }
        }
        
        // Fallback if no skill selected
        if (player.playerSkill.skillSelect == null) {
            player.playerSkill.skillSelect = player.playerSkill
                    .getSkillbyId(player.gender == ConstPlayer.TRAI_DAT
                            ? Skill.DRAGON
                            : (player.gender == ConstPlayer.NAMEC ? Skill.DEMON : Skill.GALICK));
        }
    }

    // ==================== MAGIC TREE ====================

    /**
     * Load magic tree data
     */
    public void loadMagicTree(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_magic_tree"));
        
        boolean isUpgrade = getByte(data, 0) == 1;
        long lastTimeUpgrade = getLong(data, 1);
        byte level = getByte(data, 2);
        long lastTimeHarvest = getLong(data, 3);
        byte currPea = getByte(data, 4);
        
        player.magicTree = new MagicTree(player, level, currPea, lastTimeHarvest, isUpgrade, lastTimeUpgrade);
    }

    // ==================== BLACK BALL REWARDS ====================

    /**
     * Load black ball rewards
     */
    public void loadBlackBallRewards(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("data_black_ball"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONArray reward = (JSONArray) parseArray(String.valueOf(dataArray.get(i)));
            player.rewardBlackBall.timeOutOfDateReward[i] = getLong(reward, 0);
            player.rewardBlackBall.lastTimeGetReward[i] = getLong(reward, 1);
        }
    }

    // ==================== INTRINSIC ====================

    /**
     * Load intrinsic data
     */
    public void loadIntrinsicData(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_intrinsic"));
        
        byte intrinsicId = getByte(data, 0);
        player.playerIntrinsic.intrinsic = nro.services.IntrinsicService.gI().getIntrinsicById(intrinsicId);
        player.playerIntrinsic.intrinsic.param1 = getShort(data, 1);
        player.playerIntrinsic.countOpen = getByte(data, 2);
        player.playerIntrinsic.intrinsic.param2 = getShort(data, 3);
    }

    // ==================== CHARMS ====================

    /**
     * Load charm data
     */
    public void loadCharms(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_charm"));
        
        player.charms.tdTriTue = getLong(data, 0);
        player.charms.tdManhMe = getLong(data, 1);
        player.charms.tdDaTrau = getLong(data, 2);
        player.charms.tdOaiHung = getLong(data, 3);
        player.charms.tdBatTu = getLong(data, 4);
        player.charms.tdDeoDai = getLong(data, 5);
        player.charms.tdThuHut = getLong(data, 6);
        player.charms.tdDeTu = getLong(data, 7);
        player.charms.tdTriTue3 = getLong(data, 8);
        player.charms.tdTriTue4 = getLong(data, 9);
        
        if (data.size() >= 11) {
            player.charms.tdDeTuMabu = getLong(data, 10);
        }
    }

    // ==================== MABU EGG ====================

    /**
     * Load mabu egg data
     */
    public void loadMabuEgg(Player player, ResultSet rs) throws Exception {
        JSONObject dataObject = parseObject(rs.getString("data_mabu_egg"));
        Object createTime = dataObject.get("create_time");
        
        if (createTime != null) {
            player.mabuEgg = new MabuEgg(
                player,
                Long.parseLong(String.valueOf(createTime)),
                Long.parseLong(String.valueOf(dataObject.get("time_done")))
            );
        }
    }

    // ==================== COLLECTION BOOK ====================

    /**
     * Load collection book
     */
    public void loadCollectionBook(Player player, ResultSet rs) throws Exception {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        java.util.List<Card> cards = gson.fromJson(
            rs.getString("collection_book"),
            new com.google.gson.reflect.TypeToken<java.util.List<Card>>(){}.getType()
        );
        
        CollectionBook book = new CollectionBook(player);
        if (cards != null) {
            book.setCards(cards);
        } else {
            book.setCards(new java.util.ArrayList<>());
        }
        book.init();
        player.setCollectionBook(book);
    }

    // ==================== MINI PET & PET FOLLOW ====================

    /**
     * Load mini pets and pet follow
     */
    public void loadMiniPetsAndFollow(Player player, ResultSet rs) {
        try {
            java.util.List<Item> itemsBody = player.inventory.itemsBody;
            
            // Mini pet (slot 11)
            if (itemsBody.get(11).isNotNullItem()) {
                nro.models.player.MiniPet.callMiniPet(player, itemsBody.get(11).template.id);
            }
            
            // Pet follow (slot 10)
            if (itemsBody.get(10).isNotNullItem()) {
                PetFollow pet = PetFollowManager.gI().findByID(itemsBody.get(10).getId());
                player.setPetFollow(pet);
                nro.services.PlayerService.gI().sendPetFollow(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== OTHER DATA ====================

    /**
     * Load buy/reward limits
     */
    public void loadLimits(Player player, ResultSet rs) throws Exception {
        // Buy limit
        JSONArray buyLimitArray = parseArray(rs.getString("buy_limit"));
        for (int i = 0; i < buyLimitArray.size(); i++) {
            player.buyLimit[i] = Byte.parseByte(buyLimitArray.get(i).toString());
        }
        
        // Reward limit
        JSONArray rewardLimitArray = parseArray(rs.getString("reward_limit"));
        player.rewardLimit = new byte[rewardLimitArray.size()];
        for (int i = 0; i < rewardLimitArray.size(); i++) {
            player.rewardLimit[i] = Byte.parseByte(rewardLimitArray.get(i).toString());
        }
    }

    /**
     * Load challenge data
     */
    public void loadChallengeData(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("challenge"));
        
        player.goldChallenge = getInt(data, 0);
        player.levelWoodChest = getInt(data, 1);
        player.receivedWoodChest = getInt(data, 2) == 1;
        player.gemChallenge = getInt(data, 3);
    }

    /**
     * Set first time login
     */
    public void setFirstTimeLogin(Player player, ResultSet rs) throws Exception {
        player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");
    }

    // ==================== ITEM TIME BUFFS ====================

    /**
     * Load item time buffs (very complex with many special cases)
     */
    public void loadItemTimeBuffs(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("data_item_time"));
        
        int timeBoKhi = getInt(dataArray, 0);
        int timeAnDanh = getInt(dataArray, 1);
        int timeOpenPower = getInt(dataArray, 2);
        int timeCuongNo = getInt(dataArray, 3);
        int timeBoHuyet = getInt(dataArray, 5);
        int timeGiapXen = getInt(dataArray, 8);
        
        int timeMayDo = 0;
        int timeMeal = 0;
        int iconMeal = 0;
        try {
            timeMayDo = getInt(dataArray, 4);
            timeMeal = getInt(dataArray, 7);
            iconMeal = getInt(dataArray, 6);
        } catch (Exception e) {
        }
        
        int timeBanhChung1 = 0;
        int timeBanhTet1 = 0;
        int timeBoKhi2 = 0;
        int timeGiapXen2 = 0;
        int timeCuongNo2 = 0;
        int timeBoHuyet2 = 0;
        if (dataArray.size() >= 15) {
            timeBanhChung1 = getInt(dataArray, 9);
            timeBanhTet1 = getInt(dataArray, 10);
            timeBoKhi2 = getInt(dataArray, 11);
            timeGiapXen2 = getInt(dataArray, 12);
            timeCuongNo2 = getInt(dataArray, 13);
            timeBoHuyet2 = getInt(dataArray, 14);
        }
        
        int timeBiNgo = 0;
        if (dataArray.size() >= 16) {
            timeBiNgo = getInt(dataArray, 15);
        }
        
        // Calculate last time usage
        player.itemTime.lastTimeBoHuyet = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet);
        player.itemTime.lastTimeBoKhi = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi);
        player.itemTime.lastTimeGiapXen = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen);
        player.itemTime.lastTimeCuongNo = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo);
        player.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoHuyet2);
        player.itemTime.lastTimeBoKhi2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeBoKhi2);
        player.itemTime.lastTimeGiapXen2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeGiapXen2);
        player.itemTime.lastTimeCuongNo2 = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeCuongNo2);
        player.itemTime.lastTimeAnDanh = System.currentTimeMillis() - (ItemTime.TIME_ITEM - timeAnDanh);
        player.itemTime.lastTimeOpenPower = System.currentTimeMillis() - (ItemTime.TIME_OPEN_POWER - timeOpenPower);
        player.itemTime.lastTimeUseMayDo = System.currentTimeMillis() - (ItemTime.TIME_MAY_DO - timeMayDo);
        player.itemTime.lastTimeEatMeal = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeMeal);
        player.itemTime.lastTimeBanhChung = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeBanhChung1);
        player.itemTime.lastTimeBanhTet = System.currentTimeMillis() - (ItemTime.TIME_EAT_MEAL - timeBanhTet1);
        
        player.itemTime.iconMeal = iconMeal;
        
        // Set usage flags
        player.itemTime.isUseBoHuyet = timeBoHuyet != 0;
        player.itemTime.isUseBoKhi = timeBoKhi != 0;
        player.itemTime.isUseGiapXen = timeGiapXen != 0;
        player.itemTime.isUseCuongNo = timeCuongNo != 0;
        player.itemTime.isUseBoHuyet2 = timeBoHuyet2 != 0;
        player.itemTime.isUseBoKhi2 = timeBoKhi2 != 0;
        player.itemTime.isUseGiapXen2 = timeGiapXen2 != 0;
        player.itemTime.isUseCuongNo2 = timeCuongNo2 != 0;
        player.itemTime.isUseAnDanh = timeAnDanh != 0;
        player.itemTime.isOpenPower = timeOpenPower != 0;
        player.itemTime.isUseMayDo = timeMayDo != 0;
        player.itemTime.isEatMeal = timeMeal != 0;
        player.itemTime.isUseBanhChung = timeBanhChung1 != 0;
        player.itemTime.isUseBanhTet = timeBanhTet1 != 0;
        
        // BiNgo effect
        player.effectSkill.isBiNgo = timeBiNgo != 0;
        player.effectSkill.lastBiNgo = System.currentTimeMillis() - (30_000 - timeBiNgo);
    }

    // ==================== TASKS & ACHIEVEMENTS ====================

    /**
     * Load main task
     */
    public void loadMainTask(Player player, ResultSet rs) throws Exception {
        JSONArray data = parseArray(rs.getString("data_task"));
        
        TaskMain taskMain = nro.services.TaskService.gI().getTaskMainById(
            player,
            getByte(data, 1)
        );
        
        taskMain.subTasks.get(getInt(data, 2)).count = getShort(data, 0);
        taskMain.index = getByte(data, 2);
        player.playerTask.taskMain = taskMain;
    }

    /**
     * Load side task (daily task)
     */
    public void loadSideTask(Player player, ResultSet rs) {
        try {
            JSONArray data = parseArray(rs.getString("data_side_task"));
            String format = "dd-MM-yyyy";
            long receivedTime = getLong(data, 4);
            java.util.Date date = new java.util.Date(receivedTime);
            
            if (nro.utils.TimeUtil.formatTime(date, format)
                    .equals(nro.utils.TimeUtil.formatTime(new java.util.Date(), format))) {
                player.playerTask.sideTask.level = getInt(data, 0);
                player.playerTask.sideTask.count = getInt(data, 1);
                player.playerTask.sideTask.leftTask = getInt(data, 2);
                player.playerTask.sideTask.template = nro.services.TaskService.gI()
                    .getSideTaskTemplateById(getInt(data, 3));
                player.playerTask.sideTask.maxCount = getInt(data, 5);
                player.playerTask.sideTask.receivedTime = receivedTime;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Load achievements
     */
    public void loadAchievements(Player player, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("achivements"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject dataObject = (JSONObject) parseObject(String.valueOf(dataArray.get(i)));
            Achivement achivement = new Achivement();
            
            achivement.setId(getInt(dataObject, "id"));
            achivement.setCount(getInt(dataObject, "count"));
            achivement.setFinish(getInt(dataObject, "finish") == 1);
            achivement.setReceive(getInt(dataObject, "receive") == 1);
            
            AchivementTemplate a = AchiveManager.getInstance().findByID(achivement.getId());
            achivement.setName(a.getName());
            achivement.setDetail(a.getDetail());
            achivement.setMaxCount(a.getMaxCount());
            achivement.setMoney(a.getMoney());
            
            player.playerTask.achivements.add(achivement);
        }
        
        // Add missing achievements
        java.util.List<AchivementTemplate> listAchivements = AchiveManager.getInstance().getList();
        if (dataArray.size() < listAchivements.size()) {
            for (int i = dataArray.size(); i < listAchivements.size(); i++) {
                AchivementTemplate a = AchiveManager.getInstance().findByID(i);
                if (a != null) {
                    Achivement achivement = new Achivement();
                    achivement.setId(a.getId());
                    achivement.setCount(0);
                    achivement.setFinish(false);
                    achivement.setReceive(false);
                    achivement.setName(a.getName());
                    achivement.setDetail(a.getDetail());
                    achivement.setMaxCount(a.getMaxCount());
                    achivement.setMoney(a.getMoney());
                    player.playerTask.achivements.add(achivement);
                }
            }
        }
    }

    // ==================== PET DATA ====================

    /**
     * Load full pet data (pet info, points, body items, skills)
     */
    public void loadPetData(Player player, ResultSet rs, nro.server.io.Session session) throws Exception {
        JSONObject dataObject = parseObject(rs.getString("pet_info"));
        
        if (String.valueOf(dataObject).equals("{}")) {
            return; // No pet
        }
        
        Pet pet = new Pet(player);
        pet.id = -player.id;
        pet.gender = getByte(dataObject, "gender");
        pet.isMabu = getByte(dataObject, "is_mabu") == 1;
        pet.isBulo = getByte(dataObject, "is_Bulo") == 1;
        pet.isCellBao = getByte(dataObject, "is_CellBao") == 1;
        pet.isBillNhi = getByte(dataObject, "is_BillNhi") == 1;
        pet.isFideTrau = getByte(dataObject, "is_FideTrau") == 1;
        pet.isSuperPicolo = getByte(dataObject, "is_SuperPicolo") == 1;
        pet.name = String.valueOf(dataObject.get("name"));
        
        // Fusion data
        player.fusion.typeFusion = getByte(dataObject, "type_fusion");
        player.fusion.lastTimeFusion = System.currentTimeMillis()
                - (Fusion.TIME_FUSION - getInt(dataObject, "left_fusion"));
        pet.status = getByte(dataObject, "status");
        
        // Pet points
        loadPetPoints(pet, rs);
        
        // Pet body items
        loadPetBodyItems(pet, rs);
        
        // Pet skills
        loadPetSkills(pet, rs);
        
        player.pet = pet;
    }

    /**
     * Load pet points
     */
    private void loadPetPoints(Pet pet, ResultSet rs) throws Exception {
        JSONObject data = parseObject(rs.getString("pet_point"));
        
        pet.nPoint.stamina = getShort(data, "stamina");
        pet.nPoint.maxStamina = getShort(data, "max_stamina");
        pet.nPoint.hpg = getInt(data, "hpg");
        pet.nPoint.mpg = getInt(data, "mpg");
        pet.nPoint.dameg = getInt(data, "damg");
        pet.nPoint.defg = getInt(data, "defg");
        pet.nPoint.critg = getInt(data, "critg");
        pet.nPoint.power = getLong(data, "power");
        pet.nPoint.tiemNang = getLong(data, "tiem_nang");
        pet.nPoint.limitPower = getByte(data, "limit_power");
        
        int hp = getInt(data, "hp");
        int mp = getInt(data, "mp");
        pet.nPoint.hp = hp;
        pet.nPoint.mp = mp;
    }

    /**
     * Load pet body items
     */
    private void loadPetBodyItems(Pet pet, ResultSet rs) throws Exception {
        java.util.List<Item> items = parseItems(rs.getString("pet_body"));
        pet.inventory.itemsBody.addAll(items);
    }

    /**
     * Load pet skills
     */
    private void loadPetSkills(Pet pet, ResultSet rs) throws Exception {
        JSONArray dataArray = parseArray(rs.getString("pet_skill"));
        
        for (int i = 0; i < dataArray.size(); i++) {
            JSONArray skillTemp = (JSONArray) dataArray.get(i);
            int tempId = getInt(skillTemp, 0);
            byte point = getByte(skillTemp, 1);
            
            Skill skill = null;
            if (point != 0) {
                skill = SkillUtil.createSkill(tempId, point);
            } else {
                skill = SkillUtil.createSkillLevel0(tempId);
            }
            
            // Set cooldown for specific skills
            switch (skill.template.id) {
                case Skill.KAMEJOKO:
                case Skill.MASENKO:
                case Skill.ANTOMIC:
                    skill.coolDown = 1000;
                    break;
            }
            
            pet.playerSkill.skills.add(skill);
        }
    }
    
}
