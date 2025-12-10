package nro.ahwuocdz;

import nro.consts.ConstItem;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.server.Manager;
import nro.services.EffectSkillService;
import nro.services.RewardService;
import nro.services.Service;
import nro.utils.Util;

public class boss_world extends Boss {
    public boss_world() {
        super(BossFactory.BOSS_WORLD, BossData.BOSS_WORLD);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void rewards(Player pl) {
        int location_x = this.location.x;
        int location_y = this.zone.map.yPhysicInTop(location_x, this.location.y - 24);

        for (int i = 0; i < 5; i++) {
            int dropX = location_x + (i * 10);
            ItemMap itemMap = new ItemMap(this.zone, Util.nextInt(1945, 1946), 1, dropX, location_y, -1);
            Service.getInstance().dropItemMap(zone, itemMap);
        }
        for (int i = 0; i < 10; i++) {
            int dropX = location_x + (i * 10);
            ItemMap itemMap = new ItemMap(this.zone, Util.nextInt(15, 17), 1, dropX, location_y, -1);
            Service.getInstance().dropItemMap(zone, itemMap);
        }
        ItemMap itemMap = null;
        if (Util.isTrueDrop(10, 100, pl)) {
            int[] set1 = { 562, 564, 566, 561 };
            itemMap = new ItemMap(this.zone, set1[Util.nextInt(0, set1.length - 1)], 1, location_x, location_y, pl.id);
            RewardService.gI().initBaseOptionClothes(itemMap.itemTemplate.id, itemMap.itemTemplate.type,
                    itemMap.options);
            RewardService.gI().initStarOption(itemMap, new RewardService.RatioStar[] {
                    new RewardService.RatioStar((byte) 1, 1, 2),
                    new RewardService.RatioStar((byte) 2, 1, 3),
                    new RewardService.RatioStar((byte) 3, 1, 4),
                    new RewardService.RatioStar((byte) 4, 1, 5),
                    new RewardService.RatioStar((byte) 5, 1, 6),
                    new RewardService.RatioStar((byte) 6, 1, 7),
                    new RewardService.RatioStar((byte) 7, 1, 8) });
        } else if (Util.isTrueDrop(10, 100, pl)) {
            int[] set2 = { 555, 556, 563, 557, 558, 565, 559, 567, 560 };
            itemMap = new ItemMap(this.zone, set2[Util.nextInt(0, set2.length - 1)], 1, location_x, location_y, pl.id);
            RewardService.gI().initBaseOptionClothes(itemMap.itemTemplate.id, itemMap.itemTemplate.type,
                    itemMap.options);
            RewardService.gI().initStarOption(itemMap, new RewardService.RatioStar[] {
                    new RewardService.RatioStar((byte) 1, 1, 2),
                    new RewardService.RatioStar((byte) 2, 1, 3),
                    new RewardService.RatioStar((byte) 3, 1, 4),
                    new RewardService.RatioStar((byte) 4, 1, 5),
                    new RewardService.RatioStar((byte) 5, 1, 6),
                    new RewardService.RatioStar((byte) 6, 1, 7),
                    new RewardService.RatioStar((byte) 7, 1, 8) });
        } else if (Util.isTrueDrop(1, 5, pl)) {
            itemMap = new ItemMap(this.zone, 15, 1, location_x, location_y, pl.id);
        } else if (Util.isTrueDrop(1, 2, pl)) {
            itemMap = new ItemMap(this.zone, 16, 1, location_x, location_y, pl.id);
        }

        if (Manager.EVENT_SEVER == 4 && itemMap == null) {
            itemMap = new ItemMap(this.zone,
                    ConstItem.LIST_ITEM_NLSK_TET_2023[Util.nextInt(0, ConstItem.LIST_ITEM_NLSK_TET_2023.length - 1)], 1,
                    location_x, location_y, pl.id);
            itemMap.options.add(new ItemOption(74, 0));
        }

        if (itemMap != null) {
            Service.getInstance().dropItemMap(zone, itemMap);
        }
    }

    @Override
    public void idle() {

    }

    @Override
    public void checkPlayerDie(Player pl) {

    }

    @Override
    public int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage / 2);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            damage = methodBeforeGetDamage(damage, plAtt);
            damage = damage / 10;
            this.nPoint.subHP(damage);
            if (isDie()) {
                rewards(plAtt);
                notifyPlayeKill(plAtt);
                die();
            }
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void initTalk() {

    }

    @Override
    public void leaveMap() {
        this.setJustRestToFuture();
        super.leaveMap();
    }
}
