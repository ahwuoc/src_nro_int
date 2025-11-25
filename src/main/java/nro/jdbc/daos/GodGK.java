package nro.jdbc.daos;

import nro.consts.ConstAchive;
import nro.jdbc.DBService;
import nro.models.player.*;
import nro.server.Client;
import nro.server.Manager;
import nro.server.io.Session;
import nro.server.model.AntiLogin;
import nro.services.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ❤Girlkun75❤
 * @copyright ❤ahwuocdz❤
 */
public class GodGK {

    public static boolean login(Session session, AntiLogin al) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = DBService.gI().getConnectionForLogin();
            String query = "select * from account where username = ? and password = ? limit 1";
            ps = conn.prepareStatement(query);
            ps.setString(1, session.uu);
            ps.setString(2, session.pp);
            rs = ps.executeQuery();
            if (rs.next()) {
                session.userId = rs.getInt("account.id");
                Session plInGame = Client.gI().getSession(session);
                if (plInGame != null) {
                    Service.getInstance().sendThongBaoOK(plInGame, "Máy chủ tắt hoặc mất sóng!");
                    Client.gI().kickSession(plInGame);
                    Service.getInstance().sendThongBaoOK(session, "Máy chủ tắt hoặc mất sóng!");
                    return false;
                }

                session.isAdmin = rs.getBoolean("is_admin");
                session.lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                session.actived = rs.getBoolean("active");
                session.goldBar = rs.getInt("thoi_vang");
                session.vndBar = rs.getInt("vnd");
                session.tongnap = rs.getInt("tongnap");
                session.dataReward = rs.getString("reward");
                if (rs.getTimestamp("last_time_login").getTime() > session.lastTimeLogout) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đang đăng nhập máy chủ khác!");
                    return false;
                }
                if (rs.getBoolean("ban")) {
                    Service.getInstance().sendThongBaoOK(session, "Tài khoản đã bị khóa do vi phạm điều khoản!");
                } else {
                    long lastTimeLogout = rs.getTimestamp("last_time_logout").getTime();
                    int secondsPass = (int) ((System.currentTimeMillis() - lastTimeLogout) / 1000);
                    if (secondsPass < Manager.SECOND_WAIT_LOGIN && !session.isAdmin) {
                        Service.getInstance().sendThongBaoOK(session, "Vui lòng chờ "
                                + (Manager.SECOND_WAIT_LOGIN - secondsPass) + " giây để đăng nhập lại.");
                    }
                }
                al.reset();
                return true;
            } else {
                Service.getInstance().sendThongBaoOK(session, "Thông tin tài khoản hoặc mật khẩu không chính xác");
                al.wrong();
                // Anti login
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
        }
        return false;
    }

    public static Player loadPlayer(Session session) {
        return loadPlayerDirect(session);
    }

    private static Player loadPlayerDirect(Session session) {
        long startTime = System.currentTimeMillis();

        try {
            Connection connection = DBService.gI().getConnectionForLogin();
            PreparedStatement ps = connection.prepareStatement("select * from player where account_id = ? limit 1");
            ps.setInt(1, session.userId);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) {
                    int plHp = 200000000;
                    int plMp = 200000000;
                    JSONValue jv = new JSONValue();
                    JSONArray dataArray = null;
                    JSONObject dataObject = null;

                    Player player = new Player();
                    PlayerLoader loader = new PlayerLoader();

                    // base info - REFACTORED
                    loader.loadBasicInfo(player, rs);

                    // clan data - REFACTORED
                    loader.loadClanData(player, rs, Manager.SERVER);
                    // event data - REFACTORED
                    loader.loadEventData(player, rs);
                    player.event.setDiemTichLuy(session.diemTichNap);

                    // data kim lượng - REFACTORED
                    loader.loadInventory(player, rs);

                    // data titles (danh hieu 1-5) - REFACTORED
                    loader.loadTitles(player, rs);

                    player.server = session.server;

                    // data location - REFACTORED
                    loader.loadLocation(player, rs);

                    // data player points - REFACTORED
                    int[] hpMp = loader.loadPlayerPoints(player, rs);
                    plHp = hpMp[0];
                    plMp = hpMp[1];

                    // data magic tree & black ball - REFACTORED
                    loader.loadMagicTree(player, rs);
                    loader.loadBlackBallRewards(player, rs);

                    // data items (body + bag + box + lucky_round) - REFACTORED
                    loader.loadAllItems(player, rs);

                    // data friends & enemies - REFACTORED
                    loader.loadFriendsAndEnemies(player, rs);

                    // data intrinsic - REFACTORED
                    loader.loadIntrinsicData(player, rs);

                    // data item time buffs - REFACTORED
                    loader.loadItemTimeBuffs(player, rs);

                    // data tasks & achievements - REFACTORED
                    loader.loadMainTask(player, rs);
                    loader.loadSideTask(player, rs);
                    loader.loadAchievements(player, rs);

                    // data mabu egg & charms - REFACTORED
                    loader.loadMabuEgg(player, rs);
                    loader.loadCharms(player, rs);

                    // data skills & shortcuts - REFACTORED
                    loader.loadSkills(player, rs);
                    loader.loadSkillShortcuts(player, rs);

                    // data collection book - REFACTORED
                    loader.loadCollectionBook(player, rs);

                    // mini pet & pet follow - REFACTORED
                    loader.loadMiniPetsAndFollow(player, rs);

                    // other data - REFACTORED
                    loader.setFirstTimeLogin(player, rs);
                    loader.loadLimits(player, rs);
                    loader.loadChallengeData(player, rs);

                    PlayerService.gI().dailyLogin(player);

                    // data pet - REFACTORED
                    loader.loadPetData(player, rs, session);

                    // Ruby handling
                    if (session.ruby > 0) {
                        player.inventory.ruby += session.ruby;
                        player.playerTask.achivements.get(ConstAchive.LAN_DAU_NAP_NGOC).count += session.ruby;
                        PlayerDAO.subRuby(player, session.userId, session.ruby);
                    }

                    // Set HP/MP
                    player.nPoint.hp = plHp;
                    player.nPoint.mp = plMp;

                    // Set session player
                    session.player = player;

                    // Update timestamps
                    PreparedStatement ps2 = connection
                            .prepareStatement("update account set last_time_login = ?, ip_address = ? where id = ?");
                    ps2.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps2.setString(2, session.ipAddress);
                    ps2.setInt(3, session.userId);
                    ps2.executeUpdate();
                    ps2.close();

                    PreparedStatement ps3 = connection
                            .prepareStatement("update player set lastimelogin = ? where account_id = ?");
                    ps3.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps3.setInt(2, session.userId);
                    ps3.executeUpdate();
                    ps3.close();

                    PreparedStatement ps4 = connection
                            .prepareStatement("update player set tongnap = ? where account_id = ?");
                    ps4.setInt(1, session.tongnap);
                    ps4.setInt(2, session.userId);
                    ps4.executeUpdate();
                    ps4.close();

                    long totalTime = System.currentTimeMillis() - startTime;
                    System.out.println("[Direct] ===== TOTAL TIME: " + totalTime + "ms =====");

                    return player;
                }
            } finally {
                rs.close();
                ps.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            session.dataLoadFailed = true;
        }
        return null;
    }
}
