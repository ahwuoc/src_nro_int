package nro.services.task;

import nro.consts.ConstNpc;
import nro.jdbc.daos.PlayerTaskDetyDAO;
import nro.models.player.Player;
import nro.models.task.PlayerTaskDety;
import nro.models.task.TaskDetyTemplate;
import nro.manager.TaskDetyManager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import nro.utils.Util;

import java.util.List;

/**
 * Quản lý nhiệm vụ đệ tử
 * @author 💖 ahwuocdz 💖
 */
public class TaskDeTy {
    
    private static TaskDeTy instance;
    
    public static TaskDeTy gI() {
        if (instance == null) {
            instance = new TaskDeTy();
        }
        return instance;
    }
    
    /**
     * Lấy hoặc tạo PlayerTaskDety cho player
     */
    public PlayerTaskDety getPlayerTask(Player player) {
        if (player.taskDety == null) {
            player.taskDety = PlayerTaskDetyDAO.gI().getByPlayerId(player.id);
        }
        return player.taskDety;
    }
    
    /**
     * Lưu tiến độ nhiệm vụ của player
     */
    public void savePlayerTask(Player player) {
        if (player.taskDety != null) {
            PlayerTaskDetyDAO.gI().save(player.taskDety);
        }
    }
    
    /**
     * Hiển thị menu chính nhiệm vụ đệ tử
     */
    public void showMainMenu(Player player, int npcId) {
        PlayerTaskDety task = getPlayerTask(player);
        task.checkAndResetDaily();
        
        String text = "Ngươi có dám đương đầu thử thách,\n"
                + "thu thập sức mạnh và rèn luyện đệ tử của mình?\n\n"
                + "Lượt còn lại hôm nay: " + task.getRemainingCount() + "/" + PlayerTaskDety.MAX_DAILY_COUNT;
        
        if (task.hasActiveTask()) {
            TaskDetyTemplate template = TaskDetyManager.gI().getTaskById(task.getCurrentTaskId());
            if (template != null) {
                text += "\n\nNhiệm vụ hiện tại: " + template.getModeName()
                      + "\nMap: " + template.getMapName()
                      + "\nQuái: " + template.getMobName()
                      + "\nTiến độ: " + task.getCurrentKillCount() + "/" + template.getKillCount();
            }
        }
        
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_TASK_DETY_MAIN, npcId, text,
                "Nhiệm vụ", "Info Đệ Tử", "Đột Phá Level Đệ", "Từ chối");
    }
    
    /**
     * Hiển thị menu chọn độ khó nhiệm vụ
     */
    public void showDifficultyMenu(Player player, int npcId) {
        PlayerTaskDety task = getPlayerTask(player);
        
        // Kiểm tra đang có nhiệm vụ chưa hoàn thành
        if (task.hasActiveTask()) {
            Service.getInstance().sendThongBao(player, "Bạn đang có nhiệm vụ chưa hoàn thành!");
            return;
        }
        
        // Kiểm tra còn lượt không
        if (!task.canAcceptTask()) {
            Service.getInstance().sendThongBao(player, "Bạn đã hết lượt nhận nhiệm vụ hôm nay!");
            return;
        }
        
        String text = "Hãy chọn độ khó nhiệm vụ:\n\n"
                + "Lượt còn lại: " + task.getRemainingCount() + "/" + PlayerTaskDety.MAX_DAILY_COUNT;
        
        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_TASK_DETY_DIFFICULTY, npcId, text,
                "Nhiệm vụ Dễ", "Nhiệm vụ Khó", "Nhiệm vụ Siêu Khó", "Quay lại");
    }
    
    /**
     * Hiển thị thông tin đệ tử
     */
    public void showPetInfo(Player player, int npcId) {
        if (player.pet == null) {
            String text = "Ngươi chưa có đệ tử!\nHãy hoàn thành nhiệm vụ để nhận đệ tử.";
            NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, text, "Đóng");
            return;
        }
        
        // Bonus từ level
        int levelBonus = player.pet.getBonusPercent();
        String levelBonusText = levelBonus > 0 
                ? " (+" + levelBonus + "% chỉ số)" 
                : "";
        
        // Loại đệ và bonus Porata
        nro.models.player.Pet.PetType petType = player.pet.getPetType();
        String petTypeText = petType.getDisplayName();
        int porataBonus = petType.getBonus();
        String porataBonusText = porataBonus > 0 
                ? " (+" + porataBonus + "% khi hợp thể Porata)"
                : "";
        
        String text = "Thông tin đệ tử của ngươi:\n\n"
                + "Tên: " + player.pet.name + "\n"
                + "Loại: " + petTypeText + porataBonusText + "\n"
                + "Level: " + player.pet.level + (player.pet.isMaxLevel() ? " (MAX)" : "") + levelBonusText + "\n"
                + "Exp: " + player.pet.expLevel + "/" + player.pet.getExpRequired() + "\n"
                + "Sức mạnh: " + player.pet.nPoint.power + "\n"
                + "HP: " + player.pet.nPoint.hp + "/" + player.pet.nPoint.hpMax + "\n"
                + "KI: " + player.pet.nPoint.mp + "/" + player.pet.nPoint.mpMax + "\n"
                + "Sức đánh: " + player.pet.nPoint.dame;
        
        NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, text, "Đóng");
    }

    /**
     * Hiển thị menu đột phá level đệ tử
     */
    public void showBreakthroughMenu(Player player, int npcId) {
        // Kiểm tra có đệ tử không
        if (player.pet == null) {
            String text = "Ngươi chưa có đệ tử!\nHãy hoàn thành nhiệm vụ để nhận đệ tử.";
            NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, text, "Đóng");
            return;
        }
        
        // Kiểm tra đã max level chưa
        if (player.pet.isMaxLevel()) {
            String text = "Đệ tử " + player.pet.name + " đã đạt level tối đa!\n"
                    + "Level: " + player.pet.level + " (MAX)";
            NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, text, "Đóng");
            return;
        }
        
        // Hiển thị thông tin đột phá
        long expRequired = player.pet.getExpRequired();
        int attempts = player.pet.calculateBreakthroughAttempts();
        
        String text = "=== ĐỘT PHÁ LEVEL ĐỆ TỬ ===\n\n"
                + "Đệ tử: " + player.pet.name + "\n"
                + "Level hiện tại: " + player.pet.level + "\n"
                + "Exp tích lũy: " + player.pet.accumulatedExp + "\n"
                + "Exp cần/lần: " + expRequired + "\n"
                + "Số lần có thể đột phá: " + attempts + "\n\n"
                + "Tỉ lệ thành công: 50%\n"
                + "(Thất bại vẫn mất exp)";
        
        if (attempts > 0) {
            NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_TASK_DETY_BREAKTHROUGH, npcId, text, 
                    "Đột Phá", "Quay lại");
        } else {
            NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, 
                    text + "\n\n[!] Không đủ exp để đột phá!", "Đóng");
        }
    }

    /**
     * Xử lý xác nhận đột phá
     */
    public void handleBreakthroughConfirm(Player player, int npcId, int select) {
        if (select != 0) { // Không phải "Đột Phá"
            showMainMenu(player, npcId);
            return;
        }
        
        if (player.pet == null) {
            Service.getInstance().sendThongBao(player, "Bạn chưa có đệ tử!");
            return;
        }
        
        if (!player.pet.canBreakthrough()) {
            Service.getInstance().sendThongBao(player, "Không thể đột phá! Kiểm tra exp hoặc level.");
            return;
        }
        
        // Thực hiện đột phá
        nro.models.player.BreakthroughResult result = player.pet.attemptBreakthrough();
        
        String text;
        if (result.isSuccess()) {
            text = "=== ĐỘT PHÁ THÀNH CÔNG ===\n\n"
                    + "Đệ tử " + player.pet.name + " đã lên Level " + result.getNewLevel() + "!\n"
                    + "Exp đã dùng: " + result.getExpUsed() + "\n"
                    + "Exp còn lại: " + result.getRemainingExp();
            Service.getInstance().chatJustForMe(player, player.pet, 
                    "Sư phụ ơi, con lên Level " + result.getNewLevel() + " rồi!");
        } else {
            text = "=== ĐỘT PHÁ THẤT BẠI ===\n\n"
                    + "Đệ tử " + player.pet.name + " vẫn ở Level " + result.getOldLevel() + "\n"
                    + "Exp đã mất: " + result.getExpUsed() + "\n"
                    + "Exp còn lại: " + result.getRemainingExp();
            Service.getInstance().chatJustForMe(player, player.pet, 
                    "Sư phụ ơi, con thất bại rồi... huhu");
        }
        
        // Hiển thị kết quả và cho phép tiếp tục đột phá
        int remainingAttempts = player.pet.calculateBreakthroughAttempts();
        if (remainingAttempts > 0 && !player.pet.isMaxLevel()) {
            text += "\n\nCòn " + remainingAttempts + " lần đột phá.";
            NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_TASK_DETY_BREAKTHROUGH, npcId, text, 
                    "Tiếp tục Đột Phá", "Quay lại");
        } else {
            NpcService.gI().createMenuConMeo(player, ConstNpc.BASE_MENU, npcId, text, "Đóng");
        }
    }

    /**
     * Hiển thị xác nhận nhận nhiệm vụ theo độ khó
     */
    private void showTaskConfirm(Player player, int npcId, int mode) {
        String modeName;
        String reward;
        int menuIndex;
        
        switch (mode) {
            case TaskDetyTemplate.MODE_EASY:
                modeName = "DỄ";
                reward = "ít";
                menuIndex = ConstNpc.MENU_TASK_DETY_EASY;
                break;
            case TaskDetyTemplate.MODE_NORMAL:
                modeName = "KHÓ";
                reward = "nhiều";
                menuIndex = ConstNpc.MENU_TASK_DETY_HARD;
                break;
            case TaskDetyTemplate.MODE_HARD:
                modeName = "SIÊU KHÓ";
                reward = "cực nhiều";
                menuIndex = ConstNpc.MENU_TASK_DETY_EXTREME;
                break;
            default:
                return;
        }
        
        PlayerTaskDety task = getPlayerTask(player);
        String text = "Bạn đã chọn nhiệm vụ " + modeName + "\n\n"
                + "Phần thưởng: " + reward + " quà hấp dẫn\n"
                + "Lượt còn lại: " + task.getRemainingCount() + "/" + PlayerTaskDety.MAX_DAILY_COUNT + "\n\n"
                + "Bạn có muốn nhận không?";
        
        NpcService.gI().createMenuConMeo(player, menuIndex, npcId, text, "Nhận", "Từ chối");
    }
    
    /**
     * Xử lý khi player chọn option trong menu
     */
    public void handleMenuSelect(Player player, int npcId, int select) {
        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.MENU_TASK_DETY_MAIN:
                handleMainMenuSelect(player, npcId, select);
                break;
            case ConstNpc.MENU_TASK_DETY_DIFFICULTY:
                handleDifficultyMenuSelect(player, npcId, select);
                break;
            case ConstNpc.MENU_TASK_DETY_EASY:
                handleTaskAccept(player, npcId, select, TaskDetyTemplate.MODE_EASY);
                break;
            case ConstNpc.MENU_TASK_DETY_HARD:
                handleTaskAccept(player, npcId, select, TaskDetyTemplate.MODE_NORMAL);
                break;
            case ConstNpc.MENU_TASK_DETY_EXTREME:
                handleTaskAccept(player, npcId, select, TaskDetyTemplate.MODE_HARD);
                break;
            case ConstNpc.MENU_TASK_DETY_BREAKTHROUGH:
                handleBreakthroughConfirm(player, npcId, select);
                break;
        }
    }
    
    private void handleMainMenuSelect(Player player, int npcId, int select) {
        switch (select) {
            case 0: // Nhiệm vụ
                showDifficultyMenu(player, npcId);
                break;
            case 1: // Info Đệ Tử
                showPetInfo(player, npcId);
                break;
            case 2: // Đột Phá Level Đệ
                showBreakthroughMenu(player, npcId);
                break;
            case 3: // Từ chối
                break;
        }
    }
    
    private void handleDifficultyMenuSelect(Player player, int npcId, int select) {
        switch (select) {
            case 0: // Nhiệm vụ Dễ
                showTaskConfirm(player, npcId, TaskDetyTemplate.MODE_EASY);
                break;
            case 1: // Nhiệm vụ Khó
                showTaskConfirm(player, npcId, TaskDetyTemplate.MODE_NORMAL);
                break;
            case 2: // Nhiệm vụ Siêu Khó
                showTaskConfirm(player, npcId, TaskDetyTemplate.MODE_HARD);
                break;
            case 3: // Quay lại
                showMainMenu(player, npcId);
                break;
        }
    }
    
    private void handleTaskAccept(Player player, int npcId, int select, int mode) {
        if (select == 0) { // Nhận
            PlayerTaskDety playerTask = getPlayerTask(player);
            
            // Kiểm tra lại lượt
            if (!playerTask.canAcceptTask()) {
                Service.getInstance().sendThongBao(player, "Bạn đã hết lượt nhận nhiệm vụ hôm nay!");
                return;
            }
            
            // Random nhiệm vụ từ danh sách theo mode
            List<TaskDetyTemplate> tasks = TaskDetyManager.gI().getTasksByMode(mode);
            if (!tasks.isEmpty()) {
                TaskDetyTemplate task = tasks.get(Util.nextInt(0, tasks.size() - 1));
                
                // Lưu nhiệm vụ vào player
                playerTask.acceptTask(task.getId());
                savePlayerTask(player);
                
                NpcService.gI().createTutorial(player, npcId, 
                        "Đã nhận nhiệm vụ " + task.getModeName() + "!\n"
                        + "Mục tiêu: Giết " + task.getKillCount() + " quái\n"
                        + "Hãy cố gắng hoàn thành nhé!");
            } else {
                Service.getInstance().sendThongBao(player, "Hiện tại chưa có nhiệm vụ nào!");
            }
        }
        // select == 1: Từ chối -> đóng menu
    }
    
    /**
     * Xử lý khi đệ tử (pet) giết quái
     * @param pet - Pet đã giết quái
     * @param mobId - ID quái bị giết
     */
    public void onKillMob(Player pet, int mobId) {
        if (pet == null || !(pet instanceof nro.models.player.Pet)) {
            return;
        }
        nro.models.player.Pet petObj = (nro.models.player.Pet) pet;
        Player owner = petObj.master;
        if (owner == null) {
            return;
        }
        
        PlayerTaskDety playerTask = getPlayerTask(owner);
        if (!playerTask.hasActiveTask()) return;
        
        TaskDetyTemplate template = TaskDetyManager.gI().getTaskById(playerTask.getCurrentTaskId());
        if (template == null) return;
        
        // Kiểm tra đúng loại quái
        if (template.getMobId() == mobId) {
            int oldCount = playerTask.getCurrentKillCount();
            playerTask.addKill(1);
            int newCount = playerTask.getCurrentKillCount();
            int maxCount = template.getKillCount();
            
            // Tính % cũ và mới
            int oldPercent = (oldCount * 100) / maxCount;
            int newPercent = (newCount * 100) / maxCount;
            
            // Thông báo mỗi khi đạt mốc 20%, 40%, 60%, 80%
            if ((newPercent >= 20 && oldPercent < 20) ||
                (newPercent >= 40 && oldPercent < 40) ||
                (newPercent >= 60 && oldPercent < 60) ||
                (newPercent >= 80 && oldPercent < 80)) {
                Service.getInstance().sendThongBao(owner, 
                    "Nhiệm vụ đệ tử: " + newCount + "/" + maxCount + " (" + newPercent + "%)");
            }
            
            // Kiểm tra hoàn thành
            if (newCount >= maxCount) {
                completeTask(owner, template);
            } else {
                // Cập nhật tiến độ vào DB
                PlayerTaskDetyDAO.gI().updateKillCount(owner.id, newCount);
            }
        }
    }
    
    /**
     * Hoàn thành nhiệm vụ và phát thưởng
     */
    private void completeTask(Player player, TaskDetyTemplate template) {
        PlayerTaskDety playerTask = getPlayerTask(player);
        
        // Phát thưởng
        StringBuilder rewardText = new StringBuilder();
        for (TaskDetyTemplate.ItemReward reward : template.getItemRewards()) {
            nro.models.item.Item item = ItemService.gI().createNewItem((short) reward.getItemId(), reward.getQuantity());
            InventoryService.gI().addItemBag(player, item, 0);
            rewardText.append("\n+ ").append(item.template.name).append(" x").append(reward.getQuantity());
        }
        InventoryService.gI().sendItemBags(player);
        
        // Reset nhiệm vụ
        playerTask.completeTask();
        savePlayerTask(player);
        
        Service.getInstance().sendThongBao(player, "Chúc mừng! Bạn đã hoàn thành nhiệm vụ " + template.getModeName() + "!\nPhần thưởng:" + rewardText);
    }
}
