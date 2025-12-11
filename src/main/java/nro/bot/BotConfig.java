package nro.bot;

/**
 * Configuration constants for Bot behavior.
 * Easy to modify without changing Bot logic.
 */
public class BotConfig {

    // ==================== COMBAT ====================
    /** Melee attack range in pixels */
    public static final short ATTACK_RANGE_MELEE = 50;

    /** Distance to pick up items */
    public static final int PICKUP_RANGE = 50;

    /** Max items to pick per update cycle */
    public static final int MAX_PICKUP_PER_CYCLE = 5;

    // ==================== MOVEMENT ====================
    /** Movement speed when walking to mob (min pixels) */
    public static final int MOVE_SPEED_TO_MOB_MIN = 5;

    /** Movement speed when walking to mob (max pixels) */
    public static final int MOVE_SPEED_TO_MOB_MAX = 10;

    /** Movement speed when moving to waypoint (min pixels) */
    public static final int MOVE_SPEED_TO_WAYPOINT_MIN = 20;

    /** Movement speed when moving to waypoint (max pixels) */
    public static final int MOVE_SPEED_TO_WAYPOINT_MAX = 50;

    /** Distance threshold to consider "at waypoint" */
    public static final int WAYPOINT_REACH_DISTANCE = 20;

    /** Interval for map movement checks (ms) */
    public static final int MAP_MOVE_CHECK_INTERVAL = 100;

    // ==================== TIMING ====================
    /** Time to hunt before moving to next map (ms) */
    public static final long HUNTING_DURATION = 10000;

    /** Idle movement interval min (ms) */
    public static final int IDLE_MOVE_INTERVAL_MIN = 3000;

    /** Idle movement interval max (ms) */
    public static final int IDLE_MOVE_INTERVAL_MAX = 6000;

    /** Idle movement distance */
    public static final int IDLE_MOVE_DISTANCE = 10;

    // ==================== MAP ROTATION ====================
    /**
     * List of map IDs for bot to rotate through by planet
     * [0] = Trái Đất, [1] = Namek, [2] = Xayda
     */
    public static final int[][] MAP_LIST = {
        {0, 1, 2},     // Hành tinh Trái Đất
        {7, 8, 9},     // Hành tinh Namek
        {14, 15, 16}   // Hành tinh Xayda
    };

    // ==================== MAP MOVEMENT BEHAVIOR ====================
    /** Whether bot should avoid returning to the map it just left */
    public static final boolean AVOID_PREVIOUS_MAP = true;

    /** Whether bot should prefer maps with more mobs */
    public static final boolean PREFER_MOB_MAPS = true;

    /** Weight multiplier for mob-based map selection (higher = stronger preference for maps with more mobs) */
    public static final double MOB_WEIGHT_MULTIPLIER = 2.0;

    // ==================== APPEARANCE ====================
    /** Default appearance [gender][head/body/leg] - matches new player defaults */
    public static final short[][] BOT_APPEARANCE = {
        {6, 57, 58},   // Trai Dat (gender 0)
        {29, 59, 60},  // Namec (gender 1)
        {28, 57, 58}   // Xayda (gender 2)
    };

    // ==================== TITLE (DANH HIỆU) ====================
    /** List of title part IDs for bots to use randomly */
    public static final int[] TITLE_PARTS = {
        86, 88, 214, 89, 217, 218, 87, 85, 215
    };

    /** Interval to change title (ms) - 30-60 seconds */
    public static final int TITLE_CHANGE_INTERVAL_MIN = 30000;
    public static final int TITLE_CHANGE_INTERVAL_MAX = 60000;

    // ==================== FLAG BAG ====================
    /** List of flag bag IDs for bots to use randomly */
    public static final int[] FLAG_BAG_IDS = {
        102, 104, 105, 106, 107, 108, 109
    };

    /** Interval to change flag bag (ms) - 45-90 seconds */
    public static final int FLAG_BAG_CHANGE_INTERVAL_MIN = 45000;
    public static final int FLAG_BAG_CHANGE_INTERVAL_MAX = 90000;

    // ==================== PET (ĐỆ TỬ) ====================
    /** Whether bots should have pets by default */
    public static final boolean BOT_HAS_PET = true;
    
    // ==================== BOSS HUNTING ====================
    /** 
     * List of boss IDs that bot can hunt
     * Bot will teleport to boss location if boss is alive
     */
    public static final int[] BOSS_IDS = {
         -1007221
    };
    
    /** Time to attack boss before checking for another (ms) */
    public static final long BOSS_ATTACK_DURATION = 30000;
    
    // ==================== AUTO REVIVE ====================
    /** Whether bot should auto revive when dead */
    public static final boolean AUTO_REVIVE = true;
    
    /** Delay before auto revive (ms) - 3 seconds */
    public static final long AUTO_REVIVE_DELAY = 3000;
}
