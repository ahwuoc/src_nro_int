package nro.ahwuocdz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import nro.jdbc.daos.DungeonDAO;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.services.func.ChangeMapService;
import nro.services.MapService;
import nro.services.Service;
import nro.utils.TimeUtil;

public class DungeonManage {

    public static final int MAP_ID = 146;
    public static final int KICK_MAP_ID = 2;
    public static final int KICK_ZONE_ID = -1;
    public static final int KICK_X = 164;

    public static class TimeRange {
        public final int openHour, openMin, openSec;
        public final int closeHour, closeMin, closeSec;

        public TimeRange(int openHour, int openMin, int openSec,
                int closeHour, int closeMin, int closeSec) {
            this.openHour = openHour;
            this.openMin = openMin;
            this.openSec = openSec;
            this.closeHour = closeHour;
            this.closeMin = closeMin;
            this.closeSec = closeSec;
        }
    }

    public static final TimeRange[] TIME_RANGES = {
            new TimeRange(7, 0, 0, 23, 0, 0), // 12:00 - 13:00
    };

    public static final int AVAILABLE = 7;
    public static final int MAX_PARTICIPATION_PER_DAY = 3;
    public static final int PENALTY_FOR_FAILURE = 1;

    private static DungeonManage instance;
    public static long[] TIME_OPEN_ARRAY;
    public static long[] TIME_CLOSE_ARRAY;

    private int day = -1;
    private long lastResetTime = 0;

    private Map<String, DungeonInstance> activeInstances;
    private Map<Long, String> playerToInstance;
    private Map<Long, DungeonZone> playerPrivateZones; // Zone riêng cho mỗi player
    private AtomicInteger zoneIdCounter = new AtomicInteger(1000); // ID zone bắt đầu từ 1000

    private DungeonManage() {
        this.activeInstances = new ConcurrentHashMap<>();
        this.playerToInstance = new ConcurrentHashMap<>();
        this.playerPrivateZones = new ConcurrentHashMap<>();
        // Tạo bảng database nếu chưa có
        DungeonDAO.createTableIfNotExists();
    }

    public static DungeonManage gI() {
        if (instance == null) {
            instance = new DungeonManage();
        }
        instance.setTime();
        return instance;
    }

    public DungeonInstance createDungeonInstance(Player player) {
        return createDungeonInstance(player, false);
    }

    public DungeonInstance createDungeonInstance(Player player, boolean skipDecrementAttempts) {
        System.out.println("[Dungeon] createDungeonInstance for: " + player.name + ", skipDecrement: " + skipDecrementAttempts);
        
        if (player.zone == null) {
            System.out.println("[Dungeon] FAILED: player.zone is null");
            return null;
        }
        
        if (player.zone.map.mapId != MAP_ID) {
            System.out.println("[Dungeon] FAILED: player not in dungeon map. Current map: " + player.zone.map.mapId);
            return null;
        }

        // Nếu không dùng item thì kiểm tra lượt free
        if (!skipDecrementAttempts && !canPlayerJoinDungeon(player)) {
            System.out.println("[Dungeon] FAILED: canPlayerJoinDungeon = false");
            Service.getInstance().sendThongBao(player, "Bạn đã hết lượt tham gia Địa Cung hôm nay!");
            return null;
        }

        String existingInstanceId = playerToInstance.get(player.id);
        if (existingInstanceId != null) {
            DungeonInstance existingInstance = activeInstances.get(existingInstanceId);
            if (existingInstance != null && existingInstance.isActive()) {
                System.out.println("[Dungeon] Returning existing instance");
                return existingInstance;
            } else {
                cleanupInstance(existingInstanceId);
            }
        }

        String instanceId = UUID.randomUUID().toString();

        // Tạo zone riêng cho player
        DungeonZone privateZone = createPrivateZone(player);
        if (privateZone == null) {
            System.out.println("[Dungeon] FAILED: createPrivateZone returned null");
            Service.getInstance().sendThongBao(player, "Không thể tạo phòng Địa Cung!");
            return null;
        }
        
        System.out.println("[Dungeon] Created private zone: " + privateZone.zoneId);

        DungeonInstance dungeonInstance = new DungeonInstance(instanceId, privateZone, player);
        privateZone.setDungeonInstance(dungeonInstance);

        activeInstances.put(instanceId, dungeonInstance);
        playerToInstance.put(player.id, instanceId);
        playerPrivateZones.put(player.id, privateZone);

        try {
            // Chỉ trừ lượt nếu không dùng item
            if (!skipDecrementAttempts) {
                incrementPlayerParticipation(player);
            } else {
                Service.getInstance().sendThongBao(player, "Vào Địa Cung bằng vé!");
            }
            movePlayerToZone(player, privateZone);
            dungeonInstance.startWave();
            return dungeonInstance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tạo zone riêng cho player
     */
    private DungeonZone createPrivateZone(Player player) {
        try {
            nro.models.map.Map dungeonMap = MapService.gI().getMapById(MAP_ID);
            if (dungeonMap == null)
                return null;

            int newZoneId = zoneIdCounter.incrementAndGet();
            DungeonZone privateZone = new DungeonZone(dungeonMap, newZoneId, player);
            dungeonMap.zones.add(privateZone);

            return privateZone;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void movePlayerToZone(Player player, Zone zone) {
        try {
            if (player.zone != null && player.zone.zoneId != zone.zoneId) {
                player.zone.removePlayer(player);
            }
            zone.addPlayer(player);
            player.zone = zone;
            zone.mapInfo(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getActiveInstanceCount() {
        return activeInstances.size();
    }

    public DungeonInstance getPlayerInstance(Player player) {
        String instanceId = playerToInstance.get(player.id);
        if (instanceId != null) {
            return activeInstances.get(instanceId);
        }
        return null;
    }

    public void updateInstances() {
        List<String> instancesToRemove = new ArrayList<>();
        for (Map.Entry<String, DungeonInstance> entry : activeInstances.entrySet()) {
            DungeonInstance dungeonInstance = entry.getValue();
            if (dungeonInstance.isActive()) {
                dungeonInstance.update();
            } else {
                instancesToRemove.add(entry.getKey());
            }
        }
        for (String instanceId : instancesToRemove) {
            cleanupInstance(instanceId);
        }
    }

    private void cleanupInstance(String instanceId) {
        DungeonInstance dungeonInstance = activeInstances.remove(instanceId);
        if (dungeonInstance != null) {
            long playerId = dungeonInstance.getOwner().id;
            playerToInstance.remove(playerId);

            // Đóng và xóa zone riêng
            DungeonZone privateZone = playerPrivateZones.remove(playerId);
            if (privateZone != null) {
                privateZone.close();
            }
        }
    }

    public void cleanupPlayerInstance(Player player) {
        String instanceId = playerToInstance.get(player.id);
        if (instanceId != null) {
            cleanupInstance(instanceId);
        }
    }

    public void removePlayerCompletely(Player player) {
        String instanceId = playerToInstance.get(player.id);
        if (instanceId != null) {
            cleanupInstance(instanceId);
        }
    }

    public void onPlayerLeaveDungeon(Player player) {
        try {
            if (player == null)
                return;

            String instanceId = playerToInstance.get(player.id);
            if (instanceId != null) {
                DungeonInstance dungeonInstance = activeInstances.get(instanceId);
                if (dungeonInstance != null && dungeonInstance.isActive()) {
                    if (dungeonInstance.isDungeonStarted()) {
                        Service.getInstance().sendThongBao(player, "Bạn đã rời khỏi Địa Cung!");
                        penalizePlayerForFailure(player);
                    }
                }
            }
            removePlayerCompletely(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DungeonZone getPlayerZone(Player player) {
        return playerPrivateZones.get(player.id);
    }

    public void setTime() {
        if (day == -1 || day != TimeUtil.getCurrDay()) {
            resetDailyDungeon();
            day = TimeUtil.getCurrDay();
            try {
                TIME_OPEN_ARRAY = new long[TIME_RANGES.length];
                TIME_CLOSE_ARRAY = new long[TIME_RANGES.length];
                for (int i = 0; i < TIME_RANGES.length; i++) {
                    TimeRange range = TIME_RANGES[i];
                    TIME_OPEN_ARRAY[i] = TimeUtil.getTime(TimeUtil.getTimeNow("dd/MM/yyyy") + " "
                            + range.openHour + ":" + range.openMin + ":" + range.openSec, "dd/MM/yyyy HH:mm:ss");
                    TIME_CLOSE_ARRAY[i] = TimeUtil.getTime(TimeUtil.getTimeNow("dd/MM/yyyy") + " "
                            + range.closeHour + ":" + range.closeMin + ":" + range.closeSec, "dd/MM/yyyy HH:mm:ss");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update(Player player) {
        if (player.zone == null || player.zone.map.mapId != MAP_ID) {
            return;
        }
        try {
            if (player.isAdmin())
                return;

            long now = System.currentTimeMillis();
            boolean isInTimeSlot = false;
            for (int i = 0; i < TIME_RANGES.length; i++) {
                if (now >= TIME_OPEN_ARRAY[i] && now <= TIME_CLOSE_ARRAY[i]) {
                    isInTimeSlot = true;
                    break;
                }
            }
            if (!isInTimeSlot) {
                cleanupPlayerInstance(player);
                kickOutOfMap(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void globalUpdate() {
        updateInstances();
        checkAndResetDaily();
    }

    private void kickOutOfMap(Player player) {
        Service.getInstance().sendThongBao(player, "Không Trong Thời Gian Diễn Ra Phó Bản!");
        ChangeMapService.gI().changeMapBySpaceShip(player, KICK_MAP_ID, KICK_ZONE_ID, KICK_X);
    }

    public void changeMap(Player player, byte index) {
        try {
            if (player.isAdmin()) {
                ChangeMapService.gI().changeMap(player, MAP_ID, -1, 50, 50);
                return;
            }

            long now = System.currentTimeMillis();
            boolean isInTimeSlot = false;
            for (int i = 0; i < TIME_RANGES.length; i++) {
                if (now >= TIME_OPEN_ARRAY[i] && now <= TIME_CLOSE_ARRAY[i]) {
                    isInTimeSlot = true;
                    break;
                }
            }

            if (isInTimeSlot) {
                ChangeMapService.gI().changeMap(player, MAP_ID, -1, 50, 50);
            } else {
                Service.getInstance().sendThongBao(player, "Phó Bản Địa Cung Chưa Mở!");
                Service.getInstance().hideWaitDialog(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void joinMapDiacung(Player player) {
        joinMapDiacung(player, false);
    }

    public void joinMapDiacung(Player player, boolean usedItem) {
        if (!player.isAdmin()) {
            long now = System.currentTimeMillis();
            boolean isInTimeSlot = false;
            for (int i = 0; i < TIME_RANGES.length; i++) {
                if (now >= TIME_OPEN_ARRAY[i] && now <= TIME_CLOSE_ARRAY[i]) {
                    isInTimeSlot = true;
                    break;
                }
            }
            if (!isInTimeSlot) {
                Service.getInstance().sendThongBao(player, "Phó Bản Địa Cung Chưa Mở!");
                Service.getInstance().hideWaitDialog(player);
                return;
            }
        }

        // Kiểm tra player đã có zone riêng chưa
        DungeonZone existingZone = playerPrivateZones.get(player.id);
        if (existingZone != null && !existingZone.isClosed()) {
            ChangeMapService.gI().changeMap(player, MAP_ID, existingZone.zoneId, 50, 50);
        } else {
            // Vào map trước, sau đó tạo zone riêng
            ChangeMapService.gI().changeMapNonSpaceship(player, MAP_ID, -1, 50);
        }

        final boolean skipDecrementAttempts = usedItem;
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (player.zone != null && player.zone.map.mapId == MAP_ID) {
                    DungeonInstance dungeonInstance = createDungeonInstance(player, skipDecrementAttempts);
                    if (dungeonInstance != null) {
                        Service.getInstance().sendThongBao(player, "Chào mừng đến với Địa Cung!");
                    } else {
                        Service.getInstance().sendThongBao(player, "Địa Cung đang quá tải!");
                        ChangeMapService.gI().changeMapBySpaceShip(player, KICK_MAP_ID, KICK_ZONE_ID, KICK_X);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public int getActiveZoneCount() {
        return playerPrivateZones.size();
    }

    public void fixPlayerZoneAssignment(Player player) {
        try {
            DungeonZone savedZone = playerPrivateZones.get(player.id);
            if (savedZone != null && !savedZone.isClosed() && player.zone != null && player.zone.map.mapId == MAP_ID) {
                if (player.zone.zoneId != savedZone.zoneId) {
                    movePlayerToZone(player, savedZone);
                    Service.getInstance().sendThongBao(player, "Đã sửa lỗi zone!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerInDungeon(Player player) {
        try {
            if (player == null || player.zone == null)
                return false;
            if (player.zone.map.mapId != MAP_ID)
                return false;

            String instanceId = playerToInstance.get(player.id);
            if (instanceId == null)
                return false;

            DungeonInstance dungeonInstance = activeInstances.get(instanceId);
            return dungeonInstance != null && dungeonInstance.isActive();
        } catch (Exception e) {
            return false;
        }
    }

    public void preventZoneChange(Player player) {
        if (isPlayerInDungeon(player)) {
            Service.getInstance().sendThongBaoOK(player, "Không thể đổi khu vực khi đang trong Địa Cung!");
        }
    }

    public String getCurrentTimeSlotInfo() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < TIME_RANGES.length; i++) {
            if (now >= TIME_OPEN_ARRAY[i] && now <= TIME_CLOSE_ARRAY[i]) {
                TimeRange range = TIME_RANGES[i];
                return String.format("Khung giờ %d: %02d:%02d - %02d:%02d",
                        i + 1, range.openHour, range.openMin, range.closeHour, range.closeMin);
            }
        }
        return "Địa Cung đang đóng cửa";
    }

    /**
     * Lấy danh sách khung giờ dạng text để hiển thị
     */
    public static String getTimeRangesText() {
        StringBuilder sb = new StringBuilder();
        for (TimeRange range : TIME_RANGES) {
            sb.append(String.format("|1|%02d:%02d - %02d:%02d\n", 
                range.openHour, range.openMin, range.closeHour, range.closeMin));
        }
        return sb.toString();
    }

    public String getPlayerParticipationInfo(Player player) {
        try {
            if (player == null)
                return "Không có thông tin";
            int remainingAttempts = getPlayerRemainingAttempts(player.id);
            return String.format("Số lượt còn lại: %d/%d", remainingAttempts, MAX_PARTICIPATION_PER_DAY);
        } catch (Exception e) {
            return "Lỗi khi lấy thông tin";
        }
    }

    public void resetDailyDungeon() {
        try {
            lastResetTime = System.currentTimeMillis();

            List<Player> playersToKick = new ArrayList<>();
            for (Map.Entry<Long, String> entry : playerToInstance.entrySet()) {
                Player player = findPlayerById(entry.getKey());
                if (player != null && player.zone != null && player.zone.map.mapId == MAP_ID) {
                    playersToKick.add(player);
                }
            }

            for (Player player : playersToKick) {
                try {
                    Service.getInstance().sendThongBao(player, "Địa Cung đã reset!");
                    ChangeMapService.gI().changeMapBySpaceShip(player, KICK_MAP_ID, KICK_ZONE_ID, KICK_X);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Đóng tất cả zone riêng
            for (DungeonZone zone : playerPrivateZones.values()) {
                zone.close();
            }

            activeInstances.clear();
            playerToInstance.clear();
            playerPrivateZones.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Player findPlayerById(long playerId) {
        try {
            nro.models.map.Map dungeonMap = MapService.gI().getMapById(MAP_ID);
            if (dungeonMap != null && dungeonMap.zones != null) {
                for (Zone zone : dungeonMap.zones) {
                    for (Player player : zone.getHumanoids()) {
                        if (player.id == playerId)
                            return player;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void checkAndResetDaily() {
        try {
            long currentTime = System.currentTimeMillis();
            long oneDayInMillis = 24 * 60 * 60 * 1000;
            if (lastResetTime == 0 || (currentTime - lastResetTime) >= oneDayInMillis) {
                resetDailyDungeon();
                lastResetTime = currentTime;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canPlayerJoinDungeon(Player player) {
        try {
            if (player == null)
                return false;
            return getPlayerRemainingAttempts(player.id) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public int getPlayerRemainingAttempts(long playerId) {
        return DungeonDAO.getRemainingAttempts(playerId);
    }

    public int getPlayerParticipationCount(long playerId) {
        return DungeonDAO.getParticipationCount(playerId);
    }

    public void incrementPlayerParticipation(Player player) {
        try {
            DungeonDAO.decrementAttempts(player.id);
            int remaining = DungeonDAO.getRemainingAttempts(player.id);
            Service.getInstance().sendThongBao(player, "Bạn còn " + remaining + " lần tham gia!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void penalizePlayerForFailure(Player player) {
        try {
            DungeonDAO.penalizePlayer(player.id, PENALTY_FOR_FAILURE);
            int remaining = DungeonDAO.getRemainingAttempts(player.id);

            if (remaining <= 0) {
                Service.getInstance().sendThongBao(player, "Bạn đã hết lượt tham gia Địa Cung hôm nay!");
            } else {
                Service.getInstance().sendThongBao(player,
                        "Bạn bị mất " + PENALTY_FOR_FAILURE + " lượt! Còn lại: " + remaining);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateHighestWave(Player player, int wave) {
        DungeonDAO.updateHighestWave(player.id, wave);
    }

    public int getHighestWave(Player player) {
        return DungeonDAO.getHighestWave(player.id);
    }

    /**
     * Xử lý khi player login - kiểm tra nếu đang ở map dungeon thì đưa ra ngoài
     * Gọi method này trước khi player.zone.addPlayer(player)
     */
    public void handlePlayerLogin(Player player) {
        try {
            if (player == null || player.zone == null) {
                return;
            }
            
            // Nếu player đang ở map dungeon
            if (player.zone.map.mapId == MAP_ID) {
                System.out.println("[Dungeon] Player " + player.name + " login at dungeon map, relocating...");
                
                // Xóa dữ liệu dungeon cũ của player
                removePlayerCompletely(player);
                
                // Chuyển player về map an toàn
                nro.models.map.Map safeMap = MapService.gI().getMapById(KICK_MAP_ID);
                if (safeMap != null && safeMap.zones != null && !safeMap.zones.isEmpty()) {
                    Zone safeZone = safeMap.zones.get(0);
                    player.zone = safeZone;
                    player.location.x = KICK_X;
                    player.location.y = 336;
                    System.out.println("[Dungeon] Relocated player to map " + KICK_MAP_ID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra zone có phải là dungeon zone riêng không
     */
    public boolean isDungeonPrivateZone(Zone zone) {
        if (zone == null) return false;
        return zone instanceof DungeonZone || playerPrivateZones.containsValue(zone);
    }
}
