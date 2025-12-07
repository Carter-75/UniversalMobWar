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
        register("zombie", "g", "z");
        register("husk", "g", "z");
        register("drowned", "g", "z", "trident");
        register("skeleton", "g", "pro", "bow");
        register("stray", "g", "bow", "pro");
        register("bogged", "g", "bow", "pro");
        register("wither_skeleton", "g");
        register("zombified_piglin", "g", "z");
        register("phantom", "g", "nw");
        register("zoglin", "g", "nw", "z");
        register("zombie_villager", "g", "z");
        register("zombie_horse", "gp");
        register("skeleton_horse", "gp");
        
        register("witch", "g", "nw", "pro");
        register("vex", "g");
        register("ravager", "g", "nw");
        
        // Piglins
        register("piglin", "g"); 
        register("piglin_brute", "g");
        
        // Arthropods
        register("spider", "g", "nw");
        register("cave_spider", "g", "nw");
        register("silverfish", "g", "nw");
        register("endermite", "g", "nw");
        register("bee", "g");
        
        // Nether
        register("blaze", "g", "pro", "nw");
        register("ghast", "g");
        register("magma_cube", "g", "nw");
        register("hoglin", "g", "nw");
        register("strider", "gp");
        
        // End
        register("enderman", "g");
        register("shulker", "g", "nw", "pro");
        
        // Ocean
        register("guardian", "g");
        register("elder_guardian", "g", "nw");
        
        // Others
        register("creeper", "g", "nw");
        register("slime", "g", "nw");
        register("iron_golem", "g");
        register("snow_golem", "g", "pro", "nw");
        register("breeze", "g", "pro", "nw");
        register("warden", "g");
        
        // Passives
        register("cow", "gp");
        register("sheep", "gp");
        register("pig", "gp");
        register("chicken", "gp");
        register("wolf", "g", "nw");
        register("cat", "gp");
        register("horse", "gp");
        register("donkey", "gp");
        register("mule", "gp");
        register("llama", "gp");
        register("panda", "gp");
        register("fox", "gp");
        register("polar_bear", "g", "nw");
        register("villager", "gp");
        register("wandering_trader", "gp");
        register("goat", "gp");
        register("frog", "gp");
        register("tadpole", "gp");
        register("allay", "gp"); 
        register("armadillo", "gp");
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
        register("trader_llama", "gp");
    }

    private static void register(String name, String... categories) {
        MOB_CATEGORIES.put(name, new HashSet<>(Arrays.asList(categories)));
    }

    public static Set<String> getMobCategories(MobEntity mob) {
           // Try to derive a stable short name for the mob to match our registration table.
           // Translation keys may be like: "entity.minecraft.zombie" or "entity.modid.mobname".
           String raw = mob.getType().getTranslationKey();
           if (raw == null) raw = "";
           // Normalize separators so we can split on either "." or ":"
           raw = raw.replace(':', '.');
           String name = raw.replaceAll("^entity\\.|^minecraft\\.", "").toLowerCase();
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
        
        // Piglin Deterministic Choice (based on UUID for consistency across reloads)
        if (name.equals("piglin")) {
            // Use UUID hash for deterministic but varied results
            if (mob.getUuid().hashCode() % 2 == 0) {
                categories.add("sword");
            } else {
                categories.add("bow");
            }
        }

        // Debug logging for mob categories and archetype
        if (mod.universalmobwar.config.ModConfig.getInstance().debugLogging) {
            String archetype = detectArchetypeFromCategories(categories);
            mod.universalmobwar.UniversalMobWarMod.LOGGER.info("Mob {} classified as archetype: {}, categories: {}", name, archetype, categories);
        }

        return categories;
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
        String archetype = detectArchetype(mob);
        // Map archetype to upgrade path (for UpgradeSystem)
        return archetype;
    }

    public static void addSpecialTags() {
        mod.universalmobwar.config.ModConfig config = mod.universalmobwar.config.ModConfig.getInstance();
        for (String mob : config.specialMobs) {
            Set<String> tags = MOB_CATEGORIES.get(mob);
            if (tags != null) {
                tags.add(mob); // add the mob name as tag, e.g. "witch" for witch
            }
        }
    }
}
