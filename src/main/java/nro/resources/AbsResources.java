/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.resources;

import com.google.gson.Gson;
import nro.resources.entity.EffectData;
import nro.resources.entity.ImageByName;
import nro.resources.entity.MobData;
import nro.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author 💖 ahwuocdz 💖
 */
@Getter
@Setter
public abstract class AbsResources {

    private File folder;
    private HashMap<String, byte[]> datas;
    private int[] dataVersion;
    private byte[][] smallVersion;
    private byte[][] backgroundVersion;
    private HashMap<String, ImageByName> imageByNames;
    private List<MobData> mobDatas;
    private List<EffectData> effectDatas;

    public AbsResources() {
        this.datas = new HashMap<>();
    }

    public void init() {
        initDataVersion();
        initBGSmallVersion();
        initSmallVersion();
        initIBN();
        initMobData();
        initEffectData();
    }

    public void initEffectData() {
        Gson g = new Gson();
        File folder = new File(this.folder, "effect_data");
        File[] listFiles = folder.listFiles();
        effectDatas = new ArrayList<>();
        for (File file : listFiles) {
            try {
                String json = Files.readString(file.toPath());
                if (!json.equals("")) {
                    effectDatas.add(g.fromJson(json, EffectData.class));
                }
            } catch (IOException ex) {
                Logger.getLogger(AbsResources.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void initMobData() {
        Gson g = new Gson();
        File folder = new File(this.folder, "monster_data");
        File[] listFiles = folder.listFiles();
        mobDatas = new ArrayList<>();
        for (File file : listFiles) {
            try {
                String json = Files.readString(file.toPath());
                if (!json.equals("")) {
                    mobDatas.add(g.fromJson(json, MobData.class));
                }
            } catch (IOException ex) {
                Logger.getLogger(AbsResources.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        mobDatas.forEach(m -> m.setData());
    }

    public void initIBN() {
        String json = readString("ibn.json");
        if (!json.equals("")) {
            JSONArray jIBN = new JSONArray(json);
            imageByNames = new HashMap<>();
            int len = jIBN.length();
            for (int i = 0; i < len; i++) {
                JSONObject obj = jIBN.getJSONObject(i);
                String filename = obj.getString("filename");
                int nFame = obj.getInt("number_frame");
                imageByNames.put(filename, ImageByName.builder()
                        .filename(filename)
                        .nFame(nFame)
                        .build());
            }
        }
    }

    public void initDataVersion() {
        dataVersion = new int[4];
        for (int i = 0; i < 4; i++) {
            File folder = new File(this.folder, "data");
            int ver = (int) FileUtils.getFolderSize(folder);
            dataVersion[i] = ver;
        }
    }

    public void initBGSmallVersion() {
        try {
            backgroundVersion = new byte[4][];
            for (int i = 0; i < 4; i++) {
                File file = new File(this.folder, "/image/" + (i + 1) + "/bg/");
                File[] files = file.listFiles();
                int max = 0;
                for (File f : files) {
                    String name = f.getName();
                    int id = Integer.parseInt(FileUtils.cutPng(name));
                    if (id > max) {
                        max = id;
                    }
                }
                backgroundVersion[i] = new byte[max + 1];
                for (File f : files) {
                    String name = f.getName();
                    int id = Integer.parseInt(FileUtils.cutPng(name));
                    backgroundVersion[i][id] = (byte) (Files.readAllBytes(f.toPath()).length % 127);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initSmallVersion() {
        try {
            smallVersion = new byte[4][];
            for (int i = 0; i < 4; i++) {
                File file = new File(this.folder, "/image/" + (i + 1) + "/icon/");
                File[] files = file.listFiles();
                int max = 0;
                for (File f : files) {
                    String name = f.getName();
                    name = FileUtils.cutPng(name);
                    int id = Integer.parseInt(name);
                    if (id > max) {
                        max = id;
                    }
                }
                smallVersion[i] = new byte[max + 1];
                for (File f : files) {
                    String name = f.getName();
                    name = FileUtils.cutPng(name);
                    int id = Integer.parseInt(name);
                    smallVersion[i][id] = (byte) (Files.readAllBytes(f.toPath()).length % 127);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void setFolder(String folder) {
        this.folder = new File("resources", folder);
    }

    public byte[] readAllBytes(String path) {
        try {
            return Files.readAllBytes(new File(this.folder, path).toPath());
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    public List<String> readAllLines(String path) {
        try {
            return Files.readAllLines(new File(this.folder, path).toPath());
        } catch (IOException ex) {
            return List.of();
        }
    }

    public String readString(String path) {
        try {
            File f = new File(this.folder, path);
            return Files.readString(new File(this.folder, path).toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public byte[] getRawIconData(int zoomLevel, int iconID) {
        return getScaledImage("/image/" + zoomLevel + "/icon/" + iconID + ".png", zoomLevel);
    }

    public byte[] getRawBGData(int zoomLevel, int bg) {
        return getScaledImage("/image/" + zoomLevel + "/bg/" + bg + ".png", zoomLevel);
    }

    public byte[] getRawIBNData(int zoomLevel, String filename) {
        return getScaledImage("/image/" + zoomLevel + "/imgbyname/" + filename + ".png", zoomLevel);
    }

    public byte[] getRawMobData(int zoomLevel, int id) {
        return getScaledImage("/image/" + zoomLevel + "/monster/" + id + ".png", zoomLevel);
    }

    public byte[] getRawEffectData(int zoomLevel, int id) {
        return getScaledImage("/image/" + zoomLevel + "/effect/" + id + ".png", zoomLevel);
    }
    
    /**
     * Lấy ảnh và tự động scale từ x4 nếu cần
     * Ưu tiên: Đọc file zoom level yêu cầu -> Nếu không có thì scale từ x4
     */
    private byte[] getScaledImage(String path, int zoomLevel) {
        // Thử đọc file ở zoom level yêu cầu trước
        byte[] data = readAllBytes(path);
        if (data.length > 0) {
            return data; // Có file sẵn, dùng luôn
        }
        
        // Không có file, thử scale từ x4
        if (zoomLevel < 4) {
            String x4Path = path.replace("/image/" + zoomLevel + "/", "/image/4/");
            byte[] x4Data = readAllBytes(x4Path);
            if (x4Data.length > 0) {
                return nro.utils.ImageScaler.scaleImage(x4Data, zoomLevel);
            }
        }
        
        return new byte[0]; // Không tìm thấy
    }

    public void putData(String key, byte[] data) {
        datas.put(key, data);
    }

    public byte[] getData(String key) {
        return datas.get(key);
    }

    public ImageByName getIBN(String key) {
        return imageByNames.get(key);
    }

    public MobData getMobData(int id) {
        for (MobData mob : mobDatas) {
            if (mob.getId() == id) {
                return mob;
            }
        }
        return null;
    }

    public EffectData getEffectData(int id) {
        for (EffectData eff : effectDatas) {
            if (eff.getId() == id) {
                return eff;
            }
        }
        return null;
    }

}
