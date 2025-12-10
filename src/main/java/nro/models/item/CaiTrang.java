package nro.models.item;

/**
 * Đại diện cho một bộ cải trang với head, body, leg, bag
 * @author Kitak
 */
public class CaiTrang {
    public final int tempId;
    public final int head;
    public final int body;
    public final int leg;
    public final int bag;

    public CaiTrang(int tempId, int head, int body, int leg, int bag) {
        this.tempId = tempId;
        this.head = head;
        this.body = body;
        this.leg = leg;
        this.bag = bag;
    }

    /**
     * Lấy mảng ID [head, body, leg, bag]
     */
    public int[] getID() {
        return new int[]{head, body, leg, bag};
    }

    public int getHead() {
        return head;
    }

    public int getBody() {
        return body;
    }

    public int getLeg() {
        return leg;
    }

    public int getBag() {
        return bag;
    }

    @Override
    public String toString() {
        return String.format("CaiTrang{tempId=%d, head=%d, body=%d, leg=%d, bag=%d}", 
            tempId, head, body, leg, bag);
    }
}
