package nro.consts;

/**
 * Quản lý chỉ số của pet
 * Cấu trúc: [hp, mp, dame, def, crit]
 */
public class PetStats {
    public final int hp;
    public final int mp;
    public final int dame;
    public final int def;
    public final int crit;

    public PetStats(int hp, int mp, int dame, int def, int crit) {
        this.hp = hp;
        this.mp = mp;
        this.dame = dame;
        this.def = def;
        this.crit = crit;
    }

    public int[] toArray() {
        return new int[]{hp, mp, dame, def, crit};
    }

    @Override
    public String toString() {
        return String.format("PetStats{hp=%d, mp=%d, dame=%d, def=%d, crit=%d}", 
            hp, mp, dame, def, crit);
    }
}
