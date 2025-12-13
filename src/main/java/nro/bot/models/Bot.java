package nro.bot.models;

import nro.bot.BotConfig;
import nro.consts.ConstPlayer;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.services.*;
import nro.utils.Util;

/**
 * Bot Monster Hunter - Automated character that hunts monsters.
 * Extends Player to integrate with the game's map, display, and combat systems.
 * Similar to Pet but operates independently without a master.
 */
public class Bot extends Player {

    // Bot status constants
    public static final byte IDLE = 0;
    public static final byte HUNTING = 1;
    public static final byte DEFEATED = 2;
    public static final byte MOVE_MAP = 3;
    public static final byte PICK_ITEM = 4;
    public static final byte TALK_NPC = 5;
    public static final byte ATTACK_BOSS = 6;
    public static final byte AFK = 7;

    // Bot state
    public byte botStatus = IDLE;
    
    // Target tracking
    private Mob currentTarget;
    private nro.models.map.ItemMap targetItem;
    private nro.models.npc.Npc targetNpc;
    private long lastTimeAttack;
    private long lastTimeMoveIdle;
    private int timeMoveIdle;
    private long lastTimeMoveToMob;
    private long lastTimePickItem;
    private long lastTimeMoveToItem;
    private java.util.Set<Integer> recentlyPickedItems = new java.util.HashSet<>();
    
    // Map movement tracking
    private int currentMapIndex = 0;
    private nro.models.map.WayPoint targetWaypoint = null;
    private long lastTimeCheckMap = 0;
    private long huntingStartTime = 0;
    private long huntingDuration = BotConfig.HUNTING_DURATION; // Random per bot
    private int previousMapId = -1; // Track last map for avoidance
    
    // NPC interaction tracking
    private long lastTimeChangeOutfit = 0;
    private boolean autoChangeOutfit = false;
    private int outfitNpcId = 0;
    
    // Farm mob outfit tracking
    private long lastTimeFarmOutfitChange = 0;
    
    // NPC visitor mode tracking
    private long npcVisitStartTime = 0;
    private long npcVisitDuration = 0;
    private int currentNpcIndex = 0;
    private long lastTimeZoneSwitchNpc = 0;
    private long zoneSwitchIntervalNpc = 0;
    
    // Boss hunting tracking
    private nro.models.boss.Boss targetBoss = null;
    private long bossAttackStartTime = 0;
    private int currentSkillIndex = 0; // For skill rotation
    
    // Title (danh hiệu) tracking
    private long lastTimeChangeTitle = 0;
    private int currentTitlePart = -1;
    
    // Flag bag tracking
    private long lastTimeChangeFlagBag = 0;
    private int currentFlagBagId = -1;
    
    // Pet (đệ tử) for bot
    private boolean hasPet = false;
    
    // AFK mode tracking
    private long afkStartTime = 0;
    private long afkDuration = 0;
    private long lastTimeAfkIdle = 0;
    private long lastTimeEatPea = 0;
    private long lastTimeZoneSwitch = 0;
    
    // Auto revive tracking
    private long lastTimeRevive = 0;
    private byte previousStatus = HUNTING; // Status before death

    // Statistics
    private int monstersDefeated;
    private long totalExperienceGained;

    public Bot() {
        super();
        this.isBot = true;
        this.botStatus = IDLE;
        this.monstersDefeated = 0;
        this.totalExperienceGained = 0;
    }

    /**
     * Create a bot with specified parameters
     */
    public Bot(String name, byte gender) {
        super();
        this.name = name;
        this.gender = gender;
        short[] heads = BotConfig.BOT_HEAD_APPEARANCE[gender];
        this.head = heads[nro.utils.Util.nextInt(0, heads.length - 1)];
        this.isBot = true;
        this.botStatus = IDLE;
        this.monstersDefeated = 0;
        this.totalExperienceGained = 0;
    }
    @Override
    public void update() {
        try {
     
            if (isDie() || botStatus == DEFEATED) {
                autoRevive();
                return;
            }
            if (effectSkill != null && effectSkill.isHaveEffectSkill()) {
                return;
            }
            
            // Update pet - call pet.update() so pet can attack mobs
            if (pet != null && !pet.isDie()) {
                pet.update();
            }
            
            // Update auto fusion with pet
            updateAutoFusion();
            
            // Update pet follow
            updatePetFollow();
            
            switch (botStatus) {
                case IDLE -> {
                    moveIdle();
                }
                case HUNTING -> {
                    hunt();
                }
                case MOVE_MAP -> {
                    moveToNextMap();
                }
                case PICK_ITEM -> {
                    moveToItem();
                }
                case TALK_NPC -> {
                    goToNpc();
                }
                case ATTACK_BOSS -> {
                    attackBoss();
                }
                case AFK -> {
                    afkMode();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start hunting monsters
     */
    public void startHunting() {
        if (botStatus == DEFEATED) {
            return;
        }
        botStatus = HUNTING;
        huntingStartTime = System.currentTimeMillis();
        huntingDuration = Util.nextInt(BotConfig.HUNTING_DURATION_MIN, BotConfig.HUNTING_DURATION_MAX);
    }
    
    /**
     * Set hunting start time offset to prevent bots from moving together
     * @param offsetMs Offset in milliseconds
     */
    public void setHuntingStartTimeOffset(int offsetMs) {
        // Subtract offset so each bot has different "start time"
        huntingStartTime = System.currentTimeMillis() - offsetMs;
        huntingDuration = Util.nextInt(BotConfig.HUNTING_DURATION_MIN, BotConfig.HUNTING_DURATION_MAX);
    }
    
    /**
     * Set boss attack offset to prevent bots from attacking in sync
     * @param offsetMs Offset in milliseconds
     */
    public void setBossAttackOffset(int offsetMs) {
        lastTimeAttack = System.currentTimeMillis() - offsetMs;
    }
    public void stopHunting() {
        botStatus = IDLE;
        currentTarget = null;
    }

    // Hunting attack delay tracking
    private long lastTimeHuntAttack = 0;
    private int huntAttackDelay = 0;
    
    /**
     * Main hunting logic - Move like a human player with smooth skill usage
     * Applies Pet-like logic: move close first, then attack
     */
    private void hunt() {
        // Check if it's time to move to next map (random 8-15 seconds per bot)
        if (huntingStartTime > 0 && System.currentTimeMillis() - huntingStartTime >= huntingDuration) {
            startMovingMaps();
            return;
        }
        
        // Auto change farm outfit periodically
        if (Util.canDoWithTime(lastTimeFarmOutfitChange, Util.nextInt(BotConfig.FARM_MOB_OUTFIT_CHANGE_MIN, BotConfig.FARM_MOB_OUTFIT_CHANGE_MAX))) {
            equipFarmMobOutfit();
            lastTimeFarmOutfitChange = System.currentTimeMillis();
        }
        
        // Check for items to pick up - switch to PICK_ITEM mode if found (configurable)
        if (BotConfig.ENABLE_PICKUP_ITEM) {
            nro.models.map.ItemMap nearbyItem = findNearbyItem();
            if (nearbyItem != null) {
                targetItem = nearbyItem;
                botStatus = PICK_ITEM;
                return;
            }
        }
        
        currentTarget = findMobToAttack();
         
        if (currentTarget != null) {
            int distance = Util.getDistance(this, currentTarget);
            
            // Must be within attack range to use any skill
            if (distance <= BotConfig.ATTACK_RANGE_MELEE) {
                if (!Util.canDoWithTime(lastTimeHuntAttack, huntAttackDelay)) {
                    return; // Wait for delay
                }
                Skill meleeSkill = getSkillByType(0);
                if (meleeSkill != null && meleeSkill.template != null) {
                    this.playerSkill.skillSelect = meleeSkill;
                    if (SkillService.gI().canUseSkillWithCooldown(this)) {
                        if (SkillService.gI().canUseSkillWithMana(this)) {
                            PlayerService.gI().playerMove(this, currentTarget.location.x + Util.nextInt(-20, 20), currentTarget.location.y);
                            SkillService.gI().useSkill(this, null, currentTarget, null);
                            // Set random delay for next attack
                            lastTimeHuntAttack = System.currentTimeMillis();
                            huntAttackDelay = Util.nextInt(BotConfig.HUNTING_ATTACK_DELAY_MIN, BotConfig.HUNTING_ATTACK_DELAY_MAX);
                            return;
                        }
                    }
                }
                
                // Try ranged skill (type 1) if melee not available
                Skill rangedSkill = getSkillByType(1);
                if (rangedSkill != null && rangedSkill.template != null) {
                    this.playerSkill.skillSelect = rangedSkill;
                    if (SkillService.gI().canUseSkillWithCooldown(this)) {
                        if (SkillService.gI().canUseSkillWithMana(this)) {
                            SkillService.gI().useSkill(this, null, currentTarget, null);
                            // Set random delay for next attack
                            lastTimeHuntAttack = System.currentTimeMillis();
                            huntAttackDelay = Util.nextInt(BotConfig.HUNTING_ATTACK_DELAY_MIN, BotConfig.HUNTING_ATTACK_DELAY_MAX);
                            return;
                        }
                    }
                }
            } else {
                // Too far - move closer first
                walkTowardsMob();
                return;
            }
            
            // Move closer if too far
            if (distance > BotConfig.ATTACK_RANGE_MELEE) {
                walkTowardsMob();
            }
        } else {
            moveIdle();
        }
    }
    
    /**
     * Walk towards current target with human-like movement
     */
    private void walkTowardsMob() {
        if (currentTarget == null) return;
        int targetX = currentTarget.location.x;
        int targetY = currentTarget.location.y;
        int dx = targetX - this.location.x;
        
        // Move slowly for smooth movement
        int moveSpeed = Util.nextInt(BotConfig.MOVE_SPEED_TO_MOB_MIN, BotConfig.MOVE_SPEED_TO_MOB_MAX);
        
        // Always move TOWARDS the mob, never away
        if (Math.abs(dx) > BotConfig.ATTACK_RANGE_MELEE) {
            // Still far - keep moving closer
            if (dx > 0) {
                this.location.x += moveSpeed;
            } else {
                this.location.x -= moveSpeed;
            }
        } else {
            // Close enough - stop at attack range with small random offset
            this.location.x = targetX + Util.nextInt(-15, 15);
        }
        this.location.y = targetY;
        PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        lastTimeMoveToMob = System.currentTimeMillis();
    }

    
    /**
     * Find nearest item in zone that bot can pick up
     */
    private nro.models.map.ItemMap findNearbyItem() {
        if (zone == null) {
            return null;
        }
        
        try {
            java.util.List<nro.models.map.ItemMap> items = zone.getItemMapsForPlayer(this);
            if (items == null || items.isEmpty()) {
                return null;
            }
            
            nro.models.map.ItemMap nearestItem = null;
            int nearestDistance = Integer.MAX_VALUE;
            
            for (nro.models.map.ItemMap item : items) {
                if (item != null && !recentlyPickedItems.contains(item.itemMapId)) {
                    if (item.playerId != -1 && item.playerId != this.id) {
                        continue;
                    }
                    
                    int distance = Math.abs(this.location.x - item.x) + Math.abs(this.location.y - item.y);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestItem = item;
                    }
                }
            }
            return nearestItem;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Move to target item and pick it up
     */
    private void moveToItem() {
        // Check every 100ms
        if (!Util.canDoWithTime(lastTimeMoveToItem, 100)) {
            return;
        }
        lastTimeMoveToItem = System.currentTimeMillis();
        
        if (targetItem == null || zone == null) {
            botStatus = HUNTING;
            return;
        }
        nro.models.map.ItemMap item = zone.getItemMapByItemMapId(targetItem.itemMapId);
        if (item == null) {
            targetItem = null;
            botStatus = HUNTING;
            return;
        }
        
        int dx = item.x - this.location.x;
        int distance = Math.abs(dx);
        
        if (distance <= BotConfig.PICKUP_RANGE) {
            zone.pickItem(this, item.itemMapId);
            recentlyPickedItems.add(item.itemMapId);
            targetItem = null;
            botStatus = HUNTING;
        } else {
            int moveSpeed = Util.nextInt(10, 20);
            if (dx > 0) {
                this.location.x += Math.min(moveSpeed, dx);
            } else {
                this.location.x -= Math.min(moveSpeed, -dx);
            }
            int groundY = zone.map.yPhysicInTop(this.location.x, 0);
            if (groundY > 0) {
                this.location.y = groundY;
            }
            
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }
    
    /**
     * Find a mob to attack in the current zone
     */
    private Mob findMobToAttack() {
        if (zone == null) {
            return null;
        }

        Mob nearestMob = null;
        int nearestDistance = Integer.MAX_VALUE;

        synchronized (zone.mobs) {
            for (Mob mob : zone.mobs) {
                if (mob != null && !mob.isDie()) {
                    int distance = Util.getDistance(this, mob);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestMob = mob;
                    }
                }
            }
        }
        return nearestMob;
    }





    /**
     * Idle movement when not hunting
     */
    private void moveIdle() {
        if (Util.canDoWithTime(lastTimeMoveIdle, timeMoveIdle)) {
            int newX = location.x + Util.nextInt(-10, 10);
            int newY = location.y;
            PlayerService.gI().playerMove(this, newX, newY);
            lastTimeMoveIdle = System.currentTimeMillis();
            timeMoveIdle = Util.nextInt(3000, 6000);
        }
    }

    /**
     * Called when bot defeats a monster
     */
    public void onMonsterDefeated(Mob mob, long expGained) {
        monstersDefeated++;
        totalExperienceGained += expGained;
    }

    /**
     * Move to next map using targetWaypoint selected by startMovingMaps()
     * If no targetWaypoint, falls back to sequential map rotation
     */
    private void moveToNextMap() {
        if (!Util.canDoWithTime(lastTimeCheckMap, BotConfig.WAYPOINT_MOVE_INTERVAL)) {
            return;
        }
        lastTimeCheckMap = System.currentTimeMillis();
        
        if (zone == null || zone.map == null) {
            return;
        }
        if (targetWaypoint == null) {
            int[] planetMaps = BotConfig.MAP_LIST[this.gender];
            int targetMapId = planetMaps[currentMapIndex];
            if (zone.map.mapId == targetMapId) {
                botStatus = HUNTING;
                huntingStartTime = System.currentTimeMillis();
                huntingDuration = Util.nextInt(BotConfig.HUNTING_DURATION_MIN, BotConfig.HUNTING_DURATION_MAX);
                return;
            }
            targetWaypoint = findWaypointToMap(targetMapId);
            if (targetWaypoint == null) {
                currentMapIndex = (currentMapIndex + 1) % planetMaps.length;
                return;
            }
        }
        int waypointX = (targetWaypoint.minX + targetWaypoint.maxX) / 2;
        int dx = waypointX - this.location.x;
        int distanceX = Math.abs(dx);
        
        if (distanceX > BotConfig.WAYPOINT_REACH_DISTANCE) {
            int moveSpeed = Util.nextInt(BotConfig.MOVE_SPEED_TO_WAYPOINT_MIN, BotConfig.MOVE_SPEED_TO_WAYPOINT_MAX);
            if (dx > 0) {
                this.location.x += Math.min(moveSpeed, dx);
            } else {
                this.location.x -= Math.min(moveSpeed, -dx);
            }
            int groundY = zone.map.yPhysicInTop(this.location.x, 0);
            if (groundY > 0) {
                this.location.y = groundY;
            }
            
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        } else {
            int goMapId = targetWaypoint.goMap;
            int goX = targetWaypoint.goX;
            int goY = targetWaypoint.goY;
            
            // Find least populated zone in target map
            nro.models.map.Zone zoneJoin = findLeastPopulatedZoneInMap(goMapId);
            if (zoneJoin == null) {
                // Fallback to default
                zoneJoin = MapService.gI().getMapCanJoin(this, goMapId);
            }
            
            if (zoneJoin != null) {
                if (zone != null && zone.map != null) {
                    previousMapId = zone.map.mapId;
                }
                this.location.x = goX;
                this.location.y = goY;
                MapService.gI().goToMap(this, zoneJoin);
                zoneJoin.load_Me_To_Another(this);
                botStatus = HUNTING;
                huntingStartTime = System.currentTimeMillis();
                huntingDuration = Util.nextInt(BotConfig.HUNTING_DURATION_MIN, BotConfig.HUNTING_DURATION_MAX);
            }
            targetWaypoint = null;
        }
    }
    
    /**
     * Find the least populated zone in a map
     * @param mapId Map ID to search
     * @return Zone with fewest players, or null if map not found
     */
    private nro.models.map.Zone findLeastPopulatedZoneInMap(int mapId) {
        nro.models.map.Map map = MapService.gI().getMapById(mapId);
        if (map == null || map.zones == null) {
            return null;
        }
        
        nro.models.map.Zone bestZone = null;
        int minPlayers = Integer.MAX_VALUE;
        
        for (nro.models.map.Zone z : map.zones) {
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
     * Find waypoint that leads to target map
     */
    private nro.models.map.WayPoint findWaypointToMap(int targetMapId) {
        if (zone == null || zone.map == null || zone.map.wayPoints == null) {
            return null;
        }
        
        for (nro.models.map.WayPoint wp : zone.map.wayPoints) {
            if (wp.goMap == targetMapId) {
                return wp;
            }
        }
        return null;
    }
    
    /**
     * Start moving between maps on bot's planet
     * Randomly chooses from available waypoints, avoiding previous map if configured
     * When PREFER_MOB_MAPS is enabled, uses weighted selection favoring maps with more mobs
     */
    public void startMovingMaps() {
        botStatus = MOVE_MAP;
        targetWaypoint = null;
        huntingStartTime = 0;
        
        // Find all available waypoints and pick one
        if (zone != null && zone.map != null && zone.map.wayPoints != null) {
            int[] planetMaps = BotConfig.MAP_LIST[this.gender];
            java.util.List<nro.models.map.WayPoint> validWaypoints = new java.util.ArrayList<>();
            
            // Find waypoints that lead to maps in our planet
            for (nro.models.map.WayPoint wp : zone.map.wayPoints) {
                for (int mapId : planetMaps) {
                    if (wp.goMap == mapId && wp.goMap != zone.map.mapId) {
                        validWaypoints.add(wp);
                        break;
                    }
                }
            }
            
            // Filter out previous map if configured
            validWaypoints = filterWaypoints(validWaypoints);
            
            // Select waypoint based on configuration
            if (!validWaypoints.isEmpty()) {
                if (BotConfig.PREFER_MOB_MAPS) {
                    // Use weighted selection based on mob count
                    targetWaypoint = selectWeightedWaypoint(validWaypoints);
                } else {
                    // Use simple random selection
                    targetWaypoint = validWaypoints.get(Util.nextInt(0, validWaypoints.size() - 1));
                }
                return;
            }
        }
        
        // Fallback: use sequential if no waypoints found
        int[] planetMaps = BotConfig.MAP_LIST[this.gender];
        currentMapIndex = (currentMapIndex + 1) % planetMaps.length;
    }
    
    /**
     * Filter waypoints to exclude previous map (when AVOID_PREVIOUS_MAP is enabled)
     * If all waypoints are filtered out, returns original list as fallback
     * 
     * @param waypoints List of valid waypoints to filter
     * @return Filtered list of waypoints, or original list if all would be filtered
     */
    private java.util.List<nro.models.map.WayPoint> filterWaypoints(java.util.List<nro.models.map.WayPoint> waypoints) {
        // If feature is disabled or no previous map, return original list
        if (!BotConfig.AVOID_PREVIOUS_MAP || previousMapId < 0) {
            return waypoints;
        }
        
        // If only one waypoint, return it (fallback behavior per Requirement 1.2)
        if (waypoints.size() <= 1) {
            return waypoints;
        }
        
        // Filter out waypoints that lead to previous map
        java.util.List<nro.models.map.WayPoint> filtered = new java.util.ArrayList<>();
        for (nro.models.map.WayPoint wp : waypoints) {
            if (wp.goMap != previousMapId) {
                filtered.add(wp);
            }
        }
        
        // If all waypoints were filtered out, return original list as fallback
        if (filtered.isEmpty()) {
            return waypoints;
        }
        
        return filtered;
    }
    
    /**
     * Get count of alive mobs in a map's zone
     * Returns 0 if zone not found or error occurs
     * 
     * @param mapId The map ID to check mob count for
     * @return Number of alive mobs in the zone, or 0 if unavailable
     */
    private int getMobCountForMap(int mapId) {
        try {
            nro.models.map.Zone targetZone = MapService.gI().getMapCanJoin(this, mapId);
            if (targetZone == null || targetZone.mobs == null) {
                return 0;
            }
            
            int aliveCount = 0;
            synchronized (targetZone.mobs) {
                for (nro.models.mob.Mob mob : targetZone.mobs) {
                    if (mob != null && !mob.isDie()) {
                        aliveCount++;
                    }
                }
            }
            return aliveCount;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Select a waypoint using weighted random selection based on mob count
     * Maps with more mobs have higher probability of being selected
     * Uses MOB_WEIGHT_MULTIPLIER to scale the weight difference
     * 
     * @param waypoints List of valid waypoints to choose from
     * @return Selected waypoint based on weighted random selection
     */
    private nro.models.map.WayPoint selectWeightedWaypoint(java.util.List<nro.models.map.WayPoint> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return null;
        }
        
        // If only one waypoint, return it directly
        if (waypoints.size() == 1) {
            return waypoints.get(0);
        }
        double[] weights = new double[waypoints.size()];
        double totalWeight = 0;
        
        for (int i = 0; i < waypoints.size(); i++) {
            nro.models.map.WayPoint wp = waypoints.get(i);
            int mobCount = getMobCountForMap(wp.goMap);
            double weight = 1.0 + (mobCount * BotConfig.MOB_WEIGHT_MULTIPLIER);
            weights[i] = weight;
            totalWeight += weight;
        }
        
        // If total weight is 0 (shouldn't happen), fall back to random selection
        if (totalWeight <= 0) {
            return waypoints.get(Util.nextInt(0, waypoints.size() - 1));
        }
        
        // Perform weighted random selection
        double randomValue = Math.random() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < waypoints.size(); i++) {
            cumulativeWeight += weights[i];
            if (randomValue <= cumulativeWeight) {
                nro.models.map.WayPoint selected = waypoints.get(i);
                return selected;
            }
        }
        return waypoints.get(waypoints.size() - 1);
    }
    
    /**
     * Start attacking boss - find alive boss and travel to it
     * Also upgrades bot with full skills for boss fight
     */
    public void startAttackBoss() {
        try {
            targetBoss = findAliveBoss();
            if (targetBoss != null && targetBoss.zone != null) {
                botStatus = ATTACK_BOSS;
                bossAttackStartTime = System.currentTimeMillis();
                nro.bot.BotManager.gI().initializeFullSkills(this);
                this.nPoint.hpMax = Util.nextInt(500_000,5_000_000);
                this.nPoint.mpMax = 500_000_000;
                this.nPoint.dame = 2_000_000;
                this.nPoint.stamina = 10000;
                this.nPoint.maxStamina = 10000;
                this.nPoint.setFullHpMp();
                nro.models.map.Zone bossZone = targetBoss.zone;
                // Spawn bot xung quanh boss (trái hoặc phải)
                int offset = Util.nextInt(-200, 200);
                int bossX = targetBoss.location.x + offset;
                this.location.x = Math.max(50, Math.min(bossX, 500));
                try {
                    this.location.y = bossZone.map.yPhysicInTop(this.location.x, 0);
                } catch (Exception ex) {
                    this.location.y = targetBoss.location.y; // Fallback to boss Y
                }
                MapService.gI().goToMap(this, bossZone);
                bossZone.load_Me_To_Another(this);
            } else {
                botStatus = HUNTING;
                huntingStartTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
            botStatus = HUNTING;
            huntingStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Find an alive boss from BOSS_IDS list
     */
    private nro.models.boss.Boss findAliveBoss() {
        java.util.List<nro.models.boss.Boss> aliveBosses = new java.util.ArrayList<>();
        
        for (nro.models.boss.Boss boss : nro.models.boss.BossManager.BOSSES_IN_GAME) {
            if (boss != null && !boss.isDie() && boss.zone != null) {
                if (boss.name != null && boss.name.contains("Broly")) {
                    continue;
                }
                aliveBosses.add(boss);
            }
        }
        
        if (aliveBosses.isEmpty()) {
            return null;
        }
        return aliveBosses.get(Util.nextInt(0, aliveBosses.size() - 1));
    }
    
    /**
     * Attack boss logic - di chuyển đến boss và tấn công
     */
    private void attackBoss() {
        // Đổi boss mới sau một khoảng thời gian
        if (bossAttackStartTime > 0 && System.currentTimeMillis() - bossAttackStartTime >= BotConfig.BOSS_ATTACK_DURATION) {
            startAttackBoss();
            return;
        }
        // Boss chết hoặc không còn trong zone
        if (targetBoss == null || targetBoss.isDie() || targetBoss.zone != this.zone) {
            startAttackBoss();
            return;
        }
        // Delay giữa các lần tấn công
        if (!Util.canDoWithTime(lastTimeAttack, BotConfig.BOSS_ATTACK_DELAY)) {
            return;
        }
        
        // Luôn cập nhật vị trí Y theo boss (boss có thể bay lên xuống)
        if (Math.abs(this.location.y - targetBoss.location.y) > 30) {
            walkTowardsBoss();
            return;
        }
        
        int distance = Util.getDistance(this, targetBoss);
        
        // Nếu xa boss thì di chuyển lại gần
        if (distance > BotConfig.ATTACK_RANGE_MELEE) {
            walkTowardsBoss();
            return;
        }
        
        // Dùng skill đặc biệt trước
        if (trySpecialSkills()) {
            lastTimeAttack = System.currentTimeMillis();
            return;
        }
        
        // Dùng skill tấn công (type 1 - skill gây dame)
        Skill attackSkill = getSkillByType(1);
        if (attackSkill != null && attackSkill.template != null) {
            this.playerSkill.skillSelect = attackSkill;
            if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                SkillService.gI().useSkill(this, targetBoss, null, null);
                lastTimeAttack = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Try to use special skills like BIEN_KHI, KHIEN_NANG_LUONG, DE_TRUNG, etc.
     * Returns true if a special skill was used
     */
    private boolean trySpecialSkills() {
        try {
            Skill bienKhi = getSkillById(Skill.BIEN_KHI);
            if (bienKhi != null && !this.effectSkill.isMonkey) {
                this.playerSkill.skillSelect = bienKhi;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkillBienKhi(this);
                    return true;
                }
            }
            
            // Try KHIEN_NANG_LUONG (shield)
            Skill khien = getSkillById(Skill.KHIEN_NANG_LUONG);
            if (khien != null && !this.effectSkill.isShielding) {
                this.playerSkill.skillSelect = khien;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkillKhien(this);
                    return true;
                }
            }
            
            // Try DE_TRUNG (summon)
            Skill deTrung = getSkillById(Skill.DE_TRUNG);
            if (deTrung != null && this.mobMe == null) {
                this.playerSkill.skillSelect = deTrung;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkillDeTrung(this);
                    return true;
                }
            }
            
            // Try TAI_TAO_NANG_LUONG (charge energy when low HP/MP)
            Skill ttnl = getSkillById(Skill.TAI_TAO_NANG_LUONG);
            if (ttnl != null && (this.nPoint.getCurrPercentHP() <= 30 || this.nPoint.getCurrPercentMP() <= 30)) {
                this.playerSkill.skillSelect = ttnl;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkillTTNL(this);
                    return true;
                }
            }
            
            // Try THAI_DUONG_HA_SAN (special attack)
            Skill thaiDuong = getSkillById(Skill.THAI_DUONG_HA_SAN);
            if (thaiDuong != null) {
                this.playerSkill.skillSelect = thaiDuong;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    SkillService.gI().useSkillThaiDuong(this);
                    return true;
                }
            }
            
            // Try KAIOKEN (rush attack)
            Skill kaioken = getSkillById(Skill.KAIOKEN);
            if (kaioken != null && targetBoss != null) {
                this.playerSkill.skillSelect = kaioken;
                if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                    int dis = Util.getDistance(this, targetBoss);
                    if (dis > 50) {
                        PlayerService.gI().playerMove(this, targetBoss.location.x, targetBoss.location.y);
                    } else {
                        PlayerService.gI().playerMove(this, targetBoss.location.x + Util.nextInt(-20, 20), targetBoss.location.y);
                    }
                    SkillService.gI().useSkill(this, targetBoss, null, null);
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors in special skill usage
        }
        return false;
    }
    
    /**
     * Get skill by skill ID
     */
    private Skill getSkillById(int skillId) {
        if (playerSkill == null || playerSkill.skills == null || playerSkill.skills.isEmpty()) {
            return null;
        }
        
        for (Skill skill : playerSkill.skills) {
            if (skill != null && skill.template != null && skill.template.id == skillId) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * Get first skill of specific type
     * Type 0: Melee, Type 1: Ranged, Type 2: Special, Type 3: Buff
     */
    private Skill getSkillByType(int type) {
        if (playerSkill == null || playerSkill.skills == null || playerSkill.skills.isEmpty()) {
            return null;
        }
        
        for (Skill skill : playerSkill.skills) {
            if (skill != null && skill.template != null && skill.template.type == type) {
                return skill;
            }
        }
        
        return null;
    }
    
    /**
     * Di chuyển đến gần boss
     */
    private void walkTowardsBoss() {
        if (targetBoss == null) return;
        
        int targetX = targetBoss.location.x + Util.nextInt(-30, 30);
        int targetY = targetBoss.location.y;
        
        this.location.x = targetX;
        this.location.y = targetY;
        PlayerService.gI().playerMove(this, this.location.x, this.location.y);
    }
    
    /**
     * Get current target boss
     */
    public nro.models.boss.Boss getTargetBoss() {
        return targetBoss;
    }
    
    /**
     * Start going to NPC - teleport to map and stand near NPC
     * Also equip random outfit from NPC's shop (first tab = cải trang)
     */
    public void startTalkNpc(int mapId, int npcId) {
        botStatus = TALK_NPC;
        targetNpc = null;
        
        // Teleport to target map
        nro.models.map.Zone zoneJoin = MapService.gI().getMapCanJoin(this, mapId);
        if (zoneJoin != null) {
            // Find NPC in that map
            for (nro.models.npc.Npc npc : zoneJoin.map.npcs) {
                if (npc != null && npc.tempId == npcId) {
                    targetNpc = npc;
                    break;
                }
            }
            
            if (targetNpc != null) {
                // Equip random outfit BEFORE teleporting
                equipRandomOutfitFromShop();
                
                // Teleport to NPC position
                this.location.x = targetNpc.cx + Util.nextInt(-30, 30);
                this.location.y = targetNpc.cy;
                MapService.gI().goToMap(this, zoneJoin);
                zoneJoin.load_Me_To_Another(this);
                
                // Send outfit update to players in new zone
                Service.getInstance().Send_Caitrang(this);
                
                // Equip random title
                equipRandomTitle();
                
                // Equip random flag bag
                equipRandomFlagBag();
                
            } else {
                botStatus = HUNTING;
            }
        } else {
            botStatus = HUNTING;
        }
    }
    
    /**
     * Start shopping at NPC - teleport to NPC and auto-change outfit every 10s
     * Simulates a player browsing and buying outfits
     */
    public void startShoppingAtNpc(int mapId, int npcId, int shopNpcId) {
        // Enable auto outfit change
        autoChangeOutfit = true;
        outfitNpcId = shopNpcId;
        // Random initial delay (0-10s) so bots don't sync
        lastTimeChangeOutfit = System.currentTimeMillis() - Util.nextInt(0, 10000);
        // Random movement delay too
        lastTimeMoveIdle = System.currentTimeMillis() - Util.nextInt(0, 30000);
        
        // Use existing teleport logic
        startTalkNpc(mapId, npcId);
        
    }
    
    /**
     * Start AFK mode - join random map from MAP_BOT_AFK_FARM_DETU list
     * Bot stands still and lets pet do the fighting
     */
    public void startAfkMode() {
        botStatus = AFK;
        
        // Pick random map from AFK map list
        int[] afkMaps = BotConfig.MAP_BOT_AFK_FARM_DETU;
        int randomMapId = afkMaps[Util.nextInt(0, afkMaps.length - 1)];
        
        // Join the map
        nro.models.map.Zone zoneJoin = MapService.gI().getMapCanJoin(this, randomMapId);
        if (zoneJoin != null) {
            // Random position in map
            this.location.x = Util.nextInt(100, 400);
            this.location.y = 336;
            
            // Get ground Y at position
            int groundY = zoneJoin.map.yPhysicInTop(this.location.x, 0);
            if (groundY > 0) {
                this.location.y = groundY;
            }
            
            MapService.gI().goToMap(this, zoneJoin);
            zoneJoin.load_Me_To_Another(this);
            
            // Send outfit update
            Service.getInstance().Send_Caitrang(this);
            
            // Set AFK duration
            afkStartTime = System.currentTimeMillis();
            afkDuration = Util.nextInt(BotConfig.AFK_MAP_DURATION_MIN, BotConfig.AFK_MAP_DURATION_MAX);
            lastTimeAfkIdle = System.currentTimeMillis() - Util.nextInt(0, 10000); // Random offset
            
        } else {
            botStatus = IDLE;
        }
    }
    

    private void afkMode() {
        // Check if time to move to another map
        if (afkStartTime > 0 && System.currentTimeMillis() - afkStartTime >= afkDuration) {
            startAfkMode(); 
            return;
        }
        if (BotConfig.AUTO_SWITCH_ZONE && Util.canDoWithTime(lastTimeZoneSwitch, BotConfig.ZONE_SWITCH_COOLDOWN)) {
            if (isZoneCrowded()) {
                switchToLessPopulatedZone();
            }
        }
        Mob nearestMob = findMobToAttack();
        if (nearestMob != null) {
            int distance = Util.getDistance(this, nearestMob);
            if (distance > 200) {
                int dx = nearestMob.location.x - this.location.x;
                int moveSpeed = Util.nextInt(10, 20);
                if (dx > 0) {
                    this.location.x += Math.min(moveSpeed, dx - 150);
                } else {
                    this.location.x -= Math.min(moveSpeed, -dx - 150);
                }
                if (zone != null && zone.map != null) {
                    int groundY = zone.map.yPhysicInTop(this.location.x, 0);
                    if (groundY > 0) {
                        this.location.y = groundY;
                    }
                }
                PlayerService.gI().playerMove(this, this.location.x, this.location.y);
            }
        } else {
            if (Util.canDoWithTime(lastTimeAfkIdle, Util.nextInt(BotConfig.AFK_IDLE_MOVE_INTERVAL_MIN, BotConfig.AFK_IDLE_MOVE_INTERVAL_MAX))) {
                int newX = location.x + Util.nextInt(-BotConfig.AFK_IDLE_MOVE_DISTANCE, BotConfig.AFK_IDLE_MOVE_DISTANCE);
                // Use yPhysicInTop to follow terrain
                int newY = location.y;
                if (zone != null && zone.map != null) {
                    int groundY = zone.map.yPhysicInTop(newX, 0);
                    if (groundY > 0) {
                        newY = groundY;
                    }
                }
                PlayerService.gI().playerMove(this, newX, newY);
                lastTimeAfkIdle = System.currentTimeMillis();
            }
        }
        if (Util.canDoWithTime(lastTimeChangeTitle, Util.nextInt(BotConfig.TITLE_CHANGE_INTERVAL_MIN, BotConfig.TITLE_CHANGE_INTERVAL_MAX))) {
            equipRandomTitle();
            lastTimeChangeTitle = System.currentTimeMillis();
        }
        if (Util.canDoWithTime(lastTimeChangeFlagBag, Util.nextInt(BotConfig.FLAG_BAG_CHANGE_INTERVAL_MIN, BotConfig.FLAG_BAG_CHANGE_INTERVAL_MAX))) {
            equipRandomFlagBag();
            lastTimeChangeFlagBag = System.currentTimeMillis();
        }
        
        // Buff pet with pea every 20 seconds
        if (Util.canDoWithTime(lastTimeEatPea, 20000)) {
            buffPetWithPea();
            lastTimeEatPea = System.currentTimeMillis();
        }
    }
    
    /**
     * Buff pet with pea - restore HP/MP for pet
     * Similar to InventoryService.eatPea but simplified for bot
     */
    private void buffPetWithPea() {
        if (pet == null || pet.isDie()) {
            return;
        }
        
        // Restore full HP/MP for pet
        pet.nPoint.setFullHpMp();
        
        // Send info to players in zone
        if (pet.zone != null) {
            Service.getInstance().sendInfoPlayerEatPea(pet);
        }
    }
    
    /**
    /**
     * Check if current zone is crowded based on config
     * Uses either max players or player/mob ratio
     */
    private boolean isZoneCrowded() {
        if (zone == null) {
            return false;
        }
        
        int playerCount = zone.getPlayers().size();
        
        // Check max players threshold if configured
        if (BotConfig.ZONE_MAX_PLAYERS > 0) {
            return playerCount > BotConfig.ZONE_MAX_PLAYERS;
        }
        
        // Check player/mob ratio
        int mobCount = 0;
        synchronized (zone.mobs) {
            for (Mob mob : zone.mobs) {
                if (mob != null && !mob.isDie()) {
                    mobCount++;
                }
            }
        }
        
        // Avoid division by zero
        if (mobCount == 0) {
            return playerCount > 3; // Default: crowded if > 3 players and no mobs
        }
        
        double ratio = (double) playerCount / mobCount;
        return ratio > BotConfig.ZONE_PLAYER_MOB_RATIO;
    }
    
    /**
     * Switch to a less populated zone in the same map
     * Called when current zone has too many players
     */
    private void switchToLessPopulatedZone() {
        if (zone == null || zone.map == null) {
            return;
        }
        
        nro.models.map.Zone bestZone = null;
        int minPlayers = zone.getPlayers().size();
        
        // Find zone with fewer players in same map
        for (nro.models.map.Zone z : zone.map.zones) {
            if (z != null && z != zone) {
                int playerCount = z.getPlayers().size();
                if (playerCount < minPlayers) {
                    minPlayers = playerCount;
                    bestZone = z;
                }
            }
        }
        
        // Switch to less populated zone if difference is significant
        if (bestZone != null && (zone.getPlayers().size() - minPlayers) >= BotConfig.ZONE_SWITCH_MIN_DIFF) {
            MapService.gI().goToMap(this, bestZone);
            bestZone.load_Me_To_Another(this);
            lastTimeZoneSwitch = System.currentTimeMillis();
        }
    }
    
    /**
     * Equip farm mob outfit based on gender/planet
     * Uses AO_FARM_MOBS and QUAN_FARM_MOBS from BotConfig
     */
    public void equipFarmMobOutfit() {
        try {
            // Ensure itemsBody has enough slots (0=áo, 1=quần, 2=găng, 3=giày, 4=vũ khí, 5=cải trang)
            while (this.inventory.itemsBody.size() < 6) {
                this.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            
            // Get random áo for this gender
            int[] aoOptions = BotConfig.AO_FARM_MOBS[this.gender];
            int aoId = aoOptions[Util.nextInt(0, aoOptions.length - 1)];
            nro.models.item.Item ao = ItemService.gI().createNewItem((short) aoId);
            if (ao != null && ao.template != null) {
                this.inventory.itemsBody.set(0, ao);
            }
            
            // Get random quần for this gender
            int[] quanOptions = BotConfig.QUAN_FARM_MOBS[this.gender];
            int quanId = quanOptions[Util.nextInt(0, quanOptions.length - 1)];
            nro.models.item.Item quan = ItemService.gI().createNewItem((short) quanId);
            if (quan != null && quan.template != null) {
                this.inventory.itemsBody.set(1, quan);
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * Equip random outfit (cải trang) from NPC's shop
     * Public method - can be called from BotManager
     */
    public void equipRandomOutfitFromShop() {
        try {
            int shopNpcId = nro.consts.ConstNpc.SANTA; // 39
            for (nro.models.shop.Shop shop : nro.server.Manager.SHOPS) {
                if (shop.npcId == shopNpcId && shop.shopOrder == 1) {
                    // Get first tab (cải trang)
                    if (!shop.tabShops.isEmpty()) {
                        nro.models.shop.TabShop tabShop = shop.tabShops.get(0);
                        if (!tabShop.itemShops.isEmpty()) {
                            // Pick random item from tab
                            int randomIndex = Util.nextInt(0, tabShop.itemShops.size() - 1);
                            nro.models.shop.ItemShop itemShop = tabShop.itemShops.get(randomIndex);
                            
                            // Create item and equip to slot 5 (cải trang slot)
                            nro.models.item.Item item = ItemService.gI().createNewItem(itemShop.temp.id);
                            item.itemOptions.clear();
                            for (nro.models.item.ItemOption opt : itemShop.options) {
                                item.itemOptions.add(new nro.models.item.ItemOption(opt.optionTemplate.id, opt.param));
                            }
                            
                            // Ensure itemsBody has enough slots
                            while (this.inventory.itemsBody.size() <= 5) {
                                this.inventory.itemsBody.add(ItemService.gI().createItemNull());
                            }
                            
                            // Equip to slot 5 (cải trang)
                            this.inventory.itemsBody.set(5, item);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * Stay near NPC - idle and optionally change outfit periodically
     * Auto move to next NPC after duration
     */
    private void goToNpc() {
        // Already at NPC, just stay idle
        if (targetNpc == null) {
            botStatus = HUNTING;
            return;
        }
        
        // If walking to NPC, continue walking (priority over other actions)
        if (isWalkingToNpc) {
            walkToNpc();
            return;
        }
        
        // Check if time to move to next NPC
        if (npcVisitStartTime > 0 && System.currentTimeMillis() - npcVisitStartTime >= npcVisitDuration) {
            moveToNextNpc();
            return;
        }
        
        // Auto switch to random less populated zone (every 30-60 seconds)
        if (zoneSwitchIntervalNpc > 0 && Util.canDoWithTime(lastTimeZoneSwitchNpc, zoneSwitchIntervalNpc)) {
            switchToRandomLessPopulatedZone();
            lastTimeZoneSwitchNpc = System.currentTimeMillis();
            zoneSwitchIntervalNpc = Util.nextInt(30000, 60000); // Random next interval
        }
        
        // Auto change outfit every 8-15 seconds (random interval for async behavior)
        if (autoChangeOutfit && Util.canDoWithTime(lastTimeChangeOutfit, Util.nextInt(8000, 15000))) {
            equipRandomOutfitFromShop();
            Service.getInstance().Send_Caitrang(this);
            lastTimeChangeOutfit = System.currentTimeMillis();
        }
        
        // Auto change title every 30-60 seconds
        if (Util.canDoWithTime(lastTimeChangeTitle, Util.nextInt(BotConfig.TITLE_CHANGE_INTERVAL_MIN, BotConfig.TITLE_CHANGE_INTERVAL_MAX))) {
            equipRandomTitle();
            lastTimeChangeTitle = System.currentTimeMillis();
        }
        
        // Auto change flag bag every 45-90 seconds
        if (Util.canDoWithTime(lastTimeChangeFlagBag, Util.nextInt(BotConfig.FLAG_BAG_CHANGE_INTERVAL_MIN, BotConfig.FLAG_BAG_CHANGE_INTERVAL_MAX))) {
            equipRandomFlagBag();
            lastTimeChangeFlagBag = System.currentTimeMillis();
        }
        
        // Small random movement near NPC (every 30-40 seconds) - only when not walking
        if (!isWalkingToNpc && Util.canDoWithTime(lastTimeMoveIdle, Util.nextInt(30000, 40000))) {
            int newX = targetNpc.cx + Util.nextInt(-50, 50);
            this.location.x = newX;
            
            // Follow terrain
            if (zone != null && zone.map != null) {
                int groundY = zone.map.yPhysicInTop(this.location.x, 0);
                if (groundY > 0) {
                    this.location.y = groundY;
                }
            }
            
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
            lastTimeMoveIdle = System.currentTimeMillis();
        }
    }
    
    // Walking to NPC state
    private boolean isWalkingToNpc = false;
    private int walkTargetX = 0;
    private long lastTimeWalkToNpc = 0;
    
    /**
     * Move to next NPC in the visit list (same map, different NPC)
     * If NPC_VISITOR_WALK_TO_NPC is true, bot will walk slowly to NPC
     * Otherwise, bot will teleport directly
     */
    private void moveToNextNpc() {
        if (BotConfig.NPC_VISIT_IDS.length == 0) {
            return;
        }
        
        // Pick random NPC (different from current if possible)
        int newIndex;
        if (BotConfig.NPC_VISIT_IDS.length > 1) {
            do {
                newIndex = Util.nextInt(0, BotConfig.NPC_VISIT_IDS.length - 1);
            } while (newIndex == currentNpcIndex);
        } else {
            newIndex = 0;
        }
        currentNpcIndex = newIndex;
        int npcId = BotConfig.NPC_VISIT_IDS[currentNpcIndex];
        
        if (zone != null && zone.map != null) {
            for (nro.models.npc.Npc npc : zone.map.npcs) {
                if (npc != null && npc.tempId == npcId) {
                    targetNpc = npc;
                    
                    if (BotConfig.NPC_VISITOR_WALK_TO_NPC) {
                        walkTargetX = npc.cx + Util.nextInt(-30, 30);
                        isWalkingToNpc = true;
                        lastTimeWalkToNpc = System.currentTimeMillis();
                    } else {
                        this.location.x = npc.cx + Util.nextInt(-30, 30);
                        int groundY = zone.map.yPhysicInTop(this.location.x, 0);
                        if (groundY > 0) {
                            this.location.y = groundY;
                        }
                        PlayerService.gI().playerMove(this, this.location.x, this.location.y);
                        performNpcAction(currentNpcIndex);
                    }
                    break;
                }
            }
        }
        npcVisitStartTime = System.currentTimeMillis();
        npcVisitDuration = Util.nextInt(BotConfig.NPC_VISIT_DURATION_MIN, BotConfig.NPC_VISIT_DURATION_MAX);
    }
    
    /**
     * Walk slowly towards target NPC
     * Called from goToNpc() when isWalkingToNpc is true
     */
    private void walkToNpc() {
        if (targetNpc == null || zone == null) {
            isWalkingToNpc = false;
            return;
        }
        if (!Util.canDoWithTime(lastTimeWalkToNpc, 50)) {
            return;
        }
        lastTimeWalkToNpc = System.currentTimeMillis();
        int dx = walkTargetX - this.location.x;
        int distance = Math.abs(dx);
        if (distance <= 10) {
            isWalkingToNpc = false;
            this.location.x = walkTargetX;
            if (zone.map != null) {
                int groundY = zone.map.yPhysicInTop(this.location.x, 0);
                if (groundY > 0) {
                    this.location.y = groundY;
                }
            }
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
            performNpcAction(currentNpcIndex);
        } else {
            int walkSpeed = Util.nextInt(BotConfig.NPC_WALK_SPEED_MIN, BotConfig.NPC_WALK_SPEED_MAX);
            if (dx > 0) {
                this.location.x += Math.min(walkSpeed, dx);
            } else {
                this.location.x -= Math.min(walkSpeed, -dx);
            }
            if (zone.map != null) {
                int groundY = zone.map.yPhysicInTop(this.location.x, 0);
                if (groundY > 0) {
                    this.location.y = groundY;
                }
            }
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }
    
    /**
     * Perform action based on NPC type
     * @param npcIndex Index in NPC_VISIT_IDS array
     */
    private void performNpcAction(int npcIndex) {
        if (npcIndex < 0 || npcIndex >= BotConfig.NPC_VISIT_ACTIONS.length) {
            return;
        }
        
        int action = BotConfig.NPC_VISIT_ACTIONS[npcIndex];
        switch (action) {
            case BotConfig.NPC_ACTION_OUTFIT:
                // Change outfit at Santa
                equipRandomOutfitFromShop();
                Service.getInstance().Send_Caitrang(this);
                break;
                
            case BotConfig.NPC_ACTION_PET:
                // Change pet at Gogeta SSJ4
                changePetType();
                break;
                
            case BotConfig.NPC_ACTION_TITLE:
                // Change title at Toribot
                equipRandomTitle();
                break;
                
            default:
                // Do nothing
                break;
        }
    }
    
    /**
     * Change pet to random type
     */
    private void changePetType() {
        if (pet == null) {
            return;
        }
        
        // Random pet type
        nro.models.player.Pet.PetType[] types = nro.models.player.Pet.PetType.values();
        nro.models.player.Pet.PetType newType = types[Util.nextInt(0, types.length - 1)];
        
        try {
            PetService.gI().changePet(this, newType);
        } catch (Exception e) {
        }
    }
    
    /**
     * Start NPC visitor mode - visit NPCs in rotation (same map)
     */
    public void startNpcVisitorMode() {
        botStatus = TALK_NPC;
        autoChangeOutfit = true;
        
        // Start with random NPC
        currentNpcIndex = Util.nextInt(0, BotConfig.NPC_VISIT_IDS.length - 1);
        int npcId = BotConfig.NPC_VISIT_IDS[currentNpcIndex];
        
        // Find least populated zone first
        nro.models.map.Zone bestZone = findLeastPopulatedZone(BotConfig.NPC_VISITOR_MAP_ID);
        if (bestZone != null) {
            // Find NPC in that zone
            for (nro.models.npc.Npc npc : bestZone.map.npcs) {
                if (npc != null && npc.tempId == npcId) {
                    targetNpc = npc;
                    break;
                }
            }
            
            if (targetNpc != null) {
                this.location.x = targetNpc.cx + Util.nextInt(-30, 30);
                int groundY = bestZone.map.yPhysicInTop(this.location.x, 0);
                this.location.y = groundY > 0 ? groundY : targetNpc.cy;
                MapService.gI().goToMap(this, bestZone);
                bestZone.load_Me_To_Another(this);
                Service.getInstance().Send_Caitrang(this);
            }
        } else {
            // Fallback
            startTalkNpc(BotConfig.NPC_VISITOR_MAP_ID, npcId);
        }
        
        // Set visit duration
        npcVisitStartTime = System.currentTimeMillis();
        npcVisitDuration = Util.nextInt(BotConfig.NPC_VISIT_DURATION_MIN, BotConfig.NPC_VISIT_DURATION_MAX);
        
        // Init zone switch interval (random 30-60 seconds)
        lastTimeZoneSwitchNpc = System.currentTimeMillis();
        zoneSwitchIntervalNpc = Util.nextInt(30000, 60000);
    }
    
    /**
     * Switch to a random zone with fewer players than current
     * More dynamic than always picking the least populated
     * Also moves to random NPC to avoid sync
     */
    private void switchToRandomLessPopulatedZone() {
        if (zone == null || zone.map == null) {
            return;
        }
        
        int currentPlayers = zone.getPlayers().size();
        java.util.List<nro.models.map.Zone> lessPopulatedZones = new java.util.ArrayList<>();
        
        // Find all zones with fewer or equal players (more options)
        for (nro.models.map.Zone z : zone.map.zones) {
            if (z != null && z != zone && z.getPlayers().size() <= currentPlayers) {
                lessPopulatedZones.add(z);
            }
        }
        
        // Pick random zone
        if (!lessPopulatedZones.isEmpty()) {
            nro.models.map.Zone newZone = lessPopulatedZones.get(Util.nextInt(0, lessPopulatedZones.size() - 1));
            
            // Pick random NPC to go to (avoid sync)
            int newNpcIndex;
            if (BotConfig.NPC_VISIT_IDS.length > 1) {
                do {
                    newNpcIndex = Util.nextInt(0, BotConfig.NPC_VISIT_IDS.length - 1);
                } while (newNpcIndex == currentNpcIndex);
            } else {
                newNpcIndex = 0;
            }
            currentNpcIndex = newNpcIndex;
            int npcId = BotConfig.NPC_VISIT_IDS[currentNpcIndex];
            
            // Find new NPC in new zone
            nro.models.npc.Npc newNpc = null;
            for (nro.models.npc.Npc npc : newZone.map.npcs) {
                if (npc != null && npc.tempId == npcId) {
                    newNpc = npc;
                    break;
                }
            }
            
            if (newNpc != null) {
                targetNpc = newNpc;
                // Move to new NPC position
                this.location.x = newNpc.cx + Util.nextInt(-30, 30);
                int groundY = newZone.map.yPhysicInTop(this.location.x, 0);
                this.location.y = groundY > 0 ? groundY : newNpc.cy;
            }
            
            // Switch zone
            MapService.gI().goToMap(this, newZone);
            newZone.load_Me_To_Another(this);
        }
    }
    
    /**
     * Find the least populated zone in a map
     */
    private nro.models.map.Zone findLeastPopulatedZone(int mapId) {
        nro.models.map.Map map = MapService.gI().getMapById(mapId);
        if (map == null || map.zones == null) {
            return MapService.gI().getMapCanJoin(this, mapId);
        }
        
        nro.models.map.Zone bestZone = null;
        int minPlayers = Integer.MAX_VALUE;
        
        for (nro.models.map.Zone z : map.zones) {
            if (z != null) {
                int playerCount = z.getPlayers().size();
                if (playerCount < minPlayers) {
                    minPlayers = playerCount;
                    bestZone = z;
                }
            }
        }
        
        return bestZone != null ? bestZone : MapService.gI().getMapCanJoin(this, mapId);
    }
    
    /**
     * Equip random title (danh hiệu) for bot
     * Sends title to all players in map
     */
    public void equipRandomTitle() {
        try {
            // Remove old title first to prevent stacking
            if (currentTitlePart > 0) {
                removeTitleFromMap();
            }
            
            // Pick random title from config
            int randomIndex = Util.nextInt(0, BotConfig.TITLE_PARTS.length - 1);
            int titlePart = BotConfig.TITLE_PARTS[randomIndex];
            
            // Set title properties
            this.partDanhHieu = titlePart;
            this.titleitem = true;
            currentTitlePart = titlePart;
            
            // Send new title to all players in map
            sendTitleToMap(titlePart);
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Equip specific title (danh hiệu) for bot
     * @param titlePart Title part ID to equip
     */
    public void equipTitle(int titlePart) {
        if (titlePart < 0) return;
        try {
            if (currentTitlePart > 0) {
                removeTitleFromMap();
            }
            this.partDanhHieu = titlePart;
            this.titleitem = true;
            currentTitlePart = titlePart;
            sendTitleToMap(titlePart);
        } catch (Exception e) {
        }
    }
    
    /**
     * Equip specific flag bag for bot
     * @param flagBagId Flag bag ID to equip
     */
    public void equipFlagBag(int flagBagId) {
        if (flagBagId < 0) return;
        currentFlagBagId = flagBagId;
        sendFlagBagToMap();
    }
    
    /**
     * Equip specific outfit (cải trang) for bot by item ID
     * @param itemId Item ID of the outfit to equip
     */
    public void equipOutfit(int itemId) {
        if (itemId < 0) return;
        try {
            nro.models.item.Item item = ItemService.gI().createNewItem((short) itemId);
            if (item == null || item.template == null) {
                return;
            }
            
            // Ensure itemsBody has enough slots
            while (this.inventory.itemsBody.size() <= 5) {
                this.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            
            // Equip to slot 5 (cải trang)
            this.inventory.itemsBody.set(5, item);
        } catch (Exception e) {
        }
    }
    
    /**
     * Send title to all players in current map
     */
    private void sendTitleToMap(int part) {
        if (zone == null) return;
        
        try {
            nro.server.io.Message me = new nro.server.io.Message(-128);
            me.writer().writeByte(0);
            me.writer().writeInt((int) this.id);
            me.writer().writeShort(part);
            me.writer().writeByte(1);
            me.writer().writeByte(-1);
            me.writer().writeShort(50);
            me.writer().writeByte(-1);
            me.writer().writeByte(-1);
            
            // Send to all players in zone
            for (Player p : zone.getPlayers()) {
                if (p != null && !p.isBot && p.getSession() != null) {
                    p.sendMessage(me);
                }
            }
            me.cleanup();
        } catch (Exception e) {
        }
    }
    
    /**
     * Remove title from map (send remove message to all players)
     * Used internally before changing to new title
     */
    private void removeTitleFromMap() {
        if (zone == null) return;
        
        try {
            nro.server.io.Message me = new nro.server.io.Message(-128);
            me.writer().writeByte(2);
            me.writer().writeInt((int) this.id);
            
            // Send to all players in zone
            for (Player p : zone.getPlayers()) {
                if (p != null && !p.isBot && p.getSession() != null) {
                    p.sendMessage(me);
                }
            }
            me.cleanup();
        } catch (Exception e) {
        }
    }
    
    /**
     * Remove title from bot
     */
    public void removeTitle() {
        if (zone == null) return;
        
        try {
            this.titleitem = false;
            this.partDanhHieu = 0;
            currentTitlePart = -1;
            
            nro.server.io.Message me = new nro.server.io.Message(-128);
            me.writer().writeByte(2);
            me.writer().writeInt((int) this.id);
            
            // Send to all players in zone
            for (Player p : zone.getPlayers()) {
                if (p != null && !p.isBot && p.getSession() != null) {
                    p.sendMessage(me);
                }
            }
            me.cleanup();
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Get current title part
     */
    public int getCurrentTitlePart() {
        return currentTitlePart;
    }
    
    /**
     * Equip random flag bag for bot
     * Sends flag bag to all players in map
     */
    public void equipRandomFlagBag() {
        try {
            // Pick random flag bag from config
            int randomIndex = Util.nextInt(0, BotConfig.FLAG_BAG_IDS.length - 1);
            int flagBagId = BotConfig.FLAG_BAG_IDS[randomIndex];
            
            currentFlagBagId = flagBagId;
            
            // Send flag bag to all players in map
            sendFlagBagToMap();
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Send flag bag to all players in current map
     */
    private void sendFlagBagToMap() {
        if (zone == null) return;
        
        try {
            nro.server.io.Message msg = new nro.server.io.Message(-64);
            msg.writer().writeInt((int) this.id);
            msg.writer().writeByte(getFlagBag());
            
            // Send to all players in zone
            for (Player p : zone.getPlayers()) {
                if (p != null && !p.isBot && p.getSession() != null) {
                    p.sendMessage(msg);
                }
            }
            msg.cleanup();
        } catch (Exception e) {
        }
    }
    
    /**
     * Get current flag bag ID
     */
    public int getCurrentFlagBagId() {
        return currentFlagBagId;
    }
    
    /**
     * Create a pet (đệ tử) for this bot
     * Pet will follow the bot like a normal player's pet
     */
    public void createBotPet() {
        createBotPet(nro.models.player.Pet.PetType.FIDE_TRAU);
    }
    
    /**
     * Create a pet (đệ tử) for this bot with specific type
     * @param petType Type of pet (NONE, MABU, SAYAN5, etc.)
     */
    public void createBotPet(nro.models.player.Pet.PetType petType) {
        try {
            if (this.pet != null) {
                return;
            }
            
            // Create pet using PetService
            PetService.gI().createPet(this, petType);
            hasPet = true;
            
            // Wait for pet to be created then set name
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Wait for PetService to finish
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Make bot's pet follow the bot and attack mobs
     * Called in update() to keep pet near bot and in ATTACK mode
     */
    private void updatePetFollow() {
        if (pet == null || pet.isDie()) {
            return;
        }
        if (pet.zone == null || pet.zone != this.zone) {
            pet.joinMapMaster();
        }
        if (pet.status != nro.models.player.Pet.ATTACK && 
            pet.status != nro.models.player.Pet.FUSION && 
            pet.status != nro.models.player.Pet.GOHOME) {
            pet.status = nro.models.player.Pet.ATTACK;
        }
        pet.followMaster();
    }
    
    /**
     * Check if bot has a pet
     */
    public boolean hasPet() {
        return hasPet && pet != null;
    }
    
    // Fusion tracking
    private long lastTimeFusion = 0;
    private long fusionDuration = 0;
    private boolean isFusionActive = false;
    
    /**
     * Start auto fusion mode - bot will fusion with pet at random intervals
     * Call this after creating pet for bot
     */
    public void startAutoFusion() {
        if (pet == null) {
            return;
        }
        isFusionActive = true;
        lastTimeFusion = System.currentTimeMillis() - Util.nextInt(0, 30000); // Random offset
        fusionDuration = Util.nextInt(BotConfig.BOT_FUSION_INTERVAL_MIN, BotConfig.BOT_FUSION_INTERVAL_MAX);
    }
    
    /**
     * Toggle fusion with pet (Porata style)
     * If not fused -> fuse
     * If fused -> unfuse
     */
    public void toggleFusion() {
        if (pet == null || pet.isDie()) {
            return;
        }
        
        if (fusion.typeFusion == ConstPlayer.NON_FUSION) {
            // Not fused -> fuse with Porata
            doFusion();
        } else {
            // Already fused -> unfuse
            doUnfusion();
        }
    }
    
    /**
     * Perform fusion with pet (Porata)
     */
    public void doFusion() {
        if (pet == null || pet.isDie()) {
            return;
        }
        
        // Set fusion type to Porata
        fusion.typeFusion = ConstPlayer.HOP_THE_PORATA;
        pet.status = nro.models.player.Pet.FUSION;
        
        // Remove pet from map
        if (pet.zone != null) {
            MapService.gI().exitMap(pet);
        }
        
        // Send fusion effect to players
        sendFusionEffect(fusion.typeFusion);
        
        // Update appearance and stats
        Service.getInstance().Send_Caitrang(this);
        nPoint.calPoint();
        nPoint.setFullHpMp();
        Service.getInstance().point(this);
    }
    
    /**
     * Perform fusion2 with pet (Porata 2)
     */
    public void doFusion2() {
        if (pet == null || pet.isDie()) {
            return;
        }
        
        // Set fusion type to Porata 2
        fusion.typeFusion = ConstPlayer.HOP_THE_PORATA2;
        pet.status = nro.models.player.Pet.FUSION;
        
        // Remove pet from map
        if (pet.zone != null) {
            MapService.gI().exitMap(pet);
        }
        
        // Send fusion effect to players
        sendFusionEffect(fusion.typeFusion);
        
        // Update appearance and stats
        Service.getInstance().Send_Caitrang(this);
        nPoint.calPoint();
        nPoint.setFullHpMp();
        Service.getInstance().point(this);
    }
    
    /**
     * Unfuse from pet
     */
    public void doUnfusion() {
        if (pet == null) {
            return;
        }
        
        fusion.typeFusion = ConstPlayer.NON_FUSION;
        pet.status = nro.models.player.Pet.PROTECT;
        
        // Send unfusion effect
        sendFusionEffect(0);
        
        // Pet rejoins map
        pet.joinMapMaster();
        
        // Update appearance
        Service.getInstance().Send_Caitrang(this);
        Service.getInstance().point(this);
    }
    
    /**
     * Send fusion effect to all players in zone
     */
    private void sendFusionEffect(int type) {
        if (zone == null) return;
        
        try {
            nro.server.io.Message msg = new nro.server.io.Message(125);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) this.id);
            Service.getInstance().sendMessAllPlayerInMap(this, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }
    
    /**
     * Check and update auto fusion state
     * Called from update() to toggle fusion at random intervals
     */
    private void updateAutoFusion() {
        if (!isFusionActive || pet == null) {
            return;
        }
        
        if (Util.canDoWithTime(lastTimeFusion, fusionDuration)) {
            toggleFusion();
            lastTimeFusion = System.currentTimeMillis();
            fusionDuration = Util.nextInt(BotConfig.BOT_FUSION_INTERVAL_MIN, BotConfig.BOT_FUSION_INTERVAL_MAX);
        }
    }
    
    /**
     * Check if bot is currently fused
     */
    public boolean isFused() {
        return fusion.typeFusion != ConstPlayer.NON_FUSION;
    }
    
    /**
     * Revive the bot
     */
    public void revive() {
        if (botStatus == DEFEATED || isDie()) {
            nPoint.setFullHpMp();
            Service.getInstance().hsChar(this, nPoint.hpMax, nPoint.mpMax);
            lastTimeRevive = System.currentTimeMillis();
            
            // Quay lại trạng thái trước đó
            if (previousStatus == ATTACK_BOSS) {
                // Bot farm boss - tìm boss mới và tấn công tiếp
                startAttackBoss();
            } else {
                botStatus = previousStatus != DEFEATED ? previousStatus : HUNTING;
            }
        }
    }
    
    /**
     * Auto revive bot after delay with random offset to prevent sync
     */
    private void autoRevive() {
        if (!BotConfig.AUTO_REVIVE) {
            return;
        }
        
        // Save previous status before marking as defeated
        if (botStatus != DEFEATED) {
            previousStatus = botStatus;
            botStatus = DEFEATED;
            lastTimeRevive = System.currentTimeMillis() - Util.nextInt(0, 10000);
        }
        
        // Check if enough time has passed since death
        if (Util.canDoWithTime(lastTimeRevive, BotConfig.AUTO_REVIVE_DELAY)) {
            revive();
        }
    }

    // Getters
    public byte getBotStatus() {
        return botStatus;
    }
    
    public int getPreviousMapId() {
        return previousMapId;
    }

    public Mob getCurrentTarget() {
        return currentTarget;
    }

    public int getMonstersDefeated() {
        return monstersDefeated;
    }

    public long getTotalExperienceGained() {
        return totalExperienceGained;
    }

    public String getStatusText() {
        return switch (botStatus) {
            case IDLE -> "Idle";
            case HUNTING -> "Hunting";
            case DEFEATED -> "Defeated";
            case MOVE_MAP -> "Moving";
            case PICK_ITEM -> "Picking Item";
            case TALK_NPC -> "At NPC";
            case ATTACK_BOSS -> "Attacking Boss";
            case AFK -> "AFK";
            default -> "Unknown";
        };
    }

    // Override methods that require session
    @Override
    public int version() {
        return 220; // Default version for bot
    }

    @Override
    public boolean isVersionAbove(int version) {
        return version() >= version;
    }

    @Override
    public short getMount() {
        return -1; // Bot has no mount
    }

    @Override
    public byte getAura() {
        return -1; // Bot has no aura
    }

    @Override
    public byte getEffFront() {
        return 0; // Bot has no front effect
    }

    @Override
    public short getFlagBag() {
        if (currentFlagBagId > 0) {
            return (short) currentFlagBagId;
        }
        return -1;
    }

    // Override appearance methods for bot
    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        if (effectSkill != null && effectSkill.isSocola) {
            return 412;
        }
        if (inventory != null && inventory.itemsBody != null && inventory.itemsBody.size() > 5) {
            try {
                if (inventory.itemsBody.get(5).isNotNullItem()) {
                    nro.models.item.CaiTrang ct = nro.server.Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
                    if (ct != null && ct.getID()[0] != -1) {
                        return (short) ct.getID()[0];
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return BotConfig.BOT_APPEARANCE[this.gender][0];
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        }
        if (effectSkill != null && effectSkill.isSocola) {
            return 413;
        }
        // Check cải trang (slot 5)
        if (inventory != null && inventory.itemsBody != null && inventory.itemsBody.size() > 5) {
            try {
                if (inventory.itemsBody.get(5).isNotNullItem()) {
                    nro.models.item.CaiTrang ct = nro.server.Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
                    if (ct != null && ct.getID()[1] != -1) {
                        return (short) ct.getID()[1];
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        // Check equipped armor (slot 0)
        if (inventory != null && inventory.itemsBody != null && inventory.itemsBody.size() > 0) {
            try {
                if (inventory.itemsBody.get(0).isNotNullItem()) {
                    return inventory.itemsBody.get(0).template.part;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return BotConfig.BOT_APPEARANCE[this.gender][1];
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        }
        if (effectSkill != null && effectSkill.isSocola) {
            return 414;
        }
        // Check cải trang (slot 5)
        if (inventory != null && inventory.itemsBody != null && inventory.itemsBody.size() > 5) {
            try {
                if (inventory.itemsBody.get(5).isNotNullItem()) {
                    nro.models.item.CaiTrang ct = nro.server.Manager.gI().getCaiTrangByItemId(inventory.itemsBody.get(5).template.id);
                    if (ct != null && ct.getID()[2] != -1) {
                        return (short) ct.getID()[2];
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        // Check equipped pants (slot 1)
        if (inventory != null && inventory.itemsBody != null && inventory.itemsBody.size() > 1) {
            try {
                if (inventory.itemsBody.get(1).isNotNullItem()) {
                    return inventory.itemsBody.get(1).template.part;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return BotConfig.BOT_APPEARANCE[this.gender][2];
    }

    @Override
    public void dispose() {
        // Remove pet first before removing bot
        if (pet != null) {
            try {
                if (pet.zone != null) {
                    MapService.gI().exitMap(pet);
                }
                pet.dispose();
                pet = null;
                hasPet = false;
            } catch (Exception e) {
            }
        }
        
        if (zone != null) {
            MapService.gI().exitMap(this);
        }
        currentTarget = null;
        targetBoss = null;
        targetNpc = null;
        targetItem = null;
        super.dispose();
    }
}
