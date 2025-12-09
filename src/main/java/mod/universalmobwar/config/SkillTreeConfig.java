package mod.universalmobwar.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and parses the skilltree.json file containing all mob definitions,
 * upgrade costs, and progression rules.
 */
public class SkillTreeConfig {
    private static SkillTreeConfig INSTANCE;
    
    private final Map<String, MobDefinition> mobDefinitions = new HashMap<>();
    private final Map<String, List<UpgradeCost>> universalUpgrades = new HashMap<>();
    private final Map<String, List<UpgradeCost>> sharedTrees = new HashMap<>();
    private final Map<String, List<UpgradeCost>> specificTrees = new HashMap<>();
    private final Map<String, EnchantCosts> enchantCosts = new HashMap<>();
    
    // Daily scaling tiers
    private final List<DailyTier> dailyScaling = new ArrayList<>();
    
    public static class DailyTier {
        public final String days;
        public final double pointsPerDay;
        
        public DailyTier(String days, double pointsPerDay) {
            this.days = days;
            this.pointsPerDay = pointsPerDay;
        }
    }
    
    public static class UpgradeCost {
        public final int level;
        public final int cost;
        public final Map<String, Object> extra; // For effects, chances, etc.
        
        public UpgradeCost(int level, int cost, Map<String, Object> extra) {
            this.level = level;
            this.cost = cost;
            this.extra = extra != null ? extra : new HashMap<>();
        }
    }
    
    public static class EnchantCosts {
        public final Map<String, List<String>> enchants = new HashMap<>();
        
        public int getCost(String enchant, int currentLevel) {
            List<String> levels = enchants.get(enchant);
            if (levels == null || currentLevel >= levels.size()) return 0;
            String entry = levels.get(currentLevel);
            // Parse "I:3", "II:4", etc. or just "10"
            if (entry.contains(":")) {
                return Integer.parseInt(entry.split(":")[1]);
            }
            return Integer.parseInt(entry);
        }
        
        public int getMaxLevel(String enchant) {
            List<String> levels = enchants.get(enchant);
            return levels != null ? levels.size() : 0;
        }
    }
    
    private SkillTreeConfig() {
        loadSkillTree();
    }
    
    public static SkillTreeConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SkillTreeConfig();
        }
        return INSTANCE;
    }
    
    private void loadSkillTree() {
        try {
            // Load from mod resources first
            Path resourcePath = FabricLoader.getInstance().getModContainer("universalmobwar")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath("skilltree.json")
                .orElseThrow(() -> new RuntimeException("skilltree.json not found in mod resources"));
            
            String json = Files.readString(resourcePath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("universal_mob_war_skill_tree");
            
            // Parse daily scaling
            if (data.has("point_system")) {
                JsonObject pointSystem = data.getAsJsonObject("point_system");
                if (pointSystem.has("daily_scaling")) {
                    JsonArray daily = pointSystem.getAsJsonArray("daily_scaling");
                    for (JsonElement elem : daily) {
                        JsonObject tier = elem.getAsJsonObject();
                        dailyScaling.add(new DailyTier(
                            tier.get("days").getAsString(),
                            tier.get("points_per_day").getAsDouble()
                        ));
                    }
                }
            }
            
            // Parse mob list
            if (data.has("mob_list")) {
                JsonObject mobList = data.getAsJsonObject("mob_list");
                for (Map.Entry<String, JsonElement> entry : mobList.entrySet()) {
                    String mobName = entry.getKey();
                    JsonObject mobData = entry.getValue().getAsJsonObject();
                    
                    List<String> trees = new ArrayList<>();
                    if (mobData.has("trees")) {
                        JsonArray treesArray = mobData.getAsJsonArray("trees");
                        for (JsonElement tree : treesArray) {
                            trees.add(tree.getAsString());
                        }
                    }
                    
                    MobDefinition def = new MobDefinition(
                        mobName,
                        mobData.get("type").getAsString(),
                        mobData.get("weapon").getAsString(),
                        mobData.get("armor").getAsString(),
                        mobData.get("shield").getAsBoolean(),
                        trees
                    );
                    
                    mobDefinitions.put(mobName.toLowerCase(), def);
                }
            }
            
            // Parse universal upgrades
            if (data.has("universal_upgrades")) {
                JsonObject univ = data.getAsJsonObject("universal_upgrades");
                
                // Parse hostile_and_neutral_potion_effects
                if (univ.has("hostile_and_neutral_potion_effects")) {
                    JsonObject effects = univ.getAsJsonObject("hostile_and_neutral_potion_effects");
                    for (Map.Entry<String, JsonElement> entry : effects.entrySet()) {
                        String skillName = entry.getKey();
                        JsonArray levels = entry.getValue().getAsJsonArray();
                        List<UpgradeCost> costs = parseUpgradeCostArray(levels);
                        universalUpgrades.put(skillName, costs);
                    }
                }
                
                // Parse all_mobs_item_masteries
                if (univ.has("all_mobs_item_masteries")) {
                    JsonObject masteries = univ.getAsJsonObject("all_mobs_item_masteries");
                    if (masteries.has("drop_mastery")) {
                        JsonArray arr = masteries.getAsJsonArray("drop_mastery");
                        universalUpgrades.put("drop_mastery", parseUpgradeCostArray(arr));
                    }
                    if (masteries.has("durability_mastery")) {
                        JsonArray arr = masteries.getAsJsonArray("durability_mastery");
                        universalUpgrades.put("durability_mastery", parseUpgradeCostArray(arr));
                    }
                }
            }
            
            // Parse shared trees
            if (data.has("shared_trees")) {
                JsonObject shared = data.getAsJsonObject("shared_trees");
                for (Map.Entry<String, JsonElement> treeEntry : shared.entrySet()) {
                    String treeName = treeEntry.getKey(); // "zombie_z", "ranged_r"
                    JsonObject treeData = treeEntry.getValue().getAsJsonObject();
                    
                    for (Map.Entry<String, JsonElement> skillEntry : treeData.entrySet()) {
                        String skillName = skillEntry.getKey();
                        JsonArray levels = skillEntry.getValue().getAsJsonArray();
                        String key = treeName + ":" + skillName;
                        sharedTrees.put(key, parseUpgradeCostArray(levels));
                    }
                }
            }
            
            // Parse specific trees
            if (data.has("specific_trees")) {
                JsonObject specific = data.getAsJsonObject("specific_trees");
                for (Map.Entry<String, JsonElement> treeEntry : specific.entrySet()) {
                    String treeName = treeEntry.getKey(); // "creeper", "witch", "cave_spider"
                    JsonObject treeData = treeEntry.getValue().getAsJsonObject();
                    
                    for (Map.Entry<String, JsonElement> skillEntry : treeData.entrySet()) {
                        String skillName = skillEntry.getKey();
                        JsonArray levels = skillEntry.getValue().getAsJsonArray();
                        String key = treeName + ":" + skillName;
                        specificTrees.put(key, parseUpgradeCostArray(levels));
                    }
                }
            }
            
            // Parse enchant costs
            if (data.has("enchant_costs")) {
                JsonObject enchants = data.getAsJsonObject("enchant_costs");
                for (Map.Entry<String, JsonElement> entry : enchants.entrySet()) {
                    String category = entry.getKey(); // "sword", "axe", "helmet", etc.
                    JsonObject catData = entry.getValue().getAsJsonObject();
                    
                    EnchantCosts costs = new EnchantCosts();
                    for (Map.Entry<String, JsonElement> enchEntry : catData.entrySet()) {
                        String enchName = enchEntry.getKey();
                        JsonElement enchValue = enchEntry.getValue();
                        
                        List<String> costList = new ArrayList<>();
                        if (enchValue.isJsonArray()) {
                            JsonArray arr = enchValue.getAsJsonArray();
                            for (JsonElement elem : arr) {
                                costList.add(elem.getAsString());
                            }
                        } else {
                            costList.add(enchValue.getAsString());
                        }
                        
                        costs.enchants.put(enchName, costList);
                    }
                    
                    enchantCosts.put(category, costs);
                }
            }
            
            mod.universalmobwar.UniversalMobWarMod.LOGGER.info("Successfully loaded skilltree.json with {} mob definitions", mobDefinitions.size());
            
        } catch (Exception e) {
            mod.universalmobwar.UniversalMobWarMod.LOGGER.error("Failed to load skilltree.json", e);
            throw new RuntimeException("Failed to load skilltree.json", e);
        }
    }
    
    private List<UpgradeCost> parseUpgradeCostArray(JsonArray array) {
        List<UpgradeCost> costs = new ArrayList<>();
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            int level = obj.get("level").getAsInt();
            int cost = obj.get("cost").getAsInt();
            
            Map<String, Object> extra = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("level") && !key.equals("cost")) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive()) {
                        JsonPrimitive prim = value.getAsJsonPrimitive();
                        if (prim.isNumber()) {
                            extra.put(key, prim.getAsDouble());
                        } else {
                            extra.put(key, prim.getAsString());
                        }
                    }
                }
            }
            
            costs.add(new UpgradeCost(level, cost, extra));
        }
        return costs;
    }
    
    public MobDefinition getMobDefinition(String mobName) {
        return mobDefinitions.get(mobName.toLowerCase());
    }
    
    public List<UpgradeCost> getUniversalUpgrade(String skillName) {
        return universalUpgrades.getOrDefault(skillName, new ArrayList<>());
    }
    
    public List<UpgradeCost> getSharedTreeUpgrade(String treeName, String skillName) {
        String key = treeName + ":" + skillName;
        return sharedTrees.getOrDefault(key, new ArrayList<>());
    }
    
    public List<UpgradeCost> getSpecificTreeUpgrade(String treeName, String skillName) {
        String key = treeName + ":" + skillName;
        return specificTrees.getOrDefault(key, new ArrayList<>());
    }
    
    public EnchantCosts getEnchantCosts(String category) {
        return enchantCosts.get(category);
    }
    
    public List<DailyTier> getDailyScaling() {
        return new ArrayList<>(dailyScaling);
    }
    
    public double getDailyPoints(int day) {
        double total = 0;
        for (DailyTier tier : dailyScaling) {
            String[] range = tier.days.split("-");
            if (range.length == 2) {
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                if (day >= start && day <= end) {
                    int days = day - start + 1;
                    total += days * tier.pointsPerDay;
                } else if (day > end) {
                    int days = end - start + 1;
                    total += days * tier.pointsPerDay;
                }
            } else if (tier.days.endsWith("+")) {
                int start = Integer.parseInt(tier.days.replace("+", ""));
                if (day >= start) {
                    int days = day - start + 1;
                    total += days * tier.pointsPerDay;
                }
            }
        }
        return total;
    }
}
