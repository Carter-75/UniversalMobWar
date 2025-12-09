package mod.universalmobwar.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import mod.universalmobwar.UniversalMobWarMod;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads individual mob configuration from mob_configs/*.json files.
 * Each mob has its OWN complete JSON file with all upgrades, costs, enchants, etc.
 */
public class MobConfig {
    private static final Map<String, MobConfig> LOADED_CONFIGS = new HashMap<>();
    
    // Mob identity
    public final String mobName;
    public final String mobType; // "hostile", "neutral", "passive"
    public final String weapon;
    public final String armor;
    public final boolean shield;
    public final boolean startsWithWeapon;
    public final List<String> assignedTrees;
    
    // Point system (same for all mobs but stored per-mob for convenience)
    public final Map<String, Object> pointSystem;
    
    // Universal upgrades (hostile_and_neutral_potion_effects + all_mobs_item_masteries)
    public final Map<String, List<UpgradeCost>> universalUpgrades;
    
    // Equipment rules
    public final Map<String, Object> equipmentRules;
    
    // Enchant costs (ALL enchants, mob uses relevant ones)
    public final Map<String, Map<String, List<Integer>>> enchantCosts;
    
    // Skill trees specific to this mob
    public final Map<String, Map<String, List<UpgradeCost>>> skillTrees;
    
    public static class UpgradeCost {
        public final int level;
        public final int cost;
        public final Map<String, Object> extra; // effects, chances, etc.
        
        public UpgradeCost(int level, int cost, Map<String, Object> extra) {
            this.level = level;
            this.cost = cost;
            this.extra = extra != null ? extra : new HashMap<>();
        }
    }
    
    private MobConfig(JsonObject json) {
        this.mobName = json.get("mob_name").getAsString();
        this.mobType = json.get("mob_type").getAsString();
        this.weapon = json.get("weapon").getAsString();
        this.armor = json.get("armor").getAsString();
        this.shield = json.get("shield").getAsBoolean();
        this.startsWithWeapon = json.has("starts_with_weapon") ? json.get("starts_with_weapon").getAsBoolean() : false;
        
        // Parse assigned trees
        this.assignedTrees = new ArrayList<>();
        if (json.has("assigned_trees")) {
            JsonArray trees = json.getAsJsonArray("assigned_trees");
            for (JsonElement tree : trees) {
                assignedTrees.add(tree.getAsString());
            }
        }
        
        // Parse point system (as-is for now, can parse deeper if needed)
        this.pointSystem = new HashMap<>();
        if (json.has("point_system")) {
            // Store as raw object for now
            this.pointSystem.put("raw", json.get("point_system"));
        }
        
        // Parse universal upgrades
        this.universalUpgrades = new HashMap<>();
        if (json.has("universal_upgrades")) {
            JsonObject universal = json.getAsJsonObject("universal_upgrades");
            
            // Parse hostile_and_neutral_potion_effects
            if (universal.has("hostile_and_neutral_potion_effects")) {
                JsonObject effects = universal.getAsJsonObject("hostile_and_neutral_potion_effects");
                for (String key : effects.keySet()) {
                    universalUpgrades.put(key, parseUpgradeCosts(effects.getAsJsonArray(key)));
                }
            }
            
            // Parse all_mobs_item_masteries
            if (universal.has("all_mobs_item_masteries")) {
                JsonObject masteries = universal.getAsJsonObject("all_mobs_item_masteries");
                for (String key : masteries.keySet()) {
                    universalUpgrades.put(key, parseUpgradeCosts(masteries.getAsJsonArray(key)));
                }
            }
        }
        
        // Parse equipment rules (store as raw for now)
        this.equipmentRules = new HashMap<>();
        if (json.has("equipment_rules")) {
            this.equipmentRules.put("raw", json.get("equipment_rules"));
        }
        
        // Parse enchant costs
        this.enchantCosts = new HashMap<>();
        if (json.has("enchant_costs")) {
            JsonObject enchants = json.getAsJsonObject("enchant_costs");
            for (String weaponType : enchants.keySet()) {
                Map<String, List<Integer>> weaponEnchants = new HashMap<>();
                JsonObject enchantGroup = enchants.getAsJsonObject(weaponType);
                
                for (String enchantName : enchantGroup.keySet()) {
                    List<Integer> costs = new ArrayList<>();
                    JsonArray costArray = enchantGroup.getAsJsonArray(enchantName);
                    for (JsonElement cost : costArray) {
                        // Parse "I:3" or "II:4" format
                        String costStr = cost.getAsString();
                        if (costStr.contains(":")) {
                            costs.add(Integer.parseInt(costStr.split(":")[1]));
                        } else {
                            costs.add(Integer.parseInt(costStr));
                        }
                    }
                    weaponEnchants.put(enchantName, costs);
                }
                enchantCosts.put(weaponType, weaponEnchants);
            }
        }
        
        // Parse skill trees
        this.skillTrees = new HashMap<>();
        if (json.has("skill_trees")) {
            JsonObject trees = json.getAsJsonObject("skill_trees");
            for (String treeId : trees.keySet()) {
                Map<String, List<UpgradeCost>> treeSkills = new HashMap<>();
                JsonObject tree = trees.getAsJsonObject(treeId);
                
                for (String skillName : tree.keySet()) {
                    treeSkills.put(skillName, parseUpgradeCosts(tree.getAsJsonArray(skillName)));
                }
                skillTrees.put(treeId, treeSkills);
            }
        }
    }
    
    private static List<UpgradeCost> parseUpgradeCosts(JsonArray array) {
        List<UpgradeCost> costs = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement elem = array.get(i);
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                int cost = obj.get("cost").getAsInt();
                Map<String, Object> extra = new HashMap<>();
                
                // Parse any extra fields (effect, chance, etc.)
                for (String key : obj.keySet()) {
                    if (!key.equals("cost")) {
                        extra.put(key, obj.get(key));
                    }
                }
                costs.add(new UpgradeCost(i + 1, cost, extra));
            } else if (elem.isJsonPrimitive()) {
                // Simple cost number
                costs.add(new UpgradeCost(i + 1, elem.getAsInt(), null));
            }
        }
        return costs;
    }
    
    /**
     * Load mob config from mob_configs/{mobName}.json
     */
    public static MobConfig load(String mobName) {
        if (LOADED_CONFIGS.containsKey(mobName)) {
            return LOADED_CONFIGS.get(mobName);
        }
        
        try {
            Path configPath = FabricLoader.getInstance().getModContainer("universalmobwar")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath("mob_configs/" + mobName + ".json")
                .orElseThrow(() -> new RuntimeException("Config not found: " + mobName + ".json"));
            
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            
            MobConfig config = new MobConfig(root);
            LOADED_CONFIGS.put(mobName, config);
            
            UniversalMobWarMod.LOGGER.info("[MobConfig] Loaded config for: {}", mobName);
            return config;
        } catch (Exception e) {
            UniversalMobWarMod.LOGGER.error("[MobConfig] Failed to load config for: {}", mobName, e);
            return null;
        }
    }
    
    /**
     * Get upgrade cost for a universal upgrade (healing, health_boost, etc.)
     */
    public int getUniversalUpgradeCost(String upgradeName, int currentLevel) {
        List<UpgradeCost> costs = universalUpgrades.get(upgradeName);
        if (costs == null || currentLevel >= costs.size()) return 0;
        return costs.get(currentLevel).cost;
    }
    
    /**
     * Get upgrade cost for a skill tree upgrade
     */
    public int getSkillTreeUpgradeCost(String treeId, String skillName, int currentLevel) {
        Map<String, List<UpgradeCost>> tree = skillTrees.get(treeId);
        if (tree == null) return 0;
        
        List<UpgradeCost> costs = tree.get(skillName);
        if (costs == null || currentLevel >= costs.size()) return 0;
        return costs.get(currentLevel).cost;
    }
    
    /**
     * Get enchant cost
     */
    public int getEnchantCost(String weaponType, String enchantName, int currentLevel) {
        Map<String, List<Integer>> weaponEnchants = enchantCosts.get(weaponType);
        if (weaponEnchants == null) return 0;
        
        List<Integer> costs = weaponEnchants.get(enchantName);
        if (costs == null || currentLevel >= costs.size()) return 0;
        return costs.get(currentLevel);
    }
    
    /**
     * Check if mob has a specific skill tree
     */
    public boolean hasTree(String treeId) {
        return assignedTrees.contains(treeId);
    }
    
    /**
     * Check if mob is hostile
     */
    public boolean isHostile() {
        return mobType.equals("hostile");
    }
    
    /**
     * Check if mob is neutral
     */
    public boolean isNeutral() {
        return mobType.equals("neutral");
    }
    
    /**
     * Check if mob starts with weapon (ranged weapons only)
     */
    public boolean startsWithWeapon() {
        return weapon.equals("bow") || weapon.equals("crossbow") || weapon.equals("trident");
    }
    
    /**
     * Clear cache (for reload)
     */
    public static void clearCache() {
        LOADED_CONFIGS.clear();
        UniversalMobWarMod.LOGGER.info("[MobConfig] Cleared config cache");
    }
}
