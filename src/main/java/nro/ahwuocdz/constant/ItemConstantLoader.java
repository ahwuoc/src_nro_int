package nro.ahwuocdz.constant;

import nro.utils.StringUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Load item từ database và sinh ra constant
 */
public class ItemConstantLoader {

    private static Connection getDirectConnection() throws Exception {
        // Load database config from properties
        java.util.Properties props = new java.util.Properties();
        props.load(new java.io.FileInputStream("resources/config/server.properties"));

        String ip = props.getProperty("server.db.ip");
        String port = props.getProperty("server.db.port");
        String dbName = props.getProperty("server.db.name");
        String user = props.getProperty("server.db.us");
        String password = props.getProperty("server.db.pw");

        String url = "jdbc:mysql://" + ip + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC";

        return DriverManager.getConnection(url, user, password);
    }

    public static void load() {
        try (Connection con = getDirectConnection();
                PreparedStatement ps = con.prepareStatement("SELECT id, name FROM item_template WHERE name != ''")) {

            ResultSet rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                if (name != null && !name.trim().isEmpty()) {
                    ItemConstant.register(id, name);
                    count++;
                }
            }

            System.out.println("Load ItemConstant thành công (" + count + " items)");
        } catch (Exception ex) {
            System.err.println("Lỗi load ItemConstant: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void generateConstantFile() {
        try (Connection con = getDirectConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT id, name FROM item_template WHERE name != '' ORDER BY id")) {

            StringBuilder sb = new StringBuilder();
            sb.append("package nro.consts;\n\n");
            sb.append("/**\n");
            sb.append(" * Auto-generated item constants from database\n");
            sb.append(" * Format: ITEM_[ID]_[NAME]\n");
            sb.append(" * Generated at: ").append(new java.util.Date()).append("\n");
            sb.append(" */\n");
            sb.append("public class ItemId {\n\n");

            ResultSet rs = ps.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                if (name != null && !name.trim().isEmpty()) {
                    String constantName = "ITEM_" + id + "_" + generateConstantName(name);
                    sb.append("    public static final int ").append(constantName)
                            .append(" = ").append(id).append("; // ").append(name).append("\n");
                    count++;
                }
            }

            sb.append("\n}\n");

            // Ghi vào file
            java.nio.file.Files.write(
                    java.nio.file.Paths.get("src/main/java/nro/consts/ItemId.java"),
                    sb.toString().getBytes());

            System.out.println("Sinh ra ItemId.java thành công (" + count + " constants)");
        } catch (Exception ex) {
            System.err.println("Lỗi sinh ItemId.java: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static String generateConstantName(String name) {
        return StringUtil.slugify(name);
    }

    /**
     * Main method để chạy generate
     * Chạy: mvn exec:java
     * -Dexec.mainClass="nro.ahwuocdz.constant.ItemConstantLoader"
     */
    public static void main(String[] args) {
        System.out.println("Bắt đầu load ItemConstant từ database...");
        load();

        System.out.println("Bắt đầu sinh ra ItemId.java...");
        generateConstantFile();

        System.out.println("Hoàn thành!");
        System.exit(0);
    }
}
