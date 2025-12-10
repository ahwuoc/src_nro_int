package nro.server;

import nro.services.Service;
import java.io.IOException;
import java.time.LocalTime;

/**
 * Auto Maintenance - Bảo trì tự động theo lịch
 * @author ahwuocdz
 */
public class AutoMaintenance extends Thread {
    
    /**
     * Class lưu thông tin thời gian bảo trì
     */
    public static class MaintenanceTime {
        public final int hour;
        public final int minute;
        public final int countdownSeconds;
        
        public MaintenanceTime(int hour, int minute, int countdownSeconds) {
            this.hour = hour;
            this.minute = minute;
            this.countdownSeconds = countdownSeconds;
        }
        
        public MaintenanceTime(int hour, int minute) {
            this(hour, minute, 60); // Mặc định 60 giây đếm ngược
        }
        
        public boolean isTime(LocalTime currentTime) {
            return currentTime.getHour() == hour && currentTime.getMinute() == minute;
        }
        
        @Override
        public String toString() {
            return String.format("%02d:%02d", hour, minute);
        }
    }
    
    // Cấu hình bảo trì
    public static boolean enabled = false; // Bật/Tắt bảo trì tự động
    
    // Danh sách các mốc thời gian bảo trì
    public static final MaintenanceTime[] MAINTENANCE_TIMES = {
        new MaintenanceTime(6, 0, 60),   // 6:00 sáng, đếm ngược 60s
        new MaintenanceTime(19, 0, 60),  // 19:00 tối, đếm ngược 60s
    };
    
    private static AutoMaintenance instance;
    public static boolean isRunning;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRuning && !isRunning) {
            try {
                if (enabled) {
                    LocalTime currentTime = LocalTime.now();
                    for (MaintenanceTime mt : MAINTENANCE_TIMES) {
                        if (mt.isTime(currentTime)) {
                            Maintenance.gI().start(mt.countdownSeconds);
                            Service.getInstance().sendThongBaoAllPlayer(
                                    "Hệ thống bảo trì định kỳ, vui lòng thoát game để tránh mất vật phẩm");
                            isRunning = true;
                            enabled = false;
                            break;
                        }
                    }
                }
                Thread.sleep(30000); // Check mỗi 30 giây
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void runBatchFile(String batchFilePath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "start", batchFilePath);
        Process process = processBuilder.start();
        try {
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
