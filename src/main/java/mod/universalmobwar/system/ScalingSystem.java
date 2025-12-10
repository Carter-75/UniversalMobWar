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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
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
    
    // List of all available mob config files
    private static final String[] IMPLEMENTED_MOBS = {
        "allay", "armadillo", "axolotl", "bat", "bee",
        "blaze", "bogged", "breeze", "camel", "cat",
        "cave_spider", "chicken", "cod", "cow", "creeper",
        "dolphin", "donkey", "drowned", "elder_guardian"
    };
    
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
        
        // Only process upgrades every 100 ticks (5 seconds)
        long currentTick = world.getTime();
        long lastUpdate = data.getSkillData().contains("lastUpdateTick") ? 
            data.getSkillData().getLong("lastUpdateTick") : 0;
        
        boolean shouldProcessUpgrades = (currentTick - lastUpdate >= 100) || lastUpdate == 0;
        
        if (shouldProcessUpgrades) {
            // Update timestamp
            data.getSkillData().putLong("lastUpdateTick", currentTick);
            
            // Spend points on upgrades
            if (budget > 0) {
                spendPoints(mob, data, config, mobType, budget);
            }
            
            // Apply equipment (only when processing upgrades to avoid constant re-equipping)
            if (world instanceof ServerWorld serverWorld) {
                applyEquipment(mob, data, config, serverWorld);
            }
        }
        
        // Apply effects every tick (potion effects need refreshing)
        applyEffects(mob, data, config, mobType, currentTick);
        
        // Save data periodically
        if (shouldProcessUpgrades) {
            MobWarData.save(mob, data);
        }
    }
    
    // ==========================================================================
    //                           POINT CALCULATION
    // ==========================================================================
    
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
    
    // ==========================================================================
    //                           POINT SPENDING
    // ==========================================================================
    
    /**
     * Spend points on upgrades using 80/20 buy/save logic from JSON
     */
    private static void spendPoints(MobEntity mob, MobWarData data, JsonObject config, String mobType, int budget) {
        Random random = new Random(mob.getUuid().hashCode() + data.getSkillData().getLong("lastUpdateTick"));
        
        // Get buy/save chances from config
        double buyChance = 0.80;
        double saveChance = 0.20;
        
        if (config.has("point_system")) {
            JsonObject ps = config.getAsJsonObject("point_system");
            if (ps.has("buy_chance")) buyChance = ps.get("buy_chance").getAsDouble();
            if (ps.has("save_chance")) saveChance = ps.get("save_chance").getAsDouble();
        }
        
        // Get current upgrade levels from skill data
        NbtCompound skillData = data.getSkillData();
        
        // Spending loop
        int iterations = 0;
        while (budget > 0 && iterations < 50) { // Cap iterations to prevent infinite loop
            iterations++;
            
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
        
        // Check special abilities
        if (tree.has("special_abilities")) {
            JsonObject abilities = tree.getAsJsonObject("special_abilities");
            addUpgradesFromSection(abilities, skillData, budget, affordable, "ability_");
        }
        
        // Check weapon upgrades
        if (tree.has("weapon")) {
            JsonObject weapon = tree.getAsJsonObject("weapon");
            
            // Weapon enchants
            if (weapon.has("enchants")) {
                JsonObject enchants = weapon.getAsJsonObject("enchants");
                addUpgradesFromSection(enchants, skillData, budget, affordable, "weapon_enchant_");
            }
            
            // Weapon tiers
            if (weapon.has("tiers")) {
                JsonArray tiers = weapon.getAsJsonArray("tiers");
                int currentTier = skillData.getInt("weapon_tier");
                if (currentTier < tiers.size()) {
                    JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                    int cost = nextTier.get("cost").getAsInt();
                    if (cost <= budget) {
                        affordable.add(new UpgradeOption("weapon_tier", currentTier + 1, cost));
                    }
                }
            }
            
            // Drop mastery
            addMasteryUpgrades(weapon, "drop_mastery", skillData, budget, affordable, "weapon_drop_mastery");
            addMasteryUpgrades(weapon, "durability_mastery", skillData, budget, affordable, "weapon_durability_mastery");
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
        
        for (String key : section.keySet()) {
            JsonElement element = section.get(key);
            if (!element.isJsonArray()) continue;
            
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
                mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, 220, Math.min(resistanceLevel - 1, 1), false, false, true));
                
                // Check for fire resistance at level 3
                if (effects.has("resistance")) {
                    JsonArray resistanceLevels = effects.getAsJsonArray("resistance");
                    if (resistanceLevel <= resistanceLevels.size()) {
                        JsonObject levelData = resistanceLevels.get(resistanceLevel - 1).getAsJsonObject();
                        if (levelData.has("fire_resistance") && levelData.get("fire_resistance").getAsBoolean()) {
                            mob.addStatusEffect(new StatusEffectInstance(
                                StatusEffects.FIRE_RESISTANCE, 220, 0, false, false, true));
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
        
        mob.addStatusEffect(new StatusEffectInstance(effect, 220, Math.max(0, amplifier), false, false, true));
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
            applyWeapon(mob, skillData, tree.getAsJsonObject("weapon"), world);
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
            // Bows don't have material tiers - check if mob has any bow enchants purchased
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
        } else {
            // Sword tiers
            int tier = skillData.getInt("weapon_tier");
            if (tier > 0 && weaponConfig.has("tiers")) {
                JsonArray tiers = weaponConfig.getAsJsonArray("tiers");
                if (tier <= tiers.size()) {
                    JsonObject tierData = tiers.get(tier - 1).getAsJsonObject();
                    String tierName = tierData.get("tier").getAsString();
                    weapon = getWeaponByTier(tierName);
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
     * Get weapon ItemStack by tier name
     */
    private static ItemStack getWeaponByTier(String tier) {
        return switch (tier.toLowerCase()) {
            case "wooden", "wood" -> new ItemStack(Items.WOODEN_SWORD);
            case "stone" -> new ItemStack(Items.STONE_SWORD);
            case "iron" -> new ItemStack(Items.IRON_SWORD);
            case "golden", "gold" -> new ItemStack(Items.GOLDEN_SWORD);
            case "diamond" -> new ItemStack(Items.DIAMOND_SWORD);
            case "netherite" -> new ItemStack(Items.NETHERITE_SWORD);
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
            default -> null;
        };
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
