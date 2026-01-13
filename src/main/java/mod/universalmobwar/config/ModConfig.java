package mod.universalmobwar.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.ConfigData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Universal Mob War Configuration
 * 
 * The mod has 4 independent systems that can be enabled/disabled separately:
 * 1. TARGETING - Mobs fight each other
 * 2. ALLIANCE - Mobs team up against common enemies
 * 3. MOB SCALING - Mobs get stronger over time (progression system)
 * 4. MOB WARLORD - Raid boss that spawns during raids
 * 
 * Each system works independently. Disabling one doesn't affect others.
 */
@Config(name = "universalmobwar")
public class ModConfig implements ConfigData {

    // ==========================================================================
    //                              MASTER CONTROL
    // ==========================================================================
    
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean modEnabled = true;

    // ==========================================================================
    //                         SECTION 1: TARGETING SYSTEM
    // Controls whether mobs fight each other
    // ==========================================================================
    
    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean targetingEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean targetPlayers = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean ignoreSameSpecies = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean neutralMobsAlwaysAggressive = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean disableNaturalMobSpawns = false;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 5000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int rangeMultiplierPercent = 100; // 100 = 1.0x, 200 = 2.0x, etc.

    // ==========================================================================
    //                         SECTION 2: ALLIANCE SYSTEM
    // Controls whether mobs form temporary alliances
    // ==========================================================================
    
    @ConfigEntry.Category("alliance")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allianceEnabled = true;
    
    @ConfigEntry.BoundedDiscrete(min = 1000, max = 60000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int weakAllianceDurationMs = 5000; // Different species
    
    @ConfigEntry.BoundedDiscrete(min = 1000, max = 120000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int strongAllianceDurationMs = 20000; // Same species
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int allianceBreakChancePercent = 30; // Weak alliance break chance
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int strongAllianceBreakChancePercent = 5; // Strong alliance break chance

    // ==========================================================================
    //                       SECTION 3: MOB SCALING SYSTEM
    // Controls mob progression (getting stronger over time)
    // ==========================================================================
    
    @ConfigEntry.Category("scaling")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean scalingEnabled = true;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int dayScalingMultiplierPercent = 100; // 100 = 1.0x
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int killScalingMultiplierPercent = 100; // 100 = 1.0x
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int buyChancePercent = 80; // 80% chance to buy upgrade
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int saveChancePercent = 20; // 20% chance to save points

    public static final int MIN_UPGRADE_ITERATIONS = 10;
    public static final int MAX_UPGRADE_ITERATIONS = 1000;

    @ConfigEntry.BoundedDiscrete(min = MIN_UPGRADE_ITERATIONS, max = MAX_UPGRADE_ITERATIONS)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int maxUpgradeIterations = MAX_UPGRADE_ITERATIONS; // Hard cap per spend cycle
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowBossScaling = true;

    @ConfigEntry.BoundedDiscrete(min = -1, max = 100000)
    @ConfigEntry.Gui.Tooltip(count = 3)
    public int manualWorldDayOverride = -1; // -1 = use actual world time

    @ConfigEntry.Gui.Tooltip(count = 5)
    public boolean allowModdedEnchantments = true;

    // ==========================================================================
    //                       SECTION 4: MOB WARLORD SYSTEM
    // Controls the raid boss feature
    // ==========================================================================
    
    @ConfigEntry.Category("warlord")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean warlordEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 3)
    public boolean alwaysSpawnWarlordOnFinalWave = false;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int warlordSpawnChance = 25;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int warlordMinRaidLevel = 3;
    
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int warlordMinionCount = 20;
    
    @ConfigEntry.BoundedDiscrete(min = 100, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int warlordHealthMultiplierPercent = 300; // 300 = 3.0x
    
    @ConfigEntry.BoundedDiscrete(min = 100, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int warlordDamageMultiplierPercent = 200; // 200 = 2.0x

    // ==========================================================================
    //                            PERFORMANCE
    // Shared performance settings that affect all systems
    // ==========================================================================
    
    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean performanceMode = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableBatching = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableAsyncTasks = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
    @ConfigEntry.Gui.Tooltip(count = 3)
    public int maxConcurrentUpgradeJobs = 32;
    
    @ConfigEntry.BoundedDiscrete(min = 1000, max = 30000)
    @ConfigEntry.Gui.Tooltip(count = 3)
    public int upgradeProcessingTimeMs = 5000; // Default 5 seconds per skilltree.txt
    
    @ConfigEntry.BoundedDiscrete(min = 100, max = 5000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int targetingCacheMs = 1500;
    
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int targetingMaxQueriesPerTick = 50;
    
    @ConfigEntry.BoundedDiscrete(min = 50, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int mobDataSaveDebounceMs = 200;

    // ==========================================================================
    //                              VISUALS
    // ==========================================================================
    
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showTargetLines = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showLevelParticles = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean disableParticles = false;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 144)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int minFpsForVisuals = 30;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    @ConfigEntry.Gui.Tooltip(count = 1)
    public int maxParticlesPerConnection = 8;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 1)
    public int maxDrawnMinionConnections = 15;

    // ==========================================================================
    //                               DEBUG
    // ==========================================================================
    
    @ConfigEntry.Category("debug")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean debugUpgradeLog = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean debugLogging = false;

    // ==========================================================================
    //                             MOB LISTS
    // ==========================================================================
    
    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> excludedMobs = new ArrayList<>();
    
    @ConfigEntry.Gui.Excluded
    public List<String> specialMobs = Arrays.asList("witch", "creeper", "cave_spider");

    // ==========================================================================
    //                        EQUIPMENT TIERS (Hidden)
    // ==========================================================================
    
    @ConfigEntry.Gui.Excluded
    public List<String> swordTiers = Arrays.asList(
        "minecraft:wooden_sword", 
        "minecraft:stone_sword", 
        "minecraft:iron_sword", 
        "minecraft:diamond_sword", 
        "minecraft:netherite_sword"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldSwordTiers = Arrays.asList(
        "minecraft:golden_sword", 
        "minecraft:netherite_sword"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> axeTiers = Arrays.asList(
        "minecraft:wooden_axe", 
        "minecraft:stone_axe", 
        "minecraft:iron_axe", 
        "minecraft:diamond_axe", 
        "minecraft:netherite_axe"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldAxeTiers = Arrays.asList(
        "minecraft:golden_axe", 
        "minecraft:netherite_axe"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> helmetTiers = Arrays.asList(
        "minecraft:leather_helmet", 
        "minecraft:chainmail_helmet", 
        "minecraft:iron_helmet", 
        "minecraft:diamond_helmet", 
        "minecraft:netherite_helmet"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> chestTiers = Arrays.asList(
        "minecraft:leather_chestplate", 
        "minecraft:chainmail_chestplate", 
        "minecraft:iron_chestplate", 
        "minecraft:diamond_chestplate", 
        "minecraft:netherite_chestplate"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> legsTiers = Arrays.asList(
        "minecraft:leather_leggings", 
        "minecraft:chainmail_leggings", 
        "minecraft:iron_leggings", 
        "minecraft:diamond_leggings", 
        "minecraft:netherite_leggings"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> bootsTiers = Arrays.asList(
        "minecraft:leather_boots", 
        "minecraft:chainmail_boots", 
        "minecraft:iron_boots", 
        "minecraft:diamond_boots", 
        "minecraft:netherite_boots"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldHelmetTiers = Arrays.asList(
        "minecraft:golden_helmet", 
        "minecraft:netherite_helmet"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldChestTiers = Arrays.asList(
        "minecraft:golden_chestplate", 
        "minecraft:netherite_chestplate"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldLegsTiers = Arrays.asList(
        "minecraft:golden_leggings", 
        "minecraft:netherite_leggings"
    );
    
    @ConfigEntry.Gui.Excluded
    public List<String> goldBootsTiers = Arrays.asList(
        "minecraft:golden_boots", 
        "minecraft:netherite_boots"
    );

    // ==========================================================================
    //                              INTERNAL
    // ==========================================================================
    
    private static ConfigHolder<ModConfig> holder;

    public static ModConfig getInstance() {
        if (holder == null) {
            holder = AutoConfig.getConfigHolder(ModConfig.class);
        }
        return holder.getConfig();
    }

    public static void save() {
        if (holder != null) holder.save();
    }

    // ==========================================================================
    //                           HELPER METHODS
    // ==========================================================================
    
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
    
    // Convenience getters that convert percent to decimal
    public double getRangeMultiplier() {
        return rangeMultiplierPercent / 100.0;
    }
    
    public double getDayScalingMultiplier() {
        return dayScalingMultiplierPercent / 100.0;
    }
    
    public double getKillScalingMultiplier() {
        return killScalingMultiplierPercent / 100.0;
    }
    
    public double getWarlordHealthMultiplier() {
        return warlordHealthMultiplierPercent / 100.0;
    }
    
    public double getWarlordDamageMultiplier() {
        return warlordDamageMultiplierPercent / 100.0;
    }
    
    public double getBuyChance() {
        return buyChancePercent / 100.0;
    }
    
    public double getSaveChance() {
        return saveChancePercent / 100.0;
    }

    public int getMaxUpgradeIterations() {
        return Math.max(MIN_UPGRADE_ITERATIONS, Math.min(MAX_UPGRADE_ITERATIONS, maxUpgradeIterations));
    }

    public int getMaxConcurrentUpgradeJobs() {
        return Math.max(1, Math.min(256, maxConcurrentUpgradeJobs));
    }
    
    // Section enable checks
    public boolean isTargetingActive() {
        return modEnabled && targetingEnabled;
    }
    
    public boolean isAllianceActive() {
        return modEnabled && allianceEnabled;
    }
    
    public boolean isScalingActive() {
        return modEnabled && scalingEnabled;
    }
    
    public boolean isWarlordActive() {
        return modEnabled && warlordEnabled;
    }
    
    // ==========================================================================
    //                         LEGACY COMPATIBILITY
    // These fields maintain compatibility with older code
    // ==========================================================================
    
    @ConfigEntry.Gui.Excluded
    public transient boolean evolutionSystemEnabled = true; // Maps to scalingEnabled
    
    @ConfigEntry.Gui.Excluded  
    public transient boolean allianceSystemEnabled = true; // Maps to allianceEnabled
    
    @ConfigEntry.Gui.Excluded
    public transient boolean enableMobWarlord = true; // Maps to warlordEnabled
    
    @ConfigEntry.Gui.Excluded
    public transient double rangeMultiplier = 1.0; // Maps to getRangeMultiplier()
    
    @ConfigEntry.Gui.Excluded
    public transient double dayScalingMultiplier = 1.0;
    
    @ConfigEntry.Gui.Excluded
    public transient double killScalingMultiplier = 1.0;
    
    @ConfigEntry.Gui.Excluded
    public transient double warlordHealthMultiplier = 3.0;
    
    @ConfigEntry.Gui.Excluded
    public transient double warlordDamageMultiplier = 2.0;
    
    // Sync legacy fields on load
    @Override
    public void validatePostLoad() throws ValidationException {
        evolutionSystemEnabled = scalingEnabled;
        allianceSystemEnabled = allianceEnabled;
        enableMobWarlord = warlordEnabled;
        rangeMultiplier = getRangeMultiplier();
        dayScalingMultiplier = getDayScalingMultiplier();
        killScalingMultiplier = getKillScalingMultiplier();
        warlordHealthMultiplier = getWarlordHealthMultiplier();
        warlordDamageMultiplier = getWarlordDamageMultiplier();
        manualWorldDayOverride = Math.max(-1, Math.min(100000, manualWorldDayOverride));
        maxUpgradeIterations = getMaxUpgradeIterations();
    }
}
