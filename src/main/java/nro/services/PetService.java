package nro.services;

import nro.consts.ConstPlayer;
import nro.consts.PetStats;
import nro.consts.PetStatsData;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.utils.SkillUtil;
import nro.utils.Util;

/**
 * @author 💖 ahwuocdz 💖
 */
public class PetService {

    private static PetService i;

    public static PetService gI() {
        if (i == null) {
            i = new PetService();
        }
        return i;
    }

    /**
     * Tạo pet theo loại
     * @param player Player
     * @param petType Loại pet (NONE, MABU, SAYAN5, CELL_BAO, BILL_NHI, FIDE_TRAU, SUPER_PICOLO)
     * @param limitPower Giới hạn sức mạnh (optional)
     */
    public void createPet(Player player, Pet.PetType petType, byte... limitPower) {
        createPetWithGender(player, petType, null, limitPower);
    }

    /**
     * Tạo pet theo loại với gender cụ thể
     * @param player Player
     * @param petType Loại pet
     * @param gender Gender (0, 1, 2) hoặc null để random
     * @param limitPower Giới hạn sức mạnh (optional)
     */
    public void createPetWithGender(Player player, Pet.PetType petType, Byte gender, byte... limitPower) {
        new Thread(() -> {
            try {
                createNewPetByType(player, petType, gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                    player.pet.nPoint.initPowerLimit();
                }
                Thread.sleep(1000);
                String message = petType == Pet.PetType.MABU ? "Oa oa oa..." : "Xin hãy thu nhận làm đệ tử";
                Service.getInstance().chatJustForMe(player, player.pet, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Thay đổi loại pet
     * @param player Player
     * @param petType Loại pet mới
     */
    public void changePet(Player player, Pet.PetType petType) {
        changePet(player, petType, null);
    }

    /**
     * Thay đổi loại pet với gender cụ thể
     * @param player Player
     * @param petType Loại pet mới
     * @param gender Gender (0, 1, 2) hoặc null để giữ nguyên
     */
    public void changePet(Player player, Pet.PetType petType, Byte gender) {
        try {
            byte limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                player.pet.unFusion();
            }
            MapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
            
            if (gender != null) {
                createPetWithGender(player, petType, gender, limitPower);
            } else {
                createPet(player, petType, limitPower);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Backward compatibility
    public void changeNormalPet(Player player, int gender) {
        changePet(player, Pet.PetType.NONE, (byte) gender);
    }

    public void changeNormalPet(Player player) {
        changePet(player, Pet.PetType.NONE);
    }

    public void changeMabuPet(Player player) {
        changePet(player, Pet.PetType.MABU);
    }

    public void changeMabuPet(Player player, int gender) {
        changePet(player, Pet.PetType.MABU, (byte) gender);
    }

    public void changeSuperPet(Player player, int gender, Pet.PetType petType) {
        changePet(player, petType, (byte) gender);
    }

    public void changeNamePet(Player player, String name) {
        if (!InventoryService.gI().existItemBag(player, 400)) {
            Service.getInstance().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
            return;
        } else if (Util.haveSpecialCharacter(name)) {
            Service.getInstance().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
            return;
        } else if (name.length() > 10) {
            Service.getInstance().sendThongBao(player, "Tên quá dài");
            return;
        }
        MapService.gI().exitMap(player.pet);
        player.pet.name = "$" + name.toLowerCase().trim();
        InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBagByTemp(player, 400), 1);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Service.getInstance().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
            } catch (Exception e) {
            }
        }).start();
    }



    /**
     * Tạo pet mới theo PetType enum
     */
    private void createNewPetByType(Player player, Pet.PetType petType, Byte gender) {
        boolean isMabu = petType == Pet.PetType.MABU;
        boolean isBulo = petType == Pet.PetType.SAYAN5;
        boolean isCellBao = petType == Pet.PetType.CELL_BAO;
        boolean isBillNhi = petType == Pet.PetType.BILL_NHI;
        boolean isFideTrau = petType == Pet.PetType.FIDE_TRAU;
        boolean isSuperPicolo = petType == Pet.PetType.SUPER_PICOLO;

        byte genderByte = gender != null ? gender : (byte) Util.nextInt(0, 2);
        createNewPet(player, isMabu, isBulo, isCellBao, isBillNhi, isFideTrau, isSuperPicolo, genderByte);
    }

    public void createNewPet(Player player, boolean isMabu, boolean isBulo, boolean isCellBao, boolean isBillNhi,
            boolean isFideTrau, boolean isSuperPicolo, byte... gender) {
        Pet pet = new Pet(player);
        pet.isMabu = isMabu;
        pet.isBulo = isBulo;
        pet.isCellBao = isCellBao;
        pet.isBillNhi = isBillNhi;
        pet.isFideTrau = isFideTrau;
        pet.isSuperPicolo = isSuperPicolo;

        // Lấy stats từ PetStatsData
        PetStats stats = PetStatsData.getStats(isMabu, isBulo, isCellBao, isBillNhi, isFideTrau, isSuperPicolo);

        pet.name = "$" + pet.getPetType().getDisplayName();
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.nPoint.power = isMabu || isBulo || isCellBao || isBillNhi || isFideTrau || isSuperPicolo ? 1500000 : 2000;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = stats.hp;
        pet.nPoint.mpg = stats.mp;
        pet.nPoint.dameg = stats.dame;
        pet.nPoint.defg = stats.def;
        pet.nPoint.critg = stats.crit;
        
        for (int i = 0; i < 6; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 3; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.calPoint();
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    // Backward compatibility methods
    public void createNormalPet(Player player) {
        createPet(player, Pet.PetType.NONE);
    }

    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createPetWithGender(player, Pet.PetType.NONE, (byte) gender, limitPower);
    }

    public void createMabuPet(Player player, int gender) {
        createPetWithGender(player, Pet.PetType.MABU, (byte) gender);
    }
}
