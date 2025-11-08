package mod.universalmobwar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mod.universalmobwar.UniversalMobWarMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file for Universal Mob War mod.
 * Stores default gamerule values and mob exclusions.
 */
public class ModConfig {
    
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

