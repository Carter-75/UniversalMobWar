package mod.universalmobwar.system;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
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

    // Map normalized config name -> actual resource file name (preserves case)
    private static final Map<String, String> CONFIG_RESOURCE_NAMES = new ConcurrentHashMap<>();
    
    // Entity class name -> mob config name mapping
    private static final Map<String, String> ENTITY_TO_CONFIG = new ConcurrentHashMap<>();

    // Cached fingerprints per mob config to detect changes without recomputing large JSON hashes every tick
    private static final Map<String, Integer> CONFIG_FINGERPRINTS = new ConcurrentHashMap<>();
    
    // Track cooldowns for special abilities (mobUUID -> ability -> lastUseTick)
    private static final Map<UUID, Map<String, Long>> ABILITY_COOLDOWNS = new ConcurrentHashMap<>();
    private static final String ABILITY_KEY_UNDEAD_PULSE = "undead_harming_pulse";
    private static final String ABILITY_KEY_UNDEAD_BURST = "undead_harming_burst_until";
    private static final long UNDEAD_HARMING_INTERVAL_TICKS = 200L;
    private static final String ABILITY_KEY_INVIS_GLOW_NEXT = "invisibility_on_hit_glow_next";
    private static final String ABILITY_KEY_INVIS_GLOW_UNTIL = "invisibility_on_hit_glow_until";
    private static final long INVIS_GLOW_INTERVAL_TICKS = 15L;
    private static final int INVIS_GLOW_DURATION_TICKS = 8;
    private static final double DEFAULT_DAILY_POINTS = 0.1d;
    private static final DayRange[] DEFAULT_DAILY_SCALING = new DayRange[] {
        new DayRange(1, 10, 0.1d, 0),
        new DayRange(11, 15, 0.5d, 1),
        new DayRange(16, 20, 1.0d, 2),
        new DayRange(21, 25, 1.5d, 3),
        new DayRange(26, 30, 3.0d, 4),
        new DayRange(31, Integer.MAX_VALUE, 5.0d, 5)
    };
    
    private static final Set<String> KNOWN_BOSS_IDS = Set.of(
        "minecraft:ender_dragon",
        "minecraft:wither",
        "minecraft:warden",
        "minecraft:elder_guardian"
    );
    private record DayRange(int minDay, int maxDay, double pointsPerDay, int order) {}
    private static final String NBT_WEAPON_ACTIVE_KEY = "weapon_active_key";
    private static final String NBT_WEAPON_ACTIVE_SCOPED = "weapon_active_scoped";
    private static final String NBT_LAST_UPGRADE_MARKER = "umw_last_upgrade_marker";
    private static final String NBT_LAST_UPGRADE_TICK = "umw_last_upgrade_tick";
    private static final String NBT_UPGRADE_WRAP_STATE = "umw_upgrade_marker_wrapped";
    private static final String NBT_WINDOW_APPROVED = "umw_upgrade_window_approved";
    private static final String NBT_CONFIG_FINGERPRINT = "umw_config_fingerprint";
    private static final String OVERRIDE_KEY_WEAPON = "weapon_player_override";
    private static final String OVERRIDE_KEY_SHIELD = "shield_player_override";
    private static final String NBT_INITIAL_DISARMED = "umw_initial_disarmed";
    private static final String NBT_EQUIPMENT_PRIMED = "umw_equipment_primed";
    private static final String NBT_NEXT_UPGRADE_TICK = "umw_next_upgrade_tick";
    private static final String NBT_UPGRADE_PENDING = "umw_upgrade_pending";
    private static final String NBT_TOTAL_POINT_CACHE = "umw_total_point_cache";
    private static final String NBT_LAST_ACCOUNTED_DAY = "umw_last_accounted_day";
    private static final String NBT_WEAPON_LAST_ITEM = "umw_weapon_last_item";
    private static final String NBT_SHIELD_LAST_ITEM = "umw_shield_last_item";
    private static final String NBT_ARMOR_LAST_ITEM_SUFFIX = "_umw_last_item";
    private static final long DAILY_UPGRADE_INTERVAL_TICKS = 24000L;

    private static final Object UPGRADE_SCHEDULER_LOCK = new Object();
    private static long NEXT_UPGRADE_SLOT_TICK = 0L;
    private static final Map<UUID, EquipmentSnapshot> PENDING_EQUIPMENT_SNAPSHOTS = new ConcurrentHashMap<>();
    
    // List of all available mob config files (loaded dynamically)
    private static String[] IMPLEMENTED_MOBS = null;
        /**
         * Dynamically load all mob config names from the mob_configs resource directory
         */
        private static String[] getImplementedMobs() {
            if (IMPLEMENTED_MOBS != null) return IMPLEMENTED_MOBS;
            java.util.Set<String> normalizedNames = new java.util.LinkedHashSet<>();

            // Primary strategy: leverage Fabric's mod container roots so jar bundles work
            collectConfigsFromModRoots(normalizedNames);

            // Fallback for dev/runtime environments where direct resource access is available
            if (normalizedNames.isEmpty()) {
                collectConfigsFromClasspath(normalizedNames);
            }

            IMPLEMENTED_MOBS = normalizedNames.toArray(new String[0]);
            return IMPLEMENTED_MOBS;
        }

        private static void collectConfigsFromModRoots(Set<String> normalizedNames) {
            Optional<net.fabricmc.loader.api.ModContainer> container = FabricLoader.getInstance().getModContainer(UniversalMobWarMod.MODID);
            if (container.isEmpty()) {
                UniversalMobWarMod.LOGGER.warn("[ScalingSystem] Mod container missing; falling back to classpath scan");
                return;
            }

            container.get().findPath("mob_configs").ifPresentOrElse(
                path -> scanMobConfigDirectory(path, normalizedNames),
                () -> UniversalMobWarMod.LOGGER.warn("[ScalingSystem] No mob_configs directory bundled; falling back to classpath scan")
            );
        }

        private static void scanMobConfigDirectory(Path mobConfigDir, Set<String> normalizedNames) {
            if (!Files.exists(mobConfigDir)) {
                return;
            }

            try (java.util.stream.Stream<Path> stream = Files.walk(mobConfigDir, 1)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> registerMobConfigName(mobConfigDir.relativize(path), normalizedNames));
            } catch (IOException ex) {
                UniversalMobWarMod.LOGGER.error("[ScalingSystem] Failed to inspect {}: {}", mobConfigDir, ex.getMessage());
            }
        }

        private static void collectConfigsFromClasspath(Set<String> normalizedNames) {
            try {
                String resourcePath = "/mob_configs";
                java.net.URL dirURL = ScalingSystem.class.getResource(resourcePath);
                if (dirURL != null && "file".equals(dirURL.getProtocol())) {
                    java.io.File dir = new java.io.File(dirURL.toURI());
                    String[] files = dir.list((d, name) -> name.endsWith(".json"));
                    if (files != null) {
                        for (String fileName : files) {
                            registerMobConfigName(Path.of(fileName), normalizedNames);
                        }
                    }
                } else if (dirURL != null && "jar".equals(dirURL.getProtocol())) {
                    String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String entry = entries.nextElement().getName();
                            if (entry.startsWith("mob_configs/") && entry.endsWith(".json")) {
                                registerMobConfigName(Path.of(entry.substring("mob_configs/".length())), normalizedNames);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                UniversalMobWarMod.LOGGER.error("[ScalingSystem] Failed to load mob config list from classpath: {}", e.getMessage());
            }
        }

        private static void registerMobConfigName(Path relativePath, Set<String> normalizedNames) {
            if (relativePath == null) return;
            String sanitized = relativePath.toString().replace('\\', '/');
            if (!sanitized.endsWith(".json")) return;
            String withoutExt = sanitized.substring(0, sanitized.length() - 5);
            String normalized = withoutExt.toLowerCase(java.util.Locale.ROOT);
            CONFIG_RESOURCE_NAMES.putIfAbsent(normalized, withoutExt);
            normalizedNames.add(normalized);
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
        String normalized = mobName.toLowerCase(java.util.Locale.ROOT);
        String resourceName = CONFIG_RESOURCE_NAMES.getOrDefault(normalized, mobName);
        String path = "/mob_configs/" + resourceName + ".json";
        
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
                CONFIG_FINGERPRINTS.put(configMobName, computeJsonFingerprint(config));
            }
            
        } catch (Exception e) {
            UniversalMobWarMod.LOGGER.error("[ScalingSystem] Failed to load {}: {}", path, e.getMessage());
        }
    }

    private static int computeJsonFingerprint(JsonObject json) {
        if (json == null) {
            return 0;
        }
        return json.toString().hashCode();
    }

    private static int resolveMobConfigHash(JsonObject config) {
        if (config == null) {
            return 0;
        }
        String configName = config.has("mob_name")
            ? config.get("mob_name").getAsString().toLowerCase(java.util.Locale.ROOT)
            : null;
        if (configName == null) {
            return computeJsonFingerprint(config);
        }
        return CONFIG_FINGERPRINTS.computeIfAbsent(configName, key -> computeJsonFingerprint(config));
    }

    private static long computeEffectiveConfigFingerprint(JsonObject config, ModConfig modConfig) {
        int configHash = resolveMobConfigHash(config);
        if (modConfig == null) {
            return ((long) configHash) << 32;
        }
        ModConfigSnapshot snapshot = ModConfigSnapshot.capture(modConfig);
        int modHash = snapshot != null ? snapshot.hashCode() : 0;
            return (((long) configHash) << 32) ^ (modHash & 0xffffffffL);
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
    //                      EQUIPMENT STATE MANAGEMENT
    // ==========================================================================

    /**
     * Ensures MobWarData stays in sync with the mob's actual equipment state.
     * If an item breaks or disappears, downgrade tiers and reset masteries according
     * to the progression spec so the slot becomes eligible for repurchase.
     */
    public static void monitorEquipmentState(MobEntity mob, MobWarData data) {
        if (mob == null || data == null || mob.getWorld().isClient()) {
            return;
        }

        NbtCompound skillData = data.getSkillData();
        if (skillData == null || skillData.isEmpty()) {
            return;
        }

        boolean stateChanged = ensureInitialDisarm(mob, skillData);
        if (!skillData.getBoolean(NBT_EQUIPMENT_PRIMED)) {
            if (stateChanged) {
                MobWarData.save(mob, data);
            }
            return;
        }

        JsonObject config = getConfigForMob(mob);
        if (config == null || !config.has("tree")) {
            return;
        }
        JsonObject tree = config.getAsJsonObject("tree");
        ServerWorld serverWorld = mob.getWorld() instanceof ServerWorld sw ? sw : null;

        JsonElement weaponElement = tree.has("weapon") ? tree.get("weapon") : null;
        JsonObject lockedWeapon = weaponElement != null ? getLockedWeaponForMob(weaponElement, mob) : null;
        boolean scopedWeapon = lockedWeapon != null && hasMultipleWeaponOptions(weaponElement);
        String weaponScopeKey = scopedWeapon ? getWeaponScopeIdentifier(lockedWeapon) : "";

        boolean hasWeaponEquipped = skillData.getBoolean("weapon_equipped");
        boolean shouldHaveWeapon = skillData.getInt("weapon_tier") > 0;
        boolean weaponOverride = isPlayerOverrideActive(skillData, OVERRIDE_KEY_WEAPON);
        ItemStack currentWeapon = mob.getEquippedStack(EquipmentSlot.MAINHAND);
        boolean weaponMissing = currentWeapon.isEmpty();
        boolean weaponMismatch = false;
        if (!weaponMissing && lockedWeapon != null && shouldHaveWeapon) {
            weaponMismatch = !isExpectedWeaponItem(currentWeapon, lockedWeapon, skillData.getInt("weapon_tier"));
        }
        boolean weaponMatches = !weaponMissing && !weaponMismatch && lockedWeapon != null && shouldHaveWeapon;
        if (weaponMatches) {
            if (!hasWeaponEquipped) {
                skillData.putBoolean("weapon_equipped", true);
                hasWeaponEquipped = true;
            }
            if (weaponOverride) {
                setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
                weaponOverride = false;
            }
        }
        if (!weaponMissing && weaponMismatch) {
            if (!weaponOverride) {
                setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, true);
                weaponOverride = true;
            }
            if (!hasWeaponEquipped) {
                skillData.putBoolean("weapon_equipped", true);
                hasWeaponEquipped = true;
            }
        }
        if (weaponMissing && weaponOverride) {
            setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
            weaponOverride = false;
        }
        if (hasWeaponEquipped && weaponMissing) {
            mob.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (handleWeaponBreak(mob, data) && serverWorld != null && lockedWeapon != null) {
                applyWeapon(mob, skillData, lockedWeapon, serverWorld, scopedWeapon, weaponScopeKey, null, false);
            }
        } else if (!hasWeaponEquipped && shouldHaveWeapon && serverWorld != null && lockedWeapon != null && !weaponOverride) {
            applyWeapon(mob, skillData, lockedWeapon, serverWorld, scopedWeapon, weaponScopeKey, null, false);
        }

        boolean hasShieldEquipped = skillData.getBoolean("shield_equipped");
        boolean shouldHaveShield = skillData.getInt("has_shield") > 0;
        boolean shieldOverride = isPlayerOverrideActive(skillData, OVERRIDE_KEY_SHIELD);
        ItemStack currentShield = mob.getEquippedStack(EquipmentSlot.OFFHAND);
        boolean shieldMissing = currentShield.isEmpty();
        boolean shieldMatches = !shieldMissing && currentShield.isOf(Items.SHIELD);
        if (shieldMatches && shouldHaveShield) {
            if (!hasShieldEquipped) {
                skillData.putBoolean("shield_equipped", true);
                hasShieldEquipped = true;
            }
            if (shieldOverride) {
                setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);
                shieldOverride = false;
            }
        } else if (!shieldMissing && !shieldMatches) {
            if (!shieldOverride) {
                setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, true);
                shieldOverride = true;
            }
            if (!hasShieldEquipped) {
                skillData.putBoolean("shield_equipped", true);
                hasShieldEquipped = true;
            }
        }
        if (shieldMissing && shieldOverride) {
            setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);
            shieldOverride = false;
        }
        if (hasShieldEquipped && shieldMissing) {
            if (handleShieldBreak(mob, data) && serverWorld != null && tree.has("shield")) {
                applyShield(mob, skillData, tree.getAsJsonObject("shield"), serverWorld, null, false);
            }
        } else if (!hasShieldEquipped && shouldHaveShield && !shieldMissing && currentShield.isOf(Items.SHIELD)) {
            skillData.putBoolean("shield_equipped", true);
        } else if (!hasShieldEquipped && shouldHaveShield && serverWorld != null && tree.has("shield") && !shieldOverride) {
            applyShield(mob, skillData, tree.getAsJsonObject("shield"), serverWorld, null, false);
        }

        monitorArmorSlot(mob, data, tree, EquipmentSlot.HEAD, "helmet", serverWorld);
        monitorArmorSlot(mob, data, tree, EquipmentSlot.CHEST, "chestplate", serverWorld);
        monitorArmorSlot(mob, data, tree, EquipmentSlot.LEGS, "leggings", serverWorld);
        monitorArmorSlot(mob, data, tree, EquipmentSlot.FEET, "boots", serverWorld);

        if (stateChanged) {
            MobWarData.save(mob, data);
        }
    }

    private static boolean ensureInitialDisarm(MobEntity mob, NbtCompound skillData) {
        if (skillData == null || skillData.getBoolean(NBT_INITIAL_DISARMED)) {
            return false;
        }
        clearMobEquipment(mob);
        resetEquippedFlags(skillData);
        skillData.putBoolean(NBT_INITIAL_DISARMED, true);
        skillData.putBoolean(NBT_EQUIPMENT_PRIMED, false);
        return true;
    }

    private static void clearMobEquipment(MobEntity mob) {
        if (mob == null) {
            return;
        }
        mob.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        mob.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
        mob.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
        mob.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
    }

    private static void abortUpgradesForMob(MobEntity mob, MobWarData data) {
        if (mob == null || data == null) {
            return;
        }
        UUID mobUuid = mob.getUuid();
        UpgradeJobScheduler.getInstance().cancel(mobUuid);
        PENDING_EQUIPMENT_SNAPSHOTS.remove(mobUuid);
        NbtCompound skillData = data.getSkillData();
        if (skillData != null) {
            clearUpgradeSchedule(skillData);
        }
    }

    private static void resetUpgradesForConfigChange(MobEntity mob, MobWarData data, long newFingerprint) {
        if (mob == null || data == null) {
            return;
        }
        UUID mobUuid = mob.getUuid();
        UpgradeJobScheduler.getInstance().cancel(mobUuid);
        PENDING_EQUIPMENT_SNAPSHOTS.remove(mobUuid);
        NbtCompound previousData = data.getSkillData();
        boolean hadPrimedEquipment = previousData != null && previousData.getBoolean(NBT_EQUIPMENT_PRIMED);
        if (previousData != null) {
            clearUpgradeSchedule(previousData);
        }
        if (hadPrimedEquipment) {
            clearMobEquipment(mob);
        }
        NbtCompound resetData = new NbtCompound();
        resetData.putLong(NBT_CONFIG_FINGERPRINT, newFingerprint);
        resetData.putBoolean(NBT_EQUIPMENT_PRIMED, false);
        resetEquippedFlags(resetData);
        data.setSkillData(resetData);
        data.setSpentPoints(0);
        MobWarData.save(mob, data);
    }

    private static void resetEquippedFlags(NbtCompound skillData) {
        if (skillData == null) {
            return;
        }
        skillData.putBoolean("weapon_equipped", false);
        skillData.putBoolean("shield_equipped", false);
        skillData.putBoolean("helmet_equipped", false);
        skillData.putBoolean("chestplate_equipped", false);
        skillData.putBoolean("leggings_equipped", false);
        skillData.putBoolean("boots_equipped", false);
    }

    /**
     * Called during {@link MobEntity#initialize} to bootstrap upgrades directly from spawn logic.
     * This leverages Minecraft's native spawning pipeline so mobs that have never upgraded (or
     * have gone more than a day without upgrading) immediately run a scaling pass instead of
     * waiting for the tick scheduler to catch up later.
     */
    public static void handleSpawnBootstrap(MobEntity mob, SpawnReason spawnReason, MobWarData data) {
        if (mob == null || data == null) {
            return;
        }
        World world = mob.getWorld();
        if (world == null || world.isClient()) {
            return;
        }

        ModConfig modConfig = ModConfig.getInstance();
        if (!modConfig.isScalingActive()) {
            return;
        }

        JsonObject config = getConfigForMob(mob);
        if (config == null) {
            return;
        }

        NbtCompound skillData = data.getSkillData();
        boolean firstCycle = skillData == null || !skillData.contains(NBT_LAST_UPGRADE_MARKER);
        long currentTick = world.getTime();
        boolean cooldownElapsed = hasUpgradeCooldownElapsed(skillData, currentTick);

        if (firstCycle || cooldownElapsed) {
            processMobTick(mob, world, data, true);
        }
    }
    
    // ==========================================================================
    //                           MAIN ENTRY POINT
    // ==========================================================================
    
    /**
     * MAIN ENTRY POINT - Called from MobDataMixin on every mob tick
     * This single method handles ALL scaling logic for ALL mobs
     */
    public static void processMobTick(MobEntity mob, World world, MobWarData data) {
        processMobTick(mob, world, data, false);
    }

    private static void processMobTick(MobEntity mob, World world, MobWarData data, boolean forceImmediateUpgrade) {
        if (world.isClient()) {
            return;
        }

        if (mob == null || data == null || !mob.isAlive() || mob.isRemoved()) {
            abortUpgradesForMob(mob, data);
            return;
        }

        ModConfig modConfig = ModConfig.getInstance();
        if (!modConfig.isScalingActive()) {
            return;
        }

        Identifier entityId = resolveEntityId(mob);
        String entityIdStr = entityId != null ? entityId.toString() : mob.getType().toString();

            if (modConfig.isMobExcluded(entityIdStr)) return;
            if (!modConfig.allowBossScaling && isBossEntity(entityId)) return;

        JsonObject config = getConfigForMob(mob);
        if (config == null) {
            return;
        }

        NbtCompound skillData = data.getSkillData();
        if (skillData == null) {
            skillData = new NbtCompound();
            data.setSkillData(skillData);
        }

        long configFingerprint = computeEffectiveConfigFingerprint(config, modConfig);
        if (skillData.contains(NBT_CONFIG_FINGERPRINT)) {
            long storedFingerprint = skillData.getLong(NBT_CONFIG_FINGERPRINT);
            if (storedFingerprint != configFingerprint) {
                resetUpgradesForConfigChange(mob, data, configFingerprint);
                skillData = data.getSkillData();
                if (skillData == null) {
                    skillData = new NbtCompound();
                    data.setSkillData(skillData);
                }
            }
        } else {
            skillData.putLong(NBT_CONFIG_FINGERPRINT, configFingerprint);
        }
        skillData.putLong(NBT_CONFIG_FINGERPRINT, configFingerprint);

        String mobType = config.has("mob_type") ? config.get("mob_type").getAsString() : "hostile";
        refreshMissingEffects(mob, skillData, config, mobType);

        double dayPoints = calculateWorldAgePoints(world, config, modConfig);
        int killCount = data.getKillCount();
        double killScaling = getKillScalingFactor(config);
        double killPoints = killCount * killScaling * Math.max(0.0, modConfig.getKillScalingMultiplier());

        int worldDays = resolveConfiguredWorldDays(world, modConfig);
        boolean hasLastAccountedDay = skillData.contains(NBT_LAST_ACCOUNTED_DAY);
        boolean hasDayPointCache = skillData.contains(NBT_TOTAL_POINT_CACHE);
        int lastAccountedDay = hasLastAccountedDay ? skillData.getInt(NBT_LAST_ACCOUNTED_DAY) : worldDays;
        boolean skillDataDirty = false;
        double dayPointCache = hasDayPointCache
            ? skillData.getDouble(NBT_TOTAL_POINT_CACHE)
            : Math.max(0.0, data.getSkillPoints() - killPoints);

        if (!hasLastAccountedDay || !hasDayPointCache) {
            double backlogPoints = Math.max(0.0, dayPoints);
            dayPointCache = backlogPoints;
            lastAccountedDay = worldDays;
            skillData.putInt(NBT_LAST_ACCOUNTED_DAY, lastAccountedDay);
            skillData.putDouble(NBT_TOTAL_POINT_CACHE, dayPointCache);
            data.setSkillPoints(Math.max(0.0, dayPointCache + killPoints));
            skillDataDirty = true;
        }

        if (worldDays > lastAccountedDay) {
            double addedPoints = calculateWorldAgePointsForRange(lastAccountedDay + 1, worldDays, config, modConfig);
            if (addedPoints > 0.0) {
                dayPointCache += addedPoints;
                skillData.putDouble(NBT_TOTAL_POINT_CACHE, dayPointCache);
                skillDataDirty = true;
            }
            skillData.putInt(NBT_LAST_ACCOUNTED_DAY, worldDays);
            skillDataDirty = true;
        } else if (worldDays < lastAccountedDay) {
            skillData.putInt(NBT_LAST_ACCOUNTED_DAY, worldDays);
            skillDataDirty = true;
        }

        double totalPoints = Math.max(0.0, dayPointCache + killPoints);
        data.setSkillPoints(totalPoints);

        double spentPoints = data.getSpentPoints();
        int budget = (int)Math.max(0, Math.floor(totalPoints - spentPoints));

        long currentTick = world.getTime();
        int currentTimeOfDay = getCurrentTimeOfDay(world);
        UUID mobUuid = mob.getUuid();
        handleUndeadHealingPulse(mob, skillData, currentTick);
        handleInvisibilityGlowFlicker(mob, currentTick);

        UpgradeJobScheduler scheduler = UpgradeJobScheduler.getInstance();
        boolean asyncEnabled = modConfig.enableAsyncTasks;
        boolean stateChanged = skillDataDirty;
        boolean appliedEquipment = false;

        if (asyncEnabled) {
            UpgradeJobResult completedResult = scheduler.pollResult(mobUuid);
            if (completedResult != null) {
                EquipmentSnapshot snapshot = PENDING_EQUIPMENT_SNAPSHOTS.remove(mobUuid);
                if (snapshot == null) {
                    snapshot = EquipmentSnapshot.capture(mob, skillData);
                }
                applyUpgradeComputation(mob, data, config, mobType, currentTick, currentTimeOfDay, completedResult.computation(), snapshot);
                clearUpgradeSchedule(data.getSkillData());
                skillData = data.getSkillData();
                spentPoints = data.getSpentPoints();
                budget = (int)Math.max(0, Math.floor(totalPoints - spentPoints));
                stateChanged = true;
                appliedEquipment = true;
            }
        }

        boolean firstUpgradeCycle = !skillData.contains(NBT_LAST_UPGRADE_MARKER);
        boolean readyForNextCycle = isUpgradeWindowOpen(skillData, currentTimeOfDay, currentTick);
        boolean shouldRequestUpgrade = firstUpgradeCycle || readyForNextCycle;
        boolean upgradePending = skillData.getBoolean(NBT_UPGRADE_PENDING);
        boolean windowApproved = skillData.getBoolean(NBT_WINDOW_APPROVED);
        boolean upgradeScheduleReady = isUpgradeScheduleReady(skillData, currentTick);

        if (forceImmediateUpgrade && shouldRequestUpgrade && !upgradePending) {
            ModConfigSnapshot configSnapshot = ModConfigSnapshot.capture(modConfig);
            boolean handled = false;
            if (asyncEnabled) {
                handled = submitUpgradeJob(
                    mob,
                    data,
                    config,
                    mobType,
                    skillData,
                    modConfig,
                    configSnapshot,
                    currentTick,
                    budget,
                    totalPoints,
                    spentPoints,
                    killCount,
                    true
                );
            } else {
                handled = executeUpgradeNow(
                    mob,
                    data,
                    config,
                    mobType,
                    currentTick,
                    currentTimeOfDay,
                    budget,
                    totalPoints,
                    spentPoints,
                    configSnapshot,
                    killCount
                );
            }
            if (handled) {
                MobWarData.save(mob, data);
                return;
            }
        }

        if (readyForNextCycle && upgradePending && !windowApproved) {
            skillData.putBoolean(NBT_WINDOW_APPROVED, true);
            windowApproved = true;
        }

        if (shouldRequestUpgrade && !upgradePending) {
            boolean approveNow = readyForNextCycle || firstUpgradeCycle;
            scheduleUpgradePass(skillData, currentTick, modConfig, approveNow);
            upgradePending = true;
            windowApproved = approveNow;
            MobWarData.save(mob, data);
        }

        boolean canSyncEquipment = !upgradeScheduleReady
            && !skillData.getBoolean(NBT_UPGRADE_PENDING)
            && skillData.getBoolean(NBT_EQUIPMENT_PRIMED);
        boolean needsEquipmentSync = canSyncEquipment && requiresEquipmentSync(mob, skillData);

        if (upgradeScheduleReady && !readyForNextCycle && !windowApproved) {
            long windowDelayTicks = Math.max(1L, computeTicksUntilUpgradeWindow(skillData, currentTimeOfDay, currentTick));
            skillData.putBoolean(NBT_WINDOW_APPROVED, false);
            deferUpgradePass(skillData, currentTick, windowDelayTicks);
            upgradeScheduleReady = false;
        }

        if (upgradeScheduleReady) {
            if (asyncEnabled) {
                boolean jobActive = scheduler.isJobActive(mobUuid) || PENDING_EQUIPMENT_SNAPSHOTS.containsKey(mobUuid);
                if (!jobActive) {
                    ModConfigSnapshot configSnapshot = ModConfigSnapshot.capture(modConfig);
                    boolean started = submitUpgradeJob(
                        mob,
                        data,
                        config,
                        mobType,
                        skillData,
                        modConfig,
                        configSnapshot,
                        currentTick,
                        budget,
                        totalPoints,
                        spentPoints,
                        killCount,
                        false
                    );
                    if (!started) {
                        int maxConcurrentJobs = Math.max(1, modConfig.getMaxConcurrentUpgradeJobs());
                        int activeJobs = scheduler.getActiveJobCount();
                        long delayTicks = computeConcurrencyBackoffTicks(modConfig, activeJobs, maxConcurrentJobs);
                        deferUpgradePass(skillData, currentTick, delayTicks);
                        upgradeScheduleReady = false;
                    }
                }
            } else {
                ModConfigSnapshot configSnapshot = ModConfigSnapshot.capture(modConfig);
                boolean applied = executeUpgradeNow(
                    mob,
                    data,
                    config,
                    mobType,
                    currentTick,
                    currentTimeOfDay,
                    budget,
                    totalPoints,
                    spentPoints,
                    configSnapshot,
                    killCount
                );
                if (applied) {
                    skillData = data.getSkillData();
                    spentPoints = data.getSpentPoints();
                    budget = (int)Math.max(0, Math.floor(totalPoints - spentPoints));
                    stateChanged = true;
                    appliedEquipment = true;
                }
                upgradeScheduleReady = false;
            }
        } else if (world instanceof ServerWorld serverWorld && needsEquipmentSync) {
            applyEquipment(mob, data, config, serverWorld, null, false);
            appliedEquipment = true;
            stateChanged = true;
        }

        if (stateChanged || appliedEquipment) {
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
        int worldDays = resolveConfiguredWorldDays(world, modConfig);
        return calculateWorldAgePointsThroughDay(worldDays, config, modConfig);
    }

    private static double calculateWorldAgePointsThroughDay(int worldDays, JsonObject config, ModConfig modConfig) {
        if (worldDays <= 0) {
            return 0.0;
        }

        JsonObject pointSystem = getPointSystem(config);
        JsonArray dailyScaling = pointSystem != null && pointSystem.has("daily_scaling")
            ? pointSystem.getAsJsonArray("daily_scaling")
            : null;

        double totalPoints = dailyScaling == null
            ? computeDefaultScalingPoints(worldDays)
            : computeCustomScalingPoints(worldDays, dailyScaling);

        double dayMultiplier = Math.max(0.0, modConfig.getDayScalingMultiplier());
        return totalPoints * dayMultiplier;
    }

    private static double calculateWorldAgePointsForRange(int startDay, int endDay, JsonObject config, ModConfig modConfig) {
        if (endDay < startDay) {
            return 0.0;
        }
        int safeStart = Math.max(1, startDay);
        int safeEnd = Math.max(1, endDay);
        if (safeEnd < safeStart) {
            return 0.0;
        }
        double endTotal = calculateWorldAgePointsThroughDay(safeEnd, config, modConfig);
        double startTotal = calculateWorldAgePointsThroughDay(safeStart - 1, config, modConfig);
        return Math.max(0.0, endTotal - startTotal);
    }

    private static double computeDefaultScalingPoints(int worldDays) {
        double total = 0.0d;
        for (DayRange range : DEFAULT_DAILY_SCALING) {
            if (worldDays < range.minDay()) {
                break;
            }
            total += accumulateSegment(range.minDay(), Math.min(worldDays, range.maxDay()), range.pointsPerDay());
            if (range.maxDay() >= worldDays) {
                break;
            }
        }
        return total;
    }

    private static double computeCustomScalingPoints(int worldDays, JsonArray dailyScaling) {
        if (dailyScaling == null || dailyScaling.isEmpty()) {
            return computeDefaultScalingPoints(worldDays);
        }

        List<DayRange> ranges = new ArrayList<>(dailyScaling.size());
        int order = 0;
        for (JsonElement element : dailyScaling) {
            if (!element.isJsonObject()) {
                continue;
            }
            try {
                JsonObject range = element.getAsJsonObject();
                int minDay = Math.max(1, range.has("days_min") ? range.get("days_min").getAsInt() : 1);
                int maxDayRaw = range.has("days_max") ? range.get("days_max").getAsInt() : -1;
                int maxDay = maxDayRaw < 0 ? Integer.MAX_VALUE : Math.max(minDay, maxDayRaw);
                double pointsPerDay = range.has("points_per_day")
                    ? range.get("points_per_day").getAsDouble()
                    : DEFAULT_DAILY_POINTS;
                ranges.add(new DayRange(minDay, maxDay, pointsPerDay, order++));
            } catch (Exception ignored) {
                // Ignore malformed entries to avoid destabilizing the tick loop
            }
        }

        if (ranges.isEmpty()) {
            return computeDefaultScalingPoints(worldDays);
        }

        ranges.sort(Comparator.comparingInt(DayRange::minDay).thenComparingInt(DayRange::order));

        double total = 0.0d;
        int currentDay = 1;

        for (DayRange range : ranges) {
            if (currentDay > worldDays) {
                break;
            }

            if (range.minDay() > currentDay) {
                int gapEnd = Math.min(worldDays, range.minDay() - 1);
                total += accumulateDefaultSegment(currentDay, gapEnd);
                currentDay = gapEnd + 1;
            }

            int effectiveStart = Math.max(currentDay, range.minDay());
            int effectiveEnd = Math.min(worldDays, range.maxDay());
            if (effectiveEnd >= effectiveStart) {
                total += accumulateSegment(effectiveStart, effectiveEnd, range.pointsPerDay());
                currentDay = effectiveEnd + 1;
            }
        }

        if (currentDay <= worldDays) {
            total += accumulateDefaultSegment(currentDay, worldDays);
        }

        return total;
    }

    private static double accumulateSegment(int startDay, int endDay, double pointsPerDay) {
        if (endDay < startDay) {
            return 0.0d;
        }
        long days = (long) endDay - startDay + 1L;
        return days * pointsPerDay;
    }

    private static double accumulateDefaultSegment(int startDay, int endDay) {
        return accumulateSegment(startDay, endDay, DEFAULT_DAILY_POINTS);
    }

    private static int getCurrentTimeOfDay(World world) {
        if (world == null) {
            return 0;
        }
        long timeOfDay = world.getTimeOfDay();
        return (int) Math.floorMod(timeOfDay, 24000L);
    }

    private static boolean isUpgradeWindowOpen(NbtCompound skillData, int currentTimeOfDay, long currentTick) {
        if (skillData == null || !skillData.contains(NBT_LAST_UPGRADE_MARKER)) {
            return true;
        }

        if (hasUpgradeCooldownElapsed(skillData, currentTick)) {
            skillData.putBoolean(NBT_UPGRADE_WRAP_STATE, true);
            return true;
        }

        int marker = skillData.getInt(NBT_LAST_UPGRADE_MARKER);
        boolean wrapped = skillData.getBoolean(NBT_UPGRADE_WRAP_STATE);
        if (!wrapped && currentTimeOfDay < marker) {
            skillData.putBoolean(NBT_UPGRADE_WRAP_STATE, true);
            wrapped = true;
        }
        return wrapped && currentTimeOfDay >= marker;
    }

    private static boolean hasUpgradeCooldownElapsed(NbtCompound skillData, long currentTick) {
        if (skillData == null) {
            return true;
        }
        if (!skillData.contains(NBT_LAST_UPGRADE_TICK)) {
            return false;
        }
        long lastTick = skillData.getLong(NBT_LAST_UPGRADE_TICK);
        if (lastTick <= 0L || currentTick <= lastTick) {
            return true;
        }
        long elapsed = currentTick - lastTick;
        return elapsed >= DAILY_UPGRADE_INTERVAL_TICKS;
    }

    private static int resolveConfiguredWorldDays(World world, ModConfig modConfig) {
        if (modConfig.manualWorldDayOverride >= 0) {
            return modConfig.manualWorldDayOverride;
        }
        long worldTicks = Math.max(0L, world.getTime());
        return (int) (worldTicks / 24000L);
    }

    private static long getUpgradeProcessingDelayTicks(ModConfig modConfig) {
        if (modConfig == null) {
            return 20L * 5L;
        }
        int ms = Math.max(1000, Math.min(30000, modConfig.upgradeProcessingTimeMs));
        return Math.max(1L, Math.round(ms / 50.0));
    }

    private static void scheduleUpgradePass(NbtCompound skillData, long currentTick, ModConfig modConfig, boolean windowApproved) {
        if (skillData == null) {
            return;
        }
        long delay = getUpgradeProcessingDelayTicks(modConfig);
        long preferred = currentTick + delay;
        boolean bypassDelay = UpgradeJobScheduler.getInstance().isIdle();
        long scheduled = reserveUpgradeSlot(currentTick, preferred, delay, bypassDelay);
        skillData.putBoolean(NBT_UPGRADE_PENDING, true);
        skillData.putBoolean(NBT_WINDOW_APPROVED, windowApproved);
        skillData.putLong(NBT_NEXT_UPGRADE_TICK, scheduled);
    }

    private static void deferUpgradePass(NbtCompound skillData, long currentTick, long delayTicks) {
        if (skillData == null) {
            return;
        }
        long adjustedDelay = Math.max(1L, delayTicks);
        long deferredTick = currentTick + adjustedDelay;
        skillData.putBoolean(NBT_UPGRADE_PENDING, true);
        skillData.putLong(NBT_NEXT_UPGRADE_TICK, deferredTick);
    }

    private static long reserveUpgradeSlot(long currentTick, long preferredTick, long delayTicks, boolean bypassDelay) {
        synchronized (UPGRADE_SCHEDULER_LOCK) {
            long coreFactor = Math.max(1L, Runtime.getRuntime().availableProcessors());
            long spacing = Math.max(1L, delayTicks / coreFactor);
            long scheduled = bypassDelay
                ? Math.max(currentTick, NEXT_UPGRADE_SLOT_TICK)
                : Math.max(preferredTick, NEXT_UPGRADE_SLOT_TICK);
            NEXT_UPGRADE_SLOT_TICK = scheduled + spacing;
            return scheduled;
        }
    }

    private static long computeUpgradeSeed(UUID mobUuid, long currentTick, int budget) {
        long seed = 0L;
        if (mobUuid != null) {
            seed ^= mobUuid.getMostSignificantBits();
            seed = Long.rotateLeft(seed, 13) ^ mobUuid.getLeastSignificantBits();
        }
        seed ^= Long.rotateLeft(currentTick, 7);
        seed ^= ((long) budget << 3);
        return seed;
    }

    private static long computeConcurrencyBackoffTicks(ModConfig modConfig, int activeJobs, int maxConcurrentJobs) {
        long baseDelay = Math.max(20L, getUpgradeProcessingDelayTicks(modConfig));
        if (maxConcurrentJobs <= 0 || activeJobs <= maxConcurrentJobs) {
            return baseDelay;
        }
        long overload = Math.max(1L, activeJobs - maxConcurrentJobs);
        long step = Math.max(5L, baseDelay / 4L);
        return baseDelay + (overload * step);
    }

    private static long computeTicksUntilUpgradeWindow(NbtCompound skillData, int currentTimeOfDay, long currentTick) {
        if (skillData == null || !skillData.contains(NBT_LAST_UPGRADE_MARKER)) {
            return 0L;
        }

        if (!skillData.contains(NBT_LAST_UPGRADE_TICK)) {
            return computeLegacyTicksUntilUpgradeWindow(skillData, currentTimeOfDay);
        }

        long lastTick = skillData.getLong(NBT_LAST_UPGRADE_TICK);
        if (currentTick <= lastTick) {
            return 0L;
        }
        long elapsed = currentTick - lastTick;
        if (elapsed >= DAILY_UPGRADE_INTERVAL_TICKS) {
            return 0L;
        }
        return DAILY_UPGRADE_INTERVAL_TICKS - elapsed;
    }

    private static long computeLegacyTicksUntilUpgradeWindow(NbtCompound skillData, int currentTimeOfDay) {
        int marker = skillData.getInt(NBT_LAST_UPGRADE_MARKER);
        boolean wrapped = skillData.getBoolean(NBT_UPGRADE_WRAP_STATE);
        int ticksPerDay = (int) DAILY_UPGRADE_INTERVAL_TICKS;

        if (!wrapped) {
            int ticksToMidnight = Math.max(0, ticksPerDay - currentTimeOfDay);
            return (long) ticksToMidnight + Math.max(0, marker);
        }

        if (currentTimeOfDay >= marker) {
            return 0L;
        }

        return Math.max(1L, marker - currentTimeOfDay);
    }

    private static boolean isUpgradeScheduleReady(NbtCompound skillData, long currentTick) {
        if (skillData == null || !skillData.getBoolean(NBT_UPGRADE_PENDING)) {
            return false;
        }
        long readyTick = skillData.getLong(NBT_NEXT_UPGRADE_TICK);
        return readyTick > 0L && currentTick >= readyTick;
    }

    private static void clearUpgradeSchedule(NbtCompound skillData) {
        if (skillData == null) {
            return;
        }
        skillData.putBoolean(NBT_UPGRADE_PENDING, false);
        skillData.putBoolean(NBT_WINDOW_APPROVED, false);
        skillData.remove(NBT_NEXT_UPGRADE_TICK);
    }

    private static void lockEquipmentForUpgrade(NbtCompound skillData) {
        if (skillData == null) {
            return;
        }
        resetEquippedFlags(skillData);
        setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
        setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);
        skillData.putBoolean(NBT_EQUIPMENT_PRIMED, false);
    }

    private static boolean submitUpgradeJob(
        MobEntity mob,
        MobWarData data,
        JsonObject config,
        String mobType,
        NbtCompound skillData,
        ModConfig modConfig,
        ModConfigSnapshot configSnapshot,
        long currentTick,
        int budget,
        double totalPoints,
        double spentPoints,
        int killCount,
        boolean bypassConcurrencyLimit
    ) {
        if (mob == null || data == null || config == null || skillData == null || modConfig == null || configSnapshot == null) {
            return false;
        }

        UpgradeJobScheduler scheduler = UpgradeJobScheduler.getInstance();
        if (!bypassConcurrencyLimit) {
            int maxConcurrentJobs = Math.max(1, modConfig.getMaxConcurrentUpgradeJobs());
            if (scheduler.getActiveJobCount() >= maxConcurrentJobs) {
                return false;
            }
        }

        UUID mobUuid = mob.getUuid();
        EquipmentSnapshot snapshot = EquipmentSnapshot.capture(mob, skillData);
        PENDING_EQUIPMENT_SNAPSHOTS.put(mobUuid, snapshot);
        lockEquipmentForUpgrade(skillData);
        long seed = computeUpgradeSeed(mobUuid, currentTick, budget);
        MobUpgradeJob job = new MobUpgradeJob(
            mobUuid,
            config,
            mobType,
            skillData.copy(),
            budget,
            totalPoints,
            spentPoints,
            configSnapshot,
            killCount,
            currentTick,
            seed
        );
        scheduler.submit(mobUuid, job);
        skillData.putBoolean(NBT_UPGRADE_PENDING, true);
        skillData.putBoolean(NBT_WINDOW_APPROVED, true);
        skillData.putLong(NBT_NEXT_UPGRADE_TICK, currentTick);
        return true;
    }

    private static boolean executeUpgradeNow(
        MobEntity mob,
        MobWarData data,
        JsonObject config,
        String mobType,
        long currentTick,
        int currentTimeOfDay,
        int budget,
        double totalPoints,
        double spentPoints,
        ModConfigSnapshot configSnapshot,
        int killCount
    ) {
        if (mob == null || data == null || config == null || configSnapshot == null) {
            return false;
        }

        NbtCompound skillData = data.getSkillData();
        if (skillData == null) {
            skillData = new NbtCompound();
            data.setSkillData(skillData);
        }

        EquipmentSnapshot snapshot = EquipmentSnapshot.capture(mob, skillData);
        lockEquipmentForUpgrade(skillData);
        long seed = computeUpgradeSeed(mob.getUuid(), currentTick, budget);
        UpgradeComputationResult computation = calculateUpgradeResult(
            mob.getUuid(),
            skillData.copy(),
            config,
            mobType,
            budget,
            totalPoints,
            spentPoints,
            configSnapshot,
            seed,
            killCount
        );

        if (computation != null) {
            applyUpgradeComputation(mob, data, config, mobType, currentTick, currentTimeOfDay, computation, snapshot);
            clearUpgradeSchedule(data.getSkillData());
            return true;
        }

        clearUpgradeSchedule(skillData);
        return false;
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

    private static boolean isBossEntity(Identifier entityId) {
        return entityId != null && KNOWN_BOSS_IDS.contains(entityId.toString());
    }
    
    // ==========================================================================
    //                           POINT SPENDING
    // ==========================================================================
    
    /**
     * Calculates upgrade purchases using the buy/save logic described in JSON configs.
     */
    private static UpgradeComputationResult calculateUpgradeResult(
            UUID mobUuid,
            NbtCompound skillData,
            JsonObject config,
            String mobType,
            int budget,
            double totalPoints,
            double spentPoints,
            ModConfigSnapshot configSnapshot,
            long rngSeed,
            int killCount
    ) {
        if (mobUuid == null || skillData == null || config == null || configSnapshot == null) {
            return null;
        }

        Random random = new Random(rngSeed);
        UpgradeLogBuffer logBuffer = new UpgradeLogBuffer(configSnapshot.debugLogging());

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

        double configBuy = configSnapshot.buyChance();
        double configSave = configSnapshot.saveChance();
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

        int iterationCap = Math.max(1, configSnapshot.iterationCap());
        logBuffer.logStart(budget, totalPoints, spentPoints, killCount, buyChance, saveChance);

        int iterations = 0;
        boolean purchasedUpgrade = false;
        String exitReason = "Budget exhausted";
        while (iterations < iterationCap) {
            iterations++;

            List<UpgradeOption> affordable = getAffordableUpgrades(mobUuid, config, mobType, skillData, budget);

            if (affordable.isEmpty()) {
                exitReason = "No affordable upgrades remaining";
                logBuffer.logIteration(iterations, 0.0, affordable.size(), exitReason);
                break;
            }

            double roll = random.nextDouble();
            logBuffer.logIteration(iterations, roll, affordable.size(), null);
            if (roll < saveChance) {
                exitReason = "Save roll triggered";
                logBuffer.log(exitReason + " (carrying " + budget + " points)");
                break;
            }
            if (roll >= saveChance + buyChance) {
                logBuffer.log("Roll outside buy/save window; retrying next iteration");
                continue;
            }

            UpgradeOption chosen = affordable.get(random.nextInt(affordable.size()));
            int previousLevel = skillData.getInt(chosen.key);
            skillData.putInt(chosen.key, chosen.newLevel);
            handleTierPromotion(skillData, chosen.key, previousLevel, chosen.newLevel);
            spentPoints += chosen.cost;
            budget -= chosen.cost;
            purchasedUpgrade = true;
            logBuffer.logPurchase(chosen, budget);
        }

        if (iterations >= iterationCap && "Budget exhausted".equals(exitReason)) {
            exitReason = "Iteration cap reached";
        }

        logBuffer.logCompletion(purchasedUpgrade, budget, exitReason);
        return new UpgradeComputationResult(skillData, spentPoints, purchasedUpgrade, Math.max(budget, 0), logBuffer.entries());
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

    private static void applyUpgradeComputation(
            MobEntity mob,
            MobWarData data,
            JsonObject config,
            String mobType,
            long currentTick,
            int currentTimeOfDay,
            UpgradeComputationResult computation,
            EquipmentSnapshot snapshot) {
        if (mob == null || data == null || config == null || computation == null) {
            return;
        }
        NbtCompound computedData = computation.skillData();
        if (computedData == null) {
            return;
        }

        data.setSkillData(computedData);
        data.setSpentPoints(computation.spentPoints());
        NbtCompound skillData = data.getSkillData();
        if (skillData == null) {
            return;
        }

        skillData.putInt(NBT_LAST_UPGRADE_MARKER, currentTimeOfDay);
        skillData.putLong(NBT_LAST_UPGRADE_TICK, currentTick);
        skillData.putBoolean(NBT_UPGRADE_WRAP_STATE, false);
        skillData.putBoolean(NBT_EQUIPMENT_PRIMED, false);

        UpgradeLogger logger = new UpgradeLogger(mob);
        logger.replay(computation.logEntries());

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            // Force overrides during upgrade passes so new tiers replace previously equipped items
            // instead of being blocked by the player-override guard that protects manual gear swaps.
            applyEquipment(mob, data, config, serverWorld, snapshot, true);
        }

        applyEffects(mob, data, config, mobType, currentTick);

        if (computation.purchasedUpgrade()) {
            spawnUpgradeParticles(mob);
        }
    }
    
    /**
     * Get list of affordable upgrades from the mob's JSON config
     */
    private static List<UpgradeOption> getAffordableUpgrades(UUID mobUuid, JsonObject config, String mobType, 
            NbtCompound skillData, int budget) {
        
        List<UpgradeOption> affordable = new ArrayList<>();
        
        if (!config.has("tree")) return affordable;
        JsonObject tree = config.getAsJsonObject("tree");
        JsonElement weaponElement = tree.has("weapon") ? tree.get("weapon") : null;
        JsonObject lockedWeapon = weaponElement != null ? getLockedWeaponForMob(weaponElement, mobUuid) : null;
        
        // Check potion effects based on mob type
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        
        if (tree.has(effectsKey)) {
            JsonObject effects = tree.getAsJsonObject(effectsKey);
            addUpgradesFromSection(effects, skillData, budget, affordable, "effect_");
        }
        
        // Determine locked weapon type or attack capability (needed for special abilities filtering)
        String attackCapability = null; // "ranged", "melee", or "both"
        
        if (lockedWeapon != null && lockedWeapon.has("weapon_type")) {
            String weaponType = lockedWeapon.get("weapon_type").getAsString();
            attackCapability = isRangedWeaponType(weaponType) ? "ranged" : "melee";
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
        if (weaponElement != null) {
            int currentTier = skillData.getInt("weapon_tier");
            boolean scopedWeapon = hasMultipleWeaponOptions(weaponElement);
            String weaponScopeKey = scopedWeapon && lockedWeapon != null ? getWeaponScopeIdentifier(lockedWeapon) : "";
            String weaponEnchantPrefix = getWeaponEnchantPrefix(scopedWeapon, weaponScopeKey);
            String weaponDropKey = getWeaponScopedKey("weapon_drop_mastery", scopedWeapon, weaponScopeKey);
            String weaponDurabilityKey = getWeaponScopedKey("weapon_durability_mastery", scopedWeapon, weaponScopeKey);
            if (lockedWeapon != null) {
                int baseCost = lockedWeapon.has("base_cost") ? lockedWeapon.get("base_cost").getAsInt() : 0;
                if (currentTier == 0) {
                    if (baseCost <= budget) {
                        affordable.add(new UpgradeOption("weapon_tier", 1, baseCost));
                    }
                } else if (lockedWeapon.has("tiers")) {
                    JsonArray tiers = lockedWeapon.getAsJsonArray("tiers");
                    if (tiers.size() > 0 && currentTier < tiers.size()) {
                        if (meetsTierUpgradePrereqs(skillData, "weapon", lockedWeapon, weaponEnchantPrefix, weaponDropKey, weaponDurabilityKey)) {
                            JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                            int tierCost = nextTier.get("cost").getAsInt();
                            if (tierCost <= budget) {
                                affordable.add(new UpgradeOption("weapon_tier", currentTier + 1, tierCost));
                            }
                        }
                    }
                }
            }
            
            // Only upgrade the locked weapon's enchants and masteries
            
            // Weapon enchants (only after the mob owns the weapon)
            if (lockedWeapon != null && currentTier > 0 && lockedWeapon.has("enchants")) {
                JsonObject enchants = lockedWeapon.getAsJsonObject("enchants");
                addUpgradesFromSection(enchants, skillData, budget, affordable, weaponEnchantPrefix);
            }
            
            // Weapon masteries require an equipped weapon
            if (lockedWeapon != null && currentTier > 0) {
                addMasteryUpgrades(lockedWeapon, "drop_mastery", skillData, budget, affordable, weaponDropKey);
                addMasteryUpgrades(lockedWeapon, "durability_mastery", skillData, budget, affordable, weaponDurabilityKey);
            }
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
            
            if (hasShield > 0) {
                addMasteryUpgrades(shield, "drop_mastery", skillData, budget, affordable, "shield_drop_mastery");
                addMasteryUpgrades(shield, "durability_mastery", skillData, budget, affordable, "shield_durability_mastery");
            }
        }
        
        // Check armor upgrades
        for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots"}) {
            if (tree.has(slot)) {
                JsonObject armor = tree.getAsJsonObject(slot);
                
                // Armor tiers
                int currentTier = skillData.getInt(slot + "_tier");

                if (armor.has("tiers")) {
                    JsonArray tiers = armor.getAsJsonArray("tiers");
                    if (currentTier < tiers.size()) {
                        JsonObject nextTier = tiers.get(currentTier).getAsJsonObject();
                        int cost = nextTier.get("cost").getAsInt();
                        if (cost <= budget && meetsTierUpgradePrereqs(skillData, slot, armor, slot + "_enchant_")) {
                            affordable.add(new UpgradeOption(slot + "_tier", currentTier + 1, cost));
                        }
                    }
                }

                // Armor enchants
                if (currentTier > 0 && armor.has("enchants")) {
                    JsonObject enchants = armor.getAsJsonObject("enchants");
                    addUpgradesFromSection(enchants, skillData, budget, affordable, slot + "_enchant_");
                }
                
                if (currentTier > 0) {
                    addMasteryUpgrades(armor, "drop_mastery", skillData, budget, affordable, slot + "_drop_mastery");
                    addMasteryUpgrades(armor, "durability_mastery", skillData, budget, affordable, slot + "_durability_mastery");
                }
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
            
            // Apply regeneration
            applyPotionEffect(mob, skillData, effects, "regeneration", StatusEffects.REGENERATION, "effect_regeneration", "regen_level");
            
            // Apply health boost
            applyPotionEffect(mob, skillData, effects, "health_boost", StatusEffects.HEALTH_BOOST, "effect_health_boost", null);
            
            // Apply resistance (and optional fire resistance) directly from config
            applyResistanceEffect(mob, skillData, effects);
            
            // Apply strength
            applyPotionEffect(mob, skillData, effects, "strength", StatusEffects.STRENGTH, "effect_strength", "strength_level");
            
            // Apply speed
            applyPotionEffect(mob, skillData, effects, "speed", StatusEffects.SPEED, "effect_speed", "speed_level");
            
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
        
        boolean showParticles = !ModConfig.getInstance().disableParticles;
        mob.addStatusEffect(new StatusEffectInstance(
            effect,
            StatusEffectInstance.INFINITE,
            Math.max(0, amplifier),
            false,
            showParticles,
            true
        ));
    }

    private static void applyResistanceEffect(MobEntity mob, NbtCompound skillData, JsonObject effects) {
        if (skillData == null || effects == null || !effects.has("resistance")) {
            return;
        }
        int resistanceLevel = skillData.getInt("effect_resistance");
        if (resistanceLevel <= 0) {
            return;
        }
        JsonArray resistanceLevels = effects.getAsJsonArray("resistance");
        int resolvedLevel = Math.min(resistanceLevel, resistanceLevels.size());
        if (resolvedLevel <= 0) {
            return;
        }
        JsonObject levelData = resistanceLevels.get(resolvedLevel - 1).getAsJsonObject();
        int amplifier = levelData.has("resistance_level")
            ? Math.max(0, levelData.get("resistance_level").getAsInt() - 1)
            : Math.max(0, resolvedLevel - 1);
        boolean showParticles = !ModConfig.getInstance().disableParticles;
        mob.addStatusEffect(new StatusEffectInstance(
            StatusEffects.RESISTANCE,
            StatusEffectInstance.INFINITE,
            amplifier,
            false,
            showParticles,
            true
        ));
        if (levelData.has("fire_resistance") && levelData.get("fire_resistance").getAsBoolean()) {
            mob.addStatusEffect(new StatusEffectInstance(
                StatusEffects.FIRE_RESISTANCE,
                StatusEffectInstance.INFINITE,
                0,
                false,
                showParticles,
                true
            ));
        }
    }

    private static void ensureResistanceEffects(MobEntity mob, NbtCompound skillData, JsonObject effects) {
        if (mob == null || skillData == null || effects == null || !effects.has("resistance")) {
            return;
        }
        int resistanceLevel = skillData.getInt("effect_resistance");
        if (resistanceLevel <= 0) {
            return;
        }
        JsonArray resistanceLevels = effects.getAsJsonArray("resistance");
        int resolvedLevel = Math.min(resistanceLevel, resistanceLevels.size());
        if (resolvedLevel <= 0) {
            return;
        }
        boolean needsFire = resistanceLevelGrantsFire(resistanceLevels, resolvedLevel);
        boolean missingResistance = !mob.hasStatusEffect(StatusEffects.RESISTANCE);
        boolean missingFire = needsFire && !mob.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
        if (missingResistance || missingFire) {
            applyResistanceEffect(mob, skillData, effects);
        }
    }

    private static boolean resistanceLevelGrantsFire(JsonArray resistanceLevels, int resolvedLevel) {
        if (resistanceLevels == null || resolvedLevel <= 0 || resolvedLevel > resistanceLevels.size()) {
            return false;
        }
        JsonObject levelData = resistanceLevels.get(resolvedLevel - 1).getAsJsonObject();
        return levelData.has("fire_resistance") && levelData.get("fire_resistance").getAsBoolean();
    }

    private static void refreshMissingEffects(MobEntity mob, NbtCompound skillData, JsonObject config, String mobType) {
        if (skillData == null || mob == null || config == null || !config.has("tree")) {
            return;
        }
        JsonObject tree = config.getAsJsonObject("tree");
        String effectsKey = mobType.equals("passive") ? "passive_potion_effects" : "hostile_neutral_potion_effects";
        if (!tree.has(effectsKey)) {
            return;
        }
        JsonObject effects = tree.getAsJsonObject(effectsKey);
        ensureEffectPresent(mob, skillData, effects, "regeneration", StatusEffects.REGENERATION, "effect_regeneration", "regen_level");
        ensureEffectPresent(mob, skillData, effects, "health_boost", StatusEffects.HEALTH_BOOST, "effect_health_boost", null);
        ensureEffectPresent(mob, skillData, effects, "strength", StatusEffects.STRENGTH, "effect_strength", "strength_level");
        ensureEffectPresent(mob, skillData, effects, "speed", StatusEffects.SPEED, "effect_speed", "speed_level");
        ensureResistanceEffects(mob, skillData, effects);
    }

    private static void ensureEffectPresent(MobEntity mob, NbtCompound skillData, JsonObject effects,
            String effectName, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
            String skillKey, String levelKey) {
        if (skillData.getInt(skillKey) <= 0) {
            return;
        }
        if (mob.hasStatusEffect(effect)) {
            return;
        }
        applyPotionEffect(mob, skillData, effects, effectName, effect, skillKey, levelKey);
    }

    private static void handleUndeadHealingPulse(MobEntity mob, NbtCompound skillData, long currentTick) {
        if (mob == null || skillData == null) {
            return;
        }
        if (!isUndeadMob(mob)) {
            return;
        }
        int regenerationLevel = skillData.getInt("effect_regeneration");
        if (regenerationLevel <= 0) {
            return;
        }

        UUID mobUuid = mob.getUuid();
        Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
        long lastPulse = cooldowns.getOrDefault(ABILITY_KEY_UNDEAD_PULSE, Long.MIN_VALUE);
        if (currentTick - lastPulse < UNDEAD_HARMING_INTERVAL_TICKS) {
            return;
        }

        int baseAmplifier = Math.max(0, Math.min(regenerationLevel, 2) - 1);
        applyInstantDamagePulse(mob, baseAmplifier);
        cooldowns.put(ABILITY_KEY_UNDEAD_PULSE, currentTick);

        long burstUntil = cooldowns.getOrDefault(ABILITY_KEY_UNDEAD_BURST, 0L);
        if (burstUntil > 0L && burstUntil < currentTick) {
            cooldowns.remove(ABILITY_KEY_UNDEAD_BURST);
            burstUntil = 0L;
        }

        if (regenerationLevel >= 3 && burstUntil >= currentTick) {
            applyInstantDamagePulse(mob, 2);
        }
    }

    private static void applyInstantDamagePulse(MobEntity mob, int amplifier) {
        if (mob == null) {
            return;
        }
        boolean showParticles = !ModConfig.getInstance().disableParticles;
        mob.addStatusEffect(new StatusEffectInstance(
            StatusEffects.INSTANT_DAMAGE,
            1,
            Math.max(0, amplifier),
            false,
            showParticles,
            true
        ));
    }

    private static boolean isUndeadMob(MobEntity mob) {
        return mob != null && mob.getType().isIn(EntityTypeTags.UNDEAD);
    }

    private static void handleInvisibilityGlowFlicker(MobEntity mob, long currentTick) {
        if (mob == null || mob.getWorld().isClient()) {
            return;
        }
        Map<String, Long> cooldowns = ABILITY_COOLDOWNS.get(mob.getUuid());
        if (cooldowns == null) {
            return;
        }
        long glowUntil = cooldowns.getOrDefault(ABILITY_KEY_INVIS_GLOW_UNTIL, 0L);
        if (glowUntil <= currentTick || !mob.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            cooldowns.remove(ABILITY_KEY_INVIS_GLOW_UNTIL);
            cooldowns.remove(ABILITY_KEY_INVIS_GLOW_NEXT);
            return;
        }
        long nextGlow = cooldowns.getOrDefault(ABILITY_KEY_INVIS_GLOW_NEXT, 0L);
        if (currentTick >= nextGlow) {
            mob.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING,
                INVIS_GLOW_DURATION_TICKS,
                0,
                false,
                true,
                true
            ));
            cooldowns.put(ABILITY_KEY_INVIS_GLOW_NEXT, currentTick + INVIS_GLOW_INTERVAL_TICKS);
        }
    }

    private static void startInvisibilityGlowFlicker(MobEntity mob, Map<String, Long> cooldowns,
            long currentTick, int durationSeconds) {
        if (mob == null || cooldowns == null) {
            return;
        }
        long windowTicks = Math.max(40L, durationSeconds * 20L);
        cooldowns.put(ABILITY_KEY_INVIS_GLOW_UNTIL, currentTick + windowTicks);
        cooldowns.put(ABILITY_KEY_INVIS_GLOW_NEXT, currentTick);
        mob.addStatusEffect(new StatusEffectInstance(
            StatusEffects.GLOWING,
            INVIS_GLOW_DURATION_TICKS,
            0,
            false,
            true,
            true
        ));
    }

    // ==========================================================================
    //                           EQUIPMENT APPLICATION
    // ==========================================================================
    
    /**
     * Apply equipment based on upgrade levels
     */
    private static void applyEquipment(MobEntity mob, MobWarData data, JsonObject config, ServerWorld world,
            EquipmentSnapshot snapshot, boolean forceOverride) {
        NbtCompound skillData = data.getSkillData();
        
        if (!config.has("tree")) return;
        JsonObject tree = config.getAsJsonObject("tree");
        
        logEquipmentDebug(mob, "equipment", "Applying equipment (forceOverride=" + forceOverride + ")");

        // Apply weapon
        if (tree.has("weapon")) {
            JsonElement weaponElement = tree.get("weapon");
            JsonObject weapon = getLockedWeaponForMob(weaponElement, mob);
            if (weapon != null) {
                boolean scopedWeapon = hasMultipleWeaponOptions(weaponElement);
                String weaponKey = scopedWeapon ? getWeaponScopeIdentifier(weapon) : "";
                applyWeapon(mob, skillData, weapon, world, scopedWeapon, weaponKey, snapshot, forceOverride);
            } else {
                logEquipmentDebug(mob, "weapon", "Weapon config missing after locking—skipping equip");
            }
        } else {
            logEquipmentDebug(mob, "weapon", "Mob tree does not define weapon entry");
        }
        
        // Apply shield
        if (tree.has("shield")) {
            applyShield(mob, skillData, tree.getAsJsonObject("shield"), world, snapshot, forceOverride);
        } else {
            logEquipmentDebug(mob, "shield", "Mob tree does not define shield entry");
        }
        
        // Apply armor
        applyArmor(mob, skillData, tree, "helmet", EquipmentSlot.HEAD, world, snapshot, forceOverride);
        applyArmor(mob, skillData, tree, "chestplate", EquipmentSlot.CHEST, world, snapshot, forceOverride);
        applyArmor(mob, skillData, tree, "leggings", EquipmentSlot.LEGS, world, snapshot, forceOverride);
        applyArmor(mob, skillData, tree, "boots", EquipmentSlot.FEET, world, snapshot, forceOverride);

        skillData.putBoolean(NBT_EQUIPMENT_PRIMED, true);
    }
    
    /**
     * Apply weapon with enchants
     */
        private static void applyWeapon(MobEntity mob, NbtCompound skillData, JsonObject weaponConfig,
            ServerWorld world, boolean scopedWeapon, String weaponScopeKey,
            EquipmentSnapshot snapshot, boolean forceOverride) {
        String weaponType = weaponConfig.has("weapon_type") ? weaponConfig.get("weapon_type").getAsString() : "sword";
        int weaponTierLevel = skillData.getInt("weapon_tier");
        if (weaponTierLevel <= 0) {
            logEquipmentDebug(mob, "weapon", "No weapon tier purchased yet—skipping equip");
            skillData.putBoolean("weapon_equipped", false);
            setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
            trackEquippedItem(skillData, NBT_WEAPON_LAST_ITEM, ItemStack.EMPTY);
            return;
        }
        boolean previouslyEquipped = skillData.getBoolean("weapon_equipped");
        String enchantPrefix = getWeaponEnchantPrefix(scopedWeapon, weaponScopeKey);
        String durabilityKey = getWeaponScopedKey("weapon_durability_mastery", scopedWeapon, weaponScopeKey);
        String dropMasteryKey = getWeaponScopedKey("weapon_drop_mastery", scopedWeapon, weaponScopeKey);

        int tierCount = isRangedWeaponType(weaponType) ? weaponTierLevel : getTierCount(weaponConfig);
        if (!isRangedWeaponType(weaponType) && tierCount > 0 && weaponTierLevel > tierCount) {
            logEquipmentDebug(mob, "weapon", "Tier " + weaponTierLevel + " exceeds defined tiers (" + tierCount + "); clamping");
            weaponTierLevel = tierCount;
            skillData.putInt("weapon_tier", weaponTierLevel);
        }

        ItemStack weapon = getExpectedWeaponStack(weaponConfig, weaponTierLevel);

        if (weapon == null || weapon.isEmpty()) {
            logEquipmentDebug(mob, "weapon", "Unable to create item for type " + weaponType + " at tier " + weaponTierLevel);
            return;
        }

        ItemStack current = mob.getEquippedStack(EquipmentSlot.MAINHAND);
        boolean scalingSuppliedCurrent = isScalingEquippedItem(skillData, NBT_WEAPON_LAST_ITEM, current);
        boolean allowOverwrite = forceOverride || scalingSuppliedCurrent || previouslyEquipped;
        if (!current.isEmpty() && !current.isOf(weapon.getItem())) {
            if (allowOverwrite) {
                mob.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                logEquipmentDebug(mob, "weapon", "Cleared previously equipped item to apply new tier");
            } else {
                logEquipmentDebug(mob, "weapon", "Detected mismatched player item; marking override");
                setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, true);
                skillData.putBoolean("weapon_equipped", true);
                trackEquippedItem(skillData, NBT_WEAPON_LAST_ITEM, ItemStack.EMPTY);
                return;
            }
        }

        skillData.putBoolean("weapon_equipped", false);
        setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
        
        // Apply enchants
        if (weaponConfig.has("enchants")) {
            applyEnchantments(weapon, skillData, weaponConfig.getAsJsonObject("enchants"), enchantPrefix, world);
        }
        
        // Apply durability mastery or preserve previous damage if mastery unchanged
        ItemStack previousWeapon = snapshot != null ? snapshot.getMainHand() : ItemStack.EMPTY;
        if (!tryPreserveDurability(weapon, previousWeapon,
                durabilityKey, skillData.getInt(durabilityKey), snapshot)) {
            applyDurabilityMastery(weapon, skillData, weaponConfig, durabilityKey, previousWeapon);
        }
        
        // Equip it
        mob.equipStack(EquipmentSlot.MAINHAND, weapon);
        logEquipmentDebug(mob, "weapon", "Equipped tier " + weaponTierLevel + " item " + weapon.getItem());
        skillData.putBoolean("weapon_equipped", true);
        trackEquippedItem(skillData, NBT_WEAPON_LAST_ITEM, weapon);
        applyDropChance(mob, EquipmentSlot.MAINHAND, skillData.getInt(dropMasteryKey));
        skillData.putString(NBT_WEAPON_ACTIVE_KEY, scopedWeapon ? weaponScopeKey : "");
        skillData.putBoolean(NBT_WEAPON_ACTIVE_SCOPED, scopedWeapon);
    }
    
    /**
     * Apply shield with enchants
     */
    private static void applyShield(MobEntity mob, NbtCompound skillData, JsonObject shieldConfig, ServerWorld world,
            EquipmentSnapshot snapshot, boolean forceOverride) {
        int hasShield = skillData.getInt("has_shield");
        if (hasShield <= 0) {
            logEquipmentDebug(mob, "shield", "Shield not purchased—clearing slot");
            skillData.putBoolean("shield_equipped", false);
            setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);
            trackEquippedItem(skillData, NBT_SHIELD_LAST_ITEM, ItemStack.EMPTY);
            return;
        }

        boolean previouslyEquipped = skillData.getBoolean("shield_equipped");
        ItemStack current = mob.getEquippedStack(EquipmentSlot.OFFHAND);
        boolean scalingSuppliedCurrent = isScalingEquippedItem(skillData, NBT_SHIELD_LAST_ITEM, current);
        boolean allowOverwrite = forceOverride || scalingSuppliedCurrent || previouslyEquipped;
        if (!current.isEmpty() && !current.isOf(Items.SHIELD)) {
            if (allowOverwrite) {
                mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                logEquipmentDebug(mob, "shield", "Cleared previously equipped item to apply purchased shield");
            } else {
                logEquipmentDebug(mob, "shield", "Detected mismatched player item; marking override");
                setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, true);
                skillData.putBoolean("shield_equipped", true);
                trackEquippedItem(skillData, NBT_SHIELD_LAST_ITEM, ItemStack.EMPTY);
                return;
            }
        }

        skillData.putBoolean("shield_equipped", false);
        setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);

        ItemStack shield = new ItemStack(Items.SHIELD);
        
        // Apply enchants
        if (shieldConfig.has("enchants")) {
            applyEnchantments(shield, skillData, shieldConfig.getAsJsonObject("enchants"), "shield_enchant_", world);
        }
        
        // Apply durability mastery or preserve damage
        ItemStack previousShield = snapshot != null ? snapshot.getOffHand() : ItemStack.EMPTY;
        if (!tryPreserveDurability(shield, previousShield,
                "shield_durability_mastery", skillData.getInt("shield_durability_mastery"), snapshot)) {
            applyDurabilityMastery(shield, skillData, shieldConfig, "shield_durability_mastery", previousShield);
        }
        
        // Equip it
        mob.equipStack(EquipmentSlot.OFFHAND, shield);
        logEquipmentDebug(mob, "shield", "Equipped shield with enchants/masteries applied");
        skillData.putBoolean("shield_equipped", true);
        trackEquippedItem(skillData, NBT_SHIELD_LAST_ITEM, shield);
        applyDropChance(mob, EquipmentSlot.OFFHAND, skillData.getInt("shield_drop_mastery"));
    }
    
    /**
     * Apply armor piece with enchants
     */
        private static void applyArmor(MobEntity mob, NbtCompound skillData, JsonObject tree,
            String slotName, EquipmentSlot slot, ServerWorld world,
            EquipmentSnapshot snapshot, boolean forceOverride) {
        
        if (!tree.has(slotName)) {
            logEquipmentDebug(mob, slotName, "Config missing entry for this slot");
            return;
        }
        JsonObject armorConfig = tree.getAsJsonObject(slotName);
        
        int tier = skillData.getInt(slotName + "_tier");
        if (tier <= 0) {
            logEquipmentDebug(mob, slotName, "Tier 0 -> clearing slot");
            skillData.putBoolean(slotName + "_equipped", false);
            setPlayerOverride(skillData, getArmorOverrideKey(slotName), false);
            mob.equipStack(slot, ItemStack.EMPTY);
            trackEquippedItem(skillData, getArmorTrackingKey(slotName), ItemStack.EMPTY);
            return;
        }

        int tierCount = getTierCount(armorConfig);
        if (tierCount <= 0) {
            logEquipmentDebug(mob, slotName, "Tier data missing in config");
            return;
        }
        if (tier > tierCount) {
            logEquipmentDebug(mob, slotName, "Tier " + tier + " exceeds defined tiers (" + tierCount + "); clamping");
            tier = tierCount;
            skillData.putInt(slotName + "_tier", tier);
        }

        ItemStack armor = getExpectedArmorStack(armorConfig, slot, tier);
        if (armor == null || armor.isEmpty()) {
            logEquipmentDebug(mob, slotName, "No ItemStack mapping for configured tier");
            logEquipmentDebug(mob, slotName, "Failed to build armor ItemStack for tier " + tier);
            return;
        }

        ItemStack current = mob.getEquippedStack(slot);
        String trackingKey = getArmorTrackingKey(slotName);
        boolean previouslyEquipped = skillData.getBoolean(slotName + "_equipped");
        boolean scalingSuppliedCurrent = isScalingEquippedItem(skillData, trackingKey, current);
        boolean allowOverwrite = forceOverride || scalingSuppliedCurrent || previouslyEquipped;
        if (!current.isEmpty() && !current.isOf(armor.getItem())) {
            if (allowOverwrite) {
                mob.equipStack(slot, ItemStack.EMPTY);
                logEquipmentDebug(mob, slotName, "Cleared previously equipped item to apply new tier");
            } else {
                logEquipmentDebug(mob, slotName, "Detected mismatched player item; marking override");
                skillData.putBoolean(slotName + "_equipped", true);
                setPlayerOverride(skillData, getArmorOverrideKey(slotName), true);
                trackEquippedItem(skillData, trackingKey, ItemStack.EMPTY);
                return;
            }
        }

        skillData.putBoolean(slotName + "_equipped", false);
        setPlayerOverride(skillData, getArmorOverrideKey(slotName), false);
        
        // Apply enchants
        if (armorConfig.has("enchants")) {
            applyEnchantments(armor, skillData, armorConfig.getAsJsonObject("enchants"), slotName + "_enchant_", world);
        }
        
        String durabilityKey = slotName + "_durability_mastery";
        ItemStack previousArmor = snapshot != null ? snapshot.getArmor(slot) : ItemStack.EMPTY;
        if (!tryPreserveDurability(armor,
                previousArmor,
                durabilityKey,
                skillData.getInt(durabilityKey),
                snapshot)) {
            applyDurabilityMastery(armor, skillData, armorConfig, durabilityKey, previousArmor);
        }
        
        // Equip it
        mob.equipStack(slot, armor);
        logEquipmentDebug(mob, slotName, "Equipped tier " + tier + " item " + armor.getItem());
        skillData.putBoolean(slotName + "_equipped", true);
        trackEquippedItem(skillData, trackingKey, armor);
        applyDropChance(mob, slot, skillData.getInt(slotName + "_drop_mastery"));
    }

    private static boolean requiresEquipmentSync(MobEntity mob, NbtCompound skillData) {
        if (mob == null || skillData == null) {
            return false;
        }
        if (!skillData.getBoolean(NBT_EQUIPMENT_PRIMED)) {
            return false;
        }
        if (skillData.getInt("weapon_tier") > 0 && mob.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
            return true;
        }
        if (skillData.getInt("has_shield") > 0 && mob.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty()) {
            return true;
        }
        if (isArmorSlotMissing(mob, skillData, EquipmentSlot.HEAD, "helmet")) return true;
        if (isArmorSlotMissing(mob, skillData, EquipmentSlot.CHEST, "chestplate")) return true;
        if (isArmorSlotMissing(mob, skillData, EquipmentSlot.LEGS, "leggings")) return true;
        if (isArmorSlotMissing(mob, skillData, EquipmentSlot.FEET, "boots")) return true;
        return false;
    }

    private static boolean isArmorSlotMissing(MobEntity mob, NbtCompound skillData, EquipmentSlot slot, String slotPrefix) {
        return skillData.getInt(slotPrefix + "_tier") > 0 && mob.getEquippedStack(slot).isEmpty();
    }

    private static void logEquipmentDebug(MobEntity mob, String slotLabel, String message) {
        if (mob == null) {
            return;
        }
        ModConfig config = ModConfig.getInstance();
        if (!config.debugLogging) {
            return;
        }
        String mobName = mob.getDisplayName().getString();
        String uuid = mob.getUuid().toString();
        String shortId = uuid.length() > 8 ? uuid.substring(0, 8) : uuid;
        UniversalMobWarMod.LOGGER.info(
            "[ScalingSystem][Equipment] {}#{} [{}] {}",
            mobName,
            shortId,
            slotLabel,
            message
        );
    }

    private static boolean isPlayerOverrideActive(NbtCompound skillData, String key) {
        return skillData != null && key != null && skillData.getBoolean(key);
    }

    private static void setPlayerOverride(NbtCompound skillData, String key, boolean active) {
        if (skillData == null || key == null) {
            return;
        }
        skillData.putBoolean(key, active);
    }

    private static String getArmorOverrideKey(String slotPrefix) {
        return slotPrefix + "_player_override";
    }

    private static String getArmorTrackingKey(String slotPrefix) {
        return slotPrefix + NBT_ARMOR_LAST_ITEM_SUFFIX;
    }

    private static boolean isScalingEquippedItem(NbtCompound skillData, String trackingKey, ItemStack stack) {
        if (skillData == null || trackingKey == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!skillData.contains(trackingKey)) {
            return false;
        }
        String trackedId = skillData.getString(trackingKey);
        if (trackedId == null || trackedId.isEmpty()) {
            return false;
        }
        Identifier stackId = Registries.ITEM.getId(stack.getItem());
        return stackId != null && trackedId.equals(stackId.toString());
    }

    private static void trackEquippedItem(NbtCompound skillData, String trackingKey, ItemStack stack) {
        if (skillData == null || trackingKey == null) {
            return;
        }
        if (stack == null || stack.isEmpty()) {
            skillData.remove(trackingKey);
            return;
        }
        Identifier stackId = Registries.ITEM.getId(stack.getItem());
        if (stackId != null) {
            skillData.putString(trackingKey, stackId.toString());
        }
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
    private static void applyDurabilityMastery(ItemStack item, NbtCompound skillData, JsonObject config,
            String skillKey, ItemStack previousStack) {
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
        if (durabilityPercent <= 0.0) {
            durabilityPercent = getFallbackDurabilityPercent(masteryLevel);
        }
        
        // Set item damage (inverted - lower damage = more durability)
        int maxDurability = item.getMaxDamage();
        if (maxDurability > 0) {
            int targetDurability = (int) Math.round(maxDurability * Math.min(1.0, durabilityPercent));
            int previousDurability = getRemainingDurability(previousStack);
            if (previousDurability >= 0 && previousDurability > targetDurability) {
                targetDurability = Math.min(maxDurability, previousDurability);
            }
            int damageToSet = Math.max(0, Math.min(maxDurability, maxDurability - targetDurability));
            item.setDamage(damageToSet);
        }
    }

    private static int getRemainingDurability(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return -1;
        }
        return stack.getMaxDamage() - stack.getDamage();
    }

    private static boolean tryPreserveDurability(ItemStack target, ItemStack previous,
            String durabilityKey, int currentLevel, EquipmentSnapshot snapshot) {
        if (target == null || previous == null || previous.isEmpty() || snapshot == null) {
            return false;
        }
        if (durabilityKey == null || durabilityKey.isEmpty()) {
            return false;
        }
        if (!previous.isOf(target.getItem())) {
            return false;
        }
        int previousLevel = snapshot.getDurabilityLevel(durabilityKey);
        if (previousLevel != currentLevel) {
            return false;
        }
        if (target.getMaxDamage() <= 0) {
            return false;
        }
        target.setDamage(Math.min(target.getMaxDamage(), Math.max(0, previous.getDamage())));
        return true;
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
    
    private static ItemStack getExpectedWeaponStack(JsonObject weaponConfig, int tierLevel) {
        if (weaponConfig == null || tierLevel <= 0) {
            return ItemStack.EMPTY;
        }
        String weaponType = weaponConfig.has("weapon_type") ? weaponConfig.get("weapon_type").getAsString() : "sword";
        if (isRangedWeaponType(weaponType)) {
            return getRangedWeaponItem(weaponType);
        }
        if (!weaponConfig.has("tiers")) {
            return ItemStack.EMPTY;
        }
        JsonArray tiers = weaponConfig.getAsJsonArray("tiers");
        if (tiers.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int clampedTier = Math.max(1, Math.min(tierLevel, tiers.size()));
        JsonObject tierData = tiers.get(clampedTier - 1).getAsJsonObject();
        if (!tierData.has("tier")) {
            return ItemStack.EMPTY;
        }
        String tierName = tierData.get("tier").getAsString();
        return getWeaponByTier(tierName, weaponType);
    }

    private static ItemStack getRangedWeaponItem(String weaponType) {
        if (weaponType == null) {
            return ItemStack.EMPTY;
        }
        return switch (weaponType.toLowerCase(java.util.Locale.ROOT)) {
            case "bow" -> new ItemStack(Items.BOW);
            case "crossbow" -> new ItemStack(Items.CROSSBOW);
            case "trident" -> new ItemStack(Items.TRIDENT);
            default -> ItemStack.EMPTY;
        };
    }

    private static boolean isExpectedWeaponItem(ItemStack equipped, JsonObject weaponConfig, int tierLevel) {
        if (equipped == null || equipped.isEmpty()) {
            return false;
        }
        ItemStack expected = getExpectedWeaponStack(weaponConfig, tierLevel);
        return !expected.isEmpty() && equipped.isOf(expected.getItem());
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

    private static ItemStack getExpectedArmorStack(JsonObject armorConfig, EquipmentSlot slot, int tierLevel) {
        if (armorConfig == null || slot == null || tierLevel <= 0) {
            return ItemStack.EMPTY;
        }
        if (!armorConfig.has("tiers")) {
            return ItemStack.EMPTY;
        }
        JsonArray tiers = armorConfig.getAsJsonArray("tiers");
        if (tiers.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int clampedTier = Math.max(1, Math.min(tierLevel, tiers.size()));
        JsonObject tierData = tiers.get(clampedTier - 1).getAsJsonObject();
        if (!tierData.has("tier")) {
            return ItemStack.EMPTY;
        }
        String tierName = tierData.get("tier").getAsString();
        return getArmorByTierAndSlot(tierName, slot);
    }

    private static boolean isArmorItemMatchingTier(ItemStack equipped, JsonObject armorConfig, EquipmentSlot slot, int tierLevel) {
        if (equipped == null || equipped.isEmpty() || tierLevel <= 0) {
            return false;
        }
        ItemStack expected = getExpectedArmorStack(armorConfig, slot, tierLevel);
        return !expected.isEmpty() && equipped.isOf(expected.getItem());
    }

    private static int getTierCount(JsonObject config) {
        if (config == null || !config.has("tiers")) {
            return 0;
        }
        JsonArray tiers = config.getAsJsonArray("tiers");
        return Math.max(0, tiers.size());
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
     * Highlights mobs that are in a critical health state so players can spot them easily.
     */
    public static void handleCriticalGlow(MobEntity mob, DamageSource source) {
        if (mob == null || source == null) {
            return;
        }
        if (!ModConfig.getInstance().isScalingActive()) {
            return;
        }
        if (!(mob.getWorld() instanceof ServerWorld)) {
            return;
        }
        float maxHealth = mob.getMaxHealth();
        if (maxHealth <= 0.0f) {
            return;
        }
        float threshold = Math.max(2.0f, maxHealth * 0.25f);
        if (mob.getHealth() > threshold) {
            return;
        }
        if (mob.hasStatusEffect(StatusEffects.GLOWING)) {
            return;
        }
        mob.addStatusEffect(new StatusEffectInstance(
            StatusEffects.GLOWING,
            100,
            0,
            false,
            true,
            true
        ));
    }
    
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
                            StatusEffects.INVISIBILITY,
                            StatusEffectInstance.INFINITE,
                            0,
                            false,
                            false,
                            true
                        ));
                        cooldowns.put("invisibility_on_hit", currentTick);
                        startInvisibilityGlowFlicker(mob, cooldowns, currentTick, duration);
                    }
                }
            }
        }
        
        // Check on_damage_regen (from regeneration ability)
        int regenerationLevel = skillData.getInt("effect_regeneration");
        if (regenerationLevel >= 3 && effects.has("regeneration")) {
            JsonArray levels = effects.getAsJsonArray("regeneration");
            if (regenerationLevel <= levels.size()) {
                JsonObject levelData = levels.get(regenerationLevel - 1).getAsJsonObject();
                
                if (levelData.has("on_damage_regen_level")) {
                    int regenLevel = levelData.get("on_damage_regen_level").getAsInt();
                    int duration = levelData.has("on_damage_duration") ? levelData.get("on_damage_duration").getAsInt() : 10;
                    int cooldown = levelData.has("on_damage_cooldown") ? levelData.get("on_damage_cooldown").getAsInt() : 60;
                    
                    UUID mobUuid = mob.getUuid();
                    Map<String, Long> cooldowns = ABILITY_COOLDOWNS.computeIfAbsent(mobUuid, k -> new HashMap<>());
                    long lastUse = cooldowns.getOrDefault("on_damage_regen", 0L);
                    
                    if (currentTick - lastUse >= cooldown * 20L) {
                        mob.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.REGENERATION,
                            StatusEffectInstance.INFINITE,
                            regenLevel - 1,
                            false,
                            false,
                            true
                        ));
                        cooldowns.put("on_damage_regen", currentTick);
                        if (isUndeadMob(mob)) {
                            long burstWindowTicks = Math.max(20L, duration * 20L);
                            cooldowns.put(ABILITY_KEY_UNDEAD_BURST, currentTick + burstWindowTicks);
                        }
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

        applyCaveSpiderPoisonFromAbilities(skillData, abilities, target);
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
                long cooldownTicks = 1200L;
                long lastUse = cooldowns.getOrDefault("horde_summon", currentTick - cooldownTicks);

                if (currentTick - lastUse >= cooldownTicks) { // 60 seconds cooldown
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
                                reinforcement.initialize(
                                    world,
                                    world.getLocalDifficulty(reinforcement.getBlockPos()),
                                    SpawnReason.EVENT,
                                    null
                                );
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
            case "instant_health", "regeneration" -> StatusEffects.INSTANT_HEALTH;
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

        applyCaveSpiderPoisonFromAbilities(skillData, abilities, target);
    }

    private static void applyCaveSpiderPoisonFromAbilities(NbtCompound skillData, JsonObject abilities, net.minecraft.entity.LivingEntity target) {
        if (skillData == null || abilities == null || target == null) {
            return;
        }

        int poisonLevel = skillData.getInt("ability_poison_mastery");
        if (poisonLevel <= 0 || !abilities.has("poison_mastery")) {
            return;
        }

        JsonArray levels = abilities.getAsJsonArray("poison_mastery");
        if (poisonLevel > levels.size()) {
            return;
        }

        JsonObject levelData = levels.get(poisonLevel - 1).getAsJsonObject();

        int poisonEffectLevel = levelData.has("poison_level") ? levelData.get("poison_level").getAsInt() : 1;
        int duration = levelData.has("duration") ? levelData.get("duration").getAsInt() : 7;

        target.addStatusEffect(new StatusEffectInstance(
            StatusEffects.POISON, duration * 20, poisonEffectLevel - 1, false, true, true));

        if (levelData.has("wither_level")) {
            int witherLevel = levelData.get("wither_level").getAsInt();
            int witherDuration = levelData.has("wither_duration") ? levelData.get("wither_duration").getAsInt() : 10;
            target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WITHER, witherDuration * 20, witherLevel - 1, false, true, true));
        }

        if (levelData.has("slowness_level")) {
            int slownessLevel = levelData.get("slowness_level").getAsInt();
            int slownessDuration = levelData.has("slowness_duration") ? levelData.get("slowness_duration").getAsInt() : 15;
            target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, slownessDuration * 20, slownessLevel - 1, false, true, true));
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
    
    private static String formatUpgradeKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return "Unknown Upgrade";
        }
        String normalized = rawKey
            .replace("effect_", "Effect: ")
            .replace("ability_", "Ability: ")
            .replace("weapon_", "Weapon: ")
            .replace("shield_", "Shield: ")
            .replace("_tier", " Tier")
            .replace('_', ' ');
        String[] parts = normalized.trim().split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.length() > 1 ? part.substring(1) : "");
        }
        return builder.length() > 0 ? builder.toString() : rawKey;
    }

    private static boolean handleWeaponBreak(MobEntity mob, MobWarData data) {
        NbtCompound skillData = data.getSkillData();
        int tier = skillData.getInt("weapon_tier");
        if (tier <= 0) {
            skillData.putBoolean("weapon_equipped", false);
            return false;
        }
        if (tier > 1) {
            skillData.putInt("weapon_tier", tier - 1);
        } else {
            skillData.putInt("weapon_tier", 0);
        }
        boolean scopedWeapon = skillData.getBoolean(NBT_WEAPON_ACTIVE_SCOPED);
        String weaponKey = skillData.getString(NBT_WEAPON_ACTIVE_KEY);
        String enchantPrefix = getWeaponEnchantPrefix(scopedWeapon, weaponKey);
        String dropKey = getWeaponScopedKey("weapon_drop_mastery", scopedWeapon, weaponKey);
        String durabilityKey = getWeaponScopedKey("weapon_durability_mastery", scopedWeapon, weaponKey);
        clearEnchantsWithPrefix(skillData, enchantPrefix);
        resetMasteries(skillData, dropKey, durabilityKey);
        skillData.putBoolean("weapon_equipped", false);
        skillData.putString(NBT_WEAPON_ACTIVE_KEY, "");
        skillData.putBoolean(NBT_WEAPON_ACTIVE_SCOPED, false);
        setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
        MobWarData.save(mob, data);
        return true;
    }

    private static boolean handleShieldBreak(MobEntity mob, MobWarData data) {
        NbtCompound skillData = data.getSkillData();
        if (skillData.getInt("has_shield") <= 0) {
            skillData.putBoolean("shield_equipped", false);
            return false;
        }
        skillData.putInt("has_shield", 0);
        clearEnchantsWithPrefix(skillData, "shield_enchant_");
        resetMasteries(skillData, "shield");
        skillData.putBoolean("shield_equipped", false);
        setPlayerOverride(skillData, OVERRIDE_KEY_SHIELD, false);
        MobWarData.save(mob, data);
        return true;
    }

    private static boolean handleArmorBreak(MobEntity mob, MobWarData data, String slotPrefix) {
        NbtCompound skillData = data.getSkillData();
        String tierKey = slotPrefix + "_tier";
        int tier = skillData.getInt(tierKey);
        if (tier <= 0) {
            skillData.putBoolean(slotPrefix + "_equipped", false);
            return false;
        }
        if (tier > 1) {
            skillData.putInt(tierKey, tier - 1);
        } else {
            skillData.putInt(tierKey, 0);
        }
        clearEnchantsWithPrefix(skillData, slotPrefix + "_enchant_");
        resetMasteries(skillData, slotPrefix);
        skillData.putBoolean(slotPrefix + "_equipped", false);
        setPlayerOverride(skillData, getArmorOverrideKey(slotPrefix), false);
        MobWarData.save(mob, data);
        return true;
    }

    private static void monitorArmorSlot(MobEntity mob, MobWarData data, JsonObject tree, EquipmentSlot slot, String slotPrefix, ServerWorld world) {
        NbtCompound skillData = data.getSkillData();
        int tier = skillData.getInt(slotPrefix + "_tier");
        boolean markedEquipped = skillData.getBoolean(slotPrefix + "_equipped");
        String overrideKey = getArmorOverrideKey(slotPrefix);
        boolean overrideActive = isPlayerOverrideActive(skillData, overrideKey);

        if (tier <= 0) {
            if (markedEquipped) {
                skillData.putBoolean(slotPrefix + "_equipped", false);
            }
            setPlayerOverride(skillData, overrideKey, false);
            if (!mob.getEquippedStack(slot).isEmpty()) {
                mob.equipStack(slot, ItemStack.EMPTY);
            }
            return;
        }

        JsonObject armorConfig = tree.has(slotPrefix) ? tree.getAsJsonObject(slotPrefix) : null;
        if (armorConfig == null) {
            logEquipmentDebug(mob, slotPrefix, "Config missing entry for this slot");
            skillData.putBoolean(slotPrefix + "_equipped", false);
            return;
        }

        ItemStack equipped = mob.getEquippedStack(slot);
        boolean hasItem = !equipped.isEmpty();
        boolean matchesExpected = hasItem && isArmorItemMatchingTier(equipped, armorConfig, slot, tier);

        if (hasItem && matchesExpected) {
            if (!markedEquipped) {
                skillData.putBoolean(slotPrefix + "_equipped", true);
                markedEquipped = true;
            }
            if (overrideActive) {
                setPlayerOverride(skillData, overrideKey, false);
            }
            return;
        }

        if (hasItem && !matchesExpected) {
            if (!overrideActive) {
                setPlayerOverride(skillData, overrideKey, true);
            }
            if (!markedEquipped) {
                skillData.putBoolean(slotPrefix + "_equipped", true);
            }
            return;
        }

        if (!hasItem && overrideActive) {
            setPlayerOverride(skillData, overrideKey, false);
            overrideActive = false;
        }

        if (overrideActive) {
            return;
        }

        if (markedEquipped) {
            if (handleArmorBreak(mob, data, slotPrefix) && world != null) {
                applyArmor(mob, skillData, tree, slotPrefix, slot, world, null, false);
            }
            return;
        }

        if (world != null) {
            applyArmor(mob, skillData, tree, slotPrefix, slot, world, null, false);
        }
    }

    private static boolean hasEquippedItem(MobEntity mob, EquipmentSlot slot) {
        return !mob.getEquippedStack(slot).isEmpty();
    }

    private static void clearEnchantsWithPrefix(NbtCompound skillData, String prefix) {
        if (skillData == null || prefix == null) return;
        java.util.Set<String> keys = new java.util.HashSet<>(skillData.getKeys());
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                skillData.remove(key);
            }
        }
    }

    private static void resetMasteries(NbtCompound skillData, String slotPrefix) {
        resetMasteries(skillData, slotPrefix + "_drop_mastery", slotPrefix + "_durability_mastery");
    }

    private static void resetMasteries(NbtCompound skillData, String dropKey, String durabilityKey) {
        if (dropKey != null && !dropKey.isEmpty()) {
            skillData.putInt(dropKey, 0);
        }
        if (durabilityKey != null && !durabilityKey.isEmpty()) {
            skillData.putInt(durabilityKey, 0);
        }
    }

    private static void applyDropChance(MobEntity mob, EquipmentSlot slot, int masteryLevel) {
        float chance = getDropChanceForMastery(masteryLevel);
        mob.setEquipmentDropChance(slot, chance);
    }

    private static float getDropChanceForMastery(int level) {
        if (level <= 0) return 1.0f;
        if (level >= 10) return 0.01f;
        float reduction = level * 0.1f;
        return Math.max(0.01f, 1.0f - reduction);
    }

    private static double getFallbackDurabilityPercent(int masteryLevel) {
        if (masteryLevel <= 0) {
            return 0.1; // default 10%
        }
        double percent = masteryLevel / 10.0;
        return Math.min(1.0, Math.max(0.1, percent));
    }

    private static boolean meetsTierUpgradePrereqs(NbtCompound skillData, String slotPrefix, JsonObject slotConfig, String enchantPrefix) {
        String dropKey = slotPrefix + "_drop_mastery";
        String durabilityKey = slotPrefix + "_durability_mastery";
        return meetsTierUpgradePrereqs(skillData, slotPrefix, slotConfig, enchantPrefix, dropKey, durabilityKey);
    }

    private static boolean meetsTierUpgradePrereqs(NbtCompound skillData, String slotPrefix, JsonObject slotConfig,
            String enchantPrefix, String dropKey, String durabilityKey) {
        int currentTier = skillData.getInt(slotPrefix + "_tier");
        if (currentTier <= 0) {
            return true; // base acquisition
        }
        if (!hasMasteryAtMax(skillData, dropKey, slotConfig, "drop_mastery")) {
            return false;
        }
        if (!hasMasteryAtMax(skillData, durabilityKey, slotConfig, "durability_mastery")) {
            return false;
        }
        if (slotConfig != null && slotConfig.has("enchants")) {
            JsonObject enchants = slotConfig.getAsJsonObject("enchants");
            for (String key : enchants.keySet()) {
                JsonArray levels = enchants.getAsJsonArray(key);
                if (skillData.getInt(enchantPrefix + key) < levels.size()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasMasteryAtMax(NbtCompound skillData, String skillKey, JsonObject slotConfig, String jsonKey) {
        int requiredLevels = 10;
        if (slotConfig != null && slotConfig.has(jsonKey)) {
            JsonArray levels = slotConfig.getAsJsonArray(jsonKey);
            requiredLevels = Math.max(1, levels.size());
        }
        return skillData.getInt(skillKey) >= requiredLevels;
    }

    private static JsonObject getLockedWeaponForMob(JsonElement weaponElement, MobEntity mob) {
        return getLockedWeaponForMob(weaponElement, mob != null ? mob.getUuid() : null);
    }

    private static JsonObject getLockedWeaponForMob(JsonElement weaponElement, UUID mobUuid) {
        if (weaponElement == null) {
            return null;
        }
        if (weaponElement.isJsonObject()) {
            return weaponElement.getAsJsonObject();
        }
        if (!weaponElement.isJsonArray()) {
            return null;
        }
        JsonArray weapons = weaponElement.getAsJsonArray();
        if (weapons.isEmpty()) {
            return null;
        }
        int index = mobUuid != null ? Math.abs(mobUuid.hashCode()) : 0;
        index = index % weapons.size();
        return weapons.get(index).getAsJsonObject();
    }

    private static boolean hasMultipleWeaponOptions(JsonElement weaponElement) {
        return weaponElement != null && weaponElement.isJsonArray() && weaponElement.getAsJsonArray().size() > 1;
    }

    private static String getWeaponScopeIdentifier(JsonObject weaponConfig) {
        if (weaponConfig == null) {
            return "";
        }
        if (weaponConfig.has("weapon_id")) {
            String explicit = sanitizeKey(weaponConfig.get("weapon_id").getAsString());
            if (!explicit.isEmpty()) {
                return explicit;
            }
        }
        if (weaponConfig.has("weapon_type")) {
            String type = sanitizeKey(weaponConfig.get("weapon_type").getAsString());
            if (!type.isEmpty()) {
                return type;
            }
        }
        String json = GSON.toJson(weaponConfig);
        java.util.UUID derived = java.util.UUID.nameUUIDFromBytes(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return sanitizeKey(derived.toString());
    }

    private static String getWeaponScopedKey(String baseKey, boolean scoped, String weaponKey) {
        if (!scoped) {
            return baseKey;
        }
        String sanitized = sanitizeKey(weaponKey);
        if (sanitized.isEmpty()) {
            return baseKey;
        }
        return baseKey + "_" + sanitized;
    }

    private static String getWeaponEnchantPrefix(boolean scoped, String weaponKey) {
        if (!scoped) {
            return "weapon_enchant_";
        }
        String sanitized = sanitizeKey(weaponKey);
        if (sanitized.isEmpty()) {
            return "weapon_enchant_";
        }
        return "weapon_enchant_" + sanitized + "_";
    }

    private static String sanitizeKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        String normalized = rawKey.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void handleTierPromotion(NbtCompound skillData, String key, int previousLevel, int newLevel) {
        if (skillData == null || newLevel <= previousLevel) {
            return;
        }
        if ("weapon_tier".equals(key)) {
            resetWeaponProgress(skillData);
            return;
        }
        if (key.endsWith("_tier")) {
            String slotPrefix = key.substring(0, key.length() - 5);
            if (isArmorSlotPrefix(slotPrefix)) {
                resetEquipmentProgress(skillData, slotPrefix);
            }
        }
    }

    private static boolean isArmorSlotPrefix(String slotPrefix) {
        return "helmet".equals(slotPrefix) ||
            "chestplate".equals(slotPrefix) ||
            "leggings".equals(slotPrefix) ||
            "boots".equals(slotPrefix);
    }

    private static void resetWeaponProgress(NbtCompound skillData) {
        clearEnchantsWithPrefix(skillData, "weapon_enchant_");
        resetNumericKeysWithPrefix(skillData, "weapon_drop_mastery");
        resetNumericKeysWithPrefix(skillData, "weapon_durability_mastery");
        skillData.putBoolean("weapon_equipped", false);
        skillData.putString(NBT_WEAPON_ACTIVE_KEY, "");
        skillData.putBoolean(NBT_WEAPON_ACTIVE_SCOPED, false);
        setPlayerOverride(skillData, OVERRIDE_KEY_WEAPON, false);
    }

    private static void resetEquipmentProgress(NbtCompound skillData, String slotPrefix) {
        clearEnchantsWithPrefix(skillData, slotPrefix + "_enchant_");
        resetMasteries(skillData, slotPrefix);
        skillData.putBoolean(slotPrefix + "_equipped", false);
        setPlayerOverride(skillData, getArmorOverrideKey(slotPrefix), false);
    }

    private static void resetNumericKeysWithPrefix(NbtCompound skillData, String prefix) {
        if (skillData == null || prefix == null) {
            return;
        }
        Set<String> keys = new HashSet<>(skillData.getKeys());
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                skillData.putInt(key, 0);
            }
        }
    }

    private static record ModConfigSnapshot(
        double buyChance,
        double saveChance,
        int iterationCap,
        boolean debugLogging
    ) {
        static ModConfigSnapshot capture(ModConfig config) {
            if (config == null) {
                return new ModConfigSnapshot(0.80, 0.20, 1000, false);
            }
            double buy = Math.max(0.0, config.getBuyChance());
            double save = Math.max(0.0, config.getSaveChance());
            return new ModConfigSnapshot(
                buy,
                save,
                config.getMaxUpgradeIterations(),
                config.debugUpgradeLog
            );
        }
    }

    static record UpgradeJobResult(
        UUID mobUuid,
        UpgradeComputationResult computation,
        long requestedTick,
        long startedAtNanos,
        long completedAtNanos
    ) {}

    private static final class MobUpgradeJob implements Callable<UpgradeJobResult> {
        private final UUID mobUuid;
        private final JsonObject config;
        private final String mobType;
        private final NbtCompound skillData;
        private final int budget;
        private final double totalPoints;
        private final double spentPoints;
        private final ModConfigSnapshot configSnapshot;
        private final int killCount;
        private final long requestedTick;
        private final long seed;

        private MobUpgradeJob(
                UUID mobUuid,
                JsonObject config,
                String mobType,
                NbtCompound skillData,
                int budget,
                double totalPoints,
                double spentPoints,
                ModConfigSnapshot configSnapshot,
                int killCount,
                long requestedTick,
                long seed) {
            this.mobUuid = mobUuid;
            this.config = config;
            this.mobType = mobType;
            this.skillData = skillData;
            this.budget = budget;
            this.totalPoints = totalPoints;
            this.spentPoints = spentPoints;
            this.configSnapshot = configSnapshot;
            this.killCount = killCount;
            this.requestedTick = requestedTick;
            this.seed = seed;
        }

        @Override
        public UpgradeJobResult call() {
            long started = System.nanoTime();
            UpgradeComputationResult computation = calculateUpgradeResult(
                mobUuid,
                skillData,
                config,
                mobType,
                budget,
                totalPoints,
                spentPoints,
                configSnapshot,
                seed,
                killCount
            );
            if (computation == null) {
                return null;
            }
            long completed = System.nanoTime();
            return new UpgradeJobResult(mobUuid, computation, requestedTick, started, completed);
        }
    }

    private static class UpgradeLogBuffer {
        private final boolean enabled;
        private final List<String> entries;

        UpgradeLogBuffer(boolean enabled) {
            this.enabled = enabled;
            this.entries = enabled ? new ArrayList<>() : Collections.emptyList();
        }

        void logStart(int budget, double totalPoints, double spentPoints, int killCount, double buyChance, double saveChance) {
            if (!enabled) return;
            entries.add(String.format(
                Locale.US,
                "Start: budget=%d total=%.2f spent=%.2f kills=%d buy=%.0f%% save=%.0f%%",
                budget,
                totalPoints,
                spentPoints,
                killCount,
                buyChance * 100.0,
                saveChance * 100.0
            ));
        }

        void logIteration(int iteration, double roll, int affordable, String note) {
            if (!enabled) return;
            if (note != null) {
                entries.add(String.format(Locale.US, "Iteration %d: %s", iteration, note));
            } else {
                entries.add(String.format(Locale.US, "Iteration %d: roll=%.3f options=%d", iteration, roll, affordable));
            }
        }

        void logPurchase(UpgradeOption option, int remainingBudget) {
            if (!enabled) return;
            entries.add(String.format(
                Locale.US,
                "Purchased %s -> level %d (cost=%d, remaining=%d)",
                formatUpgradeKey(option.key),
                option.newLevel,
                option.cost,
                Math.max(remainingBudget, 0)
            ));
        }

        void log(String message) {
            if (!enabled) return;
            entries.add(message);
        }

        void logCompletion(boolean purchased, int remainingBudget, String exitReason) {
            if (!enabled) return;
            String status = purchased ? "completed with purchases" : "completed with no purchases";
            entries.add(String.format(Locale.US, "Pass %s. Exit: %s. Remaining budget=%d", status, exitReason, Math.max(remainingBudget, 0)));
        }

        List<String> entries() {
            return enabled ? entries : Collections.emptyList();
        }
    }

    private static class UpgradeLogger {
        private final boolean enabled;
        private final ServerWorld serverWorld;
        private final String identifier;

        UpgradeLogger(MobEntity mob) {
            ModConfig config = ModConfig.getInstance();
            if (config.debugUpgradeLog && mob.getWorld() instanceof ServerWorld sw) {
                this.enabled = true;
                this.serverWorld = sw;
                String display = mob.getDisplayName().getString();
                String shortId = mob.getUuid().toString().substring(0, 8);
                this.identifier = display + "#" + shortId;
            } else {
                this.enabled = false;
                this.serverWorld = null;
                this.identifier = "";
            }
        }

        void logStart(int budget, double totalPoints, double spentPoints, int killCount, double buyChance, double saveChance) {
            if (!enabled) return;
            log(String.format(
                java.util.Locale.US,
                "Start: budget=%d total=%.2f spent=%.2f kills=%d buy=%.0f%% save=%.0f%%",
                budget,
                totalPoints,
                spentPoints,
                killCount,
                buyChance * 100.0,
                saveChance * 100.0
            ));
        }

        void logIteration(int iteration, double roll, int affordable, String note) {
            if (!enabled) return;
            if (note != null) {
                log(String.format(java.util.Locale.US, "Iteration %d: %s", iteration, note));
            } else {
                log(String.format(java.util.Locale.US, "Iteration %d: roll=%.3f options=%d", iteration, roll, affordable));
            }
        }

        void logPurchase(UpgradeOption option, int remainingBudget) {
            if (!enabled) return;
            log(String.format(
                java.util.Locale.US,
                "Purchased %s -> level %d (cost=%d, remaining=%d)",
                formatUpgradeKey(option.key),
                option.newLevel,
                option.cost,
                Math.max(remainingBudget, 0)
            ));
        }

        void log(String message) {
            if (!enabled || serverWorld == null) return;
            MutableText prefix = Text.literal("[MobWar][" + identifier + "] ")
                .styled(style -> style.withColor(Formatting.DARK_AQUA).withBold(true));
            Text finalText = prefix.append(Text.literal(message).styled(style -> style.withColor(Formatting.WHITE)));
            serverWorld.getServer().getPlayerManager().broadcast(finalText, false);
        }

        void replay(List<String> entries) {
            if (!enabled || entries == null || entries.isEmpty()) {
                return;
            }
            for (String entry : entries) {
                log(entry);
            }
        }

        void logCompletion(boolean purchased, int remainingBudget, String exitReason) {
            if (!enabled) return;
            String status = purchased ? "completed with purchases" : "completed with no purchases";
            log(String.format(java.util.Locale.US, "Pass %s. Exit: %s. Remaining budget=%d", status, exitReason, Math.max(remainingBudget, 0)));
        }
    }

    private static class EquipmentSnapshot {
        private final ItemStack mainHand;
        private final ItemStack offHand;
        private final EnumMap<EquipmentSlot, ItemStack> armor;
        private final Map<String, Integer> durabilityLevels;

        private EquipmentSnapshot(ItemStack mainHand, ItemStack offHand, EnumMap<EquipmentSlot, ItemStack> armor,
                Map<String, Integer> durabilityLevels) {
            this.mainHand = mainHand;
            this.offHand = offHand;
            this.armor = armor;
            this.durabilityLevels = durabilityLevels;
        }

        static EquipmentSnapshot capture(MobEntity mob, NbtCompound skillData) {
            ItemStack main = mob != null ? mob.getEquippedStack(EquipmentSlot.MAINHAND).copy() : ItemStack.EMPTY;
            ItemStack off = mob != null ? mob.getEquippedStack(EquipmentSlot.OFFHAND).copy() : ItemStack.EMPTY;
            EnumMap<EquipmentSlot, ItemStack> armorMap = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                armorMap.put(slot, mob != null ? mob.getEquippedStack(slot).copy() : ItemStack.EMPTY);
            }
            Map<String, Integer> durability = new HashMap<>();
            if (skillData != null) {
                for (String key : skillData.getKeys()) {
                    if (key.endsWith("_durability_mastery")) {
                        durability.put(key, skillData.getInt(key));
                    }
                }
            }
            return new EquipmentSnapshot(main, off, armorMap, durability);
        }

        ItemStack getMainHand() {
            return mainHand == null ? ItemStack.EMPTY : mainHand;
        }

        ItemStack getOffHand() {
            return offHand == null ? ItemStack.EMPTY : offHand;
        }

        ItemStack getArmor(EquipmentSlot slot) {
            if (armor == null || slot == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = armor.get(slot);
            return stack == null ? ItemStack.EMPTY : stack;
        }

        int getDurabilityLevel(String key) {
            if (durabilityLevels == null || key == null) {
                return -1;
            }
            return durabilityLevels.getOrDefault(key, -1);
        }
    }

    private static record UpgradeComputationResult(
        NbtCompound skillData,
        double spentPoints,
        boolean purchasedUpgrade,
        int remainingBudget,
        List<String> logEntries
    ) {}

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
