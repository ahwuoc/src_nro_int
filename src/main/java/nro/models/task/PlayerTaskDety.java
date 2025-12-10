package nro.models.task;

import java.sql.Date;

/**
 * Lưu thông tin nhiệm vụ đệ tử của player
 * @author 💖 ahwuocdz 💖
 */
public class PlayerTaskDety {
    
    private long playerId;
    private int currentTaskId;      // ID nhiệm vụ đang làm (0 = chưa có)
    private int currentKillCount;   // Số quái đã giết
    private int dailyCount;         // Số lượt đã nhận hôm nay
    private Date lastResetDate;     // Ngày reset lượt cuối cùng
    
    public static final int MAX_DAILY_COUNT = 10; // Giới hạn 10 lượt/ngày
    
    public PlayerTaskDety(long playerId) {
        this.playerId = playerId;
        this.currentTaskId = 0;
        this.currentKillCount = 0;
        this.dailyCount = 0;
        this.lastResetDate = new Date(System.currentTimeMillis());
    }
    
    /**
     * Kiểm tra và reset lượt nếu sang ngày mới
     */
    public void checkAndResetDaily() {
        Date today = new Date(System.currentTimeMillis());
        if (lastResetDate == null || !isSameDay(lastResetDate, today)) {
            dailyCount = 0;
            lastResetDate = today;
        }
    }
    
    private boolean isSameDay(Date d1, Date d2) {
        return d1.toString().equals(d2.toString());
    }
    
    /**
     * Kiểm tra còn lượt nhận nhiệm vụ không
     */
    public boolean canAcceptTask() {
        checkAndResetDaily();
        return dailyCount < MAX_DAILY_COUNT;
    }
    
    /**
     * Lấy số lượt còn lại
     */
    public int getRemainingCount() {
        checkAndResetDaily();
        return MAX_DAILY_COUNT - dailyCount;
    }
    
    /**
     * Kiểm tra đang có nhiệm vụ chưa hoàn thành không
     */
    public boolean hasActiveTask() {
        return currentTaskId > 0;
    }
    
    /**
     * Nhận nhiệm vụ mới
     */
    public void acceptTask(int taskId) {
        this.currentTaskId = taskId;
        this.currentKillCount = 0;
        this.dailyCount++;
    }
    
    /**
     * Tăng số quái đã giết
     */
    public void addKill(int count) {
        this.currentKillCount += count;
    }
    
    /**
     * Hoàn thành nhiệm vụ
     */
    public void completeTask() {
        this.currentTaskId = 0;
        this.currentKillCount = 0;
    }
    
    /**
     * Hủy nhiệm vụ hiện tại
     */
    public void cancelTask() {
        this.currentTaskId = 0;
        this.currentKillCount = 0;
    }
    
    // Getters & Setters
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    
    public int getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(int currentTaskId) { this.currentTaskId = currentTaskId; }
    
    public int getCurrentKillCount() { return currentKillCount; }
    public void setCurrentKillCount(int currentKillCount) { this.currentKillCount = currentKillCount; }
    
    public int getDailyCount() { return dailyCount; }
    public void setDailyCount(int dailyCount) { this.dailyCount = dailyCount; }
    
    public Date getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(Date lastResetDate) { this.lastResetDate = lastResetDate; }
}
