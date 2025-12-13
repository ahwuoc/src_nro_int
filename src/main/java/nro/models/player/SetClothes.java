package nro.models.player;

import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.services.func.CombineServiceNew;

/**
 *
 * @author 💖 ahwuocdz 💖
 * 
 *
 */
public class SetClothes {

    private Player player;

    public SetClothes(Player player) {
        this.player = player;
    }

    public byte songoku;
    public byte thienXinHang;
    public byte kirin;

    public byte ocTieu;
    public byte pikkoroDaimao;
    public byte picolo;

    public byte kakarot;
    public byte cadic;
    public byte nappa;

    public boolean godClothes;
    public int ctHaiTac = -1;

    public byte tin_an;
    public byte nhat_an;
    public byte nguyet_an;


    /**
     * Áp dụng buff HP từ set Nhật Ấn (5 món +15% HP)
     */
    public void applyHpBuff() {
        if (this.nhat_an >= 5) {
            this.player.nPoint.hpMax += this.player.nPoint.hpMax * 15 / 100;
        }
    }
    
    /**
     * Áp dụng buff Dame từ set Tinh Ấn (5 món +15% Sát Thương)
     */
    public void applyDameBuff() {
        if (this.tin_an >= 5) {
            this.player.nPoint.dame += this.player.nPoint.dame * 15 / 100;
        }
    }
    
    /**
     * Áp dụng buff MP từ set Nguyệt Ấn (5 món +15% KI)
     */
    public void applyMpBuff() {
        if (this.nguyet_an >= 5) {
            this.player.nPoint.mpMax += this.player.nPoint.mpMax * 15 / 100;
        }
    }
    
    /**
     * Áp dụng tất cả buff từ set đồ ấn
     */
    public void applyAllBuffs() {
        applyHpBuff();
        applyDameBuff();
        applyMpBuff();
    }
    

    public void setup() {
        setDefault();
        setupSKT();
        setupAn();
        this.godClothes = true;
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id > 567 || item.template.id < 555) {
                    this.godClothes = false;
                    break;
                }
            } else {
                this.godClothes = false;
                break;
            }
        }
        Item ct = this.player.inventory.itemsBody.get(5);
        if (ct.isNotNullItem()) {
            switch (ct.template.id) {
                case 618:
                case 619:
                case 620:
                case 621:
                case 622:
                case 623:
                case 624:
                case 626:
                case 627:
                    this.ctHaiTac = ct.template.id;
                    break;
            }
        }
    }
    private void setupAn() {
    for (int i = 0; i < 5; i++) {
        Item item = this.player.inventory.itemsBody.get(i);
        if (!item.isNotNullItem()) break;

        item.itemOptions.stream()
            .map(io -> io.optionTemplate.id)
            .filter(id -> id == 34 || id == 35 || id == 36)
            .findFirst()
            .ifPresent(id -> {
                switch (id) {
                    case 34 -> tin_an++;
                    case 35 -> nguyet_an++;
                    case 36 -> nhat_an++;
                }
            });
    }
}

    private void setupSKT() {
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                boolean isActSet = false;
                for (ItemOption io : item.itemOptions) {
                    switch (io.optionTemplate.id) {
                        case 129:
                        case 141:
                            isActSet = true;
                            songoku++;
                            break;
                        case 127:
                        case 139:
                            isActSet = true;
                            thienXinHang++;
                            break;
                        case 128:
                        case 140:
                            isActSet = true;
                            kirin++;
                            break;
                        case 131:
                        case 143:
                            isActSet = true;
                            ocTieu++;
                            break;
                        case 132:
                        case 144:
                            isActSet = true;
                            pikkoroDaimao++;
                            break;
                        case 130:
                        case 142:
                            isActSet = true;
                            picolo++;
                            break;
                        case 135:
                        case 138:
                            isActSet = true;
                            nappa++;
                            break;
                        case 133:
                        case 136:
                            isActSet = true;
                            kakarot++;
                            break;
                        case 134:
                        case 137:
                            isActSet = true;
                            cadic++;
                            break;
                    }
                    if (isActSet) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    private void setDefault() {
        this.songoku = 0;
        this.thienXinHang = 0;
        this.kirin = 0;
        this.ocTieu = 0;
        this.pikkoroDaimao = 0;
        this.picolo = 0;
        this.kakarot = 0;
        this.cadic = 0;
        this.nappa = 0;
        this.godClothes = false;
        this.ctHaiTac = -1;
    }

    public void dispose() {
        this.player = null;
    }
}
