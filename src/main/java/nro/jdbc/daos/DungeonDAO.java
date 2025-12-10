package nro.jdbc.daos;

import nro.jdbc.DBService;
import nro.utils.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DAO để lưu số lượt tham gia Địa Cung của player
 * @author ahwuocdz
 */
public class DungeonDAO {

    private static final String TABLE_NAME = "dungeon_participation";

    /**
     * Tạo bảng nếu chưa tồn tại
     */
    public static void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "player_id BIGINT PRIMARY KEY, " +
                "participation_count INT DEFAULT 0, " +
                "remaining_attempts INT DEFAULT 3, " +
                "last_reset_date DATE, " +
                "highest_wave INT DEFAULT 0" +
                ")";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi tạo bảng dungeon_participation");
        }
    }

    /**
     * Lấy số lượt còn lại của player
     */
    public static int getRemainingAttempts(long playerId) {
        String sql = "SELECT remaining_attempts, last_reset_date FROM " + TABLE_NAME + " WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Date lastReset = rs.getDate("last_reset_date");
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                
                // Nếu ngày reset khác ngày hôm nay thì reset lại
                if (lastReset == null || !isSameDay(lastReset, today)) {
                    resetPlayerAttempts(playerId);
                    return 3; // MAX_PARTICIPATION_PER_DAY
                }
                return rs.getInt("remaining_attempts");
            } else {
                // Player chưa có record, tạo mới
                insertNewPlayer(playerId);
                return 3;
            }
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi lấy remaining_attempts");
            return 3;
        }
    }

    /**
     * Lấy số lần đã tham gia của player
     */
    public static int getParticipationCount(long playerId) {
        String sql = "SELECT participation_count, last_reset_date FROM " + TABLE_NAME + " WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Date lastReset = rs.getDate("last_reset_date");
                java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
                
                if (lastReset == null || !isSameDay(lastReset, today)) {
                    resetPlayerAttempts(playerId);
                    return 0;
                }
                return rs.getInt("participation_count");
            }
            return 0;
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi lấy participation_count");
            return 0;
        }
    }

    /**
     * Cập nhật số lượt khi player tham gia dungeon
     */
    public static void decrementAttempts(long playerId) {
        String sql = "UPDATE " + TABLE_NAME + " SET remaining_attempts = remaining_attempts - 1, " +
                "participation_count = participation_count + 1 WHERE player_id = ? AND remaining_attempts > 0";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Player chưa có record hoặc hết lượt
                insertNewPlayer(playerId);
                decrementAttempts(playerId);
            }
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi decrementAttempts");
        }
    }

    /**
     * Phạt player khi thất bại (trừ thêm lượt)
     */
    public static void penalizePlayer(long playerId, int penalty) {
        String sql = "UPDATE " + TABLE_NAME + " SET remaining_attempts = GREATEST(0, remaining_attempts - ?) WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, penalty);
            ps.setLong(2, playerId);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi penalizePlayer");
        }
    }

    /**
     * Cập nhật wave cao nhất
     */
    public static void updateHighestWave(long playerId, int wave) {
        String sql = "UPDATE " + TABLE_NAME + " SET highest_wave = GREATEST(highest_wave, ?) WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, wave);
            ps.setLong(2, playerId);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi updateHighestWave");
        }
    }

    /**
     * Lấy wave cao nhất của player
     */
    public static int getHighestWave(long playerId) {
        String sql = "SELECT highest_wave FROM " + TABLE_NAME + " WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("highest_wave");
            }
            return 0;
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi getHighestWave");
            return 0;
        }
    }

    /**
     * Tạo record mới cho player
     */
    private static void insertNewPlayer(long playerId) {
        String sql = "INSERT INTO " + TABLE_NAME + " (player_id, participation_count, remaining_attempts, last_reset_date, highest_wave) " +
                "VALUES (?, 0, 3, CURDATE(), 0) ON DUPLICATE KEY UPDATE player_id = player_id";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi insertNewPlayer");
        }
    }

    /**
     * Reset lượt cho player (gọi khi sang ngày mới)
     */
    private static void resetPlayerAttempts(long playerId) {
        String sql = "UPDATE " + TABLE_NAME + " SET participation_count = 0, remaining_attempts = 3, last_reset_date = CURDATE() WHERE player_id = ?";
        try (Connection con = DBService.gI().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                insertNewPlayer(playerId);
            }
        } catch (Exception e) {
            Log.error(DungeonDAO.class, e, "Lỗi resetPlayerAttempts");
        }
    }

    /**
     * Kiểm tra 2 ngày có cùng ngày không
     */
    private static boolean isSameDay(java.sql.Date date1, java.sql.Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
