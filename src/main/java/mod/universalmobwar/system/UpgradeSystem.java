package mod.universalmobwar.system;

import mod.universalmobwar.config.MobConfig;
import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.mob.MobEntity;
import java.util.*;

/**
 * NEW CLEAN UPGRADE SYSTEM
 * ONLY uses individual mob JSON files from mob_configs/
 * Each mob's JSON contains EVERYTHING needed for that mob
 */
public class UpgradeSystem {
    
    /**
     * Main entry point: Apply all upgrades to a mob based on its PowerProfile
     */
    public static void applyUpgrades(MobEntity mob, PowerProfile profile) {
        // Get mob name from entity
        String mobName = getMobNameFromEntity(mob);
        
        // Load mob-specific config
        MobConfig config = MobConfig.load(mobName);
        if (config == null) {
            return; // Mob config not found
        }
        
        // TODO: Implement upgrade simulation and application
        // This will be completely rewritten to use MobConfig
    }
    
    /**
     * Get standardized mob name from entity (e.g., "entity.minecraft.zombie" -> "Zombie")
     */
    private static String getMobNameFromEntity(MobEntity mob) {
        String key = mob.getType().getTranslationKey();
        // Remove "entity.minecraft." prefix
        String name = key.replace("entity.minecraft.", "").replace("entity.", "");
        // Capitalize first letter and convert underscores to proper case
        return capitalize(name);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        // "zombie" -> "Zombie", "wither_skeleton" -> "Wither_Skeleton"
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
}
