package nro.models.player;

import nro.consts.ConstPlayer;
import nro.consts.ItemId;
import nro.models.item.CaiTrang;
import nro.models.mob.Mob;
import nro.models.skill.Skill;
import nro.server.Manager;
import nro.server.io.Message;
import nro.services.*;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;
import nro.utils.Util;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author 💖 ahwuocdz 💖
 * 
 */
public class Pet extends Player {

    private static final byte HP = 0;
    private static final byte MP = 1;
    private static final byte DAME = 2;
    private static final byte DEF = 3;
    private static final byte CRIT = 4;
    private static final short ARANGE_CAN_ATTACK = 200;
    private static final short ARANGE_ATT_SKILL1 = 50;
    public static final int PET_SKILL_COOLDOWN = 500;
    private static final short[][] PET_ID = { { 285, 286, 287 }, { 288, 289, 290 }, { 282, 283, 284 },
            { 304, 305, 303 } };

    public static final byte FOLLOW = 0;
    public static final byte PROTECT = 1;
    public static final byte ATTACK = 2;
    public static final byte GOHOME = 3;
    public static final byte FUSION = 4;
    public static boolean ANGRY;

    public static int CAI_TRANG_MABU_ID = 578;
    public static int CAI_TRANG_NORMA_TD_ID= 1641;
    public static int CAI_TRANG_BLACK_SUPER_SAIYAN_5_ID = 2147;
    public static int CAI_TRANG_BLACK_FIDE_TRAU_ID = 2147;
    public static int CAI_TRANG_BLACK_SUPER_PICOLO_ID = 2146;





    /**
     * Enum định nghĩa loại đệ VIP và % bonus khi hợp thể Porata
     */
    public enum PetType {
        NONE(0, "Đệ tử",CAI_TRANG_NORMA_TD_ID),  
        MABU(10, "Mabư",ItemId.ITEM_578_CAI_TRANG_MA_BU),
        SAYAN5(20, "Goten Normal",ItemId.ITEM_2128_CAI_TRANG_GOTEN),
        BILL_NHI(30, "Black Goku SSJ4",ItemId.ITEM_2147_BLACK_SUPER_SAIYAN_5),
        FIDE_TRAU(30, "Gogeta SSJ4",ItemId.ITEM_2143_GOKU_CHONG_CHONG),
        CELL_BAO(40, "Goku Ultra",ItemId.ITEM_2117_CAI_TRANG_GOKU_ULTRA),
        SUPER_PICOLO(50, "Legendary Broly",ItemId.ITEM_2148_BROLY_KHOI_NGUYEN);

        private final int bonus;
        private final String displayName;
        private final int id_caitrang;

        PetType(int bonus, String Name ,int id_itemp) {
            this.bonus = bonus;
            this.displayName = Name;
            this.id_caitrang = id_itemp;
        }
        public int getBonus() {
            return bonus;
        }

        public String getDisplayName() {
            return displayName;
        }

        public CaiTrang getCaiTrang() {
            return Manager.CAI_TRANGS.stream()
                .filter(ct -> ct.tempId == id_caitrang)
                .findFirst()
                .orElse(null);
        }

        public short getHead() {
            CaiTrang ct = getCaiTrang();
            return ct != null ? (short) ct.getHead() : -1;
        }

        public short getBody() {
            CaiTrang ct = getCaiTrang();
            return ct != null ? (short) ct.getBody() : -1;
        }

        public short getLeg() {
            CaiTrang ct = getCaiTrang();
            return ct != null ? (short) ct.getLeg() : -1;
        }
    }

    public Player master;
    public byte status = 0;
    public int level = 1; // Level đệ tử
    public long expLevel = 0; // Exp để lên level
    public long accumulatedExp = 0; // Exp tích lũy cho đột phá

    public boolean isMabu;

    public boolean isBulo;

    public boolean isCellBao;

    public boolean isBillNhi;

    public boolean isFideTrau;

    public boolean isSuperPicolo;

    public boolean isTransform;

    public long lastTimeDie;

    private boolean goingHome;

    private Mob mobAttack;
    private Player playerAttack;

    private static final int TIME_WAIT_AFTER_UNFUSION = 5000;
    private long lastTimeUnfusion;

    public byte getStatus() {
        return this.status;
    }

    @Override
    public int version() {
        return 214;
    }

    public Pet(Player master) {
        this.master = master;
        this.isPet = true;
    }

    public void changeStatus(byte status) {
        if (goingHome || master.fusion.typeFusion != 0 || (this.isDie() && status == FUSION)) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        Service.getInstance().chatJustForMe(master, this, getTextStatus(status));
        if (status == GOHOME) {
            goHome();
        } else if (status == FUSION) {
            fusion(false);
        }
        this.status = status;
    }

    public void joinMapMaster() {
        if (!MapService.gI().isMapVS(master.zone.map.mapId) && !MapService.gI().isMapOfflineNe(master.zone.map.mapId)
                && !MapService.gI().isMapOffline(master.zone.map.mapId)) {
            if (status != GOHOME && status != FUSION && !isDie()) {
                this.location.x = master.location.x + Util.nextInt(-10, 10);
                this.location.y = master.location.y;
                MapService.gI().goToMap(this, master.zone);
                this.zone.load_Me_To_Another(this);
            }
        } else {
            MapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 39));
            this.zone.load_Me_To_Another(this);
        }
    }

    public void goHome() {
        if (this.status == GOHOME) {
            return;
        }
        goingHome = true;
        new Thread(() -> {
            try {
                Pet.this.status = Pet.ATTACK;
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            MapService.gI().goToMap(this, MapService.gI().getMapCanJoin(this, master.gender + 21));
            this.zone.load_Me_To_Another(this);
            Pet.this.status = Pet.GOHOME;
            goingHome = false;
        }).start();
    }

    private String getTextStatus(byte status) {
        switch (status) {
            case FOLLOW:
                return "Ok con theo sư phụ";
            case PROTECT:
                return "Ok con sẽ bảo vệ sư phụ";
            case ATTACK:
                return "Ok sư phụ để con lo cho";
            case GOHOME:
                return "Ok con về, bibi sư phụ";
            default:
                return "";
        }
    }

    public void fusion(boolean porata) {
        if (this.isDie()) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (porata) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790,
                        Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            exitMapFusion();
            fusionEffect(master.fusion.typeFusion);
            Service.getInstance().Send_Caitrang(master);
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.getInstance().point(master);
        } else {
            Service.getInstance().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void fusion2(boolean porata2) {
        if (this.isDie()) {
            Service.getInstance().sendThongBao(master, "Không thể thực hiện");
            return;
        }
        if (Util.canDoWithTime(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION)) {
            if (porata2) {
                master.fusion.typeFusion = ConstPlayer.HOP_THE_PORATA2;
            } else {
                master.fusion.lastTimeFusion = System.currentTimeMillis();
                master.fusion.typeFusion = ConstPlayer.LUONG_LONG_NHAT_THE;
                ItemTimeService.gI().sendItemTime(master, master.gender == ConstPlayer.NAMEC ? 3901 : 3790,
                        Fusion.TIME_FUSION / 1000);
            }
            this.status = FUSION;
            exitMapFusion();
            fusionEffect(master.fusion.typeFusion);
            Service.getInstance().Send_Caitrang(master);
            master.nPoint.calPoint();
            master.nPoint.setFullHpMp();
            Service.getInstance().point(master);
        } else {
            Service.getInstance().sendThongBao(this.master, "Vui lòng đợi "
                    + TimeUtil.getTimeLeft(lastTimeUnfusion, TIME_WAIT_AFTER_UNFUSION / 1000) + " nữa");
        }
    }

    public void unFusion() {
        master.fusion.typeFusion = 0;
        this.status = PROTECT;
        Service.getInstance().point(master);
        joinMapMaster();
        fusionEffect(master.fusion.typeFusion);
        Service.getInstance().Send_Caitrang(master);
        Service.getInstance().point(master);
        this.lastTimeUnfusion = System.currentTimeMillis();
    }

    private void fusionEffect(int type) {
        Message msg;
        try {
            msg = new Message(125);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) master.id);
            Service.getInstance().sendMessAllPlayerInMap(master, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    private void exitMapFusion() {
        if (this.zone != null) {
            MapService.gI().exitMap(this);
        }
    }

    public long lastTimeMoveIdle;
    private int timeMoveIdle;
    public boolean idle;

    private void moveIdle() {
        if (status == GOHOME || status == FUSION) {
            return;
        }
        if (idle && Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int dir = this.location.x - master.location.x <= 0 ? -1 : 1;
            PlayerService.gI().playerMove(this, master.location.x
                    + Util.nextInt(dir == -1 ? 30 : -50, dir == -1 ? 50 : 30), master.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(5000, 8000);
        }
    }

    private long lastTimeMoveAtHome;
    private byte directAtHome = -1;

    @Override
    public void update() {
        try {
            super.update();
            increasePoint(); // cộng chỉ số
            updatePower(); // check mở skill...
            if (isDie()) {
                if (System.currentTimeMillis() - lastTimeDie > 50000) {
                    Service.getInstance().hsChar(this, nPoint.hpMax, nPoint.mpMax);
                } else {
                    return;
                }
            }
            if (justRevived && this.zone == master.zone) {
                Service.getInstance().chatJustForMe(master, this, "Sư phụ ơi, con đây nè!");
                justRevived = false;
            }
            if (this.zone == null || this.zone != master.zone) {
                joinMapMaster();
            }
            if (master.isDie() || this.isDie() || effectSkill.isHaveEffectSkill()) {
                return;
            }
            moveIdle();
            switch (status) {
                case FOLLOW -> {
                }
                case PROTECT -> {
                    if (useSkill3() || useSkill4()) {
                        break;
                    }
                    mobAttack = findMobAttack();
                    // Debug log for bot's pet
                    if (master != null && master.isBot && mobAttack == null && System.currentTimeMillis() % 5000 < 100) {
                        System.out.println("[Pet-" + name + "] PROTECT: No mob found in zone " + (zone != null ? zone.map.mapId : "null"));
                    }
                    if (mobAttack != null) {
                        int disToMob = Util.getDistance(this, mobAttack);
                        if (disToMob <= ARANGE_ATT_SKILL1) {
                            // đấm
                            this.playerSkill.skillSelect = getSkill(1);
                            if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                if (SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20),
                                            mobAttack.location.y);
                                    SkillService.gI().useSkill(this, null, mobAttack, null);
                                } else {
                                    askPea();
                                }
                            }
                        } else {
                            // chưởng
                            this.playerSkill.skillSelect = getSkill(2);
                            if (this.playerSkill.skillSelect.skillId != -1) {
                                if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        SkillService.gI().useSkill(this, null, mobAttack, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            }
                        }

                    } else {
                        idle = true;
                    }
                }
                case ATTACK -> {
                    if (useSkill3() || useSkill4()) {
                        break;
                    }
                    mobAttack = findMobAttack();
                    // Debug log for bot's pet
                    if (master != null && master.isBot && System.currentTimeMillis() % 5000 < 100) {
                        System.out.println("[Pet-" + name + "] ATTACK mode: zone=" + (zone != null ? zone.map.mapId : "null") + 
                            ", mobFound=" + (mobAttack != null) + ", skills=" + (playerSkill != null ? playerSkill.skills.size() : 0));
                    }
                    if (mobAttack != null) {
                        int disToMob = Util.getDistance(this, mobAttack);
                        if (disToMob <= ARANGE_ATT_SKILL1) {
                            this.playerSkill.skillSelect = getSkill(1);
                            if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                if (SkillService.gI().canUseSkillWithMana(this)) {
                                    PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20),
                                            mobAttack.location.y);
                                    SkillService.gI().useSkill(this, null, mobAttack, null);
                                } else {
                                    askPea();
                                }
                            }
                        } else {
                            // Chưởng
                            this.playerSkill.skillSelect = getSkill(2);
                            if (this.playerSkill.skillSelect.skillId != -1) {
                                if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        SkillService.gI().useSkill(this, null, mobAttack, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            } else {
                                // Fallback đấm nếu không có skill chưởng
                                this.playerSkill.skillSelect = getSkill(1);
                                if (SkillService.gI().canUseSkillWithCooldown(this)) {
                                    if (SkillService.gI().canUseSkillWithMana(this)) {
                                        PlayerService.gI().playerMove(this,
                                                mobAttack.location.x + Util.nextInt(-20, 20), mobAttack.location.y);
                                        SkillService.gI().useSkill(this, null, mobAttack, null);
                                    } else {
                                        askPea();
                                    }
                                }
                            }
                        }

                    } else {
                        idle = true;
                    }
                }

                case GOHOME -> {
                    if (this.zone != null
                            && (this.zone.map.mapId == 21 || this.zone.map.mapId == 22 || this.zone.map.mapId == 23)) {
                        if (System.currentTimeMillis() - lastTimeMoveAtHome <= 5000) {
                            return;
                        } else {
                            switch (this.zone.map.mapId) {
                                case 21 -> {
                                    if (directAtHome == -1) {
                                        PlayerService.gI().playerMove(this, 250, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 200, 336);
                                        directAtHome = -1;
                                    }
                                }
                                case 22 -> {
                                    if (directAtHome == -1) {
                                        PlayerService.gI().playerMove(this, 500, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 452, 336);
                                        directAtHome = -1;
                                    }
                                }
                                case 23 -> {
                                    if (directAtHome == -1) {
                                        PlayerService.gI().playerMove(this, 250, 336);
                                        directAtHome = 1;
                                    } else {
                                        PlayerService.gI().playerMove(this, 200, 336);
                                        directAtHome = -1;
                                    }
                                }
                                default -> {
                                }
                            }
                            Service.getInstance().chatJustForMe(master, this, "Hello sư phụ!");
                            lastTimeMoveAtHome = System.currentTimeMillis();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Logger.logException(Pet.class, e);
        }
    }

    @Override
    public void dispose() {
        if (zone != null) {
            MapService.gI().exitMap(this);
        }
        this.mobAttack = null;
        this.master = null;
        super.dispose();
    }

    private long lastTimeAskPea;

    public void askPea() {
        if (this.isMabu && master.charms.tdDeTuMabu > System.currentTimeMillis()) {
            InventoryService.gI().eatPea(master);
        } else if (Util.canDoWithTime(lastTimeAskPea, 10000)) {
            if (!this.isBoss) {
                Service.getInstance().chatJustForMe(master, this, "Sư phụ ơi cho con đậu thần");
                InventoryService.gI().eatPea(master);
            }
            lastTimeAskPea = System.currentTimeMillis();
        }
    }

    private int countTTNL;

    private boolean useSkill3() {
        try {
            playerSkill.skillSelect = getSkill(3);
            if (playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.THAI_DUONG_HA_SAN -> {
                    if (SkillService.gI().canUseSkillWithCooldown(this)
                            && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkillThaiDuong(this);
                        Service.getInstance().chatJustForMe(master, this, "Thái dương hạ san");
                        return true;
                    }
                    return false;
                }
                case Skill.TAI_TAO_NANG_LUONG -> {
                    if (this.effectSkill.isCharging && this.countTTNL < Util.nextInt(3, 5)) {
                        this.countTTNL++;
                        return true;
                    }
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)
                            && (this.nPoint.getCurrPercentHP() <= 20 || this.nPoint.getCurrPercentMP() <= 20)) {
                        SkillService.gI().useSkillTTNL(this);
                        this.countTTNL = 0;
                        return true;
                    }
                    return false;
                }
                case Skill.KAIOKEN -> {
                    if (SkillService.gI().canUseSkillWithCooldown(this)
                            && SkillService.gI().canUseSkillWithMana(this)) {
                        mobAttack = this.findMobAttack();
                        if (mobAttack == null) {
                            return false;
                        }
                        int dis = Util.getDistance(this, mobAttack);
                        if (dis > ARANGE_ATT_SKILL1) {
                            PlayerService.gI().playerMove(this, mobAttack.location.x, mobAttack.location.y);
                        } else {
                            if (SkillService.gI().canUseSkillWithCooldown(this)
                                    && SkillService.gI().canUseSkillWithMana(this)) {
                                PlayerService.gI().playerMove(this, mobAttack.location.x + Util.nextInt(-20, 20),
                                        mobAttack.location.y);
                            }
                        }
                        SkillService.gI().useSkill(this, playerAttack, mobAttack, null);
                        getSkill(1).lastTimeUseThisSkill = System.currentTimeMillis();
                        return true;
                    }
                    return false;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean useSkill4() {
        try {
            this.playerSkill.skillSelect = getSkill(4);
            if (this.playerSkill.skillSelect.skillId == -1) {
                return false;
            }
            switch (this.playerSkill.skillSelect.template.id) {
                case Skill.BIEN_KHI -> {
                    if (!this.effectSkill.isMonkey && SkillService.gI().canUseSkillWithCooldown(this)
                            && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkillBienKhi(this);
                        return true;
                    }
                    return false;
                }
                case Skill.KHIEN_NANG_LUONG -> {
                    if (!this.effectSkill.isShielding && SkillService.gI().canUseSkillWithCooldown(this)
                            && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkillKhien(this);
                        return true;
                    }
                    return false;
                }
                case Skill.DE_TRUNG -> {
                    if (this.mobMe == null && SkillService.gI().canUseSkillWithCooldown(this)
                            && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkillDeTrung(this);
                        return true;
                    }
                    return false;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void increasePoint() {
        if (this.nPoint != null) {
            if (status != FUSION) {
                long def = nPoint.defg;
                if (Util.isTrue(1, 30)) {
                    if (def >= 3500) {
                        this.nPoint.increasePoint(CRIT, (short) 1);
                    } else {
                        this.nPoint.increasePoint((byte) Util.nextInt(DEF, CRIT), (short) 10);
                    }
                } else {
                    this.nPoint.increasePoint((byte) Util.nextInt(HP, DAME), (short) 30);
                }
            }
        }
    }

    public void followMaster() {
        if (this.isDie() || effectSkill.isHaveEffectSkill()) {
            return;
        }
        switch (this.status) {
            case ATTACK:
                if (ANGRY) {
                    followMaster(80);
                } else {
                    if ((mobAttack != null && Util.getDistance(this, master) <= 500)) {
                        break;
                    }
                }
            case FOLLOW:
            case PROTECT:
                followMaster(60);
                break;
        }
    }

    private void followMaster(int dis) {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= dis) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, dis);
            } else {
                this.location.x = mX + Util.nextInt(0, dis);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    public short getAvatar() {
        // Pet type
        PetType petType = getPetType();
        if (petType != PetType.NONE) {
            short head = petType.getHead();
            if (head != -1) {
                return head;
            }
        }

        // Default
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][0];
        } else {
            return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getHead() {
        // Effect skill
        if (effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        if (effectSkill.isSocola || effectSkin.isSocola) {
            return 412;
        }
        if (effectSkin != null && effectSkin.isHoaDa) {
            return 454;
        }

        // Pet type
        PetType petType = getPetType();
        if (petType != PetType.NONE) {
            short head = petType.getHead();
            if (head != -1) {
                return head;
            }
        }

        // Cải trang
        if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null && ct.getHead() != -1) {
                return (short) ct.getHead();
            }
            return inventory.itemsBody.get(5).template.part;
        }

        // Default
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][0];
        } else {
            return PET_ID[3][this.gender];
        }
    }

    @Override
    public short getBody() {
        // Effect skill
        if (effectSkill.isMonkey) {
            return 193;
        }
        if (effectSkill.isSocola || effectSkin.isSocola) {
            return 413;
        }
        if (effectSkin != null && effectSkin.isHoaDa) {
            return 455;
        }

        // Pet type
        PetType petType = getPetType();
        if (petType != PetType.NONE) {
            short body = petType.getBody();
            if (body != -1) {
                return body;
            }
        }

        // Cải trang
        if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null && ct.getBody() != -1) {
                return (short) ct.getBody();
            }
        }

        // Áo
        if (inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }

        // Default
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][1];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
        }
    }

    @Override
    public short getLeg() {
        // Effect skill
        if (effectSkill.isMonkey) {
            return 194;
        }
        if (effectSkill.isSocola || effectSkin.isSocola) {
            return 414;
        }
        if (effectSkin != null && effectSkin.isHoaDa) {
            return 456;
        }

        // Pet type
        PetType petType = getPetType();
        if (petType != PetType.NONE) {
            short leg = petType.getLeg();
            if (leg != -1) {
                return leg;
            }
        }

        // Cải trang
        if (inventory.itemsBody.get(5).isNotNullItem()) {
            CaiTrang ct = Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
            if (ct != null && ct.getLeg() != -1) {
                return (short) ct.getLeg();
            }
        }

        // Quần
        if (inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }

        // Default
        if (this.nPoint.power < 1500000) {
            return PET_ID[this.gender][2];
        } else {
            return (short) (gender == ConstPlayer.NAMEC ? 60 : 58);
        }
    }

    private Mob findMobAttack() {
        int dis = ARANGE_CAN_ATTACK;
        Mob mobAtt = null;
        for (Mob mob : zone.mobs) {
            if (mob.isDie()) {
                continue;
            }
            int d = Util.getDistance(this, mob);
            if (d <= dis) {
                dis = d;
                mobAtt = mob;
            }
        }
        return mobAtt;
    }

    private void updatePower() {
        if (this.playerSkill != null) {
            switch (this.playerSkill.getSizeSkill()) {
                case 1:
                    if (this.nPoint.power >= 150000000) {
                        openSkill2();
                    }
                    break;
                case 2:
                    if (this.nPoint.power >= 1500000000) {
                        openSkill3();
                    }
                    break;
                case 3:
                    if (this.nPoint.power >= 20000000000L) {
                        openSkill4();
                    }
                    break;
            }
        }
    }

    public void openSkill2() {
        Skill skill = null;
        int tiLeKame = 40;
        int tiLeMasenko = 20;
        int tiLeAntomic = 40;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeKame) {
            skill = SkillUtil.createSkill(Skill.KAMEJOKO, 1);
        } else if (rd <= tiLeKame + tiLeMasenko) {
            skill = SkillUtil.createSkill(Skill.MASENKO, 1);
        } else if (rd <= tiLeKame + tiLeMasenko + tiLeAntomic) {
            skill = SkillUtil.createSkill(Skill.ANTOMIC, 1);
        }
        skill.coolDown = PET_SKILL_COOLDOWN; 
        this.playerSkill.skills.set(1, skill);
    }

    public void openSkill3() {
        Skill skill = null;
        int tiLeTDHS = 30;
        int tiLeTTNL = 40;
        int tiLeKOK = 30;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeTDHS) {
            skill = SkillUtil.createSkill(Skill.THAI_DUONG_HA_SAN, 1);
        } else if (rd <= tiLeTDHS + tiLeTTNL) {
            skill = SkillUtil.createSkill(Skill.TAI_TAO_NANG_LUONG, 1);
        } else if (rd <= tiLeTDHS + tiLeTTNL + tiLeKOK) {
            skill = SkillUtil.createSkill(Skill.KAIOKEN, 1);
        }
        this.playerSkill.skills.set(2, skill);
    }

    public void openSkill4() {
        Skill skill = null;
        int tiLeBienKhi = 30;
        int tiLeDeTrung = 60;
        int tiLeKNL = 10;

        int rd = Util.nextInt(1, 100);
        if (rd <= tiLeBienKhi) {
            skill = SkillUtil.createSkill(Skill.BIEN_KHI, 1);
        } else if (rd <= tiLeBienKhi + tiLeDeTrung) {
            skill = SkillUtil.createSkill(Skill.DE_TRUNG, 1);
        } else if (rd <= tiLeBienKhi + tiLeDeTrung + tiLeKNL) {
            skill = SkillUtil.createSkill(Skill.KHIEN_NANG_LUONG, 1);
        }
        this.playerSkill.skills.set(3, skill);
    }

    private Skill getSkill(int indexSkill) {
        return this.playerSkill.skills.get(indexSkill - 1);
    }

    public void transform() {
        if (this.isMabu) {
            this.isTransform = !this.isTransform;
            Service.getInstance().Send_Caitrang(this);
            Service.getInstance().chat(this, "Bư bư bư....");
        }
    }

    public void angry(Player plAtt) {
        ANGRY = true;
        if (plAtt != null) {
            this.playerAttack = plAtt;
            Service.getInstance().chatJustForMe(master, this, "Mi làm ta nổi giận rồi " + playerAttack.name
                    .replace("$", ""));
        }
    }

    // ==================== LEVEL SYSTEM ====================

    public static final int MAX_LEVEL = 7;

    /**
     * % bonus chỉ số mỗi level (HP, MP, Dame)
     */
    public static final int BONUS_PERCENT_PER_LEVEL = 5;

    /**
     * Exp cần cho từng level (level 1-7)
     */
    private static final long[] EXP_REQUIRED = {
            2000, // Level 1 -> 2
            5000, // Level 2 -> 3
            10000, // Level 3 -> 4
            20000, // Level 4 -> 5
            40000, // Level 5 -> 6
            80000, // Level 6 -> 7
            0 // Level 7 (max)
    };

    /**
     * Lấy exp cần để lên level tiếp theo
     */
    public long getExpRequired() {
        if (level <= 0 || level > MAX_LEVEL)
            return 0;
        return EXP_REQUIRED[level - 1];
    }

    /**
     * Lấy exp cần cho level cụ thể
     */
    public static long getExpRequiredForLevel(int lvl) {
        if (lvl <= 0 || lvl > MAX_LEVEL)
            return 0;
        return EXP_REQUIRED[lvl - 1];
    }

    public boolean addExp(long exp) {
        if (level >= MAX_LEVEL) {
            if (master != null) {
                Service.getInstance().sendThongBao(master,
                        "Đệ tử " + name + " đã đạt level tối đa!");
            }
            return false;
        }

        this.accumulatedExp += exp;

        if (master != null) {
            Service.getInstance().sendThongBao(master,
                    "Đệ tử " + name + " nhận được " + exp + " exp! (Tích lũy: " + accumulatedExp + ")");
        }

        return true;
    }

    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    public int calculateBreakthroughAttempts() {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        long expRequired = getExpRequired();
        if (expRequired <= 0) {
            return 0;
        }
        return (int) (accumulatedExp / expRequired);
    }

    public boolean canBreakthrough() {
        if (level >= MAX_LEVEL) {
            return false;
        }
        return accumulatedExp >= getExpRequired();
    }

    public BreakthroughResult attemptBreakthrough() {
        int oldLevel = this.level;
        long expRequired = getExpRequired();

        // Kiểm tra điều kiện
        if (level >= MAX_LEVEL || accumulatedExp < expRequired) {
            return new BreakthroughResult(false, oldLevel, oldLevel, 0, accumulatedExp);
        }

        // Trừ exp
        accumulatedExp -= expRequired;

        // Random 50% thành công
        boolean success = Util.nextInt(0, 99) < 50;

        if (success) {
            level++;
        }

        return new BreakthroughResult(success, oldLevel, level, expRequired, accumulatedExp);
    }

    /**
     * Lấy level hiện tại của pet
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Lấy exp hiện tại của pet
     */
    public long getExpLevel() {
        return this.expLevel;
    }

    /**
     * Lấy % bonus chỉ số từ level (HP, MP, Dame)
     * Level 1: 0%, Level 2: 5%, Level 3: 10%, ..., Level 7: 30%
     */
    public int getBonusPercent() {
        if (level <= 1)
            return 0;
        return (level - 1) * BONUS_PERCENT_PER_LEVEL;
    }

    /**
     * Lấy loại đệ VIP hiện tại
     */
    public PetType getPetType() {
        if (isCellBao)
            return PetType.CELL_BAO;
        if (isBillNhi)
            return PetType.BILL_NHI;
        if (isFideTrau)
            return PetType.FIDE_TRAU;
        if (isBulo)
            return PetType.SAYAN5;
        if (isMabu)
            return PetType.MABU;
        if (isSuperPicolo)
            return PetType.SUPER_PICOLO;
        return PetType.NONE;
    }

    /**
     * Kiểm tra có đang hợp thể Porata không
     */
    public boolean isInPorataFusion() {
        int fusionType = master.fusion.typeFusion;
        return fusionType == ConstPlayer.HOP_THE_PORATA || fusionType == ConstPlayer.HOP_THE_PORATA2;
    }

    public int getPorataBonus() {
        if (!isInPorataFusion()) {
            return 0;
        }
        return getPetType().getBonus();
    }
}
