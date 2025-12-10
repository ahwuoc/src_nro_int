package nro.ahwuocdz;

import nro.consts.ConstNpc;
import nro.models.boss.Boss;
import nro.models.boss.BossFactory;
import nro.models.player.Player;
import nro.server.Client;
import nro.server.ServerManager;
import nro.services.NpcService;
import nro.services.Service;

/**
 * Menu Admin để spawn boss
 * @author ahwuocdz
 */
public class AdminBossMenu {
    
    // Menu index cho các trang boss
    public static final int MENU_BOSS_PAGE_1 = 17998;
    public static final int MENU_BOSS_PAGE_2 = 17999;
    public static final int MENU_BOSS_PAGE_3 = 18000;
    
    /**
     * Hiển thị menu chính admin
     */
    public static void showMainMenu(Player player) {
        String text = "|7|-----ahwuocdz-----\n"
                + "|1|Số người chơi đang online: " + Client.gI().getPlayers().size() + "\n"
                + "|8|Current thread server: " + Thread.activeCount() + "\n"
                + "|6|Time start server: " + ServerManager.timeStart;
        
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, 17997, text,
                "Boss Trang 1", "Boss Trang 2", "Boss Trang 3", "Tìm kiếm\nngười chơi", "Đóng");
    }
    
    /**
     * Hiển thị menu boss trang 1
     */
    public static void showBossPage1(Player player) {
        String text = "|7|Chọn Boss để spawn:\n\n"
                + "|1|1. Broly\n"
                + "|2|2. Siêu Bọ Hung\n"
                + "|3|3. Xên Bọ Hung\n"
                + "|4|4. Xên Bọ Hung 1\n"
                + "|5|5. Cooler";
        
        NpcService.gI().createMenuConMeo(player, MENU_BOSS_PAGE_1, 17997, text,
                "Broly", "Siêu Bọ Hung", "Xên Bọ Hung", "Xên BH 1", "Cooler", "Quay lại");
    }
    
    /**
     * Hiển thị menu boss trang 2
     */
    public static void showBossPage2(Player player) {
        String text = "|7|Chọn Boss để spawn:\n\n"
                + "|1|1. Chill\n"
                + "|2|2. Black Goku\n"
                + "|3|3. King Kong\n"
                + "|4|4. Android 13\n"
                + "|5|5. Android 20";
        
        NpcService.gI().createMenuConMeo(player, MENU_BOSS_PAGE_2, 17997, text,
                "Chill", "Black Goku", "King Kong", "Android 13", "Android 20", "Quay lại");
    }
    
    /**
     * Hiển thị menu boss trang 3
     */
    public static void showBossPage3(Player player) {
        String text = "|7|Chọn Boss để spawn:\n\n"
                + "|1|1. Super Broly\n"
                + "|2|2. Thiên Sứ\n"
                + "|3|3. Boss World\n"
                + "|4|4. Kuku\n"
                + "|5|5. Rambo";
        
        NpcService.gI().createMenuConMeo(player, MENU_BOSS_PAGE_3, 17997, text,
                "Super Broly", "Thiên Sứ", "Boss World", "Kuku", "Rambo", "Quay lại");
    }
    
    /**
     * Xử lý chọn menu
     */
    public static void handleMenuSelect(Player player, int menuIndex, int select) {
        switch (menuIndex) {
            case ConstNpc.MENU_ADMIN:
                handleMainMenu(player, select);
                break;
            case MENU_BOSS_PAGE_1:
                handleBossPage1(player, select);
                break;
            case MENU_BOSS_PAGE_2:
                handleBossPage2(player, select);
                break;
            case MENU_BOSS_PAGE_3:
                handleBossPage3(player, select);
                break;
        }
    }
    
    private static void handleMainMenu(Player player, int select) {
        switch (select) {
            case 0: // Boss Trang 1
                showBossPage1(player);
                break;
            case 1: // Boss Trang 2
                showBossPage2(player);
                break;
            case 2: // Boss Trang 3
                showBossPage3(player);
                break;
            case 3: // Tìm kiếm người chơi
                // TODO: Implement search player
                break;
            case 4: // Đóng
                break;
        }
    }
    
    private static void handleBossPage1(Player player, int select) {
        switch (select) {
            case 0:
                spawnBoss(player, BossFactory.BROLY, "Broly");
                break;
            case 1:
                spawnBoss(player, BossFactory.SIEU_BO_HUNG, "Siêu Bọ Hung");
                break;
            case 2:
                spawnBoss(player, BossFactory.XEN_BO_HUNG, "Xên Bọ Hung");
                break;
            case 3:
                spawnBoss(player, BossFactory.XEN_BO_HUNG_1, "Xên Bọ Hung 1");
                break;
            case 4:
                spawnBoss(player, BossFactory.COOLER, "Cooler");
                break;
            case 5: // Quay lại
                showMainMenu(player);
                break;
        }
    }
    
    private static void handleBossPage2(Player player, int select) {
        switch (select) {
            case 0:
                spawnBoss(player, BossFactory.CHILL, "Chill");
                break;
            case 1:
                spawnBoss(player, BossFactory.BLACKGOKU, "Black Goku");
                break;
            case 2:
                spawnBoss(player, BossFactory.KINGKONG, "King Kong");
                break;
            case 3:
                spawnBoss(player, BossFactory.ANDROID_13, "Android 13");
                break;
            case 4:
                spawnBoss(player, BossFactory.ANDROID_20, "Android 20");
                break;
            case 5: // Quay lại
                showMainMenu(player);
                break;
        }
    }
    
    private static void handleBossPage3(Player player, int select) {
        switch (select) {
            case 0:
                spawnBoss(player, BossFactory.SUPER_BROLY_RED, "Super Broly");
                break;
            case 1:
                spawnBoss(player, BossFactory.THIENSU, "Thiên Sứ");
                break;
            case 2:
                spawnBoss(player, BossFactory.BOSS_WORLD, "Boss World");
                break;
            case 3:
                spawnBoss(player, BossFactory.KUKU, "Kuku");
                break;
            case 4:
                spawnBoss(player, BossFactory.RAMBO, "Rambo");
                break;
            case 5: // Quay lại
                showMainMenu(player);
                break;
        }
    }
    
    /**
     * Spawn boss
     */
    private static void spawnBoss(Player player, int bossId, String bossName) {
        try {
            Boss boss = BossFactory.createBoss(bossId);
            if (boss != null) {
                Service.getInstance().sendThongBao(player, "Đã spawn " + bossName + " thành công!");
            } else {
                Service.getInstance().sendThongBao(player, "Không thể spawn " + bossName + "!");
            }
        } catch (Exception e) {
            Service.getInstance().sendThongBao(player, "Lỗi khi spawn boss: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
