package mod.universalmobwar.system;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         SCALING SYSTEM                                    ║
 * ║                                                                           ║
 * ║  THE SINGLE FILE THAT CONTROLS ALL MOB PROGRESSION                        ║
 * ║                                                                           ║
 * ║  This system:                                                             ║
 * ║    1. Loads ALL mob JSON configs from mob_configs/*.json                  ║
 * ║    2. Calculates points based on world age (from JSON daily_scaling)      ║
 * ║    3. Spends points on upgrades (80% buy / 20% save logic)                ║
 * ║    4. Applies effects (potion effects, equipment, special abilities)      ║
 * ║                                                                           ║
 * ║  To add a new mob:                                                        ║
 * ║    1. Create mob_configs/[mobname].json with upgrade tree                 ║
 * ║    2. That's it! This system handles everything else.                     ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ScalingSystem {

    private static final Gson GSON = new Gson();
    
    // Cache of loaded mob configs: mob_name -> JsonObject
    private static final Map<String, JsonObject> MOB_CONFIGS = new ConcurrentHashMap<>();
    
    // Entity class name -> mob config name mapping
    private static final Map<String, String> ENTITY_TO_CONFIG = new ConcurrentHashMap<>();
    
    // Track which mobs have been initialized this session
    private static final Set<UUID> INITIALIZED_MOBS = ConcurrentHashMap.newKeySet();
    
    // List of all available mob config files (the 10 implemented ones)
    private static final String[] IMPLEMENTED_MOBS = {
        "allay", "armadillo", "axolotl", "bat", "bee",
        "blaze", "bogged", "breeze", "camel", "cat"
    };
    
    private static boolean configsLoaded = false;
    
    /**
     * Initialize the scaling system - load all JSON configs
     */
    public static void initialize() {
        if (configsLoaded) return;
        
        UniversalMobWarMod.LOGGER.info("[ScalingSystem] Loading mob configurations...");
        
        for (String mobName : IMPLEMENTED_MOBS) {
            loadMobConfig(mobName);
        }
        
        configsLoaded = true;
        UniversalMobWarMod.LOGGER.info("[ScalingSystem] Loaded {} mob configurations", MOB_CONFIGS.size());
    }
    
    /**
     * Load a single mob's JSON config
     */
    private static void loadMobConfig(String mobName) {
        String path = "/mob_configs/" + mobName + ".json";
        
        try (InputStream is = ScalingSystem.class.getResourceAsStream(path)) {
            if (is == null) {
                UniversalMobWarMod.LOGGER.warn("[ScalingSystem] Config not found: {}", path);
                return;
            }
            
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject config = GSON.fromJson(reader, JsonObject.class);
            
            if (config != null && config.has("mob_name") && config.has("entity_class")) {
                String configMobName = config.get("mob_name").getAsString().toLowerCase();
                String entityClass = config.get("entity_class").getAsString();
                
                MOB_CONFIGS.put(configMobName, config);
                ENTITY_TO_CONFIG.put(entityClass, configMobName);
                
                // Also map simple class name for easier lookup
                String simpleClassName = entityClass.substring(entityClass.lastIndexOf('.') + 1).toLowerCase();
                ENTITY_TO_CONFIG.put(simpleClassName, configMobName);
            }
            
        } catch (Exception e) {
            UniversalMobWarMod.LOGGER.error("[ScalingSystem] Failed to load {}: {}", path, e.getMessage());
        }
    }
    
    /**
     * Get config for a mob entity
     */
    public static JsonObject getConfigForMob(MobEntity mob) {
        if (!configsLoaded) initialize();
        
        // Try full class name first
        String className = mob.getClass().getName();
        String configName = ENTITY_TO_CONFIG.get(className);
        
        if (configName == null) {
            // Try simple class name
            String simpleName = mob.getClass().getSimpleName().toLowerCase().replace("entity", "");
            configName = ENTITY_TO_CONFIG.get(simpleName);
        }
        
        if (configName == null) {
            // Try registry name
            String registryName = mob.getType().toString().toLowerCase();
            for (String implemented : IMPLEMENTED_MOBS) {
                if (registryName.contains(implemented)) {
                    configName = implemented;
                    break;
                }
            }
        }
        
        return configName != null ? MOB_CONFIGS.get(configName) : null;
    }
    
    /**
     * Check if a mob has scaling configured
     */
    public static boolean hasScalingConfig(MobEntity mob) {
        return getConfigForMob(mob) != null;
    }
    
    /**
     * MAIN ENTRY POINT - Called from MobDataMixin on every mob tick
     * This single method handles ALL scaling logic for ALL mobs
     */
    public static void processMobTick(MobEntity mob, World world, MobWarData data) {
        // Skip client side
        if (world.isClient()) return;
        
        // Check if scaling is enabled
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        // Get config for this mob
        JsonObject config = getConfigForMob(mob);
        if (config == null) return; // No config = no scaling for this mob
        
        // Get mob type for different upgrade paths
        String mobType = config.has("mob_type") ? config.get("mob_type").getAsString() : "hostile";
        
        // Calculate total points from world age
        int totalPoints = calculateWorldAgePoints(world, config);
        
        // Add kill points (stored in MobWarData)
        totalPoints += data.getKillCount(); // 1 point per kill
        
        // Store total for reference
        data.setSkillPoints(totalPoints);
        
        // Get current spent points
        int spentPoints = (int) data.getSpentPoints();
        int budget = totalPoints - spentPoints;
        
        // Only process upgrades if we have budget and it's time
        long currentTick = world.getTime();
        long lastUpdate = data.getSkillData().contains("lastUpdateTick") ? 
            data.getSkillData().getLong("lastUpdateTick") : 0;
        
        // Process every 100 ticks (5 seconds) or on first load
        if (currentTick - lastUpdate < 100 && lastUpdate > 0) {
            // Just apply existing effects
            applyEffects(mob, data, config, mobType);
            return;
        }
        
        // Update timestamp
        data.getSkillData().putLong("lastUpdateTick", currentTick);
        
        // Spend points on upgrades
        if (budget > 0) {
            spendPoints(mob, data, config, mobType, budget);
        }
        
        // Apply all effects based on current upgrade levels
        applyEffects(mob, data, config, mobType);
        
        // Save data
        MobWarData.save(mob, data);
    }
    
    /**
     * Calculate points from world age based on JSON daily_scaling config
     */
    private static int calculateWorldAgePoints(World world, JsonObject config) {
        int worldDays = (int) (world.getTime() / 24000L);
        double totalPoints = 0.0;
        
        // Get daily_scaling from config
        JsonArray dailyScaling = null;
        if (config.has("point_system")) {
            JsonObject pointSystem = config.getAsJsonObject("point_system");
            if (pointSystem.has("daily_scaling")) {
                dailyScaling = pointSystem.getAsJsonArray("daily_scaling");
            }
        }
        
        // Default scaling if not in config
        if (dailyScaling == null) {
            for (int day = 1; day <= worldDays; day++) {
                if (day <= 10) totalPoints += 0.1;
                else if (day <= 15) totalPoints += 0.5;
                else if (day <= 20) totalPoints += 1.0;
                else if (day <= 25) totalPoints += 1.5;
                else if (day <= 30) totalPoints += 3.0;
                else totalPoints += 5.0;
            }
            return (int) totalPoints;
        }
        
        // Use config-defined scaling
        for (int day = 1; day <= worldDays; day++) {
            double pointsForDay = 0.1; // Default
            
            for (JsonElement element : dailyScaling) {
                JsonObject range = element.getAsJsonObject();
                int daysMin = range.get("days_min").getAsInt();
                int daysMax = range.get("days_max").getAsInt();
                double pointsPerDay = range.get("points_per_day").getAsDouble();
                
                // days_max of -1 means infinite
                if (day >= daysMin && (daysMax == -1 || day <= daysMax)) {
                    pointsForDay = pointsPerDay;
                    break;
                }
            }
            
            totalPoints += pointsForDay;
        }
        
        return (int) totalPoints;
    }
    
    /**
     * Spend points on upgrades using 80/20 buy/save logic from JSON
     */
    private static void spendPoints(MobEntity mob, MobWarData data, JsonObject config, String mobType, int budget) {
        Random random = new Random();
        
        // Get buy/save chances from config
        double buyChance = 0.80;
        double saveChance = 0.20;
        
        if (config.has("point_system")) {
            JsonObject ps = config.getAsJsonObject("point_system");
            if (ps.has("buy_chance")) buyChance = ps.get("buy_chance").getAsDouble();
            if (ps.has("save_chance")) saveChance = ps.get("save_chance").getAsDouble();
        }
        
        // Get current upgrade levels from skill data
        var skillData = data.getSkillData();
        
        // Spending loop
        while (budget > 0) {
            List<UpgradeOption> affordable = getAffordableUpgrades(config, mobType, skillData, budget);
            
            if (affordable.isEmpty()) break;
            
            // 20% chance to save and stop
            if (random.nextDouble() < saveChance) break;
            
            // 80% chance to buy a random affordable upgrade
            UpgradeOption chosen = affordable.get(random.nextInt(affordable.size()));
            
            // Apply the upgrade
            skillData.putInt(chosen.key, chosen.newLevel);
            data.setSpentPoints(data.getSpentPoints() + chosen.cost);
            budget -= chosen.cost;
        }
        
        data.setSkillData(skillData);
    }
    
    /**
     * Get list of affordable upgrades from the mob's JSON config
     */
    private static List<UpgradeOption> getAffordableUpgrades(JsonObject config, String mobType, 
            net.minecraft.nbt.NbtCompound skillData, int budget) {
        
        List<UpgradeOption> affordable = new ArrayList<>();
        
        if (!config.has("tree")) return affordable;
        JsonObject tree = config.getAsJsonObject("tree");
        
        // Check potion effects based on mob type
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (tree.has(effectsKey)) {
            JsonObject effects = tree.getAsJsonObject(effectsKey);
            
            for (String effectName : effects.keySet()) {
                JsonArray levels = effects.getAsJsonArray(effectName);
                int currentLevel = skillData.getInt(effectName);
                
                if (currentLevel < levels.size()) {
                    JsonObject nextLevel = levels.get(currentLevel).getAsJsonObject();
                    int cost = nextLevel.get("cost").getAsInt();
                    
                    if (cost <= budget) {
                        affordable.add(new UpgradeOption(effectName, currentLevel + 1, cost));
                    }
                }
            }
        }
        
        // Check special abilities
        if (tree.has("special_abilities")) {
            JsonObject abilities = tree.getAsJsonObject("special_abilities");
            
            for (String abilityName : abilities.keySet()) {
                JsonArray levels = abilities.getAsJsonArray(abilityName);
                int currentLevel = skillData.getInt(abilityName);
                
                if (currentLevel < levels.size()) {
                    JsonObject nextLevel = levels.get(currentLevel).getAsJsonObject();
                    int cost = nextLevel.get("cost").getAsInt();
                    
                    if (cost <= budget) {
                        affordable.add(new UpgradeOption(abilityName, currentLevel + 1, cost));
                    }
                }
            }
        }
        
        // TODO: Add equipment upgrades (weapon, armor, shield) when implemented
        
        return affordable;
    }
    
    /**
     * Apply all effects based on current upgrade levels
     */
    private static void applyEffects(MobEntity mob, MobWarData data, JsonObject config, String mobType) {
        var skillData = data.getSkillData();
        
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        // Get effects section
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (tree.has(effectsKey)) {
            JsonObject effects = tree.getAsJsonObject(effectsKey);
            
            // Apply healing/regeneration
            int healingLevel = skillData.getInt("healing");
            if (healingLevel > 0) {
                int regenLevel = Math.min(healingLevel - 1, 1); // Cap at Regen II for permanent
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION, 220, regenLevel, false, false, true));
            }
            
            // Apply health boost
            int healthBoostLevel = skillData.getInt("health_boost");
            if (healthBoostLevel > 0) {
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HEALTH_BOOST, 220, healthBoostLevel - 1, false, false, true));
            }
            
            // Apply resistance
            int resistanceLevel = skillData.getInt("resistance");
            if (resistanceLevel > 0) {
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, 220, Math.min(resistanceLevel - 1, 1), false, false, true));
                
                // Level 3 adds fire resistance
                if (resistanceLevel >= 3) {
                    mob.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE, 220, 0, false, false, true));
                }
            }
            
            // Apply strength
            int strengthLevel = skillData.getInt("strength");
            if (strengthLevel > 0) {
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH, 220, strengthLevel - 1, false, false, true));
            }
            
            // Apply speed
            int speedLevel = skillData.getInt("speed");
            if (speedLevel > 0) {
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 220, speedLevel - 1, false, false, true));
            }
            
            // Passive-only: regeneration (different from healing)
            if (mobType.equals("passive")) {
                int regenLevel = skillData.getInt("regeneration");
                if (regenLevel > 0) {
                    mob.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.REGENERATION, 220, regenLevel - 1, false, false, true));
                }
            }
        }
        
        // TODO: Apply special abilities (piercing shot, multishot, horde summon, etc.)
        // TODO: Apply equipment
    }
    
    /**
     * Get the number of loaded/implemented mob configs
     */
    public static int getImplementedMobCount() {
        if (!configsLoaded) initialize();
        return MOB_CONFIGS.size();
    }
    
    /**
     * Get total mobs (80 vanilla mobs target)
     */
    public static int getTotalMobTarget() {
        return 80;
    }
    
    /**
     * Check if scaling system is fully connected
     */
    public static boolean isFullyConnected() {
        return configsLoaded && MOB_CONFIGS.size() > 0;
    }
    
    /**
     * Helper class for upgrade options
     */
    private static class UpgradeOption {
        final String key;
        final int newLevel;
        final int cost;
        
        UpgradeOption(String key, int newLevel, int cost) {
            this.key = key;
            this.newLevel = newLevel;
            this.cost = cost;
        }
    }
}
