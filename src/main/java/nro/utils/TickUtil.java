package nro.utils;

import nro.server.TickManager;

/**
 * Utility class for tick-based timing
 * Replaces time-based operations with tick-based ones
 * 
 * @author 💖 ahwuocdz 💖
 */
public class TickUtil {
    
    /**
     * Check if action can be performed based on ticks
     * Replacement for: Util.canDoWithTime(lastTime, milliseconds)
     * 
     * @param lastTick Last tick when action was performed
     * @param seconds Seconds to wait
     * @return true if enough time has passed
     */
    public static boolean canDoWithTicks(long lastTick, int seconds) {
        long ticksRequired = TickManager.secondsToTicks(seconds);
        return TickManager.gI().canDoWithTicks(lastTick, ticksRequired);
    }
    
    /**
     * Get current tick (replacement for System.currentTimeMillis())
     */
    public static long now() {
        return TickManager.gI().getCurrentTick();
    }
    
    /**
     * Get tick that will be reached after X seconds from now
     * Replacement for: System.currentTimeMillis() + (seconds * 1000)
     */
    public static long ticksFromNow(int seconds) {
        return now() + TickManager.secondsToTicks(seconds);
    }
    
    /**
     * Check if current tick is past the target tick
     * Replacement for: System.currentTimeMillis() > targetTime
     */
    public static boolean isPast(long targetTick) {
        return now() > targetTick;
    }
    
    /**
     * Check if current tick is before the target tick
     * Replacement for: System.currentTimeMillis() < targetTime
     */
    public static boolean isBefore(long targetTick) {
        return now() < targetTick;
    }
    
    /**
     * Get seconds left from now to target tick
     */
    public static int secondsUntil(long targetTick) {
        long ticksLeft = targetTick - now();
        if (ticksLeft <= 0) {
            return 0;
        }
        return TickManager.ticksToSeconds(ticksLeft);
    }
    
    /**
     * Get display string for time remaining
     */
    public static String getTimeLeftString(long targetTick) {
        int secondsLeft = secondsUntil(targetTick);
        
        if (secondsLeft <= 0) {
            return "0 giây";
        }
        
        if (secondsLeft < 60) {
            return secondsLeft + " giây";
        }
        
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        
        if (minutes < 60) {
            if (seconds > 0) {
                return minutes + " phút " + seconds + " giây";
            }
            return minutes + " phút";
        }
        
        int hours = minutes / 60;
        minutes = minutes % 60;
        
        StringBuilder result = new StringBuilder();
        result.append(hours).append(" giờ");
        if (minutes > 0) {
            result.append(" ").append(minutes).append(" phút");
        }
        
        return result.toString();
    }
    
    /**
     * Get display string for duration between two ticks
     */
    public static String getDurationString(long startTick, long endTick) {
        long ticks = endTick - startTick;
        int seconds = TickManager.ticksToSeconds(ticks);
        
        if (seconds < 60) {
            return seconds + " giây";
        }
        
        int minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes < 60) {
            return minutes + " phút" + (seconds > 0 ? " " + seconds + " giây" : "");
        }
        
        int hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours < 24) {
            return hours + " giờ" + (minutes > 0 ? " " + minutes + " phút" : "");
        }
        
        int days = hours / 24;
        hours = hours % 24;
        
        return days + " ngày" + (hours > 0 ? " " + hours + " giờ" : "");
    }
    
    /**
     * Convert milliseconds-based duration to tick-based
     * Use this for migration: ticksFromMillis(oldTimeValue - System.currentTimeMillis())
     */
    public static long ticksFromMillis(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        return TickManager.secondsToTicks(seconds);
    }
    
    /**
     * MIGRATION HELPERS - for converting old time-based code
     */
    
    /**
     * For cooldowns stored as lastTime
     * Old: Util.canDoWithTime(lastTime, 30000)
     * New: TickUtil.canDoWithTicks(lastTick, 30)
     */
    public static long convertTimeToTick(long oldTimeValue) {
        // If oldTimeValue is 0 or negative, return 0
        if (oldTimeValue <= 0) {
            return 0;
        }
        
        // Calculate how many seconds in the past this was
        long currentTime = System.currentTimeMillis();
        long millisInPast = currentTime - oldTimeValue;
        long secondsInPast = millisInPast / 1000;
        
        // Convert to tick equivalent
        long ticksInPast = TickManager.secondsToTicks((int) secondsInPast);
        return now() - ticksInPast;
    }
    
    /**
     * For expiry times stored as endTime
     * Old: if (expireTime > System.currentTimeMillis())
     * New: if (TickUtil.isBefore(expireTick))
     */
    public static long convertExpireTimeToTick(long oldExpireTime) {
        // If already expired or 0, return 0
        if (oldExpireTime <= 0 || oldExpireTime <= System.currentTimeMillis()) {
            return 0;
        }
        
        // Calculate seconds until expiry
        long millisUntilExpiry = oldExpireTime - System.currentTimeMillis();
        long secondsUntilExpiry = millisUntilExpiry / 1000;
        
        // Convert to tick equivalent
        return now() + TickManager.secondsToTicks((int) secondsUntilExpiry);
    }
}
