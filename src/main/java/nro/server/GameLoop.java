package nro.server;

import nro.models.map.Map;
import nro.models.player.Player;
import nro.models.mob.Mob;
import nro.utils.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * GameLoop - Centralized game update loop
 * 
 * Manages all game updates (maps, players, mobs, effects) at a consistent tick rate
 * of 20 ticks per second (50ms per tick).
 * 
 * @author Kiro
 */
public class GameLoop implements Runnable {

    private static final long TICK_DURATION_MS = 50; // 50ms per tick = 20 ticks/second
    private static final int STATS_LOG_INTERVAL = 100; // Log stats every 100 ticks

    private volatile boolean running = false;
    private Thread loopThread;
    private long tickCount = 0;
    private long lastTickDuration = 0;
    private long totalTickDuration = 0;
    private long maxTickDuration = 0;
    private long minTickDuration = Long.MAX_VALUE;

    /**
     * Starts the game loop thread
     */
    public void start() {
        if (!running) {
            running = true;
            loopThread = new Thread(this, "GameLoop");
            loopThread.start();
            Log.success("GameLoop started");
        }
    }

    /**
     * Stops the game loop gracefully
     */
    public void shutdown() {
        running = false;
        Log.warning("GameLoop shutdown signal received");
    }

    /**
     * Main game loop - executes ticks at consistent rate
     */
    @Override
    public void run() {
        Log.log("GameLoop thread started");
        
        while (running) {
            try {
                long tickStart = System.currentTimeMillis();

                // Update all game entities
                updateMaps();
                updatePlayers();
                updateMobs();
                updateEffects();

                // Calculate tick duration
                long tickEnd = System.currentTimeMillis();
                lastTickDuration = tickEnd - tickStart;
                totalTickDuration += lastTickDuration;
                maxTickDuration = Math.max(maxTickDuration, lastTickDuration);
                minTickDuration = Math.min(minTickDuration, lastTickDuration);

                // Log warning if tick exceeded target duration
                if (lastTickDuration > TICK_DURATION_MS) {
                    double utilization = (lastTickDuration * 100.0) / TICK_DURATION_MS;
                    Log.warning("Tick " + tickCount + " exceeded target duration: " + lastTickDuration + "ms (" + String.format("%.1f", utilization) + "%)");
                }

                // Log statistics periodically
                if (tickCount % STATS_LOG_INTERVAL == 0 && tickCount > 0) {
                    logTickStatistics();
                }

                // Sleep to maintain tick rate
                long sleepTime = TICK_DURATION_MS - lastTickDuration;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }

                tickCount++;

            } catch (InterruptedException e) {
                Log.error(GameLoop.class, e, "GameLoop interrupted");
                break;
            } catch (Exception e) {
                Log.error(GameLoop.class, e, "Error in GameLoop");
            }
        }

        Log.log("GameLoop stopped after " + tickCount + " ticks");
    }

    /**
     * Updates all maps
     * Requirement 2.1: WHEN the GameLoop executes a tick THEN the system SHALL call update() on each map in the MAPS collection
     */
    private void updateMaps() {
        try {
            for (Map map : Manager.MAPS) {
                try {
                    if (map != null) {
                        map.update();
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error updating map: " + (map != null ? map.mapName : "unknown"));
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error in updateMaps");
        }
    }

    /**
     * Updates all active players
     */
    private void updatePlayers() {
        try {
            // Get all active players from the server
            List<Player> players = getActivePlayers();
            for (Player player : players) {
                try {
                    if (player != null && !player.isDisposed()) {
                        player.update();
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error updating player: " + (player != null ? player.name : "unknown"));
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error in updatePlayers");
        }
    }

    /**
     * Updates all active mobs
     */
    private void updateMobs() {
        try {
            for (Map map : Manager.MAPS) {
                try {
                    if (map != null && map.zones != null) {
                        for (nro.models.map.Zone zone : map.zones) {
                            try {
                                if (zone != null && zone.mobs != null) {
                                    for (Mob mob : new ArrayList<>(zone.mobs)) {
                                        try {
                                            if (mob != null) {
                                                mob.update();
                                            }
                                        } catch (Exception e) {
                                            Log.error(GameLoop.class, e, "Error updating mob");
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.error(GameLoop.class, e, "Error updating mobs in zone");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error updating mobs in map");
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error in updateMobs");
        }
    }

    /**
     * Updates all active effects
     */
    private void updateEffects() {
        try {
            for (Map map : Manager.MAPS) {
                try {
                    if (map != null && map.zones != null) {
                        for (nro.models.map.Zone zone : map.zones) {
                            try {
                                if (zone != null) {
                                    // Effects are typically managed within zones
                                    // This is a placeholder for effect updates
                                }
                            } catch (Exception e) {
                                Log.error(GameLoop.class, e, "Error updating effects in zone");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error updating effects in map");
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error in updateEffects");
        }
    }

    /**
     * Gets all active players currently connected to the server
     */
    private List<Player> getActivePlayers() {
        List<Player> players = new ArrayList<>();
        try {
            // Iterate through all zones in all maps to collect active players
            for (Map map : Manager.MAPS) {
                if (map != null && map.zones != null) {
                    for (nro.models.map.Zone zone : map.zones) {
                        if (zone != null) {
                            List<Player> zonePlayers = zone.getPlayers();
                            if (zonePlayers != null) {
                                players.addAll(zonePlayers);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error getting active players");
        }
        return players;
    }

    /**
     * Logs tick statistics
     */
    private void logTickStatistics() {
        try {
            double averageTickDuration = totalTickDuration / (double) tickCount;
            int playerCount = getActivePlayers().size();
            int mobCount = getActiveMobCount();
            int effectCount = getActiveEffectCount();
            
            Log.log(String.format(
                "Tick Statistics [%d]: Avg=%.2fms, Max=%dms, Min=%dms, Players=%d, Mobs=%d, Effects=%d",
                tickCount,
                averageTickDuration,
                maxTickDuration,
                minTickDuration,
                playerCount,
                mobCount,
                effectCount
            ));
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error logging tick statistics");
        }
    }

    /**
     * Gets the count of all active mobs
     */
    private int getActiveMobCount() {
        int count = 0;
        try {
            for (Map map : Manager.MAPS) {
                if (map != null && map.zones != null) {
                    for (nro.models.map.Zone zone : map.zones) {
                        if (zone != null && zone.mobs != null) {
                            count += zone.mobs.size();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error counting active mobs");
        }
        return count;
    }

    /**
     * Gets the count of all active effects
     * Requirement 10.3: WHEN the GameLoop is running THEN the system SHALL track the number of active effects
     */
    private int getActiveEffectCount() {
        int count = 0;
        try {
            // Count effects on all players
            List<Player> players = getActivePlayers();
            for (Player player : players) {
                try {
                    if (player != null && player.effectSkill != null) {
                        // Count active effects on this player
                        if (player.effectSkill.isHaveEffectSkill()) {
                            count++;
                        }
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error counting effects on player");
                }
            }

            // Count effects on all mobs
            for (Map map : Manager.MAPS) {
                try {
                    if (map != null && map.zones != null) {
                        for (nro.models.map.Zone zone : map.zones) {
                            try {
                                if (zone != null && zone.mobs != null) {
                                    for (Mob mob : zone.mobs) {
                                        try {
                                            if (mob != null && mob.effectSkill != null) {
                                                // Count active effects on this mob
                                                if (mob.effectSkill.isHaveEffectSkill()) {
                                                    count++;
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.error(GameLoop.class, e, "Error counting effects on mob");
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.error(GameLoop.class, e, "Error counting effects in zone");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.error(GameLoop.class, e, "Error counting effects in map");
                }
            }
        } catch (Exception e) {
            Log.error(GameLoop.class, e, "Error in getActiveEffectCount");
        }
        return count;
    }

    /**
     * Gets the duration of the last tick in milliseconds
     */
    public long getLastTickDuration() {
        return lastTickDuration;
    }

    /**
     * Gets the current tick count
     */
    public long getTickCount() {
        return tickCount;
    }

    /**
     * Gets whether the game loop is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets metrics about the game loop
     * Requirement 10.4: WHEN performance metrics are requested THEN the system SHALL return current counts and average tick duration
     */
    public GameLoopMetrics getMetrics() {
        return new GameLoopMetrics(
            tickCount,
            lastTickDuration,
            totalTickDuration / Math.max(tickCount, 1),
            maxTickDuration,
            minTickDuration,
            getActivePlayers().size(),
            getActiveMobCount(),
            getActiveEffectCount()
        );
    }

    /**
     * Data class for game loop metrics
     */
    public static class GameLoopMetrics {
        public final long tickCount;
        public final long lastTickDuration;
        public final double averageTickDuration;
        public final long maxTickDuration;
        public final long minTickDuration;
        public final int playerCount;
        public final int mobCount;
        public final int effectCount;

        public GameLoopMetrics(long tickCount, long lastTickDuration, double averageTickDuration,
                             long maxTickDuration, long minTickDuration, int playerCount, int mobCount, int effectCount) {
            this.tickCount = tickCount;
            this.lastTickDuration = lastTickDuration;
            this.averageTickDuration = averageTickDuration;
            this.maxTickDuration = maxTickDuration;
            this.minTickDuration = minTickDuration;
            this.playerCount = playerCount;
            this.mobCount = mobCount;
            this.effectCount = effectCount;
        }
    }
}
