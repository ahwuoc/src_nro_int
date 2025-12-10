package nro.consts;

import nro.models.player.Pet;
import nro.utils.Util;

/**
 * Quản lý chỉ số của các loại pet
 */
public class PetStatsData {

    // Pet thường
    public static PetStats getNormalPetStats() {
        int hp = Util.nextInt(40, 105) * 20;
        int mp = Util.nextInt(40, 105) * 20;
        int dame = Util.nextInt(20, 45);
        int def = Util.nextInt(9, 50);
        int crit = Util.nextInt(0, 2);
        return new PetStats(hp, mp, dame, def, crit);
    }

    // Pet Mabư
    public static PetStats getMabuPetStats() {
        int hp = Util.nextInt(40, 105) * 20;
        int mp = Util.nextInt(40, 105) * 20;
        int dame = Util.nextInt(50, 120);
        int def = Util.nextInt(9, 50);
        int crit = Util.nextInt(0, 2);
        return new PetStats(hp, mp, dame, def, crit);
    }

    // Pet siêu (Bulo, CellBao, BillNhi, FideTrau, SuperPicolo)
    public static PetStats getSuperPetStats() {
        int hp = Util.nextInt(40, 105) * 20;
        int mp = Util.nextInt(40, 105) * 20;
        int dame = Util.nextInt(50, 120);
        int def = Util.nextInt(9, 200);
        int crit = Util.nextInt(0, 2);
        return new PetStats(hp, mp, dame, def, crit);
    }

    /**
     * Lấy stats theo loại pet
     */
    public static PetStats getStats(boolean isMabu, boolean isBulo, boolean isCellBao, 
                                     boolean isBillNhi, boolean isFideTrau, boolean isSuperPicolo) {
        if (isMabu) {
            return getMabuPetStats();
        }
        if (isBulo || isCellBao || isBillNhi || isFideTrau || isSuperPicolo) {
            return getSuperPetStats();
        }
        return getNormalPetStats();
    }

    /**
     * Lấy stats theo PetType
     */
    public static PetStats getStats(Pet.PetType petType) {
        return switch (petType) {
            case MABU -> getMabuPetStats();
            case SAYAN5, CELL_BAO, BILL_NHI, FIDE_TRAU, SUPER_PICOLO -> getSuperPetStats();
            default -> getNormalPetStats();
        };
    }
}
