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
        register("ally", "gp");
        register("armadillo", "gp");
        register("axolotl", "gp");
        register("bat", "gp", "nw");
        register("bee", "g");
        register("blaze", "g", "pro", "nw");
        register("bogged", "g");
        register("breeze", "g", "pro", "nw");
        register("camel", "gp");
        register("cat", "gp");
        register("cave_spider", "g", "nw");
        register("chicken", "gp");
        register("cod", "gp");
        register("cow", "gp");
        register("creeper", "g", "nw");
        register("dolphin", "gp");
        register("donkey", "gp");
        register("drowned", "g", "z", "trident");
        register("elder_guardian", "g", "nw");
        register("enderman", "g");
        register("endermite", "g", "nw");
        register("evoker", "g", "nw");
        register("fox", "gp");
        register("frog", "gp");
        register("ghast", "g");
        register("glow_squid", "gp");
        register("goat", "gp");
        register("guardian", "g");
        register("hoglin", "g", "nw");
        register("horse", "gp");
        register("husk", "g", "z");
        register("iron_golem", "g");
        register("llama", "gp");
        register("magma_cube", "g", "nw");
        register("mooshroom", "gp");
        register("mule", "gp");
        register("ocelot", "gp");
        register("panda", "gp");
        register("parrot", "gp");
        register("phantom", "g", "nw");
        register("pig", "gp");
        register("piglin", "g");
        register("piglin_brute", "g");
        register("pillager", "g", "pro", "bow");
        register("polar_bear", "g", "nw");
        register("pufferfish", "g", "nw");
        register("rabbit", "gp");
        register("ravager", "g", "nw");
        register("salmon", "gp");
        register("sheep", "gp");
        register("shulker", "g", "nw", "pro");
        register("silverfish", "g", "nw");
        register("skeleton", "g", "pro", "bow");
        register("skeleton_horse", "gp");
        register("slime", "g", "nw");
        register("sniffer", "gp");
        register("snow_golem", "g", "pro", "nw");
        register("spider", "g", "nw");
        register("squid", "gp");
        register("stray", "g", "bow", "pro");
        register("strider", "gp");
        register("tadpole", "gp");
        register("trader_llama", "gp");
        register("tropical_fish", "gp");
        register("turtle", "gp");
        register("vex", "g");
        register("villager", "gp");
        register("vindicator", "g", "axe");
        register("wandering_trader", "gp");
        register("warden", "g");
        register("witch", "g", "nw", "pro");
        register("wither_skeleton", "g");
        register("wolf", "g", "nw");
        register("zoglin", "g", "nw");
        register("zombie", "g", "z");
        register("zombie_horse", "gp");
        register("zombie_villager", "g", "z");
        register("zombified_piglin", "g", "z");
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

        if (MOB_CATEGORIES.containsKey(name)) {
            return MOB_CATEGORIES.get(name);
        }

        // Fallback for unknown/modded mobs
        Set<String> categories = new HashSet<>();
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
