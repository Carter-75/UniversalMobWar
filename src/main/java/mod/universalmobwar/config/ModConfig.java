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
 * Configuration file for Universal Mob War mod.
 * Stores default gamerule values and mob exclusions.
 */
@Config(name = "universalmobwar")
public class ModConfig implements ConfigData {

    // ========== GENERAL ==========
    @ConfigEntry.Category("General")
    @ConfigEntry.Gui.Tooltip
    public boolean modEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public boolean ignoreSameSpecies = true;
    @ConfigEntry.Gui.Tooltip
    public boolean targetPlayers = true;
    @ConfigEntry.Gui.Tooltip
    public boolean neutralMobsAlwaysAggressive = false;
    @ConfigEntry.Gui.Tooltip
    public boolean allianceSystemEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public double rangeMultiplier = 1.0;
    @ConfigEntry.Gui.Tooltip
    public boolean debugUpgradeLog = true;
    @ConfigEntry.Gui.Tooltip
    public boolean disableNaturalMobSpawns = false;
    @ConfigEntry.Gui.Tooltip
    public boolean evolutionSystemEnabled = true;
    @ConfigEntry.Category("Mobs")
    @ConfigEntry.Gui.Tooltip
    public List<String> excludedMobs = new ArrayList<>();
    @ConfigEntry.Gui.Tooltip
    public List<String> specialMobs = Arrays.asList("witch", "creeper", "cave_spider");

    // ========== SCALING ==========
    @ConfigEntry.Category("Scaling")
    @ConfigEntry.Gui.Tooltip
    public boolean scalingEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public double dayScalingMultiplier = 1.0;
    @ConfigEntry.Gui.Tooltip
    public double killScalingMultiplier = 1.0;
    @ConfigEntry.Gui.Tooltip
    public int maxTier = 20;
    @ConfigEntry.Gui.Tooltip
    public boolean allowBossScaling = true;
    @ConfigEntry.Gui.Tooltip
    public boolean allowModdedScaling = true;
    @ConfigEntry.Gui.Tooltip
    public boolean restrictEffectsToMobTheme = true;

    // ========== PERFORMANCE ==========
    @ConfigEntry.Category("Performance")
    @ConfigEntry.Gui.Tooltip
    public boolean performanceMode = false;
    @ConfigEntry.Gui.Tooltip
    public int targetingCacheMs = 1500;
    @ConfigEntry.Gui.Tooltip
    public int targetingMaxQueriesPerTick = 50;
    @ConfigEntry.Gui.Tooltip
    public int mobDataSaveDebounceMs = 200;
    @ConfigEntry.Gui.Tooltip
    public boolean enableBatching = true;
    @ConfigEntry.Gui.Tooltip
    public boolean enableAsyncTasks = true;
    @ConfigEntry.Gui.Tooltip
    public int maxParticlesPerConnection = 8;
    @ConfigEntry.Gui.Tooltip
    public int maxDrawnMinionConnections = 15;
    @ConfigEntry.Gui.Tooltip
    public int minFpsForVisuals = 30;
    @ConfigEntry.Gui.Tooltip
    public boolean debugLogging = false;

    // ========== VISUALS ==========
    @ConfigEntry.Category("Visuals")
    @ConfigEntry.Gui.Tooltip
    public boolean showTargetLines = true;
    @ConfigEntry.Gui.Tooltip
    public boolean showHealthBars = true;
    @ConfigEntry.Gui.Tooltip
    public boolean showMobLabels = true;
    @ConfigEntry.Gui.Tooltip
    public boolean showLevelParticles = true;
    @ConfigEntry.Gui.Tooltip
    public boolean disableParticles = false;

    // Upgrade cost arrays (hidden from GUI)
    @ConfigEntry.Gui.Excluded
    public int[] generalUpgradeCosts = {2};
    @ConfigEntry.Gui.Excluded
    public int[] generalPassiveUpgradeCosts = {2, 2, 2};
    @ConfigEntry.Gui.Excluded
    public int[] swordUpgradeCosts = {1, 1, 2, 2, 3, 3, 4, 4, 5};
    @ConfigEntry.Gui.Excluded
    public int[] tridentUpgradeCosts = {3, 3, 3};
    @ConfigEntry.Gui.Excluded
    public int[] bowUpgradeCosts = {2, 2, 2, 3, 3, 3, 3};
    @ConfigEntry.Gui.Excluded
    public int[] armorUpgradeCosts = {2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5};
    @ConfigEntry.Gui.Excluded
    public int[] zombieUpgradeCosts = {3};
    @ConfigEntry.Gui.Excluded
    public int[] projectileUpgradeCosts = {2};
    @ConfigEntry.Gui.Excluded
    public int[] creeperUpgradeCosts = {3};
    @ConfigEntry.Gui.Excluded
    public int[] witchUpgradeCosts = {3};
    @ConfigEntry.Gui.Excluded
    public int[] caveSpiderUpgradeCosts = {3};

    // Item tiers (hidden)
    @ConfigEntry.Gui.Excluded
    public List<String> swordTiers = Arrays.asList("minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:netherite_sword");
    @ConfigEntry.Gui.Excluded
    public List<String> goldSwordTiers = Arrays.asList("minecraft:golden_sword", "minecraft:netherite_sword");
    @ConfigEntry.Gui.Excluded
    public List<String> axeTiers = Arrays.asList("minecraft:wooden_axe", "minecraft:stone_axe", "minecraft:iron_axe", "minecraft:diamond_axe", "minecraft:netherite_axe");
    @ConfigEntry.Gui.Excluded
    public List<String> goldAxeTiers = Arrays.asList("minecraft:golden_axe", "minecraft:netherite_axe");
    @ConfigEntry.Gui.Excluded
    public List<String> helmetTiers = Arrays.asList("minecraft:leather_helmet", "minecraft:chainmail_helmet", "minecraft:iron_helmet", "minecraft:diamond_helmet", "minecraft:netherite_helmet");
    @ConfigEntry.Gui.Excluded
    public List<String> chestTiers = Arrays.asList("minecraft:leather_chestplate", "minecraft:chainmail_chestplate", "minecraft:iron_chestplate", "minecraft:diamond_chestplate", "minecraft:netherite_chestplate");
    @ConfigEntry.Gui.Excluded
    public List<String> legsTiers = Arrays.asList("minecraft:leather_leggings", "minecraft:chainmail_leggings", "minecraft:iron_leggings", "minecraft:diamond_leggings", "minecraft:netherite_leggings");
    @ConfigEntry.Gui.Excluded
    public List<String> bootsTiers = Arrays.asList("minecraft:leather_boots", "minecraft:chainmail_boots", "minecraft:iron_boots", "minecraft:diamond_boots", "minecraft:netherite_boots");

        // Gold armor tiers for piglins/brutes only
        @ConfigEntry.Gui.Excluded
        public List<String> goldHelmetTiers = Arrays.asList("minecraft:golden_helmet", "minecraft:netherite_helmet");
        @ConfigEntry.Gui.Excluded
        public List<String> goldChestTiers = Arrays.asList("minecraft:golden_chestplate", "minecraft:netherite_chestplate");
        @ConfigEntry.Gui.Excluded
        public List<String> goldLegsTiers = Arrays.asList("minecraft:golden_leggings", "minecraft:netherite_leggings");
        @ConfigEntry.Gui.Excluded
        public List<String> goldBootsTiers = Arrays.asList("minecraft:golden_boots", "minecraft:netherite_boots");

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

