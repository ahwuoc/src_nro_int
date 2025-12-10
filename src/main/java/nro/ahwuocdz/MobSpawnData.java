package nro.ahwuocdz;

public class MobSpawnData {
    public int tempId;
    public int hp;
    public int damage;
    public int x;
    public int y;
    public int pDame;
    public int pTiemNang;
    public int status;
    public int lastTimeDie;
    public int expReward;
    public int specialChance;

    public MobSpawnData(int tempId, int hp, int damage, int x, int y, 
                        int pDame, int pTiemNang, int status, 
                        int lastTimeDie, int expReward, int specialChance) {
        this.tempId = tempId;
        this.hp = hp;
        this.damage = damage;
        this.x = x;
        this.y = y;
        this.pDame = pDame;
        this.pTiemNang = pTiemNang;
        this.status = status;
        this.lastTimeDie = lastTimeDie;
        this.expReward = expReward;
        this.specialChance = specialChance;
    }
}
