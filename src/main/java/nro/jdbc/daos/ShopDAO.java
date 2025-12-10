package nro.jdbc.daos;

import nro.models.item.ItemOption;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.services.ItemService;
import nro.utils.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 💖 ahwuocdz 💖
 * 
 *
 */
public class ShopDAO {

    public static List<Shop> getShops(Connection con) {
        List<Shop> shops = new ArrayList<>();
        try {
            // Load all shops
            PreparedStatement psShops = con.prepareStatement("SELECT * FROM shop ORDER BY npc_id ASC, shop_order ASC");
            ResultSet rsShops = psShops.executeQuery();
            while (rsShops.next()) {
                Shop shop = new Shop();
                shop.id = rsShops.getInt("id");
                shop.npcId = rsShops.getByte("npc_id");
                shop.shopOrder = rsShops.getByte("shop_order");
                shops.add(shop);
            }
            rsShops.close();
            psShops.close();

            // Load all tabs
            PreparedStatement psTabs = con.prepareStatement("SELECT * FROM tab_shop ORDER BY shop_id, id");
            ResultSet rsTabs = psTabs.executeQuery();
            while (rsTabs.next()) {
                int shopId = rsTabs.getInt("shop_id");
                for (Shop shop : shops) {
                    if (shop.id == shopId) {
                        TabShop tab = new TabShop();
                        tab.shop = shop;
                        tab.id = rsTabs.getInt("id");
                        tab.name = rsTabs.getString("name").replaceAll("<>", "\n");
                        shop.tabShops.add(tab);
                        break;
                    }
                }
            }
            rsTabs.close();
            psTabs.close();

            // Load all items
            PreparedStatement psItems = con
                    .prepareStatement("SELECT * FROM item_shop WHERE is_sell = 1 ORDER BY tab_id, create_time DESC");
            ResultSet rsItems = psItems.executeQuery();
            while (rsItems.next()) {
                int tabId = rsItems.getInt("tab_id");
                for (Shop shop : shops) {
                    for (TabShop tab : shop.tabShops) {
                        if (tab.id == tabId) {
                            ItemShop itemShop = new ItemShop();
                            itemShop.tabShop = tab;
                            itemShop.id = rsItems.getInt("id");
                            itemShop.temp = ItemService.gI().getTemplate(rsItems.getShort("temp_id"));
                            itemShop.gold = rsItems.getInt("gold");
                            itemShop.gem = rsItems.getInt("gem");
                            itemShop.isNew = rsItems.getBoolean("is_new");
                            itemShop.itemExchange = rsItems.getInt("item_exchange");
                            if (itemShop.itemExchange != -1) {
                                itemShop.iconSpec = ItemService.gI().getTemplate(itemShop.itemExchange).iconID;
                                itemShop.costSpec = rsItems.getInt("quantity_exchange");
                            }
                            tab.itemShops.add(itemShop);
                            break;
                        }
                    }
                }
            }
            rsItems.close();
            psItems.close();

            // Load all options
            PreparedStatement psOptions = con.prepareStatement("SELECT * FROM item_shop_option");
            ResultSet rsOptions = psOptions.executeQuery();
            while (rsOptions.next()) {
                int itemShopId = rsOptions.getInt("item_shop_id");
                for (Shop shop : shops) {
                    for (TabShop tab : shop.tabShops) {
                        for (ItemShop item : tab.itemShops) {
                            if (item.id == itemShopId) {
                                item.options
                                        .add(new ItemOption(rsOptions.getInt("option_id"), rsOptions.getInt("param")));
                                break;
                            }
                        }
                    }
                }
            }
            rsOptions.close();
            psOptions.close();

        } catch (Exception e) {
            Log.error(ShopDAO.class, e);
        }
        return shops;
    }

}
