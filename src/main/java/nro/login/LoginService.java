/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nro.login;

import nro.server.Client;
import nro.server.io.Message;
import nro.server.io.Session;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author 💖 YTB KhanhDTK 💖
 */
public class LoginService {

    private LoginSession session;

    public LoginService(LoginSession session) {
        this.session = session;
    }

    public void login(byte server, int clientID, String username, String password) {
        try {
            Message ms = new Message(Cmd.LOGIN);
            DataOutputStream ds = ms.writer();
            ds.writeByte(server);
            ds.writeInt(clientID);
            ds.writeUTF(username);
            ds.writeUTF(password);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void logout(int userID) {
        try {
            Message ms = new Message(Cmd.LOGOUT);
            DataOutputStream ds = ms.writer();
            ds.writeInt(userID);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setServer(int serverID, Client client) {
        try {
            // System.out.println("add all users to the login server");
            List<Session> sessions = client.getSessions();
            synchronized (sessions) {
                List<Session> list = sessions.stream().filter((t) -> t.loginSuccess).collect(Collectors.toList());
                Message ms = new Message(Cmd.SERVER);
                DataOutputStream ds = ms.writer();
                ds.writeInt(serverID);
                ds.writeInt(list.size());
                for (Session session : list) {
                    ds.writeInt(session.id);
                    ds.writeInt(session.userId);
                    ds.writeUTF(session.uu);
                    ds.writeUTF(session.pp);
                }
                ds.flush();
                sendMessage(ms);
                ms.cleanup();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void sendMessage(Message ms) {
        session.sendMessage(ms);
    }
    
    // ==================== LOAD PLAYER DATA ====================
    
    private volatile String playerDataJson = null;
    private volatile boolean loadComplete = false;
    
    /**
     * Request full player data from Rust server (blocking with timeout)
     */
    public String loadPlayerData(int accountId, int serverId, int timeoutMs) {
        try {
            playerDataJson = null;
            loadComplete = false;
            
            // Send request
            Message ms = new Message(Cmd.LOAD_DATA_PLAYER);
            DataOutputStream ds = ms.writer();
            ds.writeInt(accountId);
            ds.writeInt(serverId);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
            
            // Wait for response with timeout
            long start = System.currentTimeMillis();
            while (!loadComplete) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    System.err.println("[Rust] Timeout loading player data for account " + accountId);
                    return null;
                }
                Thread.sleep(5); // 5ms check interval
            }
            
            return playerDataJson;
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Callback when player data response received (called by LoginController)
     */
    public void onPlayerDataReceived(String json) {
        this.playerDataJson = json;
        this.loadComplete = true;
    }
    
    // ==================== LOAD PLAYER DATA BINARY ====================
    
    private volatile byte[] playerDataBinary = null;
    private volatile boolean loadBinaryComplete = false;
    
    /**
     * Request player data from Rust server as binary (blocking with timeout)
     */
    public byte[] loadPlayerDataBinary(int accountId, int serverId, int timeoutMs) {
        try {
            playerDataBinary = null;
            loadBinaryComplete = false;
            
            // Send request
            Message ms = new Message(Cmd.LOAD_DATA_PLAYER);
            DataOutputStream ds = ms.writer();
            ds.writeInt(accountId);
            ds.writeInt(serverId);
            ds.flush();
            sendMessage(ms);
            ms.cleanup();
            
            // Wait for response with timeout
            long start = System.currentTimeMillis();
            while (!loadBinaryComplete) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    System.err.println("[Rust] Timeout loading player data for account " + accountId);
                    return null;
                }
                Thread.sleep(5); // 5ms check interval
            }
            
            return playerDataBinary;
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Callback when binary player data response received (called by LoginController)
     */
    public void onPlayerDataBinaryReceived(byte[] data) {
        this.playerDataBinary = data;
        this.loadBinaryComplete = true;
    }
}
