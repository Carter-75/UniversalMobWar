package mod.universalmobwar.system;

import mod.universalmobwar.config.MobConfig;
import net.minecraft.entity.mob.MobEntity;
import java.util.Set;
import java.util.HashSet;

/**
 * ArchetypeClassifier: Uses individual mob JSON configs from mob_configs/
 * NO MORE shared skilltree parsing - each mob has its own complete config!
 */
public class ArchetypeClassifier {
    
    /**
     * Get standardized mob name from entity (e.g., "zombie" -> "Zombie")
     */
    private static String extractMobName(MobEntity mob) {
        String key = mob.getType().getTranslationKey();
        // "entity.minecraft.zombie" -> "zombie"
        String name = key.replace("entity.minecraft.", "").replace("entity.", "");
        // Capitalize: "zombie" -> "Zombie", "wither_skeleton" -> "Wither_Skeleton"
        return capitalize(name);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                result.append('_');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Get mob categories from its individual JSON config
     * 
     * @deprecated This method is for backward compatibility only.
     *             New code should use MobConfig.load(mobName) directly!
     */
    @Deprecated
    public static Set<String> getMobCategories(MobEntity mob) {
        String mobName = extractMobName(mob);
        Set<String> categories = new HashSet<>();
        
        MobConfig config = MobConfig.load(mobName);
        if (config == null) {
            // Mob not found, return empty set
            return categories;
        }
        
        // Add base category based on type
        if (config.isHostile() || config.isNeutral()) {
            categories.add("g"); // General hostile/neutral
        } else {
            categories.add("gp"); // General passive
        }
        
        // Add tree IDs from assigned_trees
        categories.addAll(config.assignedTrees);
        
        // Add weapon category
        if (config.weapon.contains("bow")) {
            categories.add("bow");
        } else if (config.weapon.contains("crossbow")) {
            categories.add("crossbow");
        } else if (config.weapon.contains("trident")) {
            categories.add("trident");
        } else if (config.weapon.contains("sword")) {
            categories.add("sword");
        } else if (config.weapon.contains("axe")) {
            categories.add("axe");
        }
        
        return categories;
    }
    
    /**
     * Load mob configuration directly
     * THIS IS THE NEW WAY - use this instead of getMobCategories!
     */
    public static MobConfig getMobConfig(MobEntity mob) {
        String mobName = extractMobName(mob);
        return MobConfig.load(mobName);
    }
    
    /**
     * Get mob name for JSON loading
     */
    public static String getMobName(MobEntity mob) {
        return extractMobName(mob);
    }
}
