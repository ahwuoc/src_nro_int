package nro.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author 💖 ahwuocdz 💖
 * 
 *
 */
public class DBService {

    public static String DRIVER = "com.mysql.cj.jdbc.Driver";
    public static String URL = "jdbc:#0://#1:#2/#3";
    public static String DB_HOST = "localhost";
    public static int DB_PORT = 3306;
    public static String DB_NAME = "";
    public static String DB_USER = "root";
    public static String DB_PASSWORD = "123456";
    public static int MAX_CONN = 2;

    private static DBService i;
    public static String dbName;

    public static DBService gI() {
        if (i == null) {
            i = new DBService();
        }
        return i;
    }

    private DBService() {
    }

    /**
     * Get connection from HikariCP pool.
     * IMPORTANT: Always close connection after use with try-with-resources or finally block!
     */
    public Connection getConnectionForLogin() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForLogout() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForSaveData() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForGame() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForClan() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForAutoSave() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForSaveHistory() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionForGetPlayer() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnectionCreatPlayer() throws SQLException {
        return DBHika.getConnection();
    }

    public Connection getConnection() throws SQLException {
        return DBHika.getConnection();
    }

    public void release(Connection con) {
        if (con != null) {
            try {
                con.close(); // Returns connection to HikariCP pool
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public int currentActive() {
        return -1;
    }

    public int currentIdle() {
        return -1;
    }

}
