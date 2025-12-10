package nro.manager;

import nro.models.task.TaskDetyTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý danh sách nhiệm vụ đệ tử
 * @author 💖 ahwuocdz 💖
 */
public class TaskDetyManager {
    
    private static TaskDetyManager instance;
    private List<TaskDetyTemplate> tasks;
    
    private TaskDetyManager() {
        this.tasks = new ArrayList<>();
        initTasks();
    }
    
    public static TaskDetyManager gI() {
        if (instance == null) {
            instance = new TaskDetyManager();
        }
        return instance;
    }
    
    /**
     * Khởi tạo danh sách nhiệm vụ
     */
    private void initTasks() {
        // ===== NHIỆM VỤ DỄ (MODE 0) =====
        TaskDetyTemplate task1 = new TaskDetyTemplate(1, TaskDetyTemplate.MODE_EASY, 1, 1, 10);
        task1.addItemReward(1519, 1);
        tasks.add(task1);
        // ===== NHIỆM VỤ KHÓ (MODE 1) =====
        TaskDetyTemplate task4 = new TaskDetyTemplate(3, TaskDetyTemplate.MODE_NORMAL, 13, 5, 50);
        task4.addItemReward(1519, 5);
        tasks.add(task4);
        // ===== NHIỆM VỤ SIÊU KHÓ (MODE 2) =====
        TaskDetyTemplate task7 = new TaskDetyTemplate(7, TaskDetyTemplate.MODE_HARD, 18, 6, 200);
        task7.addItemReward(1519, 10);
        tasks.add(task7);
    }
    public List<TaskDetyTemplate> getAllTasks() {
        return new ArrayList<>(tasks);
    }
    public List<TaskDetyTemplate> getTasksByMode(int mode) {
        return tasks.stream()
                .filter(t -> t.getMode() == mode)
                .collect(Collectors.toList());
    }
    public TaskDetyTemplate getTaskById(int id) {
        return tasks.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    public List<TaskDetyTemplate> getEasyTasks() {
        return getTasksByMode(TaskDetyTemplate.MODE_EASY);
    }
    

    public List<TaskDetyTemplate> getNormalTasks() {
        return getTasksByMode(TaskDetyTemplate.MODE_NORMAL);
    }
    

    public List<TaskDetyTemplate> getHardTasks() {
        return getTasksByMode(TaskDetyTemplate.MODE_HARD);
    }
}
