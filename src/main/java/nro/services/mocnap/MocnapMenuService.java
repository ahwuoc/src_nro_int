package nro.services.mocnap;

import nro.consts.ConstNpc;
import nro.jdbc.DBService;
import nro.models.item.ItemOptionTemplate;
import nro.models.item.ItemTemplate;
import nro.models.player.Player;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import nro.utils.Util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý UI/Menu hiển thị mốc nạp
 * 
 * @author 💖 YTB ahwuocdz 💖
 */
public class MocnapMenuService {

    private static MocnapMenuService instance;

    public static MocnapMenuService gI() {
        if (instance == null) {
            instance = new MocnapMenuService();
        }
        return instance;
    }

    public void showMainMenu(Player player) {
        try {
            try {
                Connection conn = DBService.gI().getConnectionForGame();
                boolean loaded = MocnapService.gI().loadFromDatabase(conn);
                if (!loaded) {
                    Service.getInstance().sendThongBao(player, "Không thể tải thông tin mốc nạp");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Service.getInstance().sendThongBao(player, "Lỗi kết nối database");
                return;
            }
            int totalRecharge = Service.CheckMocNap(player, 0) ? player.getSession().tongnap : 0;
            int claimedFlags = MocnapClaimHandler.gI().getClaimedFlags((int) player.id);
            StringBuilder menuText = new StringBuilder();
            menuText.append("|7|MỐC NẠP THƯỞNG\n");
            menuText.append("|1|Tổng nạp:\n");
            menuText.append("|6|").append(Util.numberToMoney(totalRecharge)).append(" VNĐ\n");
            menuText.append("|4|━━━━━━━━━━━━━━━━\n");
            menuText.append("|2|Chọn mốc để xem chi tiết:\n");

            List<MocnapService.MocnapMilestone> milestones = MocnapService.gI().getAllMilestones();
            List<String> options = new ArrayList<>();

            for (MocnapService.MocnapMilestone milestone : milestones) {
                boolean claimed = (claimedFlags & (1 << milestone.id)) != 0;
                boolean canClaim = totalRecharge >= milestone.require && !claimed;
                String status;
                if (claimed) {
                    status = "|8|[✓ Đã nhận]";
                } else if (canClaim) {
                    status = "|7|[Có thể nhận]";
                } else {
                    status = "|3|[Chưa đủ]";
                }

                menuText.append(status).append(" ").append(milestone.title).append("\n");

                // Add option button
                options.add(milestone.title);
            }

            options.add("Đóng");

            // Show menu với tempId = -1 (menu chính)
            NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_MOCNAP, -1,
                    menuText.toString(), options.toArray(new String[0]));

        } catch (Exception e) {
            e.printStackTrace();
            Service.getInstance().sendThongBao(player, "Lỗi hiển thị mốc nạp");
        }
    }

    /**
     * Hiển thị chi tiết 1 mốc nạp cụ thể
     */
    public void showMilestoneDetail(Player player, int milestoneId) {
        try {
            MocnapService.MocnapMilestone milestone = MocnapService.gI().getMilestoneById(milestoneId);
            if (milestone == null) {
                Service.getInstance().sendThongBao(player, "Không tìm thấy mốc nạp");
                return;
            }

            // Lấy tổng tiền đã nạp
            int totalRecharge = Service.CheckMocNap(player, 0) ? player.getSession().tongnap : 0;

            // Lấy các mốc đã nhận từ database
            int claimedFlags = MocnapClaimHandler.gI().getClaimedFlags((int) player.id);
            boolean claimed = (claimedFlags & (1 << milestone.id)) != 0;
            boolean canClaim = totalRecharge >= milestone.require && !claimed;

            // Build menu text
            StringBuilder menuText = new StringBuilder();
            menuText.append("|7|").append(milestone.title).append("\n");
            menuText.append("|4|━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            menuText.append("|1|Yêu cầu nạp: ").append(Util.numberToMoney(milestone.require)).append(" VNĐ\n");
            menuText.append("|1|Đã nạp:  ").append(Util.numberToMoney(totalRecharge)).append(" VNĐ\n");

            if (claimed) {
                menuText.append("|8|✓ Đã nhận phần thưởng\n");
            } else if (canClaim) {
                menuText.append("|2|✓ Đủ điều kiện nhận thưởng\n");
            } else {
                int needed = milestone.require - totalRecharge;
                menuText.append("|7|✗ Cần nạp thêm: ").append(Util.numberToMoney(needed)).append(" VNĐ\n");
            }
            menuText.append("|4|━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            menuText.append("|1|Phần thưởng:\n");

            for (MocnapService.MocnapItem item : milestone.rewards) {
                ItemTemplate template = ItemService.gI().getTemplate(item.itemId);
                if (template == null) {
                    continue;
                }
                menuText.append("• ").append(template.name).append(" x").append(item.quantity).append("\n");
                if (!item.options.isEmpty()) {
                    for (MocnapService.MocnapOption opt : item.options) {
                        if (opt.id == 21)
                            continue;
                        ItemOptionTemplate optTemplate = ItemService.gI().getItemOptionTemplate(opt.id);
                        if (optTemplate != null) {
                            String optionName = optTemplate.name.replaceAll("#", String.valueOf(opt.param));
                            menuText.append("|7|  ► ").append(optionName).append("\n");
                        }
                    }
                }
            }

            List<String> options = new ArrayList<>();
            if (canClaim) {
                options.add("Nhận thưởng");
                options.add("Từ chối");
            } else {
                options.add("Đóng");
            }
            NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_MOCNAP, milestone.id,
                    menuText.toString(), options.toArray(new String[0]));

        } catch (Exception e) {
            e.printStackTrace();
            Service.getInstance().sendThongBao(player, "Lỗi hiển thị chi tiết mốc nạp");
        }
    }

    /**
     * Xử lý confirm menu mốc nạp
     */
    public void handleMenuConfirm(Player player, int tempId, int select) {
        try {
            if (tempId == -1) {
                // Menu chính - chọn mốc để xem chi tiết
                List<MocnapService.MocnapMilestone> milestones = MocnapService.gI().getAllMilestones();
                if (select >= 0 && select < milestones.size()) {
                    showMilestoneDetail(player, milestones.get(select).id);
                }
                // Nếu select == size -> Đóng, không làm gì
            } else {
                // Menu chi tiết - xử lý nhận thưởng
                MocnapService.MocnapMilestone milestone = MocnapService.gI().getMilestoneById(tempId);
                if (milestone == null) {
                    return;
                }

                if (select == 0) {
                    // Nhận thưởng
                    MocnapClaimHandler.gI().claimMilestone(player, milestone);
                }
                // select == 1 -> Từ chối hoặc Đóng, không làm gì
            }
        } catch (Exception e) {
            e.printStackTrace();
            Service.getInstance().sendThongBao(player, "Lỗi xử lý mốc nạp");
        }
    }
}
