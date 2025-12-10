package nro.ahwuocdz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class mocnap {
    private int id;
    private int required;
    private String descriptor;
    private String rewards;
    private String createdAt;

    public mocnap(int id, int required, String descriptor, String rewards, String createdAt) {
        this.id = id;
        this.required = required;
        this.descriptor = descriptor;
        this.rewards = rewards;
        this.createdAt = createdAt;
    }

    public mocnap() {
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getRequired() {
        return required;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getRewards() {
        return rewards;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Query methods
    public static List<mocnap> getAllMocNap(Connection conn) throws SQLException {
        List<mocnap> list = new ArrayList<>();
        String sql = "SELECT id, required, descriptor, rewards, created_at FROM moc_nap";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                mocnap item = new mocnap(
                    rs.getInt("id"),
                    rs.getInt("required"),
                    rs.getString("descriptor"),
                    rs.getString("rewards"),
                    rs.getString("created_at")
                );
                list.add(item);
            }
        }
        return list;
    }

    public static mocnap getMocNapById(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, required, descriptor, rewards, created_at FROM moc_nap WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new mocnap(
                        rs.getInt("id"),
                        rs.getInt("required"),
                        rs.getString("descriptor"),
                        rs.getString("rewards"),
                        rs.getString("created_at")
                    );
                }
            }
        }
        return null;
    }

    public static List<mocnap> getMocNapByRequired(Connection conn, int required) throws SQLException {
        List<mocnap> list = new ArrayList<>();
        String sql = "SELECT id, required, descriptor, rewards, created_at FROM moc_nap WHERE required = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, required);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mocnap item = new mocnap(
                        rs.getInt("id"),
                        rs.getInt("required"),
                        rs.getString("descriptor"),
                        rs.getString("rewards"),
                        rs.getString("created_at")
                    );
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "mocnap{" +
                "id=" + id +
                ", required=" + required +
                ", descriptor='" + descriptor + '\'' +
                ", rewards='" + rewards + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}