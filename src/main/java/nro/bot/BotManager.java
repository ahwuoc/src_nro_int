package nro.bot;

import nro.bot.models.Bot;
import nro.models.map.Zone;
import nro.services.ItemService;
import nro.services.MapService;
import nro.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages all bots in the game.
 */
public class BotManager {

    private static BotManager instance;
    private final ConcurrentHashMap<Long, Bot> bots;
    private long nextBotId = -1000;
    
    // Scheduler for async bot operations
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    // Bot lists by type
    private final List<Bot> botsFarmMob = new ArrayList<>();
    private final List<Bot> botsFarmBoss = new ArrayList<>();
    private final List<Bot> botsNpcVisitor = new ArrayList<>();
    private final List<Bot> botsAfk = new ArrayList<>();
    
    // Bot type constants
    public static final int TYPE_FARM_MOB = 1;
    public static final int TYPE_FARM_BOSS = 2;
    public static final int TYPE_NPC_VISITOR = 3;
    public static final int TYPE_AFK = 4;
    
    // Combined name list from BOT_NAMES and BOT_NAME_PREFIXES
    private static String[] getAllBotNames() {
        String[] prefixes = BotConfig.BOT_NAME_PREFIXES;
        String[] names = BotConfig.BOT_NAME;
        String[] combined = new String[prefixes.length + names.length];
        System.arraycopy(prefixes, 0, combined, 0, prefixes.length);
        System.arraycopy(names, 0, combined, prefixes.length, names.length);
        return combined;
    }
    
    private static final String[] BOT_NAMES = getAllBotNames();

    private BotManager() {
        bots = new ConcurrentHashMap<>();
    }

    public static BotManager gI() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }
    
    // Track used names to avoid duplicates
    private final java.util.Set<String> usedNames = new java.util.HashSet<>();
    
    private String generateBotName(int type, int index) {
        // 70% chance to use real name, 30% chance to use prefix + name
        if (Util.nextInt(1, 100) <= 70) {
            // Pick random unused name
            for (int attempt = 0; attempt < 50; attempt++) {
                String name = BOT_NAMES[Util.nextInt(0, BOT_NAMES.length - 1)];
                if (!usedNames.contains(name)) {
                    usedNames.add(name);
                    return name;
                }
            }
        }
        
        // Fallback: prefix + random name + optional number
        String[] prefixes = {"", "pro", "vip", "x", "z", "k", "m"};
        String prefix = prefixes[Util.nextInt(0, prefixes.length - 1)];
        String baseName = BOT_NAMES[Util.nextInt(0, BOT_NAMES.length - 1)];
        String suffix = Util.nextInt(1, 100) <= 30 ? String.valueOf(Util.nextInt(1, 99)) : "";
        
        String name = prefix + baseName + suffix;
        usedNames.add(name);
        return name;
    }
    
    private List<Bot> getTypeList(int type) {
        switch (type) {
            case TYPE_FARM_MOB: return botsFarmMob;
            case TYPE_FARM_BOSS: return botsFarmBoss;
            case TYPE_NPC_VISITOR: return botsNpcVisitor;
            case TYPE_AFK: return botsAfk;
            default: return new ArrayList<>();
        }
    }
    
    private String getTypeName(int type) {
        switch (type) {
            case TYPE_FARM_MOB: return "FARM_MOB";
            case TYPE_FARM_BOSS: return "FARM_BOSS";
            case TYPE_NPC_VISITOR: return "NPC_VISITOR";
            case TYPE_AFK: return "AFK";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Create bots by type and quantity with async scheduling
     * Each bot is created with random delay to prevent sync behavior
     */
    public List<Bot> createBots(int type, int quantity) {
        List<Bot> createdBots = new ArrayList<>();
        final int botType = type; // Make type final for lambda capture
        
        for (int i = 0; i < quantity; i++) {
            final int index = i;
            final byte gender = (byte) (i % 3);
            final String name = generateBotName(botType, getTypeList(botType).size() + i);
            
            // Random delay 0-3 seconds for each bot to prevent sync
            int delayMs = Util.nextInt(0, 3000) * index / Math.max(1, quantity);
            
            scheduler.schedule(() -> {
                try {
                    Bot bot = createBotInternal(name, gender, botType);
                    if (bot != null) {
                        synchronized (createdBots) {
                            createdBots.add(bot);
                        }
                        getTypeList(botType).add(bot);
                        initializeBotByType(bot, botType);
                    }
                } catch (Exception e) {
                    System.err.println("[BotManager] Error creating bot " + name + ": " + e.getMessage());
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
        
        System.out.println("[BotManager] Scheduling " + quantity + " bots of type " + getTypeName(botType));
        return createdBots;
    }
    
    /**
     * Spawn bot at its planet based on gender
     * Finds the least populated zone in the starting map
     */
    private void spawnBotAtPlanet(Bot bot) {
        int[] startMaps = {0, 7, 14}; // Trái Đất, Namek, Xayda
        int mapId = startMaps[bot.gender];
        int x = Util.nextInt(100, 400);
        int y = 336;
        
        // Find least populated zone
        Zone zone = findLeastPopulatedZone(mapId);
        if (zone == null) {
            // Fallback to default zone 0
            zone = MapService.gI().getZoneJoinByMapIdAndZoneId(bot, mapId, 0);
        }
        
        if (zone != null) {
            bot.location.x = x;
            bot.location.y = y;
            MapService.gI().goToMap(bot, zone);
            zone.load_Me_To_Another(bot);
            System.out.println("[BotManager] Spawned " + bot.name + " (gender " + bot.gender + ") at map " + zone.map.mapId + " zone " + zone.zoneId + " (" + zone.getPlayers().size() + " players)");
        } else {
            System.err.println("[BotManager] Failed to find zone for map " + mapId);
        }
    }
    
    /**
     * Find the least populated zone in a map
     * @param mapId Map ID to search
     * @return Zone with fewest players, or null if map not found
     */
    private Zone findLeastPopulatedZone(int mapId) {
        nro.models.map.Map map = MapService.gI().getMapById(mapId);
        if (map == null || map.zones == null) {
            return null;
        }
        
        Zone bestZone = null;
        int minPlayers = Integer.MAX_VALUE;
        
        for (Zone z : map.zones) {
            if (z != null) {
                int playerCount = z.getPlayers().size();
                if (playerCount < minPlayers) {
                    minPlayers = playerCount;
                    bestZone = z;
                }
            }
        }
        
        return bestZone;
    }

    /**
     * Create bot internally
     */
    private Bot createBotInternal(String name, byte gender, int type) {
        Bot bot = new Bot(name, gender);
        bot.id = nextBotId--;

        bot.nPoint.hpMax = 10000;
        bot.nPoint.mpMax = 5000;
        bot.nPoint.setFullHpMp();
        bot.nPoint.dame = 500;
        bot.nPoint.stamina = 1000;
        bot.nPoint.maxStamina = 1000;
        
        // Initialize itemsBody (equipped items) - 6 slots: armor, pants, gloves, shoes, weapon, outfit
        for (int i = 0; i < 6; i++) {
            bot.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        
        // Initialize itemsBag (inventory bag)
        for (int i = 0; i < 20; i++) {
            bot.inventory.itemsBag.add(ItemService.gI().createItemNull());
        }

        initializeBotSkills(bot, gender);
        bots.put(bot.id, bot);
        
        // Check if this bot type should have pet
        boolean shouldHavePet = switch (type) {
            case TYPE_FARM_MOB -> BotConfig.FARM_MOB_HAS_PET;
            case TYPE_FARM_BOSS -> BotConfig.FARM_BOSS_HAS_PET;
            case TYPE_AFK -> BotConfig.AFK_BOT_HAS_PET; // AFK bot always has pet
            default -> BotConfig.BOT_HAS_PET;
        };
        
        if (shouldHavePet) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    bot.createBotPet();
                    // Start auto fusion after pet is created
                    Thread.sleep(1500); // Wait for pet to fully initialize
                    if (BotConfig.BOT_AUTO_FUSION && bot.hasPet()) {
                        bot.startAutoFusion();
                    }
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
                    skill.coolDown = BotConfig.BOSS_SKILL_COOLDOWN;
                    skill.skillId = template.id;
                    bot.playerSkill.skills.add(skill);
                }
            }

            if (!bot.playerSkill.skills.isEmpty()) {
                bot.playerSkill.skillSelect = bot.playerSkill.skills.get(0);
            }
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
            botsNpcVisitor.remove(bot);
            botsAfk.remove(bot);
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
        botsNpcVisitor.clear();
        botsAfk.clear();
        System.out.println("[BotManager] Removed all bots");
    }
    
    public String getStatusSummary() {
        return String.format("Bots: %d total (Farm:%d, Boss:%d, NpcVisitor:%d, AFK:%d)",
            bots.size(), botsFarmMob.size(), botsFarmBoss.size(), botsNpcVisitor.size(), botsAfk.size());
    }
    
    /**
     * Get formatted bot statistics text for admin menu display.
     * Uses color codes for display formatting.
     * @return Formatted string with total count and per-type counts
     */
    public String getBotStatisticsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("|7|-----Bot Statistics-----\n");
        sb.append("|1|Tổng số Bot: ").append(getTotalBotCount()).append("\n");
        sb.append("|2|Farm Mob: ").append(getBotCount(TYPE_FARM_MOB)).append("\n");
        sb.append("|3|Farm Boss: ").append(getBotCount(TYPE_FARM_BOSS)).append("\n");
        sb.append("|4|NPC Visitor: ").append(getBotCount(TYPE_NPC_VISITOR)).append("\n");
        sb.append("|5|AFK: ").append(getBotCount(TYPE_AFK));
        return sb.toString();
    }
    
    /**
     * Initialize bot based on its type
     * Spawns bot at appropriate location and sets up type-specific behavior
     */
    private void initializeBotByType(Bot bot, int type) {
        try {
            switch (type) {
                case TYPE_FARM_MOB:
                    spawnBotAtPlanet(bot);
                    bot.equipFarmMobOutfit();
                    bot.startHunting();
                    bot.setHuntingStartTimeOffset(Util.nextInt(0, 5000));
                    break;
                    
                case TYPE_FARM_BOSS:
                    bot.equipRandomOutfitFromShop();
                    bot.equipRandomTitle();
                    bot.equipRandomFlagBag();
                    bot.startAttackBoss();
                    bot.setBossAttackOffset(Util.nextInt(0, 3000));
                    break;
                    
                case TYPE_NPC_VISITOR:
                    bot.equipRandomOutfitFromShop();
                    bot.equipRandomTitle();
                    bot.equipRandomFlagBag();
                    bot.startNpcVisitorMode();
                    break;
                    
                case TYPE_AFK:
                    bot.equipRandomOutfitFromShop();
                    bot.equipRandomTitle();
                    bot.equipRandomFlagBag();
                    bot.startAfkMode();
                    // Start auto fusion if bot has pet
                    if (BotConfig.BOT_AUTO_FUSION && bot.hasPet()) {
                        bot.startAutoFusion();
                    }
                    break;
                default:
                    System.err.println("[BotManager] Unknown bot type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[BotManager] Error initializing bot " + bot.name + " of type " + getTypeName(type) + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
