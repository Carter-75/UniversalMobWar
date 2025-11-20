package mod.universalmobwar.system;

import mod.universalmobwar.data.PowerProfile;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalMobScalingSystem: Handles world-day and kill-based scaling, upgrades, and tier logic for all mobs.
 * This system is only active if ModConfig.scalingEnabled is true.
 */
public class GlobalMobScalingSystem {
    // In-memory cache for active mob profiles (cleared on chunk unload)
    private static final Map<MobEntity, PowerProfile> ACTIVE_PROFILES = new HashMap<>();

    /**
     * Called when a mob spawns or enters simulation distance.
     */
    public static void onMobActivated(MobEntity mob, ServerWorld world) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        if (ACTIVE_PROFILES.containsKey(mob)) return;
        PowerProfile profile = new PowerProfile();
        // Copy base stats
        profile.baseHealth = mob.getMaxHealth();
        profile.baseDamage = getBaseDamage(mob);
        profile.baseSpeed = mob.getMovementSpeed();
        profile.baseArmor = mob.getArmor();
        profile.baseKnockbackResist = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) != null ?
            mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).getBaseValue() : 0.0;
        // Calculate scaling points
        profile.dayScalingPoints = calculateDayScalingPoints(world);
        profile.killScalingPoints = 0;
        profile.totalPoints = profile.dayScalingPoints;
        profile.tierLevel = 0;
        profile.priorityPath = detectPriorityPath(mob);
        profile.archetype = detectArchetype(mob);
        ACTIVE_PROFILES.put(mob, profile);
        updateTierAndUpgrades(mob, profile);
    }

    /**
     * Called when a mob gets a kill or assist.
     */
    public static void onMobKill(MobEntity mob, LivingEntity victim) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        PowerProfile profile = ACTIVE_PROFILES.get(mob);
        if (profile == null) return;
        double points = calculateKillPoints(mob, victim, profile);
        profile.killScalingPoints += points;
        profile.totalPoints = profile.dayScalingPoints + profile.killScalingPoints;
        updateTierAndUpgrades(mob, profile);
    }

    /**
     * Calculate day-based scaling points using a soft exponential curve.
     */
    private static double calculateDayScalingPoints(ServerWorld world) {
        long day = world.getTimeOfDay() / 24000L;
        double base = ModConfig.getInstance().dayScalingMultiplier;
        // Example: soft exponential (tweak as needed)
        return base * Math.pow(day, 1.18);
    }

    /**
     * Calculate kill-based scaling points (progressive, harder per tier).
     */
    private static double calculateKillPoints(MobEntity mob, LivingEntity victim, PowerProfile profile) {
        double base = ModConfig.getInstance().killScalingMultiplier;
        int tier = profile.tierLevel;
        // Example: each tier requires 2x more points
        return base / Math.pow(2, tier);
    }

    /**
     * Update tier and buy upgrades based on total points.
     */
    private static void updateTierAndUpgrades(MobEntity mob, PowerProfile profile) {
        int newTier = calculateTier(profile.totalPoints);
        if (newTier > profile.tierLevel) {
            profile.tierLevel = newTier;
            // Buy upgrades for new tier
            buyUpgrades(mob, profile, newTier);
        }
    }

    /**
     * Calculate tier from total points (nonlinear, hidden from player).
     */
    private static int calculateTier(double totalPoints) {
        // Example: nonlinear tier curve
        if (totalPoints < 10) return 0;
        if (totalPoints < 30) return 1;
        if (totalPoints < 80) return 2;
        if (totalPoints < 200) return 3;
        if (totalPoints < 500) return 4;
        if (totalPoints < 1200) return 5;
        if (totalPoints < 3000) return 6;
        if (totalPoints < 7000) return 7;
        if (totalPoints < 15000) return 8;
        if (totalPoints < 30000) return 9;
        return Math.min((int)(Math.log10(totalPoints) * 2), ModConfig.getInstance().maxTier);
    }

    /**
     * Buy upgrades for the mob at the given tier.
     */
    private static void buyUpgrades(MobEntity mob, PowerProfile profile, int tier) {
        // Universal upgrade tree (example: health, armor, speed, damage, etc.)
        // Add upgrades to profile.chosenUpgrades and apply effects to mob
        // (Implement full upgrade logic as needed)
    }

    /**
     * Detect mob archetype for modded mob support.
     */
    private static String detectArchetype(MobEntity mob) {
        // Use entity type, tags, attributes, etc. to classify
        // (Implement full detection logic as needed)
        return "melee";
    }

    /**
     * Detect priority path for upgrade tree.
     */
    private static String detectPriorityPath(MobEntity mob) {
        // Use mob type, NBT, or random for now
        // (Implement full detection logic as needed)
        return "universal";
    }

    /**
     * Get base damage for a mob (attribute or fallback).
     */
    private static double getBaseDamage(MobEntity mob) {
        if (mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            return mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
        }
        return 2.0;
    }
}
