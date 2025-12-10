package nro.models.item;

import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.services.ItemTimeService;
import nro.services.Service;
import nro.utils.Util;

/**
 * @author 💖 ahwuocdz 💖
 * 
 */
public class ItemTime {

    // id item text
    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;

    public static final byte TEXT_NHIEM_VU_HANG_NGAY = 2;

    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 3;

    public static final byte KHI_GAS = 4;

    public static final int TIME_ITEM = 600000;
    public static final int TIME_OPEN_POWER = 86400000;
    public static final int TIME_MAY_DO = 1800000;
    public static final int TIME_EAT_MEAL = 600000;

    public static final int TIME_DANH_NHAN_BAN = 300000;
    public static final int TIME_LUCKY_PER_USE = 600000; // 10 phút mỗi lần dùng = 600000ms
    public static final int TIME_LUCKY_MAX = 10800000; // Tối đa 3 tiếng = 10800000ms
    public static final int TIME_BUFF_TNSM_PER_USE = 600000; // 10 phút mỗi lần dùng = 600000ms
    public static final int TIME_BUFF_TNSM_MAX = 3600000; // Tối đa 1 tiếng = 3600000ms

    private Player player;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;

    public boolean isUseBanhChung;
    public boolean isUseBanhTet;
    public long lastTimeBanhChung;
    public long lastTimeBanhTet;
    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseLucky; // Item Lucky x2 tỉ lệ drop
    public long luckyTimeRemaining; // Thời gian Lucky còn lại (ms), có thể cộng dồn tối đa 3 tiếng
    
    // Buff TNSM x5
    public boolean isUseBuffX5TNSM;
    public long buffX5TNSMTimeRemaining;
    public long lastTimeBuffX5TNSM;
    
    // Buff TNSM x10
    public boolean isUseBuffX10TNSM;
    public long buffX10TNSMTimeRemaining;
    public long lastTimeBuffX10TNSM;
    
    // Buff TNSM x15
    public boolean isUseBuffX15TNSM;
    public long buffX15TNSMTimeRemaining;
    public long lastTimeBuffX15TNSM;
    
    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;
    public long lastTimeLucky;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isDanhNhanBan;
    public boolean doneDanhNhanBan = false;
    public long lasttimeDanhNhanBan;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        boolean update = false;
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL)) {
                isEatMeal = false;
                update = true;
            }
        }
        if (isDanhNhanBan) {
            if (Util.canDoWithTime(lasttimeDanhNhanBan, TIME_DANH_NHAN_BAN)) {
                isDanhNhanBan = false;
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                update = true;
            }
        }
        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                update = true;
            }
        }
        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                update = true;
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
            }
        }
        if (isUseLucky) {
            long elapsed = System.currentTimeMillis() - lastTimeLucky;
            if (elapsed >= luckyTimeRemaining) {
                isUseLucky = false;
                luckyTimeRemaining = 0;
                Service.getInstance().sendThongBao(player, "Hiệu ứng Lucky đã hết hạn!");
            }
        }
        // Buff x5 TNSM
        if (isUseBuffX5TNSM) {
            long elapsed = System.currentTimeMillis() - lastTimeBuffX5TNSM;
            if (elapsed >= buffX5TNSMTimeRemaining) {
                isUseBuffX5TNSM = false;
                buffX5TNSMTimeRemaining = 0;
                Service.getInstance().sendThongBao(player, "Hiệu ứng Buff x5 TNSM đã hết hạn!");
            }
        }
        // Buff x10 TNSM
        if (isUseBuffX10TNSM) {
            long elapsed = System.currentTimeMillis() - lastTimeBuffX10TNSM;
            if (elapsed >= buffX10TNSMTimeRemaining) {
                isUseBuffX10TNSM = false;
                buffX10TNSMTimeRemaining = 0;
                Service.getInstance().sendThongBao(player, "Hiệu ứng Buff x10 TNSM đã hết hạn!");
            }
        }
        // Buff x15 TNSM
        if (isUseBuffX15TNSM) {
            long elapsed = System.currentTimeMillis() - lastTimeBuffX15TNSM;
            if (elapsed >= buffX15TNSMTimeRemaining) {
                isUseBuffX15TNSM = false;
                buffX15TNSMTimeRemaining = 0;
                Service.getInstance().sendThongBao(player, "Hiệu ứng Buff x15 TNSM đã hết hạn!");
            }
        }
        if (isUseBanhChung) {
            if (Util.canDoWithTime(lastTimeBanhChung, TIME_ITEM)) {
                isUseBanhChung = false;
            }
        }
        if (isUseBanhTet) {
            if (Util.canDoWithTime(lastTimeBanhTet, TIME_ITEM)) {
                isUseBanhTet = false;
            }
        }
        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM)) {
                isUseBoHuyet2 = false;
                update = true;
            }
        }
        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM)) {
                isUseBoKhi2 = false;
                update = true;
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM)) {
                isUseGiapXen2 = false;
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM)) {
                isUseCuongNo2 = false;
                update = true;
            }
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                player.nPoint.initPowerLimit();
                Service.getInstance().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO)) {
                isUseMayDo = false;
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
            }
        }
        if (isUseBanhChung) {
            if (Util.canDoWithTime(lastTimeBanhChung, TIME_ITEM)) {
                isUseBanhChung = false;
                update = true;
            }
        }
        if (isUseBanhTet) {
            if (Util.canDoWithTime(lastTimeBanhTet, TIME_ITEM)) {
                isUseBanhTet = false;
                update = true;
            }
        }
        if (update) {
            Service.getInstance().point(player);
        }
    }

    public void dispose() {
        this.player = null;
    }
}
