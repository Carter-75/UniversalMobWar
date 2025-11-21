package mod.universalmobwar.system;

import net.minecraft.entity.mob.MobEntity;
// ...existing code...

/**
 * ArchetypeClassifier: Determines the archetype and priority path for any mob (vanilla or modded).
 * Deterministic, tag/attribute/behavior-based, no hardcoding of modded types.
 */
public class ArchetypeClassifier {
    public static String detectArchetype(MobEntity mob) {
        String name = mob.getType().toString().toLowerCase();
        // Vanilla detection
        if (name.contains("creeper")) return "creeper";
        if (name.contains("cave_spider")) return "spider";
        if (name.contains("spider")) return "spider";
        if (name.contains("witch")) return "witch";
        if (name.contains("skeleton")) return "skeleton";
        if (name.contains("drowned")) return "zombie";
        if (name.contains("zombie")) return "zombie";
        if (name.contains("piglin")) return "nether";
        if (name.contains("enderman")) return "end";
        if (name.contains("ender_dragon")) return "end";
        if (name.contains("warden")) return "warden";
        if (name.contains("vindicator") || name.contains("evoker") || name.contains("pillager") || name.contains("ravager") || name.contains("illager")) return "illager";
        if (name.contains("blaze") || name.contains("hoglin") || name.contains("strider") || name.contains("wither_skeleton")) return "nether";
        // Modded/unknown: use attributes
        double attack = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) != null ?
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue() : 0.0;
        boolean canFly = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FLYING_SPEED) != null;
        double maxHealth = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH) != null ?
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).getBaseValue() : 20.0;
        if (canFly) return "end"; // treat flying mobs as end archetype
        if (attack > 12.0 && maxHealth > 40.0) return "warden";
        if (attack > 8.0) return "zombie";
        if (maxHealth > 40.0) return "illager";
        // Fallback
        return "universal";
    }

    public static String detectPriorityPath(MobEntity mob) {
        String archetype = detectArchetype(mob);
        // Map archetype to upgrade path (for UpgradeSystem)
        return archetype;
    }
}
