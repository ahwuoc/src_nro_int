package nro.bot;

import nro.bot.models.Bot;
import nro.models.map.Zone;
import nro.services.ItemService;
import nro.services.MapService;
import nro.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all bots in the game.
 */
public class BotManager {

    private static BotManager instance;
    private final ConcurrentHashMap<Long, Bot> bots;
    private long nextBotId = -1000;
    
    // Bot lists by type
    private final List<Bot> botsFarmMob = new ArrayList<>();
    private final List<Bot> botsFarmBoss = new ArrayList<>();
    private final List<Bot> botsTalkNpc = new ArrayList<>();
    private final List<Bot> botsShopNpc = new ArrayList<>();
    
    // Bot type constants
    public static final int TYPE_FARM_MOB = 1;
    public static final int TYPE_FARM_BOSS = 2;
    public static final int TYPE_TALK_NPC = 3;
    public static final int TYPE_SHOP_NPC = 4;
    
    // Name prefixes
    private static final String[] NAME_PREFIXES = {"", "Hunter_", "BossKiller_", "Visitor_", "Shopper_"};
    private static final String[] NAME_SUFFIXES = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa"};

    private BotManager() {
        bots = new ConcurrentHashMap<>();
    }

    public static BotManager gI() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }
    
    private String generateBotName(int type, int index) {
        String prefix = NAME_PREFIXES[type];
        return prefix + NAME_SUFFIXES[index % NAME_SUFFIXES.length] + (index / 10 > 0 ? String.valueOf(index / 10) : "");
    }
    
    private List<Bot> getTypeList(int type) {
        switch (type) {
            case TYPE_FARM_MOB: return botsFarmMob;
            case TYPE_FARM_BOSS: return botsFarmBoss;
            case TYPE_TALK_NPC: return botsTalkNpc;
            case TYPE_SHOP_NPC: return botsShopNpc;
            default: return new ArrayList<>();
        }
    }
    
    private String getTypeName(int type) {
        switch (type) {
            case TYPE_FARM_MOB: return "FARM_MOB";
            case TYPE_FARM_BOSS: return "FARM_BOSS";
            case TYPE_TALK_NPC: return "TALK_NPC";
            case TYPE_SHOP_NPC: return "SHOP_NPC";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Create bots by type and quantity
     */
    public List<Bot> createBots(int type, int quantity) {
        List<Bot> createdBots = new ArrayList<>();
        
        for (int i = 0; i < quantity; i++) {
            byte gender = (byte) (i % 3);
            String name = generateBotName(type, getTypeList(type).size() + i);
            
            Bot bot = createBotInternal(name, gender);
            if (bot != null) {
                createdBots.add(bot);
                getTypeList(type).add(bot);
                
                // Random delay for each bot (0-10 seconds) to prevent sync movement
                int randomDelay = Util.nextInt(0, 10000);
                
                switch (type) {
                    case TYPE_FARM_MOB:
                        spawnBotAtPlanet(bot);
                        // Set random hunting start time so bots don't move together
                        bot.setHuntingStartTimeOffset(randomDelay);
                        bot.startHunting();
                        break;
                    case TYPE_FARM_BOSS:
                        bot.startAttackBoss();
                        break;
                    case TYPE_TALK_NPC:
                        bot.startTalkNpc(5, nro.consts.ConstNpc.BA_HAT_MIT);
                        break;
                    case TYPE_SHOP_NPC:
                        bot.startShoppingAtNpc(5, nro.consts.ConstNpc.SANTA, nro.consts.ConstNpc.SANTA);
                        break;
                }
            }
        }
        
        System.out.println("[BotManager] Created " + createdBots.size() + " bots of type " + getTypeName(type));
        return createdBots;
    }
    
    /**
     * Spawn bot at its planet based on gender
     */
    private void spawnBotAtPlanet(Bot bot) {
        int[] startMaps = {0, 7, 14}; // Trái Đất, Namek, Xayda
        int mapId = startMaps[bot.gender];
        int x = Util.nextInt(100, 400);
        int y = 336;
        
        Zone zone = MapService.gI().getZoneJoinByMapIdAndZoneId(bot, mapId, 0);
        
        if (zone != null) {
            bot.location.x = x;
            bot.location.y = y;
            MapService.gI().goToMap(bot, zone);
            zone.load_Me_To_Another(bot);
            System.out.println("[BotManager] Spawned " + bot.name + " (gender " + bot.gender + ") at map " + zone.map.mapId);
        } else {
            System.err.println("[BotManager] Failed to find zone for map " + mapId);
        }
    }

    /**
     * Create bot internally
     */
    private Bot createBotInternal(String name, byte gender) {
        Bot bot = new Bot(name, gender);
        bot.id = nextBotId--;

        bot.nPoint.hpMax = 10000;
        bot.nPoint.mpMax = 5000;
        bot.nPoint.setFullHpMp();
        bot.nPoint.dame = 500;
        bot.nPoint.stamina = 1000;
        bot.nPoint.maxStamina = 1000;
        
        for (int i = 0; i < 20; i++) {
            bot.inventory.itemsBag.add(ItemService.gI().createItemNull());
        }

        initializeBotSkills(bot, gender);
        bots.put(bot.id, bot);
        
        if (BotConfig.BOT_HAS_PET) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    bot.createBotPet();
                } catch (Exception e) {}
            }).start();
        }

        return bot;
    }

    private void initializeBotSkills(Bot bot, byte gender) {
        try {
            if (nro.server.Manager.NCLASS == null || nro.server.Manager.NCLASS.isEmpty()) return;

            nro.models.skill.NClass nClass = null;
            for (nro.models.skill.NClass nc : nro.server.Manager.NCLASS) {
                if (nc != null && nc.classId == gender) {
                    nClass = nc;
                    break;
                }
            }

            if (nClass == null || nClass.skillTemplatess == null || nClass.skillTemplatess.isEmpty()) return;
            
            for (int i = 0; i < Math.min(2, nClass.skillTemplatess.size()); i++) {
                nro.models.skill.SkillTemplate template = nClass.skillTemplatess.get(i);
                if (template != null) {
                    nro.models.skill.Skill skill = new nro.models.skill.Skill();
                    skill.template = template;
                    skill.point = 1;
                    skill.coolDown = 1000;
                    skill.skillId = template.id;
                    bot.playerSkill.skills.add(skill);
                }
            }

            if (!bot.playerSkill.skills.isEmpty()) {
                bot.playerSkill.skillSelect = bot.playerSkill.skills.get(0);
            }
        } catch (Exception e) {}
    }
    
    /**
     * Initialize FULL skills for bot (for boss fighting)
     */
    public void initializeFullSkills(Bot bot) {
        try {
            byte gender = bot.gender;
            if (nro.server.Manager.NCLASS == null || nro.server.Manager.NCLASS.isEmpty()) return;

            nro.models.skill.NClass nClass = null;
            for (nro.models.skill.NClass nc : nro.server.Manager.NCLASS) {
                if (nc != null && nc.classId == gender) {
                    nClass = nc;
                    break;
                }
            }

            if (nClass == null || nClass.skillTemplatess == null || nClass.skillTemplatess.isEmpty()) return;
            
            bot.playerSkill.skills.clear();
            
            for (nro.models.skill.SkillTemplate template : nClass.skillTemplatess) {
                if (template != null) {
                    nro.models.skill.Skill skill = new nro.models.skill.Skill();
                    skill.template = template;
                    skill.point = 7;
                    skill.coolDown = 500;
                    skill.skillId = template.id;
                    bot.playerSkill.skills.add(skill);
                }
            }

            if (!bot.playerSkill.skills.isEmpty()) {
                bot.playerSkill.skillSelect = bot.playerSkill.skills.get(0);
            }
            
            System.out.println("[BotManager] Bot " + bot.name + " upgraded with " + bot.playerSkill.skills.size() + " full skills");
        } catch (Exception e) {}
    }

    public void removeBotsByType(int type) {
        List<Bot> list = getTypeList(type);
        for (Bot bot : list) {
            bots.remove(bot.id);
            bot.dispose();
        }
        System.out.println("[BotManager] Removed " + list.size() + " bots of type " + getTypeName(type));
        list.clear();
    }
    
    public void removeBot(long botId) {
        Bot bot = bots.remove(botId);
        if (bot != null) {
            botsFarmMob.remove(bot);
            botsFarmBoss.remove(bot);
            botsTalkNpc.remove(bot);
            botsShopNpc.remove(bot);
            bot.dispose();
        }
    }

    public List<Bot> getAllBots() {
        return new ArrayList<>(bots.values());
    }
    
    public List<Bot> getBotsByType(int type) {
        return new ArrayList<>(getTypeList(type));
    }

    public int getBotCount(int type) {
        return getTypeList(type).size();
    }
    
    public int getTotalBotCount() {
        return bots.size();
    }

    public void removeAllBots() {
        for (Bot bot : bots.values()) {
            bot.dispose();
        }
        bots.clear();
        botsFarmMob.clear();
        botsFarmBoss.clear();
        botsTalkNpc.clear();
        botsShopNpc.clear();
        System.out.println("[BotManager] Removed all bots");
    }
    
    public String getStatusSummary() {
        return String.format("Bots: %d total (Farm:%d, Boss:%d, NPC:%d, Shop:%d)",
            bots.size(), botsFarmMob.size(), botsFarmBoss.size(), botsTalkNpc.size(), botsShopNpc.size());
    }
}
