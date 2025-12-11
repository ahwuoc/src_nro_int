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
    
    // NPC interaction tracking
    private long lastTimeChangeOutfit = 0;
    private boolean autoChangeOutfit = false;
    private int outfitNpcId = 0;
    
    // Boss hunting tracking
    private nro.models.boss.Boss targetBoss = null;
    private long bossAttackStartTime = 0;
    
    // Title (danh hiệu) tracking
    private long lastTimeChangeTitle = 0;
    private int currentTitlePart = -1;
    
    // Flag bag tracking
    private long lastTimeChangeFlagBag = 0;
    private int currentFlagBagId = -1;
    
    // Pet (đệ tử) for bot
    private boolean hasPet = false;
    
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
        this.head = BotConfig.BOT_APPEARANCE[gender][0];
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
            }
        } catch (Exception e) {
            System.err.println("[Bot] Error in update for " + name + ": " + e.getMessage());
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
        // Random hunting duration 8-15 seconds for each bot
        huntingDuration = Util.nextInt(8000, 15000);
        System.out.println("[Bot-" + name + "] Started hunting for " + (huntingDuration/1000) + "s");
    }
    
    /**
     * Set hunting start time offset to prevent bots from moving together
     * @param offsetMs Offset in milliseconds (0-10000)
     */
    public void setHuntingStartTimeOffset(int offsetMs) {
        // Subtract offset so each bot has different "start time"
        huntingStartTime = System.currentTimeMillis() - offsetMs;
        // Also randomize duration
        huntingDuration = Util.nextInt(8000, 15000);
    }

    /**
     * Stop hunting and go idle
     */
    public void stopHunting() {
        botStatus = IDLE;
        currentTarget = null;
    }

    /**
     * Main hunting logic - Move like a human player with random behavior
     */
    private void hunt() {
        // Check if it's time to move to next map (random 8-15 seconds per bot)
        if (huntingStartTime > 0 && System.currentTimeMillis() - huntingStartTime >= huntingDuration) {
            startMovingMaps();
            return;
        }
        
        // Check for items to pick up - switch to PICK_ITEM mode if found
        nro.models.map.ItemMap nearbyItem = findNearbyItem();
        if (nearbyItem != null) {
            targetItem = nearbyItem;
            botStatus = PICK_ITEM;
            System.out.println("[Bot-" + name + "] Found item: " + nearbyItem.itemTemplate.name + ", switching to PICK_ITEM");
            return;
        }
        
        currentTarget = findMobToAttack();
         
        if (currentTarget != null) {
            int distance = Util.getDistance(this, currentTarget);
            
            // Try to use ranged skill first if available
            Skill rangedSkill = getSkill(2);
            Skill meleeSkill = getSkill(1);
            
            // Debug: log skill availability once
            if (rangedSkill == null && meleeSkill == null && System.currentTimeMillis() % 10000 < 100) {
                System.out.println("[Bot-" + name + "] WARNING: No skills available!");
            }
            if (rangedSkill != null && rangedSkill.template != null) {
                if (distance > BotConfig.ATTACK_RANGE_MELEE) {
                    walkTowardsMob();
                } else {
                    this.playerSkill.skillSelect = rangedSkill;
                    if (SkillService.gI().canUseSkillWithCooldown(this)) {
                        if (SkillService.gI().canUseSkillWithMana(this)) {
                            SkillService.gI().useSkill(this, null, currentTarget, null);
                        }
                    }
                }
            } else if (distance <= BotConfig.ATTACK_RANGE_MELEE) {
                // No ranged skill, use melee if close enough
                Skill skill = getSkill(1);
                if (skill != null && skill.template != null) {
                    this.playerSkill.skillSelect = skill;
                    if (SkillService.gI().canUseSkillWithCooldown(this) && SkillService.gI().canUseSkillWithMana(this)) {
                        SkillService.gI().useSkill(this, null, currentTarget, null);
                    }
                }
            } else {
                // No ranged skill and too far for melee - walk closer
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
        
        // Check if item still exists
        nro.models.map.ItemMap item = zone.getItemMapByItemMapId(targetItem.itemMapId);
        if (item == null) {
            System.out.println("[Bot-" + name + "] Item no longer exists, back to HUNTING");
            targetItem = null;
            botStatus = HUNTING;
            return;
        }
        
        int dx = item.x - this.location.x;
        int distance = Math.abs(dx);
        
        if (distance <= BotConfig.PICKUP_RANGE) {
            // Close enough - pick up item
            System.out.println("[Bot-" + name + "] Picking up: " + item.itemTemplate.name);
            zone.pickItem(this, item.itemMapId);
            recentlyPickedItems.add(item.itemMapId);
            targetItem = null;
            botStatus = HUNTING;
        } else {
            // Move towards item
            int moveSpeed = Util.nextInt(10, 20);
            if (dx > 0) {
                this.location.x += Math.min(moveSpeed, dx);
            } else {
                this.location.x -= Math.min(moveSpeed, -dx);
            }
            
            // Follow terrain
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
     * Get skill by index (1=melee, 2=ranged, 3=special, 4=buff)
     * Returns null if no skill found
     */
    private Skill getSkill(int index) {
        if (playerSkill == null || playerSkill.skills == null || playerSkill.skills.isEmpty()) {
            return null;
        }
        
        for (Skill skill : playerSkill.skills) {
            if (skill != null && skill.template != null) {
                // Return skill based on type
                if (index == 1 && skill.template.type == 0) { // Melee
                    return skill;
                } else if (index == 2 && skill.template.type == 1) { // Ranged
                    return skill;
                } else if (index == 3 && skill.template.type == 2) { // Special
                    return skill;
                } else if (index == 4 && skill.template.type == 3) { // Buff
                    return skill;
                }
            }
        }
        
        // Fallback: return first available skill if no matching type
        if (index == 1) {
            for (Skill skill : playerSkill.skills) {
                if (skill != null && skill.template != null) {
                    return skill;
                }
            }
        }
        return null;
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
     * Move to next map in MAP_LIST based on bot's planet (gender)
     */
    private void moveToNextMap() {
        // Check every 200ms for smooth movement (5 moves per second)
        if (!Util.canDoWithTime(lastTimeCheckMap, 200)) {
            return;
        }
        lastTimeCheckMap = System.currentTimeMillis();
        
        if (zone == null || zone.map == null) {
            return;
        }
        
        // Get map list for bot's planet (based on gender: 0=Trái Đất, 1=Namek, 2=Xayda)
        int[] planetMaps = BotConfig.MAP_LIST[this.gender];
        
        // Get target map ID
        int targetMapId = planetMaps[currentMapIndex];
        
        // If already in target map, switch to hunting
        if (zone.map.mapId == targetMapId) {
            botStatus = HUNTING;
            huntingStartTime = System.currentTimeMillis();
            huntingDuration = Util.nextInt(8000, 15000); // Random 8-15s
            System.out.println("[Bot-" + name + "] Reached map " + targetMapId + ", hunting for " + (huntingDuration/1000) + "s");
            return;
        }
        
        // Find waypoint that leads to target map
        if (targetWaypoint == null) {
            targetWaypoint = findWaypointToMap(targetMapId);
            if (targetWaypoint == null) {
                System.out.println("[Bot-" + name + "] No waypoint found to map " + targetMapId);
                // Try next map in planet
                currentMapIndex = (currentMapIndex + 1) % planetMaps.length;
                return;
            }
            System.out.println("[Bot-" + name + "] Found waypoint to map " + targetMapId + 
                               " at (" + targetWaypoint.minX + "," + targetWaypoint.minY + ")");
        }
        
        // Move towards waypoint - only check X distance (Y follows terrain)
        int waypointX = (targetWaypoint.minX + targetWaypoint.maxX) / 2;
        int dx = waypointX - this.location.x;
        int distanceX = Math.abs(dx);
        
        if (distanceX > 20) {
            int moveSpeed = Util.nextInt(30, 50);
            if (dx > 0) {
                this.location.x += Math.min(moveSpeed, dx);
            } else {
                this.location.x -= Math.min(moveSpeed, -dx);
            }
            
            // Get ground Y at current X position (follows terrain)
            int groundY = zone.map.yPhysicInTop(this.location.x, 0);
            if (groundY > 0) {
                this.location.y = groundY;
            }
            
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        } else {
            // Close to waypoint, trigger map change directly
            int goMapId = targetWaypoint.goMap;
            int goX = targetWaypoint.goX;
            int goY = targetWaypoint.goY;
            System.out.println("[Bot-" + name + "] At waypoint, changing to map " + goMapId);
            
            // Get zone to join
            nro.models.map.Zone zoneJoin = MapService.gI().getMapCanJoin(this, goMapId);
            if (zoneJoin != null) {
                // Set new position
                this.location.x = goX;
                this.location.y = goY;
                // Move to new map
                MapService.gI().goToMap(this, zoneJoin);
                // Send bot info to all players in new zone
                zoneJoin.load_Me_To_Another(this);
            }
            targetWaypoint = null;
        }
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
     * Randomly chooses from available waypoints
     */
    public void startMovingMaps() {
        botStatus = MOVE_MAP;
        targetWaypoint = null;
        huntingStartTime = 0;
        
        // Find all available waypoints and pick random one
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
            
            // Pick random waypoint
            if (!validWaypoints.isEmpty()) {
                targetWaypoint = validWaypoints.get(Util.nextInt(0, validWaypoints.size() - 1));
                System.out.println("[Bot-" + name + "] Moving to map " + targetWaypoint.goMap + " (random choice)");
                return;
            }
        }
        
        // Fallback: use sequential if no waypoints found
        int[] planetMaps = BotConfig.MAP_LIST[this.gender];
        currentMapIndex = (currentMapIndex + 1) % planetMaps.length;
        System.out.println("[Bot-" + name + "] Moving to map " + planetMaps[currentMapIndex] + " (sequential)");
    }
    
    /**
     * Start attacking boss - find alive boss and teleport to it
     * Also upgrades bot with full skills for boss fight
     */
    public void startAttackBoss() {
        targetBoss = findAliveBoss();
        if (targetBoss != null && targetBoss.zone != null) {
            botStatus = ATTACK_BOSS;
            bossAttackStartTime = System.currentTimeMillis();
            
            // Upgrade bot with full skills for boss fight
            nro.bot.BotManager.gI().initializeFullSkills(this);
            
            // Boost bot stats for boss fight
            this.nPoint.hpMax = 50000000;
            this.nPoint.mpMax = 10000000;
            this.nPoint.dame = 500000;
            this.nPoint.setFullHpMp();
            
            // Teleport to boss location
            this.location.x = targetBoss.location.x + Util.nextInt(-50, 50);
            this.location.y = targetBoss.location.y;
            MapService.gI().goToMap(this, targetBoss.zone);
            targetBoss.zone.load_Me_To_Another(this);
            
            System.out.println("[Bot-" + name + "] Started attacking boss: " + targetBoss.name + " with full skills!");
        } else {
            System.out.println("[Bot-" + name + "] No alive boss found, switching to HUNTING");
            botStatus = HUNTING;
            huntingStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Find an alive boss from BOSS_IDS list
     */
    private nro.models.boss.Boss findAliveBoss() {
        for (int bossId : BotConfig.BOSS_IDS) {
            for (nro.models.boss.Boss boss : nro.models.boss.BossManager.BOSSES_IN_GAME) {
                if (boss != null && boss.id == bossId && !boss.isDie() && boss.zone != null) {
                    return boss;
                }
            }
        }
        return null;
    }
    
    /**
     * Attack boss logic - uses all available skills with delay
     */
    private void attackBoss() {
        // Check if time to find another boss
        if (bossAttackStartTime > 0 && System.currentTimeMillis() - bossAttackStartTime >= BotConfig.BOSS_ATTACK_DURATION) {
            System.out.println("[Bot-" + name + "] Boss attack duration passed, looking for another boss");
            startAttackBoss();
            return;
        }
        
        // Check if boss is still alive and in same zone
        if (targetBoss == null || targetBoss.isDie() || targetBoss.zone != this.zone) {
            System.out.println("[Bot-" + name + "] Boss died or left, finding new boss");
            startAttackBoss();
            return;
        }
        
        // Add delay between attacks (500ms minimum)
        if (!Util.canDoWithTime(lastTimeAttack, 500)) {
            return;
        }
        
        int distance = Util.getDistance(this, targetBoss);
        
        // Move closer if too far
        if (distance > 100) {
            walkTowardsBoss();
            return;
        }
        
        // Try to use any available skill
        if (playerSkill != null && playerSkill.skills != null && !playerSkill.skills.isEmpty()) {
            for (Skill skill : playerSkill.skills) {
                if (skill != null && skill.template != null) {
                    this.playerSkill.skillSelect = skill;
                    if (SkillService.gI().canUseSkillWithCooldown(this)) {
                        if (SkillService.gI().canUseSkillWithMana(this)) {
                            // Move to attack range for melee skills
                            if (skill.template.type == 0 && distance > BotConfig.ATTACK_RANGE_MELEE) {
                                walkTowardsBoss();
                                return;
                            }
                            SkillService.gI().useSkill(this, targetBoss, null, null);
                            lastTimeAttack = System.currentTimeMillis();
                            return; // Used a skill, exit
                        }
                    }
                }
            }
        }
        
        // No skill available, just move closer
        if (distance > BotConfig.ATTACK_RANGE_MELEE) {
            walkTowardsBoss();
        }
    }
    
    /**
     * Walk towards target boss
     */
    private void walkTowardsBoss() {
        if (targetBoss == null) return;
        int targetX = targetBoss.location.x;
        int targetY = targetBoss.location.y;
        int dx = targetX - this.location.x;
        
        int moveSpeed = Util.nextInt(BotConfig.MOVE_SPEED_TO_MOB_MIN, BotConfig.MOVE_SPEED_TO_MOB_MAX);
        
        if (Math.abs(dx) > BotConfig.ATTACK_RANGE_MELEE) {
            if (dx > 0) {
                this.location.x += moveSpeed;
            } else {
                this.location.x -= moveSpeed;
            }
        } else {
            this.location.x = targetX + Util.nextInt(-15, 15);
        }
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
                equipRandomOutfitFromShop(npcId);
                
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
                
                System.out.println("[Bot-" + name + "] Teleported to NPC " + npcId + " at map " + mapId + " pos(" + this.location.x + "," + this.location.y + ")");
            } else {
                System.out.println("[Bot-" + name + "] NPC " + npcId + " not found in map " + mapId);
                botStatus = HUNTING;
            }
        } else {
            System.out.println("[Bot-" + name + "] Cannot join map " + mapId);
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
        
        System.out.println("[Bot-" + name + "] Started shopping mode - will change outfit every 10s");
    }
    
    /**
     * Equip random outfit (cải trang) from NPC's shop
     */
    private void equipRandomOutfitFromShop(int npcId) {
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
                            System.out.println("[Bot-" + name + "] Equipped outfit: " + item.template.name);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[Bot-" + name + "] Error equipping outfit: " + e.getMessage());
        }
    }
    
    /**
     * Stay near NPC - idle and optionally change outfit periodically
     */
    private void goToNpc() {
        // Already at NPC, just stay idle
        if (targetNpc == null) {
            botStatus = HUNTING;
            return;
        }
        
        // Auto change outfit every 8-15 seconds (random interval for async behavior)
        if (autoChangeOutfit && Util.canDoWithTime(lastTimeChangeOutfit, Util.nextInt(8000, 15000))) {
            equipRandomOutfitFromShop(outfitNpcId);
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
        
        // Small random movement near NPC (every 30-40 seconds)
        if (Util.canDoWithTime(lastTimeMoveIdle, Util.nextInt(30000, 40000))) {
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
            
            System.out.println("[Bot-" + name + "] Equipped title part: " + titlePart);
        } catch (Exception e) {
            System.err.println("[Bot-" + name + "] Error equipping title: " + e.getMessage());
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
            System.err.println("[Bot-" + name + "] Error sending title: " + e.getMessage());
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
            System.err.println("[Bot-" + name + "] Error removing title from map: " + e.getMessage());
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
            
            System.out.println("[Bot-" + name + "] Removed title");
        } catch (Exception e) {
            System.err.println("[Bot-" + name + "] Error removing title: " + e.getMessage());
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
            
            System.out.println("[Bot-" + name + "] Equipped flag bag: " + flagBagId);
        } catch (Exception e) {
            System.err.println("[Bot-" + name + "] Error equipping flag bag: " + e.getMessage());
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
            System.err.println("[Bot-" + name + "] Error sending flag bag: " + e.getMessage());
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
        createBotPet(nro.models.player.Pet.PetType.NONE);
    }
    
    /**
     * Create a pet (đệ tử) for this bot with specific type
     * @param petType Type of pet (NONE, MABU, SAYAN5, etc.)
     */
    public void createBotPet(nro.models.player.Pet.PetType petType) {
        try {
            if (this.pet != null) {
                System.out.println("[Bot-" + name + "] Already has a pet");
                return;
            }
            
            // Create pet using PetService
            PetService.gI().createPet(this, petType);
            hasPet = true;
            
            // Wait for pet to be created then set name
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // Wait for PetService to finish
                    if (this.pet != null) {
                        this.pet.name = "$Đệ tử";
                        System.out.println("[Bot-" + name + "] Pet name set to: Đệ tử");
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
            
            System.out.println("[Bot-" + name + "] Created pet type: " + petType.getDisplayName());
        } catch (Exception e) {
            System.err.println("[Bot-" + name + "] Error creating pet: " + e.getMessage());
        }
    }
    
    /**
     * Make bot's pet follow the bot
     * Called in update() to keep pet near bot
     */
    private void updatePetFollow() {
        if (pet == null || pet.isDie()) {
            return;
        }
        
        // Make sure pet is in same zone as bot
        if (pet.zone == null || pet.zone != this.zone) {
            pet.joinMapMaster();
        }
        
        // Pet follows bot (like followMaster in Pet.java)
        pet.followMaster();
    }
    
    /**
     * Check if bot has a pet
     */
    public boolean hasPet() {
        return hasPet && pet != null;
    }
    
    /**
     * Revive the bot
     */
    public void revive() {
        if (botStatus == DEFEATED || isDie()) {
            nPoint.setFullHpMp();
            botStatus = previousStatus != DEFEATED ? previousStatus : HUNTING;
            Service.getInstance().hsChar(this, nPoint.hpMax, nPoint.mpMax);
            lastTimeRevive = System.currentTimeMillis();
            System.out.println("[Bot-" + name + "] Revived, resuming " + getStatusText());
        }
    }
    
    /**
     * Auto revive bot after delay
     */
    private void autoRevive() {
        if (!BotConfig.AUTO_REVIVE) {
            return;
        }
        
        // Save previous status before marking as defeated
        if (botStatus != DEFEATED) {
            previousStatus = botStatus;
            botStatus = DEFEATED;
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
        // Check cải trang (slot 5)
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
        if (zone != null) {
            MapService.gI().exitMap(this);
        }
        currentTarget = null;
        super.dispose();
    }
}
