package nro.ahwuocdz;

import java.util.ArrayList;
import java.util.List;

import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.server.io.Message;
import nro.services.func.ChangeMapService;
import nro.services.Service;
import nro.utils.Util;

public class DungeonInstance {

    public static final float WAVE_POWER_MULTIPLIER = 1.5f;
    public static final int BASE_KILLS_REQUIRED = 10;
    public static final int KILLS_INCREASE_PER_WAVE = 5;
    public static final long WAVE_TIME_LIMIT = 600000;
    public static final long WAVE_INTERVAL = 30000;

    public static final int COUNTDOWN_SECONDS = 5;
    public static final int COUNTDOWN_INTERVAL = 1000;
    public static final int KICK_DELAY_SECONDS = 3;
    public static final long NOTIFICATION_INTERVAL = 60000;

    public static final int BASE_MOB_LEVEL = 80;
    public static final int MOB_LEVEL_INCREASE_PER_WAVE = 5;

    public static final int KICK_MAP_ID = 2;
    public static final int KICK_ZONE_ID = -1;
    public static final int KICK_X = 164;

    // 20 con rải đều từ x=35 đến x=1379, khoảng cách ~70.7
    // hp=10000, damage=1000, pTiemNang=5
    public static MobSpawnData[] ENTRY_MOB_SPAWN = {
        new MobSpawnData(13, 10000, 1000, 35, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 106, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 177, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 248, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 319, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 390, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 461, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 532, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 603, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 674, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 745, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 816, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 887, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 958, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1029, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1100, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1171, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1242, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1313, 336, 10, 5, 5, 0, 100000, 20),
        new MobSpawnData(13, 10000, 1000, 1379, 336, 10, 5, 5, 0, 100000, 20),
    };

    private String instanceId;
    private Zone zone;
    private Player owner;
    private int currentWave;
    private boolean isActive;
    private List<Mob> currentWaveMobs;
    private DungeonWaveConfig waveConfig;
    private boolean waveCompleted;
    private long waveStartTime;
    private long waveTimeLimit;
    private int totalKillsThisWave;
    private int requiredKillsThisWave;
    private long lastNotificationTime;
    private boolean dungeonStarted;

    public DungeonInstance(String instanceId, Zone zone, Player owner) {
        this.instanceId = instanceId;
        this.zone = zone;
        this.owner = owner;
        this.currentWave = 1;
        this.isActive = true;
        this.currentWaveMobs = new ArrayList<>();
        this.waveConfig = new DungeonWaveConfig();
        this.waveCompleted = false;
        this.waveTimeLimit = WAVE_TIME_LIMIT;
        this.totalKillsThisWave = 0;
        this.requiredKillsThisWave = BASE_KILLS_REQUIRED;
        this.dungeonStarted = false;
    }


    public void update() {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();
        if (!waveCompleted && dungeonStarted) {
            updateWaveTimer(currentTime);
            if (currentTime - waveStartTime >= waveTimeLimit) {
                failWave();
            }
        }
    }

    public void startWave() {
        if (!isActive) return;
        try {
            waveCompleted = false;
            clearPreviousMobs();
            currentWaveMobs.clear();
            startWaveCountdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearPreviousMobs() {
        if (zone != null && zone.mobs != null) {
            zone.mobs.removeIf(mob -> mob.name != null && mob.name.contains("Wave"));
        }
    }

    private void startWaveCountdown() {
        Service.getInstance().sendThongBao(owner, "Chuẩn bị cho Wave " + currentWave + "!");
        new Thread(() -> {
            try {
                for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
                    Service.getInstance().sendThongBao(owner, "Wave " + currentWave + " bắt đầu trong: " + i + " giây");
                    Thread.sleep(COUNTDOWN_INTERVAL);
                }
                if (isActive) {
                    requiredKillsThisWave = BASE_KILLS_REQUIRED + (currentWave - 1) * KILLS_INCREASE_PER_WAVE;
                    executeWaveStart();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void executeWaveStart() {
        waveStartTime = System.currentTimeMillis();
        lastNotificationTime = 0;
        dungeonStarted = true;

        WaveData waveData = waveConfig.getWaveData(currentWave);
        for (int i = 0; i < waveData.mobCount; i++) {
            spawnMob(waveData, i);
        }
        zone.mapInfo(owner);
        Service.getInstance().sendThongBao(owner, "Wave " + currentWave + " bắt đầu! Tiêu diệt " + requiredKillsThisWave + " quái!");
    }

    private MobSpawnData getSpawnData(int mobIndex) {
        return ENTRY_MOB_SPAWN[mobIndex % ENTRY_MOB_SPAWN.length];
    }


    private void spawnMob(WaveData waveData, int mobIndex) {
        MobSpawnData spawnData = getSpawnData(mobIndex);
        
        int nextMobId = getNextMobId();
        Mob newMob = new Mob();
        newMob.id = nextMobId;
        newMob.tempId = spawnData.tempId;
        newMob.name = waveData.mobName + " (Wave " + currentWave + ")";
        newMob.level = (byte) waveData.mobLevel;

        int baseHp = (int) (spawnData.hp * Math.pow(WAVE_POWER_MULTIPLIER, currentWave - 1));
        int baseDamage = (int) (spawnData.damage * Math.pow(WAVE_POWER_MULTIPLIER, currentWave - 1));

        newMob.point.setHpFull(baseHp);
        newMob.point.hp = baseHp;
        newMob.point.dame = baseDamage;

        newMob.location.x = spawnData.x;
        newMob.location.y = spawnData.y;

        newMob.zone = zone;
        newMob.pDame = (byte) spawnData.pDame;
        newMob.pTiemNang = (byte) spawnData.pTiemNang;
        newMob.status = (byte) spawnData.status;
        newMob.lastTimeDie = spawnData.lastTimeDie;
        newMob.setTiemNang();

        zone.mobs.add(newMob);
        currentWaveMobs.add(newMob);
        sendMobSpawnMessage(newMob);
    }

    private void sendMobSpawnMessage(Mob mob) {
        try {
            Message msg = new Message(-13);
            msg.writer().writeByte(mob.id);
            msg.writer().writeByte(mob.tempId);
            msg.writer().writeByte(mob.lvMob);
            msg.writer().writeInt(mob.point.hp);
            Service.getInstance().sendMessAllPlayerInMap(zone, msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMobKilled() {
        totalKillsThisWave++;
        Service.getInstance().sendThongBao(owner, "Tiến độ: " + totalKillsThisWave + "/" + requiredKillsThisWave);
        if (totalKillsThisWave >= requiredKillsThisWave && !waveCompleted) {
            completeWave();
        }
    }

    private void completeWave() {
        waveCompleted = true;
        giveWaveRewards();
        Service.getInstance().sendThongBao(owner, "Wave " + currentWave + " hoàn thành!");
        totalKillsThisWave = 0;

        new Thread(() -> {
            try {
                for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
                    Service.getInstance().sendThongBao(owner, "Wave tiếp theo trong: " + i + " giây");
                    Thread.sleep(COUNTDOWN_INTERVAL);
                }
                if (isActive && owner != null && owner.zone != null) {
                    startNextWave();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void startNextWave() {
        currentWave++;
        waveCompleted = false;
        totalKillsThisWave = 0;
        startWave();
    }

    private void giveWaveRewards() {
        long totalExpReward = 0;
        for (MobSpawnData spawnData : ENTRY_MOB_SPAWN) {
            totalExpReward += spawnData.expReward * currentWave;
        }
        Service.getInstance().add_TNSM(owner, (byte) 2, totalExpReward, true);

        if (Util.isTrue(ENTRY_MOB_SPAWN[0].specialChance, 100)) {
            Service.getInstance().sendThongBao(owner, "Bạn nhận được phần thưởng đặc biệt!");
        }
    }

    private int getNextMobId() {
        int nextMobId = 0;
        for (Mob existingMob : zone.mobs) {
            if (existingMob.id >= nextMobId) {
                nextMobId = existingMob.id + 1;
            }
        }
        return nextMobId;
    }

    private void updateWaveTimer(long currentTime) {
        long timeElapsed = currentTime - waveStartTime;
        long timeRemaining = waveTimeLimit - timeElapsed;
        long minutesRemaining = timeRemaining / 60000;

        if (timeRemaining > 0 && currentTime - lastNotificationTime >= NOTIFICATION_INTERVAL) {
            if (minutesRemaining > 0) {
                Service.getInstance().sendThongBao(owner, "Còn " + minutesRemaining + " phút! Đã tiêu diệt: " + totalKillsThisWave + "/" + requiredKillsThisWave);
                lastNotificationTime = currentTime;
            }
        }
    }

    private void failWave() {
        isActive = false;
        Service.getInstance().sendThongBao(owner, "Thất bại! Chỉ tiêu diệt được " + totalKillsThisWave + "/" + requiredKillsThisWave);
        DungeonManage.gI().penalizePlayerForFailure(owner);
        kickPlayerFromDungeon();
    }

    private void kickPlayerFromDungeon() {
        try {
            Service.getInstance().sendThongBao(owner, "Bạn sẽ bị đưa ra khỏi dungeon sau " + KICK_DELAY_SECONDS + " giây...");
            if (owner.zone != null) {
                owner.zone.mobs.removeIf(mob -> mob.name != null && mob.name.contains("Wave"));
            }

            new Thread(() -> {
                try {
                    Thread.sleep(KICK_DELAY_SECONDS * 1000);
                    if (owner != null) {
                        DungeonManage.gI().removePlayerCompletely(owner);
                        ChangeMapService.gI().changeMapBySpaceShip(owner, KICK_MAP_ID, KICK_ZONE_ID, KICK_X);
                        Service.getInstance().sendThongBao(owner, "Bạn đã bị đưa ra khỏi dungeon!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Getters
    public String getInstanceId() { return instanceId; }
    public Zone getZone() { return zone; }
    public Player getOwner() { return owner; }
    public int getCurrentWave() { return currentWave; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public List<Mob> getCurrentWaveMobs() { return currentWaveMobs; }
    public int getRequiredKillsThisWave() { return requiredKillsThisWave; }
    public int getTotalKillsThisWave() { return totalKillsThisWave; }
    public boolean isDungeonStarted() { return dungeonStarted; }

    public boolean isMobFromThisInstance(int mobId) {
        if (zone != null && zone.mobs != null) {
            for (Mob mob : zone.mobs) {
                if (mob.id == mobId && mob.name != null && mob.name.contains("Wave")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class DungeonWaveConfig {
        public WaveData getWaveData(int wave) {
            WaveData data = new WaveData();
            data.mobCount = ENTRY_MOB_SPAWN.length;
            data.mobName = "Quái Địa Cung";
            data.mobLevel = BASE_MOB_LEVEL + (wave - 1) * MOB_LEVEL_INCREASE_PER_WAVE;
            return data;
        }
    }

    private static class WaveData {
        int mobCount;
        String mobName;
        int mobLevel;
    }
}
