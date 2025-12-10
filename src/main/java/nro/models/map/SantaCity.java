/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.models.map;

import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.services.MapService;
import nro.services.MobService;
import nro.services.Service;
import nro.utils.Log;
import nro.utils.Util;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * @author 💖 ahwuocdz 💖
 */
@Getter
public class SantaCity extends Map {

    // Timezone Việt Nam
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Cấu hình khung giờ mở event Hirudegarn
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
        
        public boolean isInRange(LocalTime now) {
            LocalTime open = LocalTime.of(openHour, openMin, openSec);
            LocalTime close = LocalTime.of(closeHour, closeMin, closeSec);
            return !now.isBefore(open) && now.isBefore(close);
        }
    }


    // Cấu hình các khung giờ mở event (có thể thêm nhiều khung giờ)
    public static final TimeRange[] TIME_RANGES = {
        new TimeRange(21, 0, 0, 23, 0, 0),  // 21:00 - 23:00
    };

    private boolean isOpened;
    private boolean isClosed;
    private ScheduledExecutorService scheduler;

    public SantaCity(int mapId, String mapName, byte planetId, byte tileId, byte bgId, byte bgType, byte type,
            int[][] tileMap, int[] tileTop, int zones, boolean isMapOffline, int maxPlayer, List<WayPoint> wayPoints,
            List<EffectMap> effectMaps) {
        super(mapId, mapName, planetId, tileId, bgId, bgType, type, tileMap, tileTop, zones, isMapOffline, maxPlayer,
                wayPoints, effectMaps);
    }

    @Override
    public void initZone(int number, int maxPlayer) {
        zones = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            Zone zone = new Zone(this, i, maxPlayer);
            zones.add(zone);
        }
    }

    public Zone random() {
        synchronized (zones) {
            if (!zones.isEmpty()) {
                List<Zone> list = zones.stream().filter(t -> !t.isFullPlayer()).collect(Collectors.toList());
                if (list.isEmpty()) {
                    list = zones;
                }
                int r = Util.nextInt(list.size());
                return zones.get(r);
            }
        }
        return null;
    }

    public void enter(Player player) {
        Zone zone = random();
        if (zone != null) {
            player.location.x = 100;
            player.location.y = 360;
            MapService.gI().goToMap(player, zone);
            Service.getInstance().clearMap(player);
            zone.mapInfo(player);
            player.zone.loadAnotherToMe(player);
            player.zone.load_Me_To_Another(player);
        }
    }

    public void leave(Player player) {
        Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(player, 19, 0);
        if (zone != null) {
            player.location.x = 1060;
            player.location.y = 360;
            MapService.gI().goToMap(player, zone);
            Service.getInstance().clearMap(player);
            zone.mapInfo(player);
            player.zone.loadAnotherToMe(player);
            player.zone.load_Me_To_Another(player);
        }
    }


    public void open() {
        if (!isOpened) {
            this.isOpened = true;
            System.out.println("[SantaCity] Event OPENED - " + LocalTime.now(VIETNAM_ZONE));
        }
    }

    /**
     * Kiểm tra xem hiện tại có đang trong khung giờ mở event không (theo giờ Việt Nam)
     */
    public boolean isInOpenTime() {
        LocalTime now = LocalTime.now(VIETNAM_ZONE);
        for (TimeRange range : TIME_RANGES) {
            if (range.isInRange(now)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Khởi động scheduler kiểm tra khung giờ mỗi 30 giây
     */
    public void startScheduler() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkAndUpdateStatus, 0, 30, TimeUnit.SECONDS);
        System.out.println("[SantaCity] Scheduler started - Time ranges: " + getTimeRangesInfo() + " (Vietnam timezone)");
    }

    /**
     * Kiểm tra và cập nhật trạng thái event theo khung giờ
     */
    private void checkAndUpdateStatus() {
        try {
            boolean shouldBeOpen = isInOpenTime();
            
            if (shouldBeOpen && !isOpened) {
                // Đến giờ mở
                reset();
                open();
            } else if (!shouldBeOpen && isOpened && !isClosed) {
                // Hết giờ, đóng event
                close();
            }
        } catch (Exception e) {
            Log.error(SantaCity.class, e, "Error in checkAndUpdateStatus");
        }
    }

    /**
     * Lấy thông tin khung giờ mở event dạng text
     */
    public static String getTimeRangesInfo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TIME_RANGES.length; i++) {
            TimeRange r = TIME_RANGES[i];
            sb.append(String.format("%02d:%02d - %02d:%02d", 
                r.openHour, r.openMin, r.closeHour, r.closeMin));
            if (i < TIME_RANGES.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }


    public void reset() {
        this.isOpened = false;
        this.isClosed = false;
        synchronized (zones) {
            for (Zone z : zones) {
                synchronized (z.mobs) {
                    for (Mob mob : z.mobs) {
                        MobService.gI().hoiSinhMob(mob);
                    }
                }
            }
        }
    }

    public void close() {
        if (!isClosed) {
            isClosed = true;
            System.out.println("[SantaCity] Event CLOSED - " + LocalTime.now(VIETNAM_ZONE));
            synchronized (zones) {
                for (Zone z : zones) {
                    try {
                        List<Player> players = z.getPlayers().stream().collect(Collectors.toList());
                        players.forEach(t -> {
                            if (t.isDie()) {
                                Service.getInstance().hsChar(t, t.nPoint.hpMax, t.nPoint.mpMax);
                            }
                            leave(t);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
