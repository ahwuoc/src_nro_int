package nro.models.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Template nhiệm vụ đệ tử
 * @author 💖 ahwuocdz 💖
 */
public class TaskDetyTemplate {
    
    public static final int MODE_EASY = 0;
    public static final int MODE_NORMAL = 1;
    public static final int MODE_HARD = 2;
    
    private int id;
    private int mode; // 0: easy, 1: normal, 2: hard
    private int mobId; // ID quái cần đánh
    private int mapId; // ID map
    private int killCount; // Số lượng quái cần giết
    private List<ItemReward> itemRewards; // Danh sách item thưởng
    
    public TaskDetyTemplate(int id, int mode, int mobId, int mapId, int killCount) {
        this.id = id;
        this.mode = mode;
        this.mobId = mobId;
        this.mapId = mapId;
        this.killCount = killCount;
        this.itemRewards = new ArrayList<>();
    }
    
    // Getters & Setters
    public int getId() {
        return id;
    }
    
    public int getMode() {
        return mode;
    }
    
    public String getModeName() {
        switch (mode) {
            case MODE_EASY:
                return "Dễ";
            case MODE_NORMAL:
                return "Khó";
            case MODE_HARD:
                return "Siêu Khó";
            default:
                return "Unknown";
        }
    }
    
    public int getMobId() {
        return mobId;
    }
    
    public int getMapId() {
        return mapId;
    }
    
    public int getKillCount() {
        return killCount;
    }
    
    /**
     * Lấy tên map từ mapId
     */
    public String getMapName() {
        for (nro.models.map.Map map : nro.server.Manager.MAPS) {
            if (map.mapId == this.mapId) {
                return map.mapName;
            }
        }
        return "Map " + mapId;
    }
    
    /**
     * Lấy tên mob từ mobId
     */
    public String getMobName() {
        for (nro.models.mob.MobTemplate mob : nro.server.Manager.MOB_TEMPLATES) {
            if (mob.id == this.mobId) {
                return mob.name;
            }
        }
        return "Quái " + mobId;
    }
    
    public List<ItemReward> getItemRewards() {
        return itemRewards;
    }
    
    public void addItemReward(int itemId, int quantity) {
        this.itemRewards.add(new ItemReward(itemId, quantity));
    }
    
    /**
     * Class lưu thông tin item thưởng
     */
    public static class ItemReward {
        private int itemId;
        private int quantity;
        
        public ItemReward(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
        
        public int getItemId() {
            return itemId;
        }
        
        public int getQuantity() {
            return quantity;
        }
    }
}
