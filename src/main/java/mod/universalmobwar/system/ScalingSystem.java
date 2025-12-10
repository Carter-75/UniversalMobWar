package mod.universalmobwar.system;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
 * ║    2. That's it! MobDataMixin calls processMobTick for ALL mobs.          ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ScalingSystem {

    private static final Gson GSON = new Gson();
    
    // Cache of loaded mob configs: mob_name -> JsonObject
    private static final Map<String, JsonObject> MOB_CONFIGS = new ConcurrentHashMap<>();
    
    // Entity class name -> mob config name mapping
    private static final Map<String, String> ENTITY_TO_CONFIG = new ConcurrentHashMap<>();
    
    // Track cooldowns for special abilities (mobUUID -> ability -> lastUseTick)
    private static final Map<UUID, Map<String, Long>> ABILITY_COOLDOWNS = new ConcurrentHashMap<>();
    
    private static final Set<String> KNOWN_BOSS_IDS = Set.of(
        "minecraft:ender_dragon",
        "minecraft:wither",
        "minecraft:warden",
        "minecraft:elder_guardian"
    );
    
    // List of all available mob config files (loaded dynamically)
    private static String[] IMPLEMENTED_MOBS = null;
        /**
         * Dynamically load all mob config names from the mob_configs resource directory
         */
        private static String[] getImplementedMobs() {
            if (IMPLEMENTED_MOBS != null) return IMPLEMENTED_MOBS;
            try {
                // Path to mob_configs directory in resources
                String resourcePath = "/mob_configs";
                java.net.URL dirURL = ScalingSystem.class.getResource(resourcePath);
                if (dirURL != null && dirURL.getProtocol().equals("file")) {
                    java.io.File dir = new java.io.File(dirURL.toURI());
                    String[] files = dir.list((d, name) -> name.endsWith(".json"));
                    if (files != null) {
                        IMPLEMENTED_MOBS = java.util.Arrays.stream(files)
                            .map(f -> f.substring(0, f.length() - 5).toLowerCase())
                            .toArray(String[]::new);
                    }
                } else if (dirURL != null && dirURL.getProtocol().equals("jar")) {
                    // Running from JAR: scan entries
                    String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        java.util.List<String> mobList = new java.util.ArrayList<>();
                        while (entries.hasMoreElements()) {
                            String entry = entries.nextElement().getName();
                            if (entry.startsWith("mob_configs/") && entry.endsWith(".json")) {
                                String mobName = entry.substring("mob_configs/".length(), entry.length() - 5).toLowerCase();
                                mobList.add(mobName);
                            }
                        }
                        IMPLEMENTED_MOBS = mobList.toArray(new String[0]);
                    }
                }
            } catch (Exception e) {
                UniversalMobWarMod.LOGGER.error("[ScalingSystem] Failed to load mob config list: {}", e.getMessage());
                IMPLEMENTED_MOBS = new String[0];
            }
            return IMPLEMENTED_MOBS;
        }
    
    private static boolean configsLoaded = false;
    
    // ==========================================================================
    //                           INITIALIZATION
    // ==========================================================================
    
    /**
     * Initialize the scaling system - load all JSON configs
     */
    public static void initialize() {
        if (configsLoaded) return;
        
        UniversalMobWarMod.LOGGER.info("[ScalingSystem] Loading mob configurations...");
        
        for (String mobName : getImplementedMobs()) {
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
            for (String implemented : getImplementedMobs()) {
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
    
    // ==========================================================================
    //                           MAIN ENTRY POINT
    // ==========================================================================
    
    /**
     * MAIN ENTRY POINT - Called from MobDataMixin on every mob tick
     * This single method handles ALL scaling logic for ALL mobs
     */
    public static void processMobTick(MobEntity mob, World world, MobWarData data) {
        // Skip client side
        if (world.isClient()) return;
        
        // Check if scaling is enabled
        ModConfig modConfig = ModConfig.getInstance();
        if (!modConfig.isScalingActive()) return;
        
        Identifier entityId = resolveEntityId(mob);
        String entityIdStr = entityId != null ? entityId.toString() : mob.getType().toString();
        
        if (modConfig.isMobExcluded(entityIdStr)) return;
        if (isModdedEntity(entityId)) {
            // TODO: add opt-in support for modded mobs when custom config schema exists
            return;
        }
        if (!modConfig.allowBossScaling && isBossEntity(entityId)) return;
        
        // Get config for this mob
        JsonObject config = getConfigForMob(mob);
        if (config == null) return; // No config = no scaling for this mob
        
        // Get mob type for different upgrade paths
        String mobType = config.has("mob_type") ? config.get("mob_type").getAsString() : "hostile";
        
        // Calculate total points from world age
        double totalPoints = calculateWorldAgePoints(world, config, modConfig);
        
        // Add kill points (stored in MobWarData)
        double killScaling = getKillScalingFactor(config);
        double killPoints = data.getKillCount() * killScaling * Math.max(0.0, modConfig.getKillScalingMultiplier());
        totalPoints += killPoints;
        
        // Store total for reference
        data.setSkillPoints(totalPoints);
        
        // Get current spent points
        double spentPoints = data.getSpentPoints();
        int budget = (int)Math.max(0, Math.floor(totalPoints - spentPoints));
        
        // Only process upgrades when:
        // 1. Mob just spawned (lastUpdate == 0)
        // 2. Mob hasn't been upgraded in 1+ day (24000 ticks)
        long currentTick = world.getTime();
        long lastUpdate = data.getSkillData().contains("lastUpdateTick") ? 
            data.getSkillData().getLong("lastUpdateTick") : 0;
        
        long ticksSinceLastUpdate = currentTick - lastUpdate;
        boolean shouldProcessUpgrades = (lastUpdate == 0) || (ticksSinceLastUpdate >= 24000);
        
        if (shouldProcessUpgrades) {
            // Update timestamp
            data.getSkillData().putLong("lastUpdateTick", currentTick);
            
            // Spend points on upgrades (with save chance logic loop)
            if (budget > 0) {
                spendPoints(mob, data, config, mobType, budget);
            }
            
            // Apply permanent effects only when upgrades change
            applyEffects(mob, data, config, mobType, currentTick);
            
            // Apply equipment (only when processing upgrades to avoid constant re-equipping)
            if (world instanceof ServerWorld serverWorld) {
                applyEquipment(mob, data, config, serverWorld);
            }
            
            // Save data after upgrade
            MobWarData.save(mob, data);
        }
    }
    
    // ==========================================================================
    //                           POINT CALCULATION
    // ==========================================================================
    
    /**
     * Calculate points from world age based on JSON daily_scaling config
     */
    private static double calculateWorldAgePoints(World world, JsonObject config, ModConfig modConfig) {
        int worldDays = (int) (world.getTime() / 24000L);
        double totalPoints = 0.0;
        
        // Get daily_scaling from config
        JsonObject pointSystem = getPointSystem(config);
        JsonArray dailyScaling = pointSystem != null && pointSystem.has("daily_scaling")
            ? pointSystem.getAsJsonArray("daily_scaling")
            : null;
        
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
        
        double dayMultiplier = Math.max(0.0, modConfig.getDayScalingMultiplier());
        return totalPoints * dayMultiplier;
    }

    private static JsonObject getPointSystem(JsonObject config) {
        if (config == null || !config.has("point_system")) return null;
        return config.getAsJsonObject("point_system");
    }

    private static double getKillScalingFactor(JsonObject config) {
        JsonObject pointSystem = getPointSystem(config);
        if (pointSystem != null && pointSystem.has("kill_scaling")) {
            try {
                return Math.max(0.0, pointSystem.get("kill_scaling").getAsDouble());
            } catch (Exception ignored) {
                return 1.0;
            }
        }
        return 1.0;
    }

    private static Identifier resolveEntityId(MobEntity mob) {
        return Registries.ENTITY_TYPE.getId(mob.getType());
    }

    private static boolean isModdedEntity(Identifier entityId) {
        return entityId != null && !"minecraft".equals(entityId.getNamespace());
    }

    private static boolean isBossEntity(Identifier entityId) {
        return entityId != null && KNOWN_BOSS_IDS.contains(entityId.toString());
    }
    
    // ==========================================================================
    //                           POINT SPENDING
    // ==========================================================================
    
    /**
     * Spend points on upgrades using 80/20 buy/save logic from JSON
     */
    private static void spendPoints(MobEntity mob, MobWarData data, JsonObject config, String mobType, int budget) {
        Random random = new Random(mob.getUuid().hashCode() + data.getSkillData().getLong("lastUpdateTick"));
        
        // Get buy/save chances (JSON defaults then overridden by global config slider)
        double buyChance = 0.80;
        double saveChance = 0.20;
        
        if (config.has("point_system")) {
            JsonObject ps = config.getAsJsonObject("point_system");
            if (ps.has("buy_chance")) buyChance = ps.get("buy_chance").getAsDouble();
            if (ps.has("save_chance")) saveChance = ps.get("save_chance").getAsDouble();
        }

        double totalChance = buyChance + saveChance;
        if (totalChance <= 0) {
            buyChance = 1.0;
            saveChance = 0.0;
        } else if (Math.abs(totalChance - 1.0) > 1e-6) {
            buyChance /= totalChance;
            saveChance /= totalChance;
        }

        ModConfig modConfig = ModConfig.getInstance();
        double configBuy = Math.max(0.0, Math.min(1.0, modConfig.getBuyChance()));
        double configSave = Math.max(0.0, Math.min(1.0, modConfig.getSaveChance()));
        if (configBuy > 0 || configSave > 0) {
            double configTotal = configBuy + configSave;
            if (configTotal <= 0) {
                buyChance = 1.0;
                saveChance = 0.0;
            } else {
                buyChance = configBuy / configTotal;
                saveChance = configSave / configTotal;
            }
        }
        
        // Get current upgrade levels from skill data
        NbtCompound skillData = data.getSkillData();
        
        // Spending loop
        int iterations = 0;
        boolean purchasedUpgrade = false;
        while (budget > 0 && iterations < 50) { // Cap iterations to prevent infinite loop
            iterations++;
            
            List<UpgradeOption> affordable = getAffordableUpgrades(mob, config, mobType, skillData, budget);
            
            if (affordable.isEmpty()) break;
            
            double roll = random.nextDouble();
            if (roll < saveChance) break;
            if (roll >= saveChance + buyChance) continue;
            
            // Buy a random affordable upgrade
            UpgradeOption chosen = affordable.get(random.nextInt(affordable.size()));
            
            // Apply the upgrade
            skillData.putInt(chosen.key, chosen.newLevel);
            data.setSpentPoints(data.getSpentPoints() + chosen.cost);
            budget -= chosen.cost;
            purchasedUpgrade = true;
        }
        
        data.setSkillData(skillData);
        if (purchasedUpgrade) {
            spawnUpgradeParticles(mob);
        }
    }

    private static void spawnUpgradeParticles(MobEntity mob) {
        ModConfig config = ModConfig.getInstance();
        if (config.disableParticles || !config.showLevelParticles) {
            return;
        }

        World world = mob.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        double horizontalSpread = Math.max(0.2, mob.getWidth() * 0.35);
        double verticalSpread = Math.max(0.25, mob.getHeight() * 0.4);
        serverWorld.spawnParticles(
            ParticleTypes.ENCHANT,
            mob.getX(),
            mob.getBodyY(0.6),
            mob.getZ(),
            24,
            horizontalSpread,
            verticalSpread,
            horizontalSpread,
            0.12
        );
    }
    
    /**
     * Get list of affordable upgrades from the mob's JSON config
     */
    private static List<UpgradeOption> getAffordableUpgrades(MobEntity mob, JsonObject config, String mobType, 
            NbtCompound skillData, int budget) {
        
        List<UpgradeOption> affordable = new ArrayList<>();
        
        if (!config.has("tree")) return affordable;
        JsonObject tree = config.getAsJsonObject("tree");
        
        // Check potion effects based on mob type
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (tree.has(effectsKey)) {
            JsonObject effects = tree.getAsJsonObject(effectsKey);
            addUpgradesFromSection(effects, skillData, budget, affordable, "effect_");
        }
        
        // Determine locked weapon type or attack capability (needed for special abilities filtering)
        String attackCapability = null; // "ranged", "melee", or "both"
        
        if (tree.has("weapon")) {
            JsonElement weaponElement = tree.get("weapon");
            if (weaponElement.isJsonArray()) {
                JsonArray weapons = weaponElement.getAsJsonArray();
                int index = Math.abs(mob.getUuid().hashCode()) % weapons.size();
                JsonObject lockedWeapon = weapons.get(index).getAsJsonObject();
                if (lockedWeapon.has("weapon_type")) {
                    String weaponType = lockedWeapon.get("weapon_type").getAsString();
                    attackCapability = isRangedWeaponType(weaponType) ? "ranged" : "melee";
                }
            } else {
                JsonObject weapon = weaponElement.getAsJsonObject();
                if (weapon.has("weapon_type")) {
                    String weaponType = weapon.get("weapon_type").getAsString();
                    attackCapability = isRangedWeaponType(weaponType) ? "ranged" : "melee";
                }
            }
        }
        
        // If no weapon defined but has special_abilities, check mob's natural attack type
        if (attackCapability == null && tree.has("special_abilities")) {
            JsonObject abilities = tree.getAsJsonObject("special_abilities");
            
            // If mob has ranged abilities defined, it's a ranged attacker (blaze, ghast, shulker, etc.)
            boolean hasRangedAbilities = abilities.has("piercing_shot") || 
                                         abilities.has("multishot") || 
                                         abilities.has("ranged_potion_mastery");
            
            // If mob has melee abilities defined, it's a melee attacker
            boolean hasMeleeAbilities = abilities.has("hunger_attack") || 
                                       abilities.has("cleave") || 
                                       abilities.has("life_steal");
            
            if (hasRangedAbilities && hasMeleeAbilities) {
                attackCapability = "both"; // Can use all abilities
            } else if (hasRangedAbilities) {
                attackCapability = "ranged";
            } else if (hasMeleeAbilities) {
                attackCapability = "melee";
            } else {
                attackCapability = "both"; // Unknown, allow all
            }
        }
        
        // Check special abilities (filtered by attack capability)
        if (tree.has("special_abilities")) {
            JsonObject abilities = tree.getAsJsonObject("special_abilities");
            addUpgradesFromSection(abilities, skillData, budget, affordable, "ability_", attackCapability);
        }
        
        // Check weapon upgrades
        if (tree.has("weapon")) {
            JsonElement weaponElement = tree.get("weapon");
            JsonObject lockedWeapon;
            
            if (weaponElement.isJsonArray()) {
                // Determine which weapon is locked for this mob
                JsonArray weapons = weaponElement.getAsJsonArray();
                int index = Math.abs(mob.getUuid().hashCode()) % weapons.size();
                lockedWeapon = weapons.get(index).getAsJsonObject();
                
                // Check tier upgrades for ALL weapons in array (lottery includes all weapons)
                for (int i = 0; i < weapons.size(); i++) {
                    JsonObject weaponOption = weapons.get(i).getAsJsonObject();
                    
                    // Get base cost of this weapon
                    int baseCost = weaponOption.has("base_cost") ? weaponOption.get("base_cost").getAsInt() : 0;
                    int currentTier = skillData.getInt("weapon_tier");
                    
                    if (currentTier == 0) {
                        // No weapon yet: show base cost to acquire weapon
                        if (baseCost <= budget) {
                            affordable.add(new UpgradeOption("weapon_tier", 1, baseCost));
                        }
                    } else if (weaponOption.has("tiers")) {
                        // Already have a weapon: show tier upgrades if available
                        JsonArray tiers = weaponOption.getAsJsonArray("tiers");
                        if (tiers.size() > 0 && currentTier < tiers.size()) {
                            JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                            int tierCost = nextTier.get("cost").getAsInt();
                            if (tierCost <= budget) {
                                affordable.add(new UpgradeOption("weapon_tier", currentTier + 1, tierCost));
                            }
                        }
                    }
                }
            } else {
                lockedWeapon = weaponElement.getAsJsonObject();
                
                // Single weapon: check its tiers
                if (lockedWeapon.has("tiers")) {
                    JsonArray tiers = lockedWeapon.getAsJsonArray("tiers");
                    int currentTier = skillData.getInt("weapon_tier");
                    if (currentTier < tiers.size()) {
                        JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                        int cost = nextTier.get("cost").getAsInt();
                        if (cost <= budget) {
                            affordable.add(new UpgradeOption("weapon_tier", currentTier + 1, cost));
                        }
                    }
                }
            }
            
            // Only upgrade the locked weapon's enchants and masteries
            
            // Weapon enchants
            if (lockedWeapon.has("enchants")) {
                JsonObject enchants = lockedWeapon.getAsJsonObject("enchants");
                addUpgradesFromSection(enchants, skillData, budget, affordable, "weapon_enchant_");
            }
            
            // Drop mastery
            addMasteryUpgrades(lockedWeapon, "drop_mastery", skillData, budget, affordable, "weapon_drop_mastery");
            addMasteryUpgrades(lockedWeapon, "durability_mastery", skillData, budget, affordable, "weapon_durability_mastery");
        }
        
        // Check shield upgrades
        if (tree.has("shield")) {
            JsonObject shield = tree.getAsJsonObject("shield");
            
            // Base shield cost
            int hasShield = skillData.getInt("has_shield");
            if (hasShield == 0 && shield.has("base_cost")) {
                int cost = shield.get("base_cost").getAsInt();
                if (cost <= budget) {
                    affordable.add(new UpgradeOption("has_shield", 1, cost));
                }
            }
            
            // Shield enchants (only if has shield)
            if (hasShield > 0 && shield.has("enchants")) {
                JsonObject enchants = shield.getAsJsonObject("enchants");
                addUpgradesFromSection(enchants, skillData, budget, affordable, "shield_enchant_");
            }
            
            addMasteryUpgrades(shield, "drop_mastery", skillData, budget, affordable, "shield_drop_mastery");
            addMasteryUpgrades(shield, "durability_mastery", skillData, budget, affordable, "shield_durability_mastery");
        }
        
        // Check armor upgrades
        for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            if (tree.has(slot)) {
                JsonObject armor = tree.getAsJsonObject(slot);
                
                // Armor tiers
                if (armor.has("tiers")) {
                    JsonArray tiers = armor.getAsJsonArray("tiers");
                    int currentTier = skillData.getInt(slot + "_tier");
                    if (currentTier < tiers.size()) {
                        JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                        int cost = nextTier.get("cost").getAsInt();
                        if (cost <= budget) {
                            affordable.add(new UpgradeOption(slot + "_tier", currentTier + 1, cost));
                        }
                    }
                }
                
                // Armor enchants
                if (armor.has("enchants")) {
                    JsonObject enchants = armor.getAsJsonObject("enchants");
                    addUpgradesFromSection(enchants, skillData, budget, affordable, slot + "_enchant_");
                }
                
                addMasteryUpgrades(armor, "drop_mastery", skillData, budget, affordable, slot + "_drop_mastery");
                addMasteryUpgrades(armor, "durability_mastery", skillData, budget, affordable, slot + "_durability_mastery");
            }
        }
        
        return affordable;
    }
    
    /**
     * Helper to add upgrades from a section with levels
     */
    private static void addUpgradesFromSection(JsonObject section, NbtCompound skillData, 
            int budget, List<UpgradeOption> affordable, String prefix) {
        addUpgradesFromSection(section, skillData, budget, affordable, prefix, null);
    }
    
    /**
     * Helper to add upgrades from a section with levels (with attack capability filtering)
     */
    private static void addUpgradesFromSection(JsonObject section, NbtCompound skillData, 
            int budget, List<UpgradeOption> affordable, String prefix, String attackCapability) {
        
        for (String key : section.keySet()) {
            JsonElement element = section.get(key);
            if (!element.isJsonArray()) continue;
            
            // Filter special abilities based on attack capability
            if (prefix.equals("ability_") && attackCapability != null && !attackCapability.equals("both")) {
                // Ranged abilities
                boolean isRangedAbility = key.equals("piercing_shot") || 
                                          key.equals("multishot") || 
                                          key.equals("ranged_potion_mastery");
                
                // Melee abilities
                boolean isMeleeAbility = key.equals("hunger_attack") || 
                                        key.equals("cleave") || 
                                        key.equals("life_steal");
                
                // Skip if ability doesn't match attack capability
                if (isRangedAbility && attackCapability.equals("melee")) continue;
                if (isMeleeAbility && attackCapability.equals("ranged")) continue;
            }
            
            JsonArray levels = element.getAsJsonArray();
            String fullKey = prefix + key;
            int currentLevel = skillData.getInt(fullKey);
            
            if (currentLevel < levels.size()) {
                JsonObject nextLevel = levels.get(currentLevel).getAsJsonObject();
                int cost = nextLevel.get("cost").getAsInt();
                
                if (cost <= budget) {
                    affordable.add(new UpgradeOption(fullKey, currentLevel + 1, cost));
                }
            }
        }
    }
    
    /**
     * Helper to add mastery upgrades (drop_mastery, durability_mastery)
     */
    private static void addMasteryUpgrades(JsonObject parent, String masteryKey, NbtCompound skillData,
            int budget, List<UpgradeOption> affordable, String saveKey) {
        
        if (!parent.has(masteryKey)) return;
        
        JsonArray levels = parent.getAsJsonArray(masteryKey);
        int currentLevel = skillData.getInt(saveKey);
        
        if (currentLevel < levels.size()) {
            JsonObject nextLevel = levels.get(currentLevel).getAsJsonObject();
            int cost = nextLevel.get("cost").getAsInt();
            
            if (cost <= budget) {
                affordable.add(new UpgradeOption(saveKey, currentLevel + 1, cost));
            }
        }
    }
    
    // ==========================================================================
    //                           EFFECT APPLICATION
    // ==========================================================================
    
    /**
     * Apply all effects based on current upgrade levels
     */
    private static void applyEffects(MobEntity mob, MobWarData data, JsonObject config, String mobType, long currentTick) {
        NbtCompound skillData = data.getSkillData();
        
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        // Get effects section
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (tree.has(effectsKey)) {
            JsonObject effects = tree.getAsJsonObject(effectsKey);
            
            // Apply healing/regeneration
            applyPotionEffect(mob, skillData, effects, "healing", StatusEffects.REGENERATION, "effect_healing", "regen_level");
            
            // Apply health boost
            applyPotionEffect(mob, skillData, effects, "health_boost", StatusEffects.HEALTH_BOOST, "effect_health_boost", null);
            
            // Apply resistance
            int resistanceLevel = skillData.getInt("effect_resistance");
            if (resistanceLevel > 0) {
                // Permanent effect (infinite duration)
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, StatusEffectInstance.INFINITE, Math.min(resistanceLevel - 1, 1), false, false, true));
                
                // Check for fire resistance at level 3
                if (effects.has("resistance")) {
                    JsonArray resistanceLevels = effects.getAsJsonArray("resistance");
                    if (resistanceLevel <= resistanceLevels.size()) {
                        JsonObject levelData = resistanceLevels.get(resistanceLevel - 1).getAsJsonObject();
                        if (levelData.has("fire_resistance") && levelData.get("fire_resistance").getAsBoolean()) {
                            mob.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.FIRE_RESISTANCE, StatusEffectInstance.INFINITE, 0, false, false, true));
                        }
                    }
                }
            }
            
            // Apply strength
            applyPotionEffect(mob, skillData, effects, "strength", StatusEffects.STRENGTH, "effect_strength", "strength_level");
            
            // Apply speed
            applyPotionEffect(mob, skillData, effects, "speed", StatusEffects.SPEED, "effect_speed", "speed_level");
            
            // Passive-only: regeneration (different from healing)
            if (mobType.equals("passive")) {
                applyPotionEffect(mob, skillData, effects, "regeneration", StatusEffects.REGENERATION, "effect_regeneration", null);
            }
        }
    }
    
    /**
     * Helper to apply a potion effect based on JSON level data
     */
    private static void applyPotionEffect(MobEntity mob, NbtCompound skillData, JsonObject effects,
            String effectName, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
            String skillKey, String levelKey) {
        
        int level = skillData.getInt(skillKey);
        if (level <= 0) return;
        
        int amplifier = level - 1; // Default: level 1 = amplifier 0
        
        // Try to get specific amplifier from JSON
        if (effects.has(effectName) && levelKey != null) {
            JsonArray levels = effects.getAsJsonArray(effectName);
            if (level <= levels.size()) {
                JsonObject levelData = levels.get(level - 1).getAsJsonObject();
                if (levelData.has(levelKey)) {
                    amplifier = levelData.get(levelKey).getAsInt() - 1;
                }
            }
        }
        
        // Apply permanent effect (infinite duration) - only called when upgrades change
        mob.addStatusEffect(new StatusEffectInstance(effect, StatusEffectInstance.INFINITE, Math.max(0, amplifier), false, false, true));
    }
    
    // ==========================================================================
    //                           EQUIPMENT APPLICATION
    // ==========================================================================
    
    /**
     * Apply equipment based on upgrade levels
     */
    private static void applyEquipment(MobEntity mob, MobWarData data, JsonObject config, ServerWorld world) {
        NbtCompound skillData = data.getSkillData();
        
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        // Apply weapon
        if (tree.has("weapon")) {
            JsonElement weaponElement = tree.get("weapon");
            JsonObject weapon;
            
            if (weaponElement.isJsonArray()) {
                JsonArray weapons = weaponElement.getAsJsonArray();
                int index = Math.abs(mob.getUuid().hashCode()) % weapons.size();
                weapon = weapons.get(index).getAsJsonObject();
            } else {
                weapon = weaponElement.getAsJsonObject();
            }
            
            applyWeapon(mob, skillData, weapon, world);
        }
        
        // Apply shield
        if (tree.has("shield")) {
            applyShield(mob, skillData, tree.getAsJsonObject("shield"), world);
        }
        
        // Apply armor
        applyArmor(mob, skillData, tree, "helmet", EquipmentSlot.HEAD, world);
        applyArmor(mob, skillData, tree, "chestplate", EquipmentSlot.CHEST, world);
        applyArmor(mob, skillData, tree, "leggings", EquipmentSlot.LEGS, world);
        applyArmor(mob, skillData, tree, "boots", EquipmentSlot.FEET, world);
    }
    
    /**
     * Apply weapon with enchants
     */
    private static void applyWeapon(MobEntity mob, NbtCompound skillData, JsonObject weaponConfig, ServerWorld world) {
        String weaponType = weaponConfig.has("weapon_type") ? weaponConfig.get("weapon_type").getAsString() : "sword";
        
        // Determine weapon item based on type and tier
        ItemStack weapon = null;
        
        if (weaponType.equals("bow")) {
            // Bows don't have material tiers - base_cost 0 means mob spawns with it
            int baseCost = weaponConfig.has("base_cost") ? weaponConfig.get("base_cost").getAsInt() : 0;
            if (baseCost == 0) {
                // Mob spawns with bow
                weapon = new ItemStack(Items.BOW);
            } else {
                // Mob must purchase bow - check if any enchants purchased
                boolean hasAnyBowUpgrade = false;
                if (weaponConfig.has("enchants")) {
                    JsonObject enchants = weaponConfig.getAsJsonObject("enchants");
                    for (String enchantName : enchants.keySet()) {
                        if (skillData.getInt("weapon_enchant_" + enchantName) > 0) {
                            hasAnyBowUpgrade = true;
                            break;
                        }
                    }
                }
                if (hasAnyBowUpgrade) {
                    weapon = new ItemStack(Items.BOW);
                }
            }
        } else if (weaponType.equals("crossbow")) {
            int baseCost = weaponConfig.has("base_cost") ? weaponConfig.get("base_cost").getAsInt() : 0;
            if (baseCost == 0) {
                // Mob spawns with crossbow
                weapon = new ItemStack(Items.CROSSBOW);
            } else {
                // Mob must purchase crossbow
                boolean hasAnyUpgrade = false;
                if (weaponConfig.has("enchants")) {
                    JsonObject enchants = weaponConfig.getAsJsonObject("enchants");
                    for (String enchantName : enchants.keySet()) {
                        if (skillData.getInt("weapon_enchant_" + enchantName) > 0) {
                            hasAnyUpgrade = true;
                            break;
                        }
                    }
                }
                if (hasAnyUpgrade) {
                    weapon = new ItemStack(Items.CROSSBOW);
                }
            }
        } else if (weaponType.equals("trident")) {
            // Tridents - base_cost 0 means mob spawns with it
            int baseCost = weaponConfig.has("base_cost") ? weaponConfig.get("base_cost").getAsInt() : 0;
            if (baseCost == 0) {
                // Mob spawns with trident
                weapon = new ItemStack(Items.TRIDENT);
            } else {
                // Mob must purchase trident - check if any enchants purchased
                boolean hasAnyUpgrade = false;
                if (weaponConfig.has("enchants")) {
                    JsonObject enchants = weaponConfig.getAsJsonObject("enchants");
                    for (String enchantName : enchants.keySet()) {
                        if (skillData.getInt("weapon_enchant_" + enchantName) > 0) {
                            hasAnyUpgrade = true;
                            break;
                        }
                    }
                }
                if (hasAnyUpgrade) {
                    weapon = new ItemStack(Items.TRIDENT);
                }
            }
        } else {
            // Sword/Axe tiers
            int tier = skillData.getInt("weapon_tier");
            if (tier > 0 && weaponConfig.has("tiers")) {
                JsonArray tiers = weaponConfig.getAsJsonArray("tiers");
                if (tier <= tiers.size()) {
                    JsonObject tierData = tiers.get(tier - 1).getAsJsonObject();
                    String tierName = tierData.get("tier").getAsString();
                    weapon = getWeaponByTier(tierName, weaponType);
                }
            }
        }
        
        if (weapon == null || weapon.isEmpty()) return;
        
        // Apply enchants
        if (weaponConfig.has("enchants")) {
            applyEnchantments(weapon, skillData, weaponConfig.getAsJsonObject("enchants"), "weapon_enchant_", world);
        }
        
        // Apply durability mastery - set durability based on level
        applyDurabilityMastery(weapon, skillData, weaponConfig, "weapon_durability_mastery");
        
        // Equip it
        mob.equipStack(EquipmentSlot.MAINHAND, weapon);
    }
    
    /**
     * Apply shield with enchants
     */
    private static void applyShield(MobEntity mob, NbtCompound skillData, JsonObject shieldConfig, ServerWorld world) {
        int hasShield = skillData.getInt("has_shield");
        if (hasShield <= 0) return;
        
        ItemStack shield = new ItemStack(Items.SHIELD);
        
        // Apply enchants
        if (shieldConfig.has("enchants")) {
            applyEnchantments(shield, skillData, shieldConfig.getAsJsonObject("enchants"), "shield_enchant_", world);
        }
        
        // Apply durability mastery
        applyDurabilityMastery(shield, skillData, shieldConfig, "shield_durability_mastery");
        
        // Equip it
        mob.equipStack(EquipmentSlot.OFFHAND, shield);
    }
    
    /**
     * Apply armor piece with enchants
     */
    private static void applyArmor(MobEntity mob, NbtCompound skillData, JsonObject tree, 
            String slotName, EquipmentSlot slot, ServerWorld world) {
        
        if (!tree.has(slotName)) return;
        JsonObject armorConfig = tree.getAsJsonObject(slotName);
        
        int tier = skillData.getInt(slotName + "_tier");
        if (tier <= 0) return;
        
        ItemStack armor = null;
        
        if (armorConfig.has("tiers")) {
            JsonArray tiers = armorConfig.getAsJsonArray("tiers");
            if (tier <= tiers.size()) {
                JsonObject tierData = tiers.get(tier - 1).getAsJsonObject();
                String tierName = tierData.get("tier").getAsString();
                armor = getArmorByTierAndSlot(tierName, slot);
            }
        }
        
        if (armor == null || armor.isEmpty()) return;
        
        // Apply enchants
        if (armorConfig.has("enchants")) {
            applyEnchantments(armor, skillData, armorConfig.getAsJsonObject("enchants"), slotName + "_enchant_", world);
        }
        
        // Apply durability mastery
        applyDurabilityMastery(armor, skillData, armorConfig, slotName + "_durability_mastery");
        
        // Equip it
        mob.equipStack(slot, armor);
    }
    
    /**
     * Apply enchantments from JSON config to an item
     */
    private static void applyEnchantments(ItemStack item, NbtCompound skillData, JsonObject enchantsConfig, 
            String prefix, ServerWorld world) {
        
        var enchantRegistry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
            item.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        
        for (String enchantName : enchantsConfig.keySet()) {
            int level = skillData.getInt(prefix + enchantName);
            if (level <= 0) continue;
            
            // Map JSON enchant name to Minecraft enchantment
            RegistryEntry<Enchantment> enchant = getEnchantmentByName(enchantName, enchantRegistry);
            if (enchant != null) {
                builder.add(enchant, level);
            }
        }
        
        item.set(DataComponentTypes.ENCHANTMENTS, builder.build());
    }
    
    /**
     * Apply durability mastery - set item durability based on upgrade level
     * Higher mastery = spawn with more durability (0.10 to 1.00 = 10% to 100%)
     */
    private static void applyDurabilityMastery(ItemStack item, NbtCompound skillData, JsonObject config, String skillKey) {
        int masteryLevel = skillData.getInt(skillKey);
        if (masteryLevel <= 0) return;
        
        // Get durability percentage from config
        double durabilityPercent = 0.10; // Default 10%
        if (config.has("durability_mastery")) {
            JsonArray levels = config.getAsJsonArray("durability_mastery");
            if (masteryLevel <= levels.size()) {
                JsonObject levelData = levels.get(masteryLevel - 1).getAsJsonObject();
                if (levelData.has("durability")) {
                    durabilityPercent = levelData.get("durability").getAsDouble();
                }
            }
        }
        
        // Set item damage (inverted - lower damage = more durability)
        int maxDurability = item.getMaxDamage();
        if (maxDurability > 0) {
            int targetDurability = (int) (maxDurability * durabilityPercent);
            int damageToSet = maxDurability - targetDurability;
            item.setDamage(Math.max(0, damageToSet));
        }
    }
    
    /**
     * Get weapon ItemStack by tier name and weapon type
     */
    private static ItemStack getWeaponByTier(String tier, String weaponType) {
        boolean isAxe = weaponType != null && weaponType.contains("axe");
        
        return switch (tier.toLowerCase()) {
            case "wooden", "wood" -> new ItemStack(isAxe ? Items.WOODEN_AXE : Items.WOODEN_SWORD);
            case "stone" -> new ItemStack(isAxe ? Items.STONE_AXE : Items.STONE_SWORD);
            case "iron" -> new ItemStack(isAxe ? Items.IRON_AXE : Items.IRON_SWORD);
            case "golden", "gold" -> new ItemStack(isAxe ? Items.GOLDEN_AXE : Items.GOLDEN_SWORD);
            case "diamond" -> new ItemStack(isAxe ? Items.DIAMOND_AXE : Items.DIAMOND_SWORD);
            case "netherite" -> new ItemStack(isAxe ? Items.NETHERITE_AXE : Items.NETHERITE_SWORD);
            default -> ItemStack.EMPTY;
        };
    }
    
    /**
     * Get armor ItemStack by tier and slot
     */
    private static ItemStack getArmorByTierAndSlot(String tier, EquipmentSlot slot) {
        return switch (tier.toLowerCase()) {
            case "leather" -> switch (slot) {
                case HEAD -> new ItemStack(Items.LEATHER_HELMET);
                case CHEST -> new ItemStack(Items.LEATHER_CHESTPLATE);
                case LEGS -> new ItemStack(Items.LEATHER_LEGGINGS);
                case FEET -> new ItemStack(Items.LEATHER_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case "chainmail", "chain" -> switch (slot) {
                case HEAD -> new ItemStack(Items.CHAINMAIL_HELMET);
                case CHEST -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                case LEGS -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                case FEET -> new ItemStack(Items.CHAINMAIL_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case "iron" -> switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case "golden", "gold" -> switch (slot) {
                case HEAD -> new ItemStack(Items.GOLDEN_HELMET);
                case CHEST -> new ItemStack(Items.GOLDEN_CHESTPLATE);
                case LEGS -> new ItemStack(Items.GOLDEN_LEGGINGS);
                case FEET -> new ItemStack(Items.GOLDEN_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case "diamond" -> switch (slot) {
                case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case "netherite" -> switch (slot) {
                case HEAD -> new ItemStack(Items.NETHERITE_HELMET);
                case CHEST -> new ItemStack(Items.NETHERITE_CHESTPLATE);
                case LEGS -> new ItemStack(Items.NETHERITE_LEGGINGS);
                case FEET -> new ItemStack(Items.NETHERITE_BOOTS);
                default -> ItemStack.EMPTY;
            };
            default -> ItemStack.EMPTY;
        };
    }
    
    /**
     * Check if a weapon type is ranged
     */
    private static boolean isRangedWeaponType(String weaponType) {
        if (weaponType == null) return false;
        String type = weaponType.toLowerCase();
        return type.equals("bow") || type.equals("crossbow") || type.equals("trident");
    }
    
    /**
     * Get enchantment registry entry by name
     */
    private static RegistryEntry<Enchantment> getEnchantmentByName(String name, 
            net.minecraft.registry.Registry<Enchantment> registry) {
        
        // Common enchantment name mappings
        String key = switch (name.toLowerCase()) {
            case "power" -> "power";
            case "punch" -> "punch";
            case "flame" -> "flame";
            case "infinity" -> "infinity";
            case "unbreaking" -> "unbreaking";
            case "mending" -> "mending";
            case "sharpness" -> "sharpness";
            case "smite" -> "smite";
            case "bane_of_arthropods", "bane" -> "bane_of_arthropods";
            case "knockback" -> "knockback";
            case "fire_aspect" -> "fire_aspect";
            case "looting" -> "looting";
            case "sweeping", "sweeping_edge" -> "sweeping_edge";
            case "protection" -> "protection";
            case "fire_protection" -> "fire_protection";
            case "blast_protection" -> "blast_protection";
            case "projectile_protection" -> "projectile_protection";
            case "thorns" -> "thorns";
            case "respiration" -> "respiration";
            case "aqua_affinity" -> "aqua_affinity";
            case "depth_strider" -> "depth_strider";
            case "frost_walker" -> "frost_walker";
            case "soul_speed" -> "soul_speed";
            case "feather_falling" -> "feather_falling";
            default -> name.toLowerCase();
        };
        
        var id = net.minecraft.util.Identifier.of("minecraft", key);
        return registry.getEntry(id).orElse(null);
    }
    
    // ==========================================================================
    //                           SPECIAL ABILITIES
    // ==========================================================================
    
    /**
     * Handle on-damage abilities like invisibility_on_hit
     * Call this from a damage event handler
     */
    public static void handleDamageAbilities(MobEntity mob, MobWarData data, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        String mobType = config.has("mob_type") ? config.get("mob_type").getAsString() : "hostile";
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (!tree.has(effectsKey)) return;
        JsonObject effects = tree.getAsJsonObject(effectsKey);
        
        // Check invisibility_on_hit
        int invisLevel = skillData.getInt("effect_invisibility_on_hit");
        if (invisLevel > 0 && effects.has("invisibility_on_hit")) {
            JsonArray levels = effects.getAsJsonArray("invisibility_on_hit");
            if (invisLevel <= levels.size()) {
                JsonObject levelData = levels.get(invisLevel - 1).getAsJsonObject();
                
                double chance = levelData.has("chance") ? levelData.get("chance").getAsDouble() : 0.1;
                int duration = levelData.has("duration") ? levelData.get("duration").getAsInt() : 5;
                int cooldown = levelData.has("cooldown") ? levelData.get("cooldown").getAsInt() : 60;
                
                // Check cooldown
                UUID mobUuid = mob.getUuid();
                Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                long lastUse = cooldowns.getOrDefault("invisibility_on_hit", 0L);
                
                if (currentTick - lastUse >= cooldown * 20L) { // cooldown is in seconds
                    // Roll chance
                    if (mob.getRandom().nextDouble() < chance) {
                        mob.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.INVISIBILITY, duration * 20, 0, false, false, true));
                        cooldowns.put("invisibility_on_hit", currentTick);
                    }
                }
            }
        }
        
        // Check on_damage_regen (from healing ability)
        int healingLevel = skillData.getInt("effect_healing");
        if (healingLevel >= 3 && effects.has("healing")) {
            JsonArray levels = effects.getAsJsonArray("healing");
            if (healingLevel <= levels.size()) {
                JsonObject levelData = levels.get(healingLevel - 1).getAsJsonObject();
                
                if (levelData.has("on_damage_regen_level")) {
                    int regenLevel = levelData.get("on_damage_regen_level").getAsInt();
                    int duration = levelData.has("on_damage_duration") ? levelData.get("on_damage_duration").getAsInt() : 10;
                    int cooldown = levelData.has("on_damage_cooldown") ? levelData.get("on_damage_cooldown").getAsInt() : 60;
                    
                    UUID mobUuid = mob.getUuid();
                    Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                    long lastUse = cooldowns.getOrDefault("on_damage_regen", 0L);
                    
                    if (currentTick - lastUse >= cooldown * 20L) {
                        mob.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.REGENERATION, duration * 20, regenLevel - 1, false, false, true));
                        cooldowns.put("on_damage_regen", currentTick);
                    }
                }
            }
        }
    }
    
    // ==========================================================================
    //                        SPECIAL ABILITY HANDLERS
    // ==========================================================================
    
    /**
     * Handle melee attack abilities like hunger_attack
     * Call this when a mob deals melee damage to a player
     */
    public static void handleMeleeAttackAbilities(MobEntity mob, MobWarData data, 
            net.minecraft.entity.LivingEntity target, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        // Hunger Attack - apply hunger effect on hit
        int hungerLevel = skillData.getInt("ability_hunger_attack");
        if (hungerLevel > 0 && abilities.has("hunger_attack")) {
            JsonArray levels = abilities.getAsJsonArray("hunger_attack");
            if (hungerLevel <= levels.size()) {
                JsonObject levelData = levels.get(hungerLevel - 1).getAsJsonObject();
                int effectLevel = levelData.has("hunger_level") ? levelData.get("hunger_level").getAsInt() : 1;
                int duration = levelData.has("duration") ? levelData.get("duration").getAsInt() : 10;
                
                target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HUNGER, duration * 20, effectLevel - 1, false, true, true));
            }
        }
    }
    
    /**
     * Handle horde summon ability - chance to summon reinforcements when damaged
     * Call this when a mob takes damage
     */
    public static void handleHordeSummon(MobEntity mob, MobWarData data, ServerWorld world, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        // Horde Summon - chance to spawn reinforcements
        int hordeLevel = skillData.getInt("ability_horde_summon");
        if (hordeLevel > 0 && abilities.has("horde_summon")) {
            JsonArray levels = abilities.getAsJsonArray("horde_summon");
            if (hordeLevel <= levels.size()) {
                JsonObject levelData = levels.get(hordeLevel - 1).getAsJsonObject();
                double chance = levelData.has("chance") ? levelData.get("chance").getAsDouble() : 0.1;
                
                // Check cooldown (60 seconds)
                UUID mobUuid = mob.getUuid();
                Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                long lastUse = cooldowns.getOrDefault("horde_summon", 0L);
                
                if (currentTick - lastUse >= 1200L) { // 60 seconds cooldown
                    if (mob.getRandom().nextDouble() < chance) {
                        // Spawn a copy of this mob type nearby
                        try {
                            MobEntity reinforcement = (MobEntity) mob.getType().create(world);
                            if (reinforcement != null) {
                                double offsetX = (mob.getRandom().nextDouble() - 0.5) * 4;
                                double offsetZ = (mob.getRandom().nextDouble() - 0.5) * 4;
                                reinforcement.refreshPositionAndAngles(
                                    mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ,
                                    mob.getRandom().nextFloat() * 360, 0);
                                world.spawnEntity(reinforcement);
                                cooldowns.put("horde_summon", currentTick);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }
    
    /**
     * Handle ranged attack abilities - piercing, multishot, potion effects
     * Call this when a mob fires a projectile
     * Returns the number of extra projectiles to fire (for multishot)
     */
    public static int handleRangedAbilities(MobEntity mob, MobWarData data, 
            net.minecraft.entity.LivingEntity target, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return 0;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return 0;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return 0;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return 0;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int extraProjectiles = 0;
        
        // Multishot - extra projectiles
        int multishotLevel = skillData.getInt("ability_multishot");
        if (multishotLevel > 0 && abilities.has("multishot")) {
            JsonArray levels = abilities.getAsJsonArray("multishot");
            if (multishotLevel <= levels.size()) {
                JsonObject levelData = levels.get(multishotLevel - 1).getAsJsonObject();
                extraProjectiles = levelData.has("extra_projectiles") ? 
                    levelData.get("extra_projectiles").getAsInt() : 1;
            }
        }
        
        return extraProjectiles;
    }
    
    /**
     * Get piercing level for projectiles
     */
    public static int getPiercingLevel(MobEntity mob, MobWarData data) {
        if (!ModConfig.getInstance().isScalingActive()) return 0;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return 0;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return 0;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return 0;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int piercingLevel = skillData.getInt("ability_piercing_shot");
        if (piercingLevel > 0 && abilities.has("piercing_shot")) {
            JsonArray levels = abilities.getAsJsonArray("piercing_shot");
            if (piercingLevel <= levels.size()) {
                JsonObject levelData = levels.get(piercingLevel - 1).getAsJsonObject();
                return levelData.has("pierce_count") ? levelData.get("pierce_count").getAsInt() : 1;
            }
        }
        return 0;
    }
    
    /**
     * Apply ranged potion effects to a hit target
     * Call this when a projectile from a mob hits a target
     */
    public static void applyRangedPotionEffects(MobEntity mob, MobWarData data, 
            net.minecraft.entity.LivingEntity target) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int potionMasteryLevel = skillData.getInt("ability_ranged_potion_mastery");
        if (potionMasteryLevel > 0 && abilities.has("ranged_potion_mastery")) {
            JsonArray levels = abilities.getAsJsonArray("ranged_potion_mastery");
            if (potionMasteryLevel <= levels.size()) {
                JsonObject levelData = levels.get(potionMasteryLevel - 1).getAsJsonObject();
                double chance = levelData.has("chance") ? levelData.get("chance").getAsDouble() : 0.2;
                
                if (mob.getRandom().nextDouble() < chance && levelData.has("effects")) {
                    JsonArray effectsArray = levelData.getAsJsonArray("effects");
                    for (JsonElement effectEl : effectsArray) {
                        JsonObject effect = effectEl.getAsJsonObject();
                        String type = effect.get("type").getAsString();
                        int level = effect.has("level") ? effect.get("level").getAsInt() : 1;
                        int duration = effect.has("duration") ? effect.get("duration").getAsInt() : 10;
                        
                        var statusEffect = getPotionEffectByName(type);
                        if (statusEffect != null) {
                            // Instant effects don't need duration
                            if (type.equals("instant_damage") || type.equals("instant_health")) {
                                target.addStatusEffect(new StatusEffectInstance(
                                    statusEffect, 1, level - 1, false, true, true));
                            } else {
                                target.addStatusEffect(new StatusEffectInstance(
                                    statusEffect, duration * 20, level - 1, false, true, true));
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get status effect by name for ranged potion mastery
     */
    private static net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> 
            getPotionEffectByName(String name) {
        return switch (name.toLowerCase()) {
            case "slowness" -> StatusEffects.SLOWNESS;
            case "weakness" -> StatusEffects.WEAKNESS;
            case "poison" -> StatusEffects.POISON;
            case "wither" -> StatusEffects.WITHER;
            case "instant_damage", "harming" -> StatusEffects.INSTANT_DAMAGE;
            case "instant_health", "healing" -> StatusEffects.INSTANT_HEALTH;
            case "blindness" -> StatusEffects.BLINDNESS;
            case "nausea" -> StatusEffects.NAUSEA;
            case "hunger" -> StatusEffects.HUNGER;
            case "mining_fatigue" -> StatusEffects.MINING_FATIGUE;
            case "levitation" -> StatusEffects.LEVITATION;
            default -> null;
        };
    }
    
    // ==========================================================================
    //                    ENDERMAN SPECIAL ABILITIES
    // ==========================================================================
    
    /**
     * Handle Enderman shadow_step ability - teleport and leave blindness area
     * Call this when Enderman teleports (in teleport event handler)
     */
    public static void handleShadowStep(MobEntity mob, MobWarData data, ServerWorld world, 
            net.minecraft.util.math.BlockPos fromPos, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int shadowStepLevel = skillData.getInt("ability_shadow_step");
        if (shadowStepLevel > 0 && abilities.has("shadow_step")) {
            JsonArray levels = abilities.getAsJsonArray("shadow_step");
            if (shadowStepLevel <= levels.size()) {
                JsonObject levelData = levels.get(shadowStepLevel - 1).getAsJsonObject();
                
                double chance = levelData.has("chance") ? levelData.get("chance").getAsDouble() : 0.2;
                int blindDuration = levelData.has("blind_duration") ? levelData.get("blind_duration").getAsInt() : 2;
                int cooldown = levelData.has("cooldown") ? levelData.get("cooldown").getAsInt() : 12;
                
                // Check cooldown
                UUID mobUuid = mob.getUuid();
                Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                long lastUse = cooldowns.getOrDefault("shadow_step", 0L);
                
                if (currentTick - lastUse >= cooldown * 20L) {
                    if (mob.getRandom().nextDouble() < chance) {
                        // Apply blindness to all entities in 3 block radius of where Enderman teleported FROM
                        double radius = 3.0;
                        world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, 
                            new net.minecraft.util.math.Box(fromPos).expand(radius),
                            entity -> entity != mob && entity instanceof net.minecraft.entity.player.PlayerEntity)
                            .forEach(entity -> {
                                entity.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.BLINDNESS, blindDuration * 20, 0, false, true, true));
                            });
                        
                        cooldowns.put("shadow_step", currentTick);
                    }
                }
            }
        }
    }
    
    /**
     * Handle Enderman void_grasp ability - check range, roll chance, apply effects
     * Call this periodically (every few seconds) to check for nearby entities
     */
    public static void handleVoidGrasp(MobEntity mob, MobWarData data, ServerWorld world, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int voidGraspLevel = skillData.getInt("ability_void_grasp");
        if (voidGraspLevel > 0 && abilities.has("void_grasp")) {
            JsonArray levels = abilities.getAsJsonArray("void_grasp");
            if (voidGraspLevel <= levels.size()) {
                JsonObject levelData = levels.get(voidGraspLevel - 1).getAsJsonObject();
                
                double chance = levelData.has("chance") ? levelData.get("chance").getAsDouble() : 0.25;
                double range = levelData.has("range") ? levelData.get("range").getAsDouble() : 10.0;
                int weaknessLevel = levelData.has("weakness_level") ? levelData.get("weakness_level").getAsInt() : 1;
                int weaknessDuration = levelData.has("weakness_duration") ? levelData.get("weakness_duration").getAsInt() : 6;
                int levitationDuration = levelData.has("levitation_duration") ? levelData.get("levitation_duration").getAsInt() : 0;
                
                // Check cooldown (3 seconds)
                UUID mobUuid = mob.getUuid();
                Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                long lastUse = cooldowns.getOrDefault("void_grasp", 0L);
                
                if (currentTick - lastUse >= 60L) { // 3 second cooldown
                    // Find entities in range
                    var nearbyEntities = world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                        mob.getBoundingBox().expand(range),
                        entity -> entity != mob && entity instanceof net.minecraft.entity.player.PlayerEntity);
                    
                    if (!nearbyEntities.isEmpty()) {
                        // Roll chance
                        if (mob.getRandom().nextDouble() < chance) {
                            // Apply effects to all entities in range
                            nearbyEntities.forEach(entity -> {
                                // Always apply weakness
                                entity.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.WEAKNESS, weaknessDuration * 20, weaknessLevel - 1, false, true, true));
                                
                                // Apply levitation if duration > 0
                                if (levitationDuration > 0) {
                                    entity.addStatusEffect(new StatusEffectInstance(
                                        StatusEffects.LEVITATION, levitationDuration * 20, 0, false, true, true));
                                }
                            });
                            
                            cooldowns.put("void_grasp", currentTick);
                        }
                    }
                }
            }
        }
    }
    
    // ==========================================================================
    //                    CAVE SPIDER SPECIAL ABILITIES
    // ==========================================================================
    
    /**
     * Get poison mastery data for Cave Spider melee attacks
     * Call this when Cave Spider deals melee damage to apply poison/wither/slowness
     */
    public static void applyCaveSpiderPoison(MobEntity mob, MobWarData data, 
            net.minecraft.entity.LivingEntity target) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int poisonLevel = skillData.getInt("ability_poison_mastery");
        if (poisonLevel > 0 && abilities.has("poison_mastery")) {
            JsonArray levels = abilities.getAsJsonArray("poison_mastery");
            if (poisonLevel <= levels.size()) {
                JsonObject levelData = levels.get(poisonLevel - 1).getAsJsonObject();
                
                int poisonEffectLevel = levelData.has("poison_level") ? levelData.get("poison_level").getAsInt() : 1;
                int duration = levelData.has("duration") ? levelData.get("duration").getAsInt() : 7;
                
                // Apply poison
                target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.POISON, duration * 20, poisonEffectLevel - 1, false, true, true));
                
                // Apply wither if level 5+
                if (levelData.has("wither_level")) {
                    int witherLevel = levelData.get("wither_level").getAsInt();
                    int witherDuration = levelData.has("wither_duration") ? levelData.get("wither_duration").getAsInt() : 10;
                    target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER, witherDuration * 20, witherLevel - 1, false, true, true));
                }
                
                // Apply slowness if level 6
                if (levelData.has("slowness_level")) {
                    int slownessLevel = levelData.get("slowness_level").getAsInt();
                    int slownessDuration = levelData.has("slowness_duration") ? levelData.get("slowness_duration").getAsInt() : 15;
                    target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, slownessDuration * 20, slownessLevel - 1, false, true, true));
                }
            }
        }
    }
    
    // ==========================================================================
    //                    CREEPER SPECIAL ABILITIES
    // ==========================================================================
    
    /**
     * Get Creeper explosion power multiplier
     * Call this when Creeper is about to explode
     */
    public static float getCreeperExplosionRadius(MobEntity mob, MobWarData data) {
        if (!ModConfig.getInstance().isScalingActive()) return 3.0f; // Default creeper explosion
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return 3.0f;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return 3.0f;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return 3.0f;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int powerLevel = skillData.getInt("ability_creeper_power");
        if (powerLevel > 0 && abilities.has("creeper_power")) {
            JsonArray levels = abilities.getAsJsonArray("creeper_power");
            if (powerLevel <= levels.size()) {
                JsonObject levelData = levels.get(powerLevel - 1).getAsJsonObject();
                if (levelData.has("explosion_radius")) {
                    return levelData.get("explosion_radius").getAsFloat();
                }
            }
        }
        
        return 3.0f; // Default
    }
    
    /**
     * Spawn potion cloud effects at Creeper explosion location
     * Call this when Creeper explodes
     */
    public static void spawnCreeperPotionCloud(MobEntity mob, MobWarData data, ServerWorld world, 
            net.minecraft.util.math.BlockPos pos) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int cloudLevel = skillData.getInt("ability_creeper_potion_cloud");
        if (cloudLevel > 0 && abilities.has("creeper_potion_cloud")) {
            JsonArray levels = abilities.getAsJsonArray("creeper_potion_cloud");
            if (cloudLevel <= levels.size()) {
                JsonObject levelData = levels.get(cloudLevel - 1).getAsJsonObject();
                
                if (levelData.has("effects")) {
                    JsonArray effectsArray = levelData.getAsJsonArray("effects");
                    
                    // Apply effects to all entities in 5 block radius
                    world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                        new net.minecraft.util.math.Box(pos).expand(5.0),
                        entity -> entity != mob && entity instanceof net.minecraft.entity.player.PlayerEntity)
                        .forEach(entity -> {
                            for (JsonElement effectEl : effectsArray) {
                                JsonObject effect = effectEl.getAsJsonObject();
                                String type = effect.get("type").getAsString();
                                int level = effect.has("level") ? effect.get("level").getAsInt() : 1;
                                int duration = effect.has("duration") ? effect.get("duration").getAsInt() : 10;
                                
                                var statusEffect = getPotionEffectByName(type);
                                if (statusEffect != null) {
                                    entity.addStatusEffect(new StatusEffectInstance(
                                        statusEffect, duration * 20, level - 1, false, true, true));
                                }
                            }
                        });
                }
            }
        }
    }
    
    // ==========================================================================
    //                    ENDER DRAGON SPECIAL ABILITIES
    // ==========================================================================
    
    /**
     * Handle Ender Dragon void bombardment - enhanced dragon fireballs
     * Call this when dragon shoots fireball projectile
     */
    public static void handleVoidBombardment(MobEntity mob, MobWarData data, ServerWorld world,
            net.minecraft.entity.projectile.DragonFireballEntity fireball, long currentTick) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int bombardLevel = skillData.getInt("ability_void_bombardment");
        if (bombardLevel > 0 && abilities.has("void_bombardment")) {
            JsonArray levels = abilities.getAsJsonArray("void_bombardment");
            if (bombardLevel <= levels.size()) {
                JsonObject levelData = levels.get(bombardLevel - 1).getAsJsonObject();
                
                // Store damage and wither data in fireball NBT for use on impact
                NbtCompound fireballData = new NbtCompound();
                if (levelData.has("projectile_damage")) {
                    fireballData.putDouble("void_damage", levelData.get("projectile_damage").getAsDouble());
                }
                if (levelData.has("wither_duration")) {
                    fireballData.putInt("void_wither", levelData.get("wither_duration").getAsInt());
                }
                
                // Note: You'll need to handle this data when the fireball impacts
                // Store in projectile custom data or similar mechanism
            }
        }
    }
    
    /**
     * Apply void bombardment effects on dragon fireball impact
     * Call this when dragon fireball hits target or ground
     */
    public static void applyVoidBombardmentEffects(MobEntity mob, MobWarData data, ServerWorld world,
            net.minecraft.util.math.Vec3d impactPos, net.minecraft.entity.LivingEntity directHit) {
        if (!ModConfig.getInstance().isScalingActive()) return;
        
        JsonObject config = getConfigForMob(mob);
        if (config == null) return;
        
        NbtCompound skillData = data.getSkillData();
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        if (!tree.has("special_abilities")) return;
        JsonObject abilities = tree.getAsJsonObject("special_abilities");
        
        int bombardLevel = skillData.getInt("ability_void_bombardment");
        if (bombardLevel > 0 && abilities.has("void_bombardment")) {
            JsonArray levels = abilities.getAsJsonArray("void_bombardment");
            if (bombardLevel <= levels.size()) {
                JsonObject levelData = levels.get(bombardLevel - 1).getAsJsonObject();
                
                double damage = levelData.has("projectile_damage") ? levelData.get("projectile_damage").getAsDouble() : 6.0;
                double radius = levelData.has("radius") ? levelData.get("radius").getAsDouble() : 3.0;
                int witherDuration = levelData.has("wither_duration") ? levelData.get("wither_duration").getAsInt() : 1;
                
                // Apply damage and wither to all entities in radius
                world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                    net.minecraft.util.math.Box.of(impactPos, radius * 2, radius * 2, radius * 2),
                    entity -> entity != mob)
                    .forEach(entity -> {
                        // Apply extra damage
                        entity.damage(world.getDamageSources().dragonBreath(), (float) damage);
                        
                        // Apply wither
                        entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.WITHER, witherDuration * 20, 0, false, true, true));
                    });
            }
        }
    }
    
    // ==========================================================================
    //                           UTILITY METHODS
    // ==========================================================================
    
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
