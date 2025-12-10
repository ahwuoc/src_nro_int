package nro.consts;

import java.util.Map;

/**
 * Quản lý dữ liệu item clothes theo gender và level
 */
public class ItemClothesData {

    public enum Gender {
        TRAI_DAT,
        NAMEK,
        XAYDA
    }

    public enum Level {
        CAP_1, CAP_2, CAP_3, CAP_4, CAP_5
    }

    /**
     * Lớp đại diện cho một bộ quần áo (áo, quần, găng, giày, rada)
     */
    public static class ClothesSet {
        private final int ao;
        private final int quan;
        private final int gang;
        private final int giay;
        private final int rada;

        public ClothesSet(int ao, int quan, int gang, int giay, int rada) {
            this.ao = ao;
            this.quan = quan;
            this.gang = gang;
            this.giay = giay;
            this.rada = rada;
        }

        public int getAo() { return ao; }
        public int getQuan() { return quan; }
        public int getGang() { return gang; }
        public int getGiay() { return giay; }
        public int getRada() { return rada; }

        public int[] toArray() {
            return new int[]{ao, quan, gang, giay, rada};
        }
    }

    private static final Map<Gender, Map<Level, ClothesSet>> DATA = Map.of(
        Gender.TRAI_DAT, Map.of(
            Level.CAP_1, new ClothesSet(ItemId.ITEM_0_AO_VAI_3_LO, ItemId.ITEM_6_QUAN_VAI_EN, ItemId.ITEM_21_GANG_VAI_EN, ItemId.ITEM_27_GIAY_NHUA, ItemId.ITEM_12_RADA_CAP_1),
            Level.CAP_2, new ClothesSet(ItemId.ITEM_33_AO_THUN_3_LO, ItemId.ITEM_35_QUAN_THUN_EN, ItemId.ITEM_24_GANG_THUN_EN, ItemId.ITEM_30_GIAY_CAO_SU, ItemId.ITEM_57_RADA_CAP_2),
            Level.CAP_3, new ClothesSet(ItemId.ITEM_3_AO_VAI_DAY, ItemId.ITEM_9_QUAN_VAI_DAY, ItemId.ITEM_37_GANG_VAI_DAY, ItemId.ITEM_36_QUAN_THUN_DAY, ItemId.ITEM_59_RADA_CAP_4),
            Level.CAP_4, new ClothesSet(ItemId.ITEM_34_AO_THUN_DAY, ItemId.ITEM_36_QUAN_THUN_DAY, ItemId.ITEM_38_GANG_THUN_DAY, ItemId.ITEM_40_GIAY_CAO_SU_E_DAY, ItemId.ITEM_186_RADA_CAP_7),
            Level.CAP_5, new ClothesSet(ItemId.ITEM_230_AO_BAC_GOKU, ItemId.ITEM_242_QUAN_BAC_GOKU, ItemId.ITEM_254_GANG_BAC_GOKU, ItemId.ITEM_266_GIAY_BAC_GOKU, ItemId.ITEM_187_RADA_CAP_8)
        ),
        Gender.NAMEK, Map.of(
            Level.CAP_1, new ClothesSet(ItemId.ITEM_1_AO_SOI_LEN, ItemId.ITEM_7_QUAN_SOI_LEN, ItemId.ITEM_22_GANG_SOI_LEN, ItemId.ITEM_28_GIAY_SOI_LEN, ItemId.ITEM_12_RADA_CAP_1),
            Level.CAP_2, new ClothesSet(ItemId.ITEM_41_AO_SOI_GAI, ItemId.ITEM_43_QUAN_SOI_GAI, ItemId.ITEM_46_GANG_SOI_GAI, ItemId.ITEM_47_GIAY_SOI_GAI, ItemId.ITEM_57_RADA_CAP_2),
            Level.CAP_3, new ClothesSet(ItemId.ITEM_4_AO_LEN_PICO, ItemId.ITEM_10_QUAN_VAI_THO_PICO, ItemId.ITEM_25_GANG_LEN_PICO, ItemId.ITEM_31_GIAY_NHUA_PICO, ItemId.ITEM_59_RADA_CAP_4),
            Level.CAP_4, new ClothesSet(ItemId.ITEM_42_AO_THUN_PICO, ItemId.ITEM_44_QUAN_THUN_PICO, ItemId.ITEM_45_GANG_THUN_PICO, ItemId.ITEM_48_GIAY_CAO_SU_PICO, ItemId.ITEM_186_RADA_CAP_7),
            Level.CAP_5, new ClothesSet(ItemId.ITEM_234_AO_SAT_TRON, ItemId.ITEM_246_QUAN_SAT_TRON, ItemId.ITEM_258_GANG_SAT_TRON, ItemId.ITEM_270_GIAY_SAT_TRON, ItemId.ITEM_187_RADA_CAP_8)
        ),
        Gender.XAYDA, Map.of(
            Level.CAP_1, new ClothesSet(ItemId.ITEM_2_AO_VAI_THO, ItemId.ITEM_8_QUAN_VAI_THO, ItemId.ITEM_23_GANG_VAI_THO, ItemId.ITEM_29_GIAY_VAI_THO, ItemId.ITEM_12_RADA_CAP_1),
            Level.CAP_2, new ClothesSet(ItemId.ITEM_49_AO_THUN_THO, ItemId.ITEM_51_QUAN_THUN_THO, ItemId.ITEM_53_GANG_THUN_THO, ItemId.ITEM_55_GIAY_CAO_SU_THO, ItemId.ITEM_57_RADA_CAP_2),
            Level.CAP_3, new ClothesSet(ItemId.ITEM_5_AO_GIAP_SAT, ItemId.ITEM_11_QUAN_GIAP_SAT, ItemId.ITEM_26_GANG_SAT, ItemId.ITEM_32_GIAY_SAT, ItemId.ITEM_59_RADA_CAP_4),
            Level.CAP_4, new ClothesSet(ItemId.ITEM_50_AO_GIAP_ONG, ItemId.ITEM_52_QUAN_GIAP_ONG, ItemId.ITEM_54_GANG_ONG, ItemId.ITEM_56_GIAY_ONG, ItemId.ITEM_186_RADA_CAP_7),
            Level.CAP_5, new ClothesSet(ItemId.ITEM_238_AO_LONG_O, ItemId.ITEM_250_QUAN_LONG_O, ItemId.ITEM_262_GANG_LONG_O, ItemId.ITEM_274_GIAY_LONG_O, ItemId.ITEM_187_RADA_CAP_8)
        )
    );

    /**
     * Lấy bộ quần áo theo gender và level
     * @param gender Gender enum
     * @param level Level enum
     * @return ClothesSet
     */
    public static ClothesSet getClothes(Gender gender, Level level) {
        return DATA.getOrDefault(gender, Map.of()).get(level);
    }

    /**
     * Lấy bộ quần áo theo int gender (0=Trai Đất, 1=Namek, 2=Xayda) và int level (1-5)
     * @param genderInt 0=Trai Đất, 1=Namek, 2=Xayda
     * @param levelInt 1-5
     * @return ClothesSet
     */
    public static ClothesSet getClothes(int genderInt, int levelInt) {
        if (genderInt < 0 || genderInt > 2 || levelInt < 1 || levelInt > 5) {
            return null;
        }
        Gender gender = Gender.values()[genderInt];
        Level level = Level.values()[levelInt - 1];
        return getClothes(gender, level);
    }

    /**
     * Lấy mảng int từ bộ quần áo
     * @param gender Gender enum
     * @param level Level enum
     * @return mảng [áo, quần, găng, giày, rada]
     */
    public static int[] getClothesArray(Gender gender, Level level) {
        ClothesSet set = getClothes(gender, level);
        return set != null ? set.toArray() : null;
    }

    /**
     * Lấy mảng int từ bộ quần áo
     * @param genderInt 0=Trai Đất, 1=Namek, 2=Xayda
     * @param levelInt 1-5
     * @return mảng [áo, quần, găng, giày, rada]
     */
    public static int[] getClothesArray(int genderInt, int levelInt) {
        ClothesSet set = getClothes(genderInt, levelInt);
        return set != null ? set.toArray() : null;
    }
}
