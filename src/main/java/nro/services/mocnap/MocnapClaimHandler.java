package nro.services.mocnap;

import nro.jdbc.DBService;
import nro.models.item.ItemOption;
import nro.models.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handler xử lý claim mốc nạp
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
     * Claim mốc nạp
     */
    public void claimMilestone(Player player, MocnapService.MocnapMilestone milestone) {
        try {
            // Check xem player đã claim chưa
            if (isAlreadyClaimed((int) player.id, milestone.id)) {
                Service.getInstance().sendThongBao(player, "Bạn đã nhận phần thưởng này rồi");
                return;
            }

            // Check xem player có đủ điều kiện không
            int totalRecharge = Service.CheckMocNap(player, 0) ? player.getSession().tongnap : 0;
            if (totalRecharge < milestone.require) {
                Service.getInstance().sendThongBao(player, "Bạn chưa đủ điều kiện nhận thưởng");
                return;
            }

            // Add items thưởng vào bag
            for (MocnapService.MocnapItem mocnapItem : milestone.rewards) {
                nro.models.item.Item item = ItemService.gI().createNewItem((short)mocnapItem.itemId, mocnapItem.quantity);
                
                // Add options
                for (MocnapService.MocnapOption opt : mocnapItem.options) {
                    item.itemOptions.add(new ItemOption(opt.id, opt.param));
                }
                
                InventoryService.gI().addItemBag(player, item, 99);
            }

            // Lưu vào database
            saveClaim((int) player.id, milestone.id);

            // Send thông báo
            InventoryService.gI().sendItemBags(player);
            Service.getInstance().sendThongBao(player, "Nhận thưởng thành công");
            
            // Refresh menu
            MocnapMenuService.gI().showMainMenu(player);
        } catch (Exception e) {
            e.printStackTrace();
            Service.getInstance().sendThongBao(player, "Lỗi nhận thưởng");
        }
    }

    /**
     * Check xem player đã claim mốc này chưa
     */
    private boolean isAlreadyClaimed(int playerId, int mocnapId) {
        try (Connection conn = DBService.gI().getConnectionForGame();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM mocnap_claimed WHERE player_id = ? AND mocnap_id = ?")) {
            ps.setInt(1, playerId);
            ps.setInt(2, mocnapId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu claim vào database
     */
    private void saveClaim(int playerId, int mocnapId) {
        try (Connection conn = DBService.gI().getConnectionForSaveData();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO mocnap_claimed (player_id, mocnap_id) VALUES (?, ?)")) {
            ps.setInt(1, playerId);
            ps.setInt(2, mocnapId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy danh sách mốc đã claim
     */
    public int getClaimedFlags(int playerId) {
        int flags = 0;
        try (Connection conn = DBService.gI().getConnectionForGame();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT mocnap_id FROM mocnap_claimed WHERE player_id = ?")) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int mocnapId = rs.getInt("mocnap_id");
                    flags |= (1 << mocnapId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flags;
    }
}
