package nro.services.mocnap;

import nro.login.LoginSession;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.player.Player;
import nro.server.ServerManager;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

/**
 * Service xử lý claim rewards mốc nạp
 * 
 * @author 💖 YTB ahwuocdz 💖
 */
public class MocnapClaimHandler {

    private static MocnapClaimHandler instance;

    public static MocnapClaimHandler gI() {
        if (instance == null) {
            instance = new MocnapClaimHandler();
        }
        return instance;
    }

    /**
     * Xử lý nhận thưởng mốc nạp
     */
    public void claimMilestone(Player player, MocnapService.MocnapMilestone milestone) {
        try {
            // 1. Kiểm tra điều kiện
            int totalRecharge = Service.CheckMocNap(player, 0) ? player.getSession().tongnap : 0;
            int claimedFlags = player.event.getMocNapDaNhan();
            boolean claimed = (claimedFlags & (1 << milestone.id)) != 0;
            boolean canClaim = totalRecharge >= milestone.require && !claimed;

            if (!canClaim) {
                Service.getInstance().sendThongBao(player, "Bạn chưa đủ điều kiện hoặc đã nhận thưởng này rồi");
                return;
            }

            // 2. Kiểm tra hành trang
            int totalItems = milestone.items.size();
            if (InventoryService.gI().getCountEmptyBag(player) < totalItems) {
                Service.getInstance().sendThongBao(player,
                        "Hành trang không đủ chỗ trống (cần " + totalItems + " ô)");
                return;
            }

            // 3. Gọi Rust server để mark claimed
            LoginSession loginSession = ServerManager.gI().getLogin();
            if (loginSession == null || !loginSession.isConnected()) {
                Service.getInstance().sendThongBao(player, "Lỗi kết nối login server");
                return;
            }

            boolean marked = loginSession.getService().markMilestoneClaimed(
                    player.getSession().userId,
                    milestone.id,
                    3000);

            if (!marked) {
                Service.getInstance().sendThongBao(player, "Lỗi lưu dữ liệu, vui lòng thử lại");
                return;
            }
            for (MocnapService.MocnapItem mocnapItem : milestone.items) {
                Item item = ItemService.gI().createNewItem((short) mocnapItem.itemId, mocnapItem.quantity);
                item.itemOptions.clear();
                for (MocnapService.MocnapOption opt : mocnapItem.options) {
                    item.itemOptions.add(new ItemOption(opt.id, opt.param));
                }
                InventoryService.gI().addItemBag(player, item, 0);
            }
            player.event.setMocNapDaNhan(claimedFlags | (1 << milestone.id));
            Service.getInstance().sendThongBao(player, "Nhận thưởng " + milestone.title + " thành công!");
            InventoryService.gI().sendItemBags(player);
            System.out.println("[MocnapClaimHandler] Player " + player.name + " claimed milestone " + milestone.id);
        } catch (Exception e) {
            e.printStackTrace();
            Service.getInstance().sendThongBao(player, "Lỗi nhận thưởng");
        }
    }
}
