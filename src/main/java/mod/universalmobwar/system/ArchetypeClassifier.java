package mod.universalmobwar.system;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 * ArchetypeClassifier: Determines the archetype and priority path for any mob (vanilla or modded).
 * Deterministic, tag/attribute/behavior-based, no hardcoding of modded types.
 */
public class ArchetypeClassifier {
    private static final Map<String, Set<String>> MOB_CATEGORIES = new HashMap<>();

    static {
        // Undead
        register("zombie", "g", "gp", "z");
        register("husk", "g", "gp", "z");
        register("drowned", "g", "gp", "z", "trident");
        register("skeleton", "g", "gp", "bow");
        register("stray", "g", "gp", "bow");
        register("bogged", "g", "gp", "bow");
        register("wither_skeleton", "g", "gp", "sword");
        register("zombified_piglin", "g", "gp", "sword");
        register("phantom", "g", "gp", "nw");
        register("zoglin", "g", "gp", "nw");
        
        // Illagers
        register("vindicator", "g", "gp", "axe");
        register("pillager", "g", "gp", "bow");
        register("evoker", "g", "gp", "nw");
        register("witch", "g", "gp", "witch");
        register("vex", "g", "gp", "sword");
        register("ravager", "g", "gp", "nw");
        
        // Piglins
        register("piglin", "g", "gp"); // Random Sword/Bow handled in getMobCategories
        register("piglin_brute", "g", "gp", "axe");
        
        // Arthropods
        register("spider", "g", "gp", "nw");
        register("cave_spider", "g", "gp", "cave_spider", "nw");
        register("silverfish", "g", "gp", "nw");
        register("endermite", "g", "gp", "nw");
        register("bee", "g", "gp", "nw");
        
        // Nether
        register("blaze", "g", "gp", "nw");
        register("ghast", "g", "gp", "nw");
        register("magma_cube", "g", "gp", "nw");
        register("hoglin", "g", "gp", "nw");
        register("strider", "gp");
        
        // End
        register("enderman", "g", "gp", "nw");
        register("shulker", "g", "gp", "nw");
        
        // Ocean
        register("guardian", "g", "gp", "nw");
        register("elder_guardian", "g", "gp", "nw");
        
        // Others
        register("creeper", "g", "gp", "creeper", "nw");
        register("slime", "g", "gp", "nw");
        register("iron_golem", "g", "gp", "nw");
        register("snow_golem", "g", "gp", "nw");
        
        // Passives
        register("cow", "gp");
        register("sheep", "gp");
        register("pig", "gp");
        register("chicken", "gp");
        register("wolf", "g", "gp", "nw");
        register("cat", "gp");
        register("horse", "gp");
        register("donkey", "gp");
        register("mule", "gp");
        register("llama", "gp");
        register("panda", "gp");
        register("fox", "gp");
        register("polar_bear", "g", "gp", "nw");
        register("villager", "gp");
        register("wandering_trader", "gp");
        register("goat", "gp");
        register("frog", "gp");
        register("tadpole", "gp");
        register("allay", "gp");
        register("axolotl", "gp");
        register("bat", "gp", "nw");
        register("camel", "gp");
        register("cod", "gp");
        register("dolphin", "gp");
        register("glow_squid", "gp");
        register("mooshroom", "gp");
        register("ocelot", "gp");
        register("parrot", "gp");
        register("pufferfish", "g", "nw");
        register("rabbit", "gp");
        register("salmon", "gp");
        register("sniffer", "gp");
        register("squid", "gp");
        register("tropical_fish", "gp");
        register("turtle", "gp");
        register("warden", "g", "nw");
    }

    private static void register(String name, String... categories) {
        MOB_CATEGORIES.put(name, new HashSet<>(Arrays.asList(categories)));
    }

    public static Set<String> getMobCategories(MobEntity mob) {
        String name = mob.getType().getTranslationKey().replaceAll("^entity\\.|^minecraft\\.", "").toLowerCase();
        // Handle modded names if needed, usually translation key is like entity.modid.name
        if (name.contains(".")) {
             String[] parts = name.split("\\.");
             name = parts[parts.length - 1];
        }

        Set<String> categories = new HashSet<>();

        if (MOB_CATEGORIES.containsKey(name)) {
            categories.addAll(MOB_CATEGORIES.get(name));
        } else {
            // Fallback for unknown/modded mobs
            double attack = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null ?
                mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue() : 0.0;
            
            if (attack > 0) {
                categories.add("g");
            } else {
                categories.add("gp");
            }
            
            // Check for held items to determine weapon type if not specified
            if (!categories.contains("nw") && !categories.contains("bow") && !categories.contains("trident") && !categories.contains("axe")) {
                 if (mob.getMainHandStack().isEmpty()) {
                     categories.add("nw");
                 }
            }
        }
        
        // Piglin Randomness (50/50 Sword or Bow)
        if (name.equals("piglin")) {
            if (mob.getWorld().random.nextBoolean()) {
                categories.add("sword");
            } else {
                categories.add("bow");
            }
        }

        return categories;
    }

    public static String detectArchetype(MobEntity mob) {
        Set<String> cats = getMobCategories(mob);
        if (cats.contains("z")) return "zombie";
        if (cats.contains("bow")) return "skeleton";
        if (cats.contains("g")) return "universal";
        return "universal";
    }

    public static String detectPriorityPath(MobEntity mob) {
        String archetype = detectArchetype(mob);
        // Map archetype to upgrade path (for UpgradeSystem)
        return archetype;
    }
}
