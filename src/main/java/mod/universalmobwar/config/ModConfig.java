package mod.universalmobwar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mod.universalmobwar.UniversalMobWarMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration file for Universal Mob War mod.
 * Stores default gamerule values and mob exclusions.
 */

public class ModConfig {
    // Global Mob Scaling System (new)
    public boolean scalingEnabled = true;
    public double dayScalingMultiplier = 1.0;
    public double killScalingMultiplier = 1.0;
    public int maxTier = 20;
    public boolean allowBossScaling = true;
    public boolean allowModdedScaling = true;
    public boolean restrictEffectsToMobTheme = true;
    // Debug flag for upgrade logging (new)
    public boolean debugUpgradeLog = false;
    // Disable all natural mob spawns (new)
    public boolean disableNaturalMobSpawns = false;
    public boolean debugLogging = false;

        // Advanced performance tuning
        public int targetingCacheMs = 1500; // Entity targeting cache duration (ms)
        public int targetingMaxQueriesPerTick = 50; // Max targeting queries per tick
        public int mobDataSaveDebounceMs = 200; // Debounce for mob NBT saves
        public boolean enableBatching = true; // Enable batching for alliance/evolution
        public boolean enableAsyncTasks = true; // Use async scheduling for heavy ops
        public int maxParticlesPerConnection = 8; // Particle lines per minion connection
        public int maxDrawnMinionConnections = 15; // Max minion lines per boss
        public int minFpsForVisuals = 30; // Hide some visuals if FPS drops below this
    
    // Default gamerule values
    public boolean modEnabled = true;
    public boolean ignoreSameSpecies = true;
    public boolean targetPlayers = true;
    public boolean neutralMobsAlwaysAggressive = false;
    public boolean allianceSystemEnabled = true;
    public boolean evolutionSystemEnabled = true;
    public double rangeMultiplier = 1.0;
    
    // Evolution system settings
    public int maxLevel = 100;
    public int killsPerLevel = 3;
    public boolean giveEquipmentToMobs = true;
    
    // Alliance system settings (two-tier system)
    public int allianceDurationTicks = 100; // 5 seconds (weak alliances)
    public int sameSpeciesAllianceDurationTicks = 400; // 20 seconds (strong alliances)
    public double allianceRange = 16.0;
    public double allianceBreakChance = 0.3; // 30% chance (weak alliances)
    public double sameSpeciesAllianceBreakChance = 0.05; // 5% chance (strong alliances)
    
    // Mob exclusions (entity type IDs)
    public List<String> excludedMobs = new ArrayList<>();
    
    // Visual settings
    public boolean showTargetLines = true;
    public boolean showHealthBars = true;
    public boolean showMobLabels = true;
    public boolean showLevelParticles = true;
    public boolean disableParticles = false; // New option to disable most particles

    // Performance presets
    public boolean performanceMode = false; // Optimizes settings for low-end PCs

    // Upgrade cost arrays (from skilltree.txt)
    public int[] generalUpgradeCosts = {2};
    public int[] generalPassiveUpgradeCosts = {2, 2, 2};
    public int[] swordUpgradeCosts = {1, 1, 2, 2, 3, 3, 4, 4, 5};
    public int[] tridentUpgradeCosts = {3, 3, 3};
    public int[] bowUpgradeCosts = {2, 2, 2, 3, 3, 3, 3};
    public int[] armorUpgradeCosts = {2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5};
    public int[] zombieUpgradeCosts = {3};
    public int[] projectileUpgradeCosts = {2};
    public int[] creeperUpgradeCosts = {3};
    public int[] witchUpgradeCosts = {3};
    public int[] caveSpiderUpgradeCosts = {3};

    // Item tiers
    public List<String> swordTiers = Arrays.asList("wooden_sword", "stone_sword", "iron_sword", "diamond_sword", "netherite_sword");
    public List<String> goldSwordTiers = Arrays.asList("golden_sword", "netherite_sword");
    public List<String> axeTiers = Arrays.asList("wooden_axe", "stone_axe", "iron_axe", "diamond_axe", "netherite_axe");
    public List<String> goldAxeTiers = Arrays.asList("golden_axe", "netherite_axe");
    public List<String> helmetTiers = Arrays.asList("leather_helmet", "chainmail_helmet", "iron_helmet", "diamond_helmet", "netherite_helmet");
    public List<String> chestTiers = Arrays.asList("leather_chestplate", "chainmail_chestplate", "iron_chestplate", "diamond_chestplate", "netherite_chestplate");
    public List<String> legsTiers = Arrays.asList("leather_leggings", "chainmail_leggings", "iron_leggings", "diamond_leggings", "netherite_leggings");
    public List<String> bootsTiers = Arrays.asList("leather_boots", "chainmail_boots", "iron_boots", "diamond_boots", "netherite_boots");

    private static final String CONFIG_FILE_NAME = "universalmobwar.json";
    private static ModConfig INSTANCE = null;
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }
    
    private static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ModConfig config = gson.fromJson(json, ModConfig.class);
                // If debugUpgradeLog or disableNaturalMobSpawns is missing (old config), default to false
                if (json != null && !json.contains("debugUpgradeLog")) {
                    config.debugUpgradeLog = false;
                }
                if (json != null && !json.contains("disableNaturalMobSpawns")) {
                    config.disableNaturalMobSpawns = false;
                }
                UniversalMobWarMod.LOGGER.info("Loaded configuration from {}", CONFIG_FILE_NAME);
                return config;
            } catch (IOException e) {
                UniversalMobWarMod.LOGGER.error("Failed to load config file, using defaults", e);
            }
        }
        // Create default config
        ModConfig defaultConfig = new ModConfig();
        defaultConfig.save();
        return defaultConfig;
    }
    
    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        try {
            String json = gson.toJson(this);
            Files.writeString(configPath, json);
            UniversalMobWarMod.LOGGER.info("Saved configuration to {}", CONFIG_FILE_NAME);
        } catch (IOException e) {
            UniversalMobWarMod.LOGGER.error("Failed to save config file", e);
        }
    }
    
    public boolean isMobExcluded(String entityTypeId) {
        return excludedMobs.contains(entityTypeId);
    }
    
    public void addExcludedMob(String entityTypeId) {
        if (!excludedMobs.contains(entityTypeId)) {
            excludedMobs.add(entityTypeId);
            save();
        }
    }
    
    public void removeExcludedMob(String entityTypeId) {
        if (excludedMobs.remove(entityTypeId)) {
            save();
        }
    }
}

