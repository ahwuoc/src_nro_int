package nro.ahwuocdz;

import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.services.Service;

/**
 * Zone riêng cho mỗi player trong Địa Cung
 * @author ahwuocdz
 */
public class DungeonZone extends Zone {
    
    private Player owner;
    private DungeonInstance dungeonInstance;
    private boolean isClosed = false;
    
    public DungeonZone(Map map, int zoneId, Player owner) {
        super(map, zoneId, 1); // maxPlayer = 1, chỉ owner được vào
        this.owner = owner;
    }
    
    public void setDungeonInstance(DungeonInstance instance) {
        this.dungeonInstance = instance;
    }
    
    public DungeonInstance getDungeonInstance() {
        return dungeonInstance;
    }
    
    public Player getOwner() {
        return owner;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Đóng zone và xóa tất cả mob
     */
    public void close() {
        if (isClosed) return;
        isClosed = true;
        
        // Xóa tất cả mob trong zone
        mobs.clear();
        
        // Xóa zone khỏi map
        if (map != null && map.zones != null) {
            map.zones.remove(this);
        }
    }
    
    /**
     * Spawn mob vào zone
     */
    public void spawnMob(Mob mob) {
        mob.zone = this;
        mob.id = mobs.size();
        mobs.add(mob);
    }
    
    /**
     * Xóa tất cả mob có tên chứa "Wave"
     */
    public void clearWaveMobs() {
        mobs.removeIf(mob -> mob.name != null && mob.name.contains("Wave"));
    }
    
    @Override
    public void addPlayer(Player player) {
        // Chỉ cho owner vào zone này
        if (player.id == owner.id || player.isPet) {
            super.addPlayer(player);
        } else {
            Service.getInstance().sendThongBao(player, "Đây là zone riêng của người chơi khác!");
        }
    }
}
