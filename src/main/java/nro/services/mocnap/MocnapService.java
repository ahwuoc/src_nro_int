package nro.services.mocnap;

import nro.login.LoginSession;
import nro.server.ServerManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 * @author 💖 YTB ahwuocdz 💖
 */
public class MocnapService {

    // private static MocnapService instance;
    private static MocnapService instance;

    // Cache data từ Login Server
    private List<MocnapMilestone> milestones = new ArrayList<>();
    private boolean loaded = false;

    public static MocnapService gI() {
        if (instance == null) {
            instance = new MocnapService();
        }
        return instance;
    }

    public boolean loadFromLoginServer(int timeoutMs) {
        try {
            LoginSession loginSession = ServerManager.gI().getLogin();
            if (loginSession == null || !loginSession.isConnected()) {
                System.err.println("[MocnapService] Login server not connected");
                return false;
            }

            byte[] data = loginSession.getService().loadMocnapRewards(timeoutMs);
            if (data == null) {
                System.err.println("[MocnapService] Failed to load mocnap rewards");
                return false;
            }

            // Parse binary data
            parseMocnapData(data);
            loaded = true;
            System.out.println("[MocnapService] Loaded " + milestones.size() + " milestones");
            return true;

        } catch (Exception e) {
            System.err.println("[MocnapService] Error loading mocnap rewards");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parse binary data từ Login Server
     * Format: [num_milestones:1][milestone_data...]
     */
    private void parseMocnapData(byte[] data) throws IOException {
        milestones.clear();
        DataInputStream dis = new DataInputStream(new java.io.ByteArrayInputStream(data));

        // Read number of milestones
        int numMilestones = dis.readUnsignedByte();

        for (int i = 0; i < numMilestones; i++) {
            MocnapMilestone milestone = new MocnapMilestone();

            // Read milestone ID (4 bytes)
            milestone.id = dis.readInt();

            // Read require amount (4 bytes)
            milestone.require = dis.readInt();

            // Read title (UTF-8 string with length prefix)
            int titleLen = dis.readUnsignedShort();
            byte[] titleBytes = new byte[titleLen];
            dis.readFully(titleBytes);
            milestone.title = new String(titleBytes, "UTF-8");

            // Read number of items
            int numItems = dis.readUnsignedByte();
            milestone.items = new ArrayList<>();

            for (int j = 0; j < numItems; j++) {
                MocnapItem item = new MocnapItem();

                // Read item ID (4 bytes)
                item.itemId = dis.readInt();

                // Read quantity (4 bytes)
                item.quantity = dis.readInt();

                // Read number of options
                int numOptions = dis.readUnsignedByte();
                item.options = new ArrayList<>();

                for (int k = 0; k < numOptions; k++) {
                    MocnapOption option = new MocnapOption();

                    // Read option ID (4 bytes)
                    option.id = dis.readInt();

                    // Read param (4 bytes)
                    option.param = dis.readInt();

                    item.options.add(option);
                }

                milestone.items.add(item);
            }

            milestones.add(milestone);
        }

        dis.close();
    }

    /**
     * Lấy tất cả mốc nạp
     */
    public List<MocnapMilestone> getAllMilestones() {
        return milestones;
    }

    /**
     * Lấy mốc nạp theo ID
     */
    public MocnapMilestone getMilestoneById(int id) {
        for (MocnapMilestone m : milestones) {
            if (m.id == id) {
                return m;
            }
        }
        return null;
    }

    /**
     * Kiểm tra đã load config chưa
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Reload config từ Login Server
     */
    public void reload() {
        loaded = false;
        loadFromLoginServer(5000);
    }

    // ==================== DATA CLASSES ====================

    public static class MocnapMilestone {
        public int id;
        public int require;
        public String title;
        public List<MocnapItem> items;
    }

    public static class MocnapItem {
        public int itemId;
        public int quantity;
        public List<MocnapOption> options;
    }

    public static class MocnapOption {
        public int id;
        public int param;
    }
}
