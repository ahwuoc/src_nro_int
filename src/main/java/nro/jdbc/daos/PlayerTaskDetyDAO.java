package nro.jdbc.daos;

import nro.jdbc.DBService;
import nro.models.task.PlayerTaskDety;
import nro.utils.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;

/**
 * DAO quản lý nhiệm vụ đệ tử của player
 * @author 💖 ahwuocdz 💖
 */
public class PlayerTaskDetyDAO {
    
    private static PlayerTaskDetyDAO instance;
    
    public static PlayerTaskDetyDAO gI() {
        if (instance == null) {
            instance = new PlayerTaskDetyDAO();
        }
        return instance;
    }
    
    /**
     * Lấy thông tin nhiệm vụ đệ tử của player
     */
    public PlayerTaskDety getByPlayerId(long playerId) {
        PlayerTaskDety task = new PlayerTaskDety(playerId);
        try {
            Connection conn = DBService.gI().getConnectionForGame();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT current_task_id, current_kill_count, daily_count, last_reset_date " +
                "FROM player_task_dety WHERE player_id = ?"
            );
            ps.setLong(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                task.setCurrentTaskId(rs.getInt("current_task_id"));
                task.setCurrentKillCount(rs.getInt("current_kill_count"));
                task.setDailyCount(rs.getInt("daily_count"));
                task.setLastResetDate(rs.getDate("last_reset_date"));
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
            Log.error(PlayerTaskDetyDAO.class, e);
        }
        return task;
    }
    
    /**
     * Lưu thông tin nhiệm vụ đệ tử của player
     */
    public void save(PlayerTaskDety task) {
        try {
            Connection conn = DBService.gI().getConnectionForGame();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO player_task_dety (player_id, current_task_id, current_kill_count, daily_count, last_reset_date) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "current_task_id = VALUES(current_task_id), " +
                "current_kill_count = VALUES(current_kill_count), " +
                "daily_count = VALUES(daily_count), " +
                "last_reset_date = VALUES(last_reset_date)"
            );
            ps.setLong(1, task.getPlayerId());
            ps.setInt(2, task.getCurrentTaskId());
            ps.setInt(3, task.getCurrentKillCount());
            ps.setInt(4, task.getDailyCount());
            ps.setDate(5, task.getLastResetDate());
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (Exception e) {
            Log.error(PlayerTaskDetyDAO.class, e);
        }
    }
    
    /**
     * Cập nhật tiến độ giết quái
     */
    public void updateKillCount(long playerId, int killCount) {
        try {
            Connection conn = DBService.gI().getConnectionForGame();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE player_task_dety SET current_kill_count = ? WHERE player_id = ?"
            );
            ps.setInt(1, killCount);
            ps.setLong(2, playerId);
            ps.executeUpdate();
            ps.close();
            conn.close();
        } catch (Exception e) {
            Log.error(PlayerTaskDetyDAO.class, e);
        }
    }
}
