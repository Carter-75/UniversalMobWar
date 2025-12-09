package mod.universalmobwar.system;

import mod.universalmobwar.config.MobDefinition;
import mod.universalmobwar.config.SkillTreeConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import java.util.Set;
import java.util.HashSet;

/**
 * ArchetypeClassifier: Determines mob categories and trees from skilltree.json.
 * All mob configurations are loaded from the JSON file - data-driven approach.
 */
public class ArchetypeClassifier {
    
    private static String extractMobName(MobEntity mob) {
        // Extract short name from translation key
        // "entity.minecraft.zombie" -> "zombie"
        // "entity.modid.custom_mob" -> "custom_mob"
        String raw = mob.getType().getTranslationKey();
        if (raw == null) raw = "";
        raw = raw.replace(':', '.');
        String name = raw.replaceAll("^entity\\.|^minecraft\\.", "").toLowerCase();
        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            name = parts[parts.length - 1];
        }
        return name;
    }

    public static Set<String> getMobCategories(MobEntity mob) {
        String mobName = extractMobName(mob);
        Set<String> categories = new HashSet<>();
        
        // Try to load from skilltree.json
        MobDefinition def = SkillTreeConfig.getInstance().getMobDefinition(mobName);
        
        if (def != null) {
            // Mob is defined in skilltree.json
            
            // Add base category based on type
            if (def.isHostile() || def.isNeutral()) {
                categories.add("g"); // General hostile tree
            } else if (def.isPassive()) {
                categories.add("gp"); // General passive tree
            }
            
            // Add weapon categories
            switch (def.weaponType) {
                case BOW, CROSSBOW -> categories.add("bow");
                case TRIDENT -> categories.add("trident");
                case NORMAL_SWORD, STONE_SWORD, GOLD_SWORD -> categories.add("sword");
                case IRON_AXE, GOLD_AXE -> categories.add("axe");
                case NONE -> categories.add("nw"); // No weapon
            }
            
            // Add special tree tags from skilltree
            for (String tree : def.trees) {
                switch (tree) {
                    case "z" -> categories.add("z"); // Zombie tree
                    case "r" -> categories.add("pro"); // Ranged/projectile tree
                    case "creeper" -> categories.add("creeper");
                    case "witch" -> categories.add("witch");
                    case "cave_spider" -> categories.add("cave_spider");
                    case "r_if_crossbow" -> {
                        // Piglin special case
                        if (mobName.equals("piglin") && mob.getUuid().hashCode() % 2 == 1) {
                            categories.add("pro");
                        }
                    }
                }
            }
            
            // Piglin special weapon assignment (50/50 sword or crossbow)
            if (mobName.equals("piglin")) {
                if (mob.getUuid().hashCode() % 2 == 0) {
                    categories.add("sword");
                    categories.remove("bow"); // Don't give bow category if melee
                } else {
                    categories.add("bow"); // Crossbow uses bow enchants
                    categories.remove("sword");
                }
            }
            
        } else {
            // Fallback for unknown/modded mobs (not in skilltree.json)
            double attack = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null ?
                mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue() : 0.0;
            
            if (attack > 0) {
                categories.add("g");
            } else {
                categories.add("gp");
            }
            
            // Check for held items
            if (!categories.contains("bow") && !categories.contains("trident") && !categories.contains("axe")) {
                if (mob.getMainHandStack().isEmpty()) {
                    categories.add("nw");
                }
            }
        }

        // Debug logging
        if (mod.universalmobwar.config.ModConfig.getInstance().debugLogging) {
            String archetype = detectArchetypeFromCategories(categories);
            mod.universalmobwar.UniversalMobWarMod.LOGGER.info("Mob {} (def: {}) classified as archetype: {}, categories: {}", 
                mobName, def != null ? "found" : "fallback", archetype, categories);
        }

        return categories;
    }
    
    public static MobDefinition getMobDefinition(MobEntity mob) {
        String mobName = extractMobName(mob);
        return SkillTreeConfig.getInstance().getMobDefinition(mobName);
    }

    public static String detectArchetype(MobEntity mob) {
        Set<String> cats = getMobCategories(mob);
        return detectArchetypeFromCategories(cats);
    }

    private static String detectArchetypeFromCategories(Set<String> cats) {
        if (cats.contains("z")) return "zombie";
        if (cats.contains("bow")) return "skeleton";
        if (cats.contains("g")) return "universal";
        return "universal";
    }

    public static String detectPriorityPath(MobEntity mob) {
        return detectArchetype(mob);
    }
}
