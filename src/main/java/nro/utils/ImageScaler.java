package nro.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility để scale ảnh PNG từ x4 xuống x3, x2, x1
 * @author 💖 ahwuocdz 💖
 */
public class ImageScaler {
    
    /**
     * Scale ảnh theo zoom level
     * @param originalData byte[] của ảnh gốc (x4 - 100%)
     * @param targetZoomLevel 1=25%, 2=50%, 3=75%, 4=100%
     * @return byte[] của ảnh đã scale
     */
    public static byte[] scaleImage(byte[] originalData, int targetZoomLevel) {
        if (targetZoomLevel == 4) {
            return originalData; // Không cần scale
        }
        
        try {
            // Đọc ảnh gốc
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalData));
            if (original == null) {
                return originalData;
            }
            
            // Tính kích thước mới
            double scale = targetZoomLevel * 0.25; // 1->0.25, 2->0.5, 3->0.75
            int newWidth = (int) (original.getWidth() * scale);
            int newHeight = (int) (original.getHeight() * scale);
            
            // Tránh size = 0
            if (newWidth < 1) newWidth = 1;
            if (newHeight < 1) newHeight = 1;
            
            // Scale ảnh với quality cao
            BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            
            // Cài đặt rendering hints để ảnh đẹp hơn
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            // Convert về byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaled, "PNG", baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            e.printStackTrace();
            return originalData; // Trả về ảnh gốc nếu lỗi
        }
    }
    
    /**
     * Kiểm tra xem file PNG có tồn tại không
     */
    public static boolean isValidPNG(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }
        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        return data[0] == (byte) 0x89 && data[1] == 0x50 && 
               data[2] == 0x4E && data[3] == 0x47;
    }
}
