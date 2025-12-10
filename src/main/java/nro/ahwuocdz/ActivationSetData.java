package nro.ahwuocdz;

/**
 * Class đại diện cho dữ liệu kích hoạt set đồ
 * Thay thế cho mảng int[][][] ACTIVATION_SET
 */
public class ActivationSetData {
    
    private final int optionId;        // ID option set (tên set)
    private final int effectId;        // ID hiệu ứng set
    private final int ratio;           // Tỉ lệ
    private final int ratioType;       // Type tỉ lệ
    private final String setName;      // Tên set (để dễ debug)
    
    // Gender constants
    public static final int GENDER_TRAI_DAT = 0;   // Trái Đất
    public static final int GENDER_NAMEK = 1;       // Namek
    public static final int GENDER_XAYDA = 2;       // Xayda
    
    // Dữ liệu set kích hoạt theo gender [gender][index]
    private static final ActivationSetData[][] ACTIVATION_SETS = {
        // Gender 0 - Trái Đất: Songoku, Thiên Xin Hàng, Kirin
        {
            new ActivationSetData(129, 141, 1, 1000, "Songoku"),
            new ActivationSetData(127, 139, 1, 1000, "Thiên Xin Hàng"),
            new ActivationSetData(128, 140, 1, 1000, "Kirin")
        },
        // Gender 1 - Namek: Ốc Tiêu, Pikkoro Daimao, Picolo
        {
            
            new ActivationSetData(132, 144, 1, 1000, "Pikkoro Daimao"),
            new ActivationSetData(130, 142, 1, 1000, "Picolo"),
            new ActivationSetData(131, 143, 1, 1000, "Ốc Tiêu")
        },
        // Gender 2 - Xayda: Kakarot, Cadic, Nappa
        {
            new ActivationSetData(135, 138, 1, 1000, "Nappa"),
            new ActivationSetData(133, 136, 1, 1000, "Kakarot"),
            new ActivationSetData(134, 137, 1, 1000, "Cadic")
        }
    };
    
    public ActivationSetData(int optionId, int effectId, int ratio, int ratioType, String setName) {
        this.optionId = optionId;
        this.effectId = effectId;
        this.ratio = ratio;
        this.ratioType = ratioType;
        this.setName = setName;
    }
    
    public ActivationSetData(int optionId, int effectId, int ratio, int ratioType) {
        this(optionId, effectId, ratio, ratioType, "");
    }
    
    // Getters
    public int getOptionId() {
        return optionId;
    }
    
    public int getEffectId() {
        return effectId;
    }
    
    public int getRatio() {
        return ratio;
    }
    
    public int getRatioType() {
        return ratioType;
    }
    
    public String getSetName() {
        return setName;
    }
    
    /**
     * Lấy ActivationSetData theo gender và index
     * @param gender 0=Trái Đất, 1=Namek, 2=Xayda
     * @param index 0-2
     */
    public static ActivationSetData get(int gender, int index) {
        if (gender < 0 || gender >= ACTIVATION_SETS.length) {
            return null;
        }
        if (index < 0 || index >= ACTIVATION_SETS[gender].length) {
            return null;
        }
        return ACTIVATION_SETS[gender][index];
    }
    
    /**
     * Lấy tất cả set của một gender
     */
    public static ActivationSetData[] getByGender(int gender) {
        if (gender < 0 || gender >= ACTIVATION_SETS.length) {
            return new ActivationSetData[0];
        }
        return ACTIVATION_SETS[gender];
    }
    
    /**
     * Tìm ActivationSetData theo optionId
     */
    public static ActivationSetData findByOptionId(int optionId) {
        for (ActivationSetData[] group : ACTIVATION_SETS) {
            for (ActivationSetData data : group) {
                if (data.getOptionId() == optionId) {
                    return data;
                }
            }
        }
        return null;
    }
    
    /**
     * Tìm ActivationSetData theo effectId
     */
    public static ActivationSetData findByEffectId(int effectId) {
        for (ActivationSetData[] group : ACTIVATION_SETS) {
            for (ActivationSetData data : group) {
                if (data.getEffectId() == effectId) {
                    return data;
                }
            }
        }
        return null;
    }
    
    /**
     * Số lượng gender
     */
    public static int getGenderCount() {
        return ACTIVATION_SETS.length;
    }
    
    /**
     * Số lượng set trong một gender
     */
    public static int getSetCount(int gender) {
        if (gender < 0 || gender >= ACTIVATION_SETS.length) {
            return 0;
        }
        return ACTIVATION_SETS[gender].length;
    }
    
    @Override
    public String toString() {
        return String.format("ActivationSetData{name='%s', optionId=%d, effectId=%d, ratio=%d, ratioType=%d}",
                setName, optionId, effectId, ratio, ratioType);
    }
}
