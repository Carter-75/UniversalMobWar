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
    // ========== CORE GAMEPLAY ==========
    @ConfigEntry.Category("core")
    @ConfigEntry.Gui.Tooltip
    public boolean modEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public boolean targetPlayers = true;
    @ConfigEntry.Gui.Tooltip
    public boolean ignoreSameSpecies = true;
    @ConfigEntry.Gui.Tooltip
    public boolean neutralMobsAlwaysAggressive = false;
    @ConfigEntry.Gui.Tooltip
    public double rangeMultiplier = 1.0;

    // ========== MOB SCALING SYSTEM ==========
    @ConfigEntry.Category("scaling")
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

    // ========== EVOLUTION & PROGRESSION ==========
    @ConfigEntry.Category("progression")
    @ConfigEntry.Gui.Tooltip
    public boolean evolutionSystemEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public int maxLevel = 100;
    @ConfigEntry.Gui.Tooltip
    public int killsPerLevel = 3;
    @ConfigEntry.Gui.Tooltip
    public boolean giveEquipmentToMobs = true;

    // ========== ALLIANCE SYSTEM ==========
    @ConfigEntry.Category("alliances")
    @ConfigEntry.Gui.Tooltip
    public boolean allianceSystemEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public int allianceDurationTicks = 100;
    @ConfigEntry.Gui.Tooltip
    public int sameSpeciesAllianceDurationTicks = 400;
    @ConfigEntry.Gui.Tooltip
    public double allianceRange = 16.0;
    @ConfigEntry.Gui.Tooltip
    public double allianceBreakChance = 0.3;
    @ConfigEntry.Gui.Tooltip
    public double sameSpeciesAllianceBreakChance = 0.05;

    // ========== VISUAL EFFECTS ==========
    @ConfigEntry.Category("visuals")
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

    // ========== PERFORMANCE & DEBUG ==========
    @ConfigEntry.Category("performance")
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
    public boolean debugUpgradeLog = true;
    @ConfigEntry.Gui.Tooltip
    public boolean debugLogging = false;
    @ConfigEntry.Gui.Tooltip
    public boolean disableNaturalMobSpawns = false;

    // ========== MOB MANAGEMENT ==========
    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip
    public List<String> excludedMobs = new ArrayList<>();
    @ConfigEntry.Gui.Tooltip
    public List<String> specialMobs = Arrays.asList("witch", "creeper", "cave_spider");

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
    public List<String> swordTiers = Arrays.asList("wooden_sword", "stone_sword", "iron_sword", "diamond_sword", "netherite_sword");
    @ConfigEntry.Gui.Excluded
    public List<String> goldSwordTiers = Arrays.asList("golden_sword", "netherite_sword");
    @ConfigEntry.Gui.Excluded
    public List<String> axeTiers = Arrays.asList("wooden_axe", "stone_axe", "iron_axe", "diamond_axe", "netherite_axe");
    @ConfigEntry.Gui.Excluded
    public List<String> goldAxeTiers = Arrays.asList("golden_axe", "netherite_axe");
    @ConfigEntry.Gui.Excluded
    public List<String> helmetTiers = Arrays.asList("leather_helmet", "chainmail_helmet", "iron_helmet", "diamond_helmet", "netherite_helmet");
    @ConfigEntry.Gui.Excluded
    public List<String> chestTiers = Arrays.asList("leather_chestplate", "chainmail_chestplate", "iron_chestplate", "diamond_chestplate", "netherite_chestplate");
    @ConfigEntry.Gui.Excluded
    public List<String> legsTiers = Arrays.asList("leather_leggings", "chainmail_leggings", "iron_leggings", "diamond_leggings", "netherite_leggings");
    @ConfigEntry.Gui.Excluded
    public List<String> bootsTiers = Arrays.asList("leather_boots", "chainmail_boots", "iron_boots", "diamond_boots", "netherite_boots");

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

