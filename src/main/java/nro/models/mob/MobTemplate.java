/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.mob;

import nro.server.Manager;

/**
 *
 * @author Kitak
 */
public class MobTemplate {

    public int id;
    public byte type;
    public String name;
    public int hp;
    public byte rangeMove;
    public byte speed;
    public byte dartType;
    public byte percentDame;
    public byte percentTiemNang;
    
    /**
     * Lấy MobTemplate theo id
     */
    public static MobTemplate getById(int mobId) {
        return Manager.MOB_TEMPLATES.stream()
                .filter(t -> t.id == mobId)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Lấy tên mob theo id
     */
    public static String getNameById(int mobId) {
        MobTemplate template = getById(mobId);
        return template != null ? template.name : "Unknown";
    }
}
