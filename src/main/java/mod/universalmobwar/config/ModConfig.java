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
 * All settings for mob evolution, alliances, and battles
 */
@Config(name = "universalmobwar")
public class ModConfig implements ConfigData {

    // ========== GENERAL SETTINGS ==========
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean modEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean evolutionSystemEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allianceSystemEnabled = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean targetPlayers = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean ignoreSameSpecies = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean neutralMobsAlwaysAggressive = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean disableNaturalMobSpawns = false;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public double rangeMultiplier = 1.0;
    
    // ========== MOB EVOLUTION ==========
    @ConfigEntry.Category("evolution")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean scalingEnabled = true;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public double dayScalingMultiplier = 1.0;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public double killScalingMultiplier = 1.0;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowBossScaling = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowModdedScaling = true;
    
    // ========== VISUALS ==========
    @ConfigEntry.Category("visuals")
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showTargetLines = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showHealthBars = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showMobLabels = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean showLevelParticles = true;
    
    @ConfigEntry.Gui.Tooltip(count = 1)
    public boolean disableParticles = false;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 144)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int minFpsForVisuals = 30;
    
    // ========== PERFORMANCE ==========
    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean performanceMode = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableBatching = true;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableAsyncTasks = true;
    
    @ConfigEntry.BoundedDiscrete(min = 100, max = 5000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int targetingCacheMs = 1500;
    
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int targetingMaxQueriesPerTick = 50;
    
    @ConfigEntry.BoundedDiscrete(min = 50, max = 1000)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int mobDataSaveDebounceMs = 200;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    @ConfigEntry.Gui.Tooltip(count = 1)
    public int maxParticlesPerConnection = 8;
    
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    @ConfigEntry.Gui.Tooltip(count = 1)
    public int maxDrawnMinionConnections = 15;
    
    // ========== DEBUG ==========
    @ConfigEntry.Category("debug")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean debugUpgradeLog = false;
    
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean debugLogging = false;
    
    // ========== MOB LISTS ==========
    @ConfigEntry.Category("mobs")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> excludedMobs = new ArrayList<>();
    
    @ConfigEntry.Gui.Excluded
    public List<String> specialMobs = Arrays.asList("witch", "creeper", "cave_spider");

    // ========== EQUIPMENT TIERS (Hidden) ==========
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

    // ========== INTERNAL ==========
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
