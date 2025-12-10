package nro.services.mocnap;

import nro.ahwuocdz.mocnap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service xử lý mốc nạp
 * Parse dữ liệu từ mocnap class
 */
public class MocnapService {
    private static MocnapService instance;
    private List<MocnapMilestone> milestones = new ArrayList<>();
    private boolean loaded = false;

    public static MocnapService gI() {
        if (instance == null) {
            instance = new MocnapService();
        }
        return instance;
    }

    /**
     * Load tất cả mốc nạp từ database
     */
    public boolean loadFromDatabase(Connection conn) {
        try {
            milestones.clear();
            List<mocnap> mocnapList = mocnap.getAllMocNap(conn);
            
            for (mocnap m : mocnapList) {
                MocnapMilestone milestone = new MocnapMilestone();
                milestone.id = m.getId();
                milestone.require = m.getRequired();
                milestone.title = m.getDescriptor(); // descriptor là text mô tả
                milestone.description = m.getDescriptor();
                
                // Parse rewards (items thưởng)
                try {
                    String rewards = m.getRewards();
                    if (rewards != null && !rewards.isEmpty() && rewards.startsWith("[")) {
                        JSONArray rewardsArray = new JSONArray(rewards);
                        for (int i = 0; i < rewardsArray.length(); i++) {
                            JSONObject itemObj = rewardsArray.getJSONObject(i);
                            MocnapItem item = new MocnapItem();
                            if (itemObj.has("item_id")) {
                                item.itemId = itemObj.getInt("item_id");
                            } else if (itemObj.has("item__id")) {
                                item.itemId = itemObj.getInt("item__id");
                            }
                            item.quantity = itemObj.getInt("item_quantity");
                            
                            JSONArray optionsArray = itemObj.optJSONArray("item_options");
                            if (optionsArray != null) {
                                for (int j = 0; j < optionsArray.length(); j++) {
                                    JSONObject optObj = optionsArray.getJSONObject(j);
                                    MocnapOption opt = new MocnapOption();
                                    opt.id = optObj.getInt("item_option_id");
                                    opt.param = optObj.getInt("item_option_param");
                                    item.options.add(opt);
                                }
                            }
                            milestone.rewards.add(item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                milestones.add(milestone);
            }
            
            milestones.sort((a, b) -> Integer.compare(a.require, b.require));
            loaded = true;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<MocnapMilestone> getAllMilestones() {
        return new ArrayList<>(milestones);
    }

    public MocnapMilestone getMilestoneById(int id) {
        for (MocnapMilestone m : milestones) {
            if (m.id == id) {
                return m;
            }
        }
        return null;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public static class MocnapMilestone {
        public int id;
        public int require;
        public String title;
        public String description;
        public List<MocnapItem> rewards = new ArrayList<>();
    }

    public static class MocnapItem {
        public int itemId;
        public int quantity;
        public List<MocnapOption> options = new ArrayList<>();
    }

    public static class MocnapOption {
        public int id;
        public int param;
    }
}
