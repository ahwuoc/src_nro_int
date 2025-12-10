package nro.consts;

/**
 * Đại diện cho một bộ quần áo gồm: áo, quần, găng, giày, rada
 */
public class ClothesSet {
    public final int ao;
    public final int quan;
    public final int gang;
    public final int giay;
    public final int rada;

    public ClothesSet(int ao, int quan, int gang, int giay, int rada) {
        this.ao = ao;
        this.quan = quan;
        this.gang = gang;
        this.giay = giay;
        this.rada = rada;
    }

    /**
     * Chuyển đổi thành mảng int
     */
    public int[] toArray() {
        return new int[]{ao, quan, gang, giay, rada};
    }

    @Override
    public String toString() {
        return String.format("ClothesSet{áo=%d, quần=%d, găng=%d, giày=%d, rada=%d}", 
            ao, quan, gang, giay, rada);
    }
}
