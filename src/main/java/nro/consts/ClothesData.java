package nro.consts;

/**
 * Dữ liệu trang phục theo chủng tộc và cấp độ
 * Cấu trúc: [Chủng tộc][Cấp độ][Loại đồ]
 * 
 * @author 💖 ahwuocdz 💖
 */
public class ClothesData {

    // ==================== CHỦNG TỘC ====================
    public static final int RACE_TRAI_DAT = 0;
    public static final int RACE_NAMEK = 1;
    public static final int RACE_XAYDA = 2;

    // ==================== CẤP ĐỘ TRANG PHỤC ====================
    public static final int LEVEL_1 = 0;  // Đồ cấp 1
    public static final int LEVEL_2 = 1;  // Đồ cấp 2
    public static final int LEVEL_3 = 2;  // Đồ cấp 3
    public static final int LEVEL_4 = 3;  // Đồ cấp 4
    public static final int LEVEL_5 = 4;  // Đồ cấp 5 (chung 3 hệ)

    // ==================== LOẠI TRANG PHỤC ====================
    public static final int SLOT_AO_1 = 0;
    public static final int SLOT_AO_2 = 1;
    public static final int SLOT_QUAN_1 = 2;
    public static final int SLOT_QUAN_2 = 3;
    public static final int SLOT_GANG_1 = 4;
    public static final int SLOT_GANG_2 = 5;
    public static final int SLOT_GANG_3 = 6;
    public static final int SLOT_GANG_4 = 7;
    public static final int SLOT_GIAY_1 = 8;
    public static final int SLOT_GIAY_2 = 9;
    public static final int SLOT_GIAY_3 = 10;
    public static final int SLOT_GIAY_4 = 11;
    public static final int SLOT_RADA = 12;

    /**
     * Bảng ID trang phục
     * [Chủng tộc][Cấp độ][Loại đồ]
     * 
     * Chủng tộc: TRAI_DAT(0), NAMEK(1), XAYDA(2)
     * Cấp độ: 1-5 (index 0-4)
     * Loại đồ: Áo1, Áo2, Quần1, Quần2, Găng1-4, Giày1-4, Rada
     */
    public static final int[][][] CLOTHES = {
        // ==================== TRÁI ĐẤT ====================
        {
            // Cấp 1: Áo, Áo2, Quần, Quần2, Găng1-4, Giày1-4, Rada
            { 0, 33, 3, 34, 136, 137, 138, 139, 230, 231, 232, 233, 555 },
            // Cấp 2
            { 6, 35, 9, 36, 140, 141, 142, 143, 242, 243, 244, 245, 556 },
            // Cấp 3
            { 21, 24, 37, 38, 144, 145, 146, 147, 254, 255, 256, 257, 562 },
            // Cấp 4
            { 27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269, 563 },
            // Cấp 5 (chung)
            { 12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561 }
        },
        // ==================== NAMEK ====================
        {
            // Cấp 1
            { 1, 41, 4, 42, 152, 153, 154, 155, 234, 235, 236, 237, 557 },
            // Cấp 2
            { 7, 43, 10, 44, 156, 157, 158, 159, 246, 247, 248, 249, 558 },
            // Cấp 3
            { 22, 46, 25, 45, 160, 161, 162, 163, 258, 259, 260, 261, 564 },
            // Cấp 4
            { 28, 47, 31, 48, 164, 165, 166, 167, 270, 271, 272, 273, 565 },
            // Cấp 5 (chung)
            { 12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561 }
        },
        // ==================== XAYDA ====================
        {
            // Cấp 1
            { 2, 49, 5, 50, 168, 169, 170, 171, 238, 239, 240, 241, 559 },
            // Cấp 2
            { 8, 51, 11, 52, 172, 173, 174, 175, 250, 251, 252, 253, 560 },
            // Cấp 3
            { 23, 53, 26, 54, 176, 177, 178, 179, 262, 263, 264, 265, 566 },
            // Cấp 4
            { 29, 55, 32, 56, 180, 181, 182, 183, 274, 275, 276, 277, 567 },
            // Cấp 5 (chung)
            { 12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561 }
        }
    };

    /**
     * Lấy ID item theo chủng tộc, cấp độ và slot
     */
    public static int getItemId(int race, int level, int slot) {
        if (race < 0 || race > 2) return -1;
        if (level < 0 || level > 4) return -1;
        if (slot < 0 || slot > 12) return -1;
        return CLOTHES[race][level][slot];
    }

    /**
     * Lấy ID áo theo chủng tộc và cấp độ
     */
    public static int getAoId(int race, int level) {
        return getItemId(race, level, SLOT_AO_1);
    }

    /**
     * Lấy ID quần theo chủng tộc và cấp độ
     */
    public static int getQuanId(int race, int level) {
        return getItemId(race, level, SLOT_QUAN_1);
    }

    /**
     * Lấy ID găng theo chủng tộc, cấp độ và index (0-3)
     */
    public static int getGangId(int race, int level, int index) {
        return getItemId(race, level, SLOT_GANG_1 + index);
    }

    /**
     * Lấy ID giày theo chủng tộc, cấp độ và index (0-3)
     */
    public static int getGiayId(int race, int level, int index) {
        return getItemId(race, level, SLOT_GIAY_1 + index);
    }

    /**
     * Lấy ID rada theo chủng tộc và cấp độ
     */
    public static int getRadaId(int race, int level) {
        return getItemId(race, level, SLOT_RADA);
    }
}
