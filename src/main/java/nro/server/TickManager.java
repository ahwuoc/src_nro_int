package nro.server;

/**
 * Tick-based time system - independent from system clock
 * Prevents issues with NTP sync, manual time changes, etc.
 * 
 * @author 💖 ahwuocdz 💖
 */
public class TickManager {
    
    private static final TickManager instance = new TickManager();
    
    // Tick configuration
    public static final int TICKS_PER_SECOND = 20; // 20 TPS = 50ms per tick
    public static final long TICK_DURATION_MS = 1000 / TICKS_PER_SECOND; // 50ms
    
    // Current tick counter (monotonically increasing)
    private volatile long currentTick = 0;
    
    // Server start time (used only for display purposes)
    private final long serverStartTime;
    
    // Last real time when tick was updated (for debugging)
    private volatile long lastUpdateRealTime;
    
    private TickManager() {
        this.serverStartTime = System.currentTimeMillis();
        this.lastUpdateRealTime = this.serverStartTime;
    }
    
    public static TickManager gI() {
        return instance;
    }
    
    /**
     * Increment tick counter (called by main game loop)
     */
    public void tick() {
        currentTick++;
        lastUpdateRealTime = System.currentTimeMillis();
    }
    
    /**
     * Get current tick count
     */
    public long getCurrentTick() {
        return currentTick;
    }
    
    /**
     * Convert seconds to ticks
     */
    public static long secondsToTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }
    
    /**
     * Convert minutes to ticks
     */
    public static long minutesToTicks(int minutes) {
        return minutes * 60L * TICKS_PER_SECOND;
    }
    
    /**
     * Convert hours to ticks
     */
    public static long hoursToTicks(int hours) {
        return hours * 3600L * TICKS_PER_SECOND;
    }
    
    /**
     * Convert ticks to seconds
     */
    public static int ticksToSeconds(long ticks) {
        return (int) (ticks / TICKS_PER_SECOND);
    }
    
    /**
     * Convert ticks to milliseconds (for display only)
     */
    public static long ticksToMillis(long ticks) {
        return ticks * TICK_DURATION_MS;
    }
    
    /**
     * Check if enough ticks have passed since lastTick
     * @param lastTick The last tick when action was performed
     * @param ticksRequired Number of ticks that must pass
     * @return true if enough ticks have passed
     */
    public boolean canDoWithTicks(long lastTick, long ticksRequired) {
        return (currentTick - lastTick) >= ticksRequired;
    }
    
    /**
     * Get ticks remaining until target
     * @param startTick When the timer started
     * @param ticksDuration Total duration in ticks
     * @return Ticks remaining (0 if expired)
     */
    public long getTicksLeft(long startTick, long ticksDuration) {
        long elapsed = currentTick - startTick;
        long remaining = ticksDuration - elapsed;
        return Math.max(0, remaining);
    }
    
    /**
     * Get seconds remaining (for display)
     */
    public int getSecondsLeft(long startTick, long ticksDuration) {
        long ticksLeft = getTicksLeft(startTick, ticksDuration);
        return ticksToSeconds(ticksLeft);
    }
    
    /**
     * Get time string for display
     */
    public String getTimeLeftString(long startTick, long ticksDuration) {
        int secondsLeft = getSecondsLeft(startTick, ticksDuration);
        
        if (secondsLeft <= 0) {
            return "0 giây";
        }
        
        if (secondsLeft < 60) {
            return secondsLeft + " giây";
        }
        
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        
        if (minutes < 60) {
            return minutes + " phút " + (seconds > 0 ? seconds + " giây" : "");
        }
        
        int hours = minutes / 60;
        minutes = minutes % 60;
        
        return hours + " giờ " + (minutes > 0 ? minutes + " phút" : "");
    }
    
    /**
     * Check if timer has expired
     */
    public boolean hasExpired(long startTick, long ticksDuration) {
        return (currentTick - startTick) >= ticksDuration;
    }
    
    /**
     * Get server uptime in seconds (for display only)
     */
    public long getServerUptimeSeconds() {
        return ticksToSeconds(currentTick);
    }
    
    /**
     * Get debug info
     */
    public String getDebugInfo() {
        long uptimeSeconds = getServerUptimeSeconds();
        long realTimePassed = (System.currentTimeMillis() - serverStartTime) / 1000;
        long drift = Math.abs(uptimeSeconds - realTimePassed);
        
        return String.format(
            "Tick: %d | Uptime: %ds | Real: %ds | Drift: %ds | TPS: %d",
            currentTick, uptimeSeconds, realTimePassed, drift, TICKS_PER_SECOND
        );
    }
    
    /**
     * Reset tick counter (only for testing)
     */
    public void resetForTesting() {
        currentTick = 0;
    }
}
