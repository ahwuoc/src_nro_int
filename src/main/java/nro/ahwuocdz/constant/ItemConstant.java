package nro.ahwuocdz.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý item name, id và slug
 * Slug được tạo tự động từ name để dễ query
 * 
 * Cách dùng:
 * 1. Load từ database: ItemConstantLoader.load()
 * 2. Sinh ra constant file: ItemConstantLoader.generateConstantFile()
 * 3. Query: ItemConstant.getBySlug("ao-vai-3-lop")
 */
public class ItemConstant {

    private static final Map<Integer, ItemInfo> itemsById = new HashMap<>();
    private static final Map<String, ItemInfo> itemsBySlug = new HashMap<>();

    /**
     * Đăng ký item
     */
    public static void register(int id, String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        
        String slug = generateSlug(name);
        ItemInfo info = new ItemInfo(id, name, slug);
        
        itemsById.put(id, info);
        itemsBySlug.put(slug, info);
    }

    /**
     * Tạo slug từ name
     */
    private static String generateSlug(String name) {
        return name
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "-")  // Thay ký tự đặc biệt bằng -
            .replaceAll("-+", "-")          // Gộp nhiều - thành 1
            .replaceAll("^-|-$", "");       // Xóa - ở đầu/cuối
    }

    /**
     * Lấy item info theo id
     */
    public static ItemInfo getById(int id) {
        return itemsById.get(id);
    }

    /**
     * Lấy item info theo slug
     */
    public static ItemInfo getBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return null;
        }
        return itemsBySlug.get(slug.toLowerCase());
    }

    /**
     * Lấy id theo slug
     */
    public static Integer getIdBySlug(String slug) {
        ItemInfo info = getBySlug(slug);
        return info != null ? info.id : null;
    }

    /**
     * Lấy name theo slug
     */
    public static String getNameBySlug(String slug) {
        ItemInfo info = getBySlug(slug);
        return info != null ? info.name : null;
    }

    /**
     * Kiểm tra item có tồn tại không
     */
    public static boolean exists(int id) {
        return itemsById.containsKey(id);
    }

    /**
     * Kiểm tra slug có tồn tại không
     */
    public static boolean existsBySlug(String slug) {
        return itemsBySlug.containsKey(slug != null ? slug.toLowerCase() : null);
    }

    /**
     * Lấy tổng số item đã đăng ký
     */
    public static int getTotalItems() {
        return itemsById.size();
    }

    /**
     * Class lưu thông tin item
     */
    public static class ItemInfo {
        public final int id;
        public final String name;
        public final String slug;

        public ItemInfo(int id, String name, String slug) {
            this.id = id;
            this.name = name;
            this.slug = slug;
        }

        @Override
        public String toString() {
            return String.format("ItemInfo{id=%d, name='%s', slug='%s'}", id, name, slug);
        }
    }
}
