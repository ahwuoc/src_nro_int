package nro.bot;

import nro.ahwuocdz.constant.ItemConstant;
import nro.consts.ConstNpc;
import nro.consts.ItemId;
import nro.utils.Util;

/**
 * Configuration constants for Bot behavior.
 * Easy to modify without changing Bot logic.
 */
public class BotConfig {

        // ==================== COMBAT ====================
        /** Melee attack range in pixels */
        public static final short ATTACK_RANGE_MELEE = 50;

        /** Whether bot should pick up items while hunting */
        public static final boolean ENABLE_PICKUP_ITEM = false;

        /** Distance to pick up items */
        public static final int PICKUP_RANGE = 10;

        /** Max items to pick per update cycle */
        public static final int MAX_PICKUP_PER_CYCLE = 5;

        /** Delay between skill attacks when hunting mobs (ms) - MIN */
        public static final int HUNTING_ATTACK_DELAY_MIN = 800;

        /** Delay between skill attacks when hunting mobs (ms) - MAX */
        public static final int HUNTING_ATTACK_DELAY_MAX = 1500;

        // ==================== MOVEMENT ====================
        /** Movement speed when walking to mob (min pixels per step) */
        public static final int MOVE_SPEED_TO_MOB_MIN = 10;

        /** Movement speed when walking to mob (max pixels per step) */
        public static final int MOVE_SPEED_TO_MOB_MAX = 20;

        /** Interval for walking to mob (ms) - how often bot moves towards mob */
        public static final int WALK_TO_MOB_INTERVAL = 200;

        /** Movement speed when moving to waypoint (min pixels per step) */
        public static final int MOVE_SPEED_TO_WAYPOINT_MIN = 30;

        /** Movement speed when moving to waypoint (max pixels per step) */
        public static final int MOVE_SPEED_TO_WAYPOINT_MAX = 50;

        /** Distance threshold to consider "at waypoint" */
        public static final int WAYPOINT_REACH_DISTANCE = 20;

        /** Interval for map movement checks (ms) */
        public static final int MAP_MOVE_CHECK_INTERVAL = 100;

        /**
         * Interval for waypoint movement (ms) - how often bot moves towards waypoint
         * Lower = smoother movement, Higher = more choppy
         * Recommended: 100-200ms for smooth movement
         */
        public static final int WAYPOINT_MOVE_INTERVAL = 150;

        // ==================== TIMING ====================
        /** Time to hunt before moving to next map - MIN (ms) */
        public static final int HUNTING_DURATION_MIN = 10000;

        /** Time to hunt before moving to next map - MAX (ms) */
        public static final int HUNTING_DURATION_MAX = 60000;

        /** Time to hunt before moving to next map (ms) - deprecated, use MIN/MAX */
        public static final long HUNTING_DURATION = 10000;

        /** Random offset when spawning bot to prevent sync movement - MIN (ms) */
        public static final int HUNTING_START_OFFSET_MIN = 0;

        /** Random offset when spawning bot to prevent sync movement - MAX (ms) */
        public static final int HUNTING_START_OFFSET_MAX = 15000;

        /** Idle movement interval min (ms) */
        public static final int IDLE_MOVE_INTERVAL_MIN = 3000;

        /** Idle movement interval max (ms) */
        public static final int IDLE_MOVE_INTERVAL_MAX = 6000;

        /** Idle movement distance */
        public static final int IDLE_MOVE_DISTANCE = 10;

        // ==================== FARM MOB OUTFIT ====================
        /** Áo cho bot farm mob theo hành tinh [gender][random options] */
        public static final int[][] AO_FARM_MOBS = {
                        { ItemId.ITEM_0_AO_VAI_3_LO, ItemId.ITEM_3_AO_VAI_DAY }, // Trái Đất: Áo vải 3 lỗ, Áo vải dày
                        { 1, 4 }, // Namek: Áo sợi len, Áo len Pico
                        { 2, 5 } // Xayda: Áo vải thô, Áo giáp sắt
        };

        /** Quần cho bot farm mob theo hành tinh [gender][random options] */
        public static final int[][] QUAN_FARM_MOBS = {
                        { ItemId.ITEM_9_QUAN_VAI_DAY, ItemId.ITEM_6_QUAN_VAI_EN }, // Trái Đất: Quần vải đen, Quần thun
                                                                                   // Pico
                        { ItemId.ITEM_7_QUAN_SOI_LEN, ItemId.ITEM_10_QUAN_VAI_THO_PICO }, // Namek: Quần sợi len, Quần
                                                                                          // thun đen
                        { ItemId.ITEM_8_QUAN_VAI_THO, ItemId.ITEM_11_QUAN_GIAP_SAT } // Xayda: Quần vải thô, Quần giáp
                                                                                     // sắt
        };

        /** Interval to change farm mob outfit (ms) - MIN */
        public static final int FARM_MOB_OUTFIT_CHANGE_MIN = 60000; // 1 phút

        /** Interval to change farm mob outfit (ms) - MAX */
        public static final int FARM_MOB_OUTFIT_CHANGE_MAX = 180000; // 3 phút

        // ==================== MAP ROTATION ====================
        /**
         * List of map IDs for bot to rotate through by planet
         * [0] = Trái Đất, [1] = Namek, [2] = Xayda
         */
        public static final int[][] MAP_LIST = {
                        { 0, 1, 2, 3, 4 }, // Hành tinh Trái Đất
                        { 7, 8, 9, 11, 12 }, // Hành tinh Namek
                        { 14, 15, 16, 17, 18 } // Hành tinh Xayda
        };

        // ==================== MAP MOVEMENT BEHAVIOR ====================
        /** Whether bot should avoid returning to the map it just left */
        public static final boolean AVOID_PREVIOUS_MAP = true;

        /** Whether bot should prefer maps with more mobs */
        public static final boolean PREFER_MOB_MAPS = true;

        /**
         * Weight multiplier for mob-based map selection (higher = stronger preference
         * for maps with more mobs)
         */
        public static final double MOB_WEIGHT_MULTIPLIER = 2.0;

        // ==================== APPEARANCE ====================
        /** Default appearance [gender][head/body/leg] - matches new player defaults */
        public static final short[][] BOT_APPEARANCE = {
                        { 0, 57, 58 }, // Trai Dat (gender 0)
                        { 29, 59, 60 }, // Namec (gender 1)
                        { 28, 57, 58 } // Xayda (gender 2)
        };

        public static final short[][] BOT_HEAD_APPEARANCE = {
                        { 0, 17, 30, 31, 64 }, // Trai Dat (gender 0)
                        { 9, 59, 29, 60, 32 }, // Namec (gender 1)
                        { 28, 27, 57, 58 } // Xayda (gender 2)
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

        public static final int[] MAP_BOT_AFK_FARM_DETU = {
                        6, 10, 19, 28, 27, 30, 31, 32, 34, 35, 36, 38, 37
        };

        // ==================== NPC VISITOR ====================
        /** Map ID where NPC visitor bots will stay */
        public static final int NPC_VISITOR_MAP_ID = 5;

        /**
         * NPC actions when visiting
         * 0 = do nothing, 1 = change outfit, 2 = change pet, 3 = change title
         */
        public static final int NPC_ACTION_NONE = 0;
        public static final int NPC_ACTION_OUTFIT = 1;
        public static final int NPC_ACTION_PET = 2;
        public static final int NPC_ACTION_TITLE = 3;

        /** List of NPC IDs for visitor bots to visit (all in same map) */
        public static final int[] NPC_VISIT_IDS = {
                        ConstNpc.SANTA, // Change outfit
                        ConstNpc.GOGETA_SSJ4, // Change pet
                        ConstNpc.TORIBOT, // Change title
                        ConstNpc.QUY_LAO_KAME, // Change title
                        ConstNpc.BA_HAT_MIT,
                        ConstNpc.KING_FURY,
                        ConstNpc.QUOC_VUONG,

        };

        /** Action for each NPC (same order as NPC_VISIT_IDS) */
        public static final int[] NPC_VISIT_ACTIONS = {
                        NPC_ACTION_OUTFIT, // Santa -> change outfit
                        NPC_ACTION_PET, // Gogeta SSJ4 -> change pet
                        NPC_ACTION_TITLE, // Toribot -> change title
                        NPC_ACTION_NONE, // Toribot -> change title
                        NPC_ACTION_NONE, // Toribot -> change title
                        NPC_ACTION_NONE, // Toribot -> change title
                        NPC_ACTION_NONE, // Toribot -> change title

        };

        public static final int NPC_VISIT_DURATION_MIN = 8000;
        public static final int NPC_VISIT_DURATION_MAX = 15000;

        /**
         * Whether NPC visitor bot should walk slowly to NPC (true) or teleport directly
         * (false)
         */
        public static final boolean NPC_VISITOR_WALK_TO_NPC = true;

        /** Walk speed when moving to NPC (pixels per update) */
        public static final int NPC_WALK_SPEED_MIN = 30;
        public static final int NPC_WALK_SPEED_MAX = 50;

        public static final int FLAG_BAG_CHANGE_INTERVAL_MIN = 45000;
        public static final int FLAG_BAG_CHANGE_INTERVAL_MAX = 90000;
        public static final boolean BOT_HAS_PET = true;

        // ==================== BOT FUSION ====================
        /** Whether bot should auto fusion with pet */
        public static final boolean BOT_AUTO_FUSION = true;
        
        /** Interval to toggle fusion (ms) - MIN */
        public static final int BOT_FUSION_INTERVAL_MIN = 30000; // 30 seconds
        
        /** Interval to toggle fusion (ms) - MAX */
        public static final int BOT_FUSION_INTERVAL_MAX = 120000; // 2 minutes
        public static final boolean FARM_MOB_HAS_PET = false;
        public static final boolean FARM_BOSS_HAS_PET = false;

        /** Whether AFK bots should have pets - MUST be true for AFK mode */
        public static final boolean AFK_BOT_HAS_PET = true;

        // ==================== AFK BOT ====================
        /**
         * Time AFK bot stays in one map before moving to another (ms) - 60-120 seconds
         */
        public static final int AFK_MAP_DURATION_MIN = 60000;
        public static final int AFK_MAP_DURATION_MAX = 120000;

        /** Small idle movement distance for AFK bot (pixels) */
        public static final int AFK_IDLE_MOVE_DISTANCE = 5;

        /** Interval for small idle movements (ms) - 10-20 seconds */
        public static final int AFK_IDLE_MOVE_INTERVAL_MIN = 10000;
        public static final int AFK_IDLE_MOVE_INTERVAL_MAX = 20000;

        // ==================== ZONE SWITCHING ====================
        /** Enable auto zone switching when zone is crowded */
        public static final boolean AUTO_SWITCH_ZONE = true;

        /**
         * Max players per zone before considering it crowded (0 = use ratio instead)
         */
        public static final int ZONE_MAX_PLAYERS = 2;

        /** Player to mob ratio threshold - switch zone if players/mobs > this value */
        public static final double ZONE_PLAYER_MOB_RATIO = 0.5;

        /** Minimum difference in players to trigger zone switch */
        public static final int ZONE_SWITCH_MIN_DIFF = 2;

        /** Cooldown between zone switches (ms) - prevent spam switching */
        public static final long ZONE_SWITCH_COOLDOWN = Util.nextInt(10000, 60000);

        // ==================== BOSS HUNTING ====================
        /**
         * List of boss IDs that bot can hunt
         * Bot will teleport to boss location if boss is alive
         */

        /** Time to attack boss before checking for another (ms) */
        public static final long BOSS_ATTACK_DURATION = 30000;

        /** Delay between boss attacks (ms) - prevents skill spam */
        public static final int BOSS_ATTACK_DELAY = 2000;

        /** Skill cooldown for boss fighting (ms) */
        public static final int BOSS_SKILL_COOLDOWN = 2000;

        // ==================== AUTO REVIVE ====================
        /** Whether bot should auto revive when dead */
        public static final boolean AUTO_REVIVE = true;

        /** Delay before auto revive (ms) - 3 seconds */
        public static final long AUTO_REVIVE_DELAY = 3000;

        // ==================== BOT NAME PREFIXES ====================
        /** List of name prefixes for bot names */

        public static final String[] BOT_NAME = {
                        "goku", "kakarot", "toandz", "phulb", "kocz", "admjn", "bankai", "tdpro", "honganh",
                        "soon",
                        "ghanguma", "yusiki", "mankanh", "dadensheng", "bomboeoi", "showem", "xinhlytinhvan", "xdbom1",
                        "hp",
                        "phuonganh", "thuhieenls", "phongsl", "siuxd", "chemchep", "xayda", "kingmid24",
                        "ngocrongvuong",
                        "lamtlam", "nemiuxinh", "ksgl1234", "bethaicute", "shinkun03", "nameclz", "phoam268", "jummi",
                        "kame2mb", "aoxdcoi82", "adminne", "dieppham", "ghost", "buonvi15cm", "hello2bom", "siutom",
                        "essitom",
                        "dzvclbacarat", "pem36m", "picolotrang", "xinhad", "minnlinh", "chiconca", "ctohellokitty",
                        "rubinkun",
                        "admin1", "paradjse", "cavecon", "zinnopros1", "kimochi", "admjn", "bankai", "tdpro", "honganh",
                        "soon",
                        "ghanguma", "yusiki", "mankanh", "dadensheng", "bomboeoi", "showem", "xinhlytinhvan", "xdbom1",
                        "hp",
                        "phuonganh", "thuhieenls", "phongsl", "siuxd", "chemchep", "xayda", "kingmid24",
                        "ngocrongvuong",
                        "lamtlam", "nemiuxinh", "ksgl1234", "bethaicute", "shinkun03", "nameclz", "phoam268", "jummi",
                        "kame2mb", "aoxdcoi82", "adminne", "dieppham", "ghost", "buonvi15cm", "hello2bom", "siutom",
                        "essitom",
                        "dzvclbacarat", "pem36m", "picolotrang", "xinhad", "minnlinh", "chiconca", "ctohellokitty",
                        "rubinkun",
                        "trumkame", "coolkiz", "minhhang", "coikon", "rubinne", "thaybat", "dpem", "hahihoooo",
                        "hangiuon", "killchiu", "iupanhiu", "baoproxd", "iphone", "jinsungwoo", "huyacmasoc", "tiupem",
                        "chillchillxd", "hoinach", "vuthinh", "ung123", "killok1", "namtrang", "lazetrumxd",
                        "provegeta",
                        "fuckyouz", "chuberong", "karatosdra", "gobekh", "angnguyenhai", "hainguyen96", "skill96",
                        "thiendevy",
                        "xenknopro", "linhjjjs1", "juanonkilll", "hhhh6", "ahuhuhehehe", "tp0512", "quyen47",
                        "trum1ben",
                        "nameck0i", "siimmshuxinh", "anhbasin", "bemthueb", "anhbaoultrax", "nameconbon", "xdgohan",
                        "trumbuliem", "trumksxaydu", "bossxemhiu", "hehe", "nhanazz", "congminhi2", "superkm",
                        "kamejoko",
                        "paigehicks", "zeroolionel", "kameonehit", "kikochiup", "thuetrummixi", "gakuteee",
                        "sayzzfacebook",
                        "lymouyen", "nameckhxn", "sindbabeebom", "berolalala", "minhthu", "hiadiobee", "rushaikij",
                        "adminz",
                        "minhtopfaceid", "congmingden", "debardock", "duyyybobin", "he325ace", "nrlolmesi",
                        "bcrndbigbang1",
                        "vclkakabut", "hetphepnamecvip", "gianggodxs", "nrophat", "namestdzddd", "lamoonhentaiz",
                        "bibabibo",
                        "kamnmenappa", "xaydaviip", "1000000000", "zunzunoc", "tieuzonnnn", "dichvuscam", "hoangnacno",
                        "xusditnuadi", "qtaiii1", "nhacluharuk", "yyeutd", "ibigcytyboim", "hunghehe", "mhunghuhut",
                        "anjiro100",
                        "kilogamhi", "zoneiloc", "tieu955hd", "zvltankame", "dyhieu", "admin2", "iammam", "trucxinhne",
                        "saitaman", "killer007", "onekill", "trumsvzd", "kingdz", "ahihimiuxiinh", "miuxiiinhnopro",
                        "ngendtdz", "duongquay", "phuonglzz", "mzmzmquan456", "241205eriic", "googlecak", "12131kamene",
                        "songokutrums", "aiyanquan123", "chimbebe", "dadencaube", "badaolon", "mupsunny"
        };
        public static final String[] BOT_NAME_PREFIXES = {
                        "pro", "pr0", "pro1", "provip", "sv", "no1", "100tr",
                        "top", "top1", "topone",
                        "vip", "vip1", "vippro",
                        "trum", "trum1", "trumvip",
                        "dz", "dzai", "xd", "zzz",
                        "s1", "s2", "s3",
                        "no1", "rank1", "god", "boss"
        };
        public static final String[] BOT_TEXT_CHAT = {
                        "xin pt voi ae", "cho minh pt voi", "pt nhanh ae oi",
                        "dcm game nhu cec", "game lol", "lag vc", "lag nhu cho",
                        "admin sv", "admin tk cho", "mod dau roi",
                        "ttv pt vip", "detu kiem dau the", "detu out dau",
                        "dcm tk zvitakmae nhe", "con cho admin", "pt tinhae ttv",
                        "may tk ngu", "sua cai cc", "getmap", "sua loz gi cung loi",

                        // NRO vibe
                        "co ai o td ko", "ai o khu 1 ko", "k1 lag qua ae",
                        "ai ban la 2s ko", "can mua sd3", "ban ngoc ko", "co ai doi sd ko",
                        "ai dan map ko", "ai danh boss chung ko", "boss sp dau roi ae",
                        "boss  tg 15p nua ra", "co ai san sp ko",
                        "hello ae", "chao mn", "treo may ti",
                        "vat nuoc roi", "it hp qua", "ki nang loi roi",
                        "cai nay danh ko len dame ta", "lag do sv ha", "sv nhu cc",

                        // PK + săn map
                        "ai pk ko", "vao k2 pk di", "pk nhay vao k1 nao",
                        "kho duong qua", "ai keo len dao than ko",

                        // Đệ tử + luyện
                        "ai day detu ko", "detu ngu vcl", "detu ko dam chay",
                        "ai buff khi cho", "buff khi voi ae oi",

                        // Toxic nhẹ
                        "dm may", "anh em o dau het roi", "co dau het roi ae",
                        "game gi toan tre trau", "map nay nguoi qua ae"
        };

}
