    // (getActiveProfile is now correctly placed inside the class below)
package mod.universalmobwar.system;

import mod.universalmobwar.data.PowerProfile;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
// ...existing code...
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalMobScalingSystem: Handles world-day and kill-based scaling, upgrades, and tier logic for all mobs.
 * This system is only active if ModConfig.scalingEnabled is true.
 */
public class GlobalMobScalingSystem {
        // Prevent infinite recursion: track mobs currently being processed
        private static final java.util.Set<MobEntity> ACTIVATION_IN_PROGRESS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    // In-memory cache for active mob profiles (cleared on chunk unload)
    private static final Map<MobEntity, PowerProfile> ACTIVE_PROFILES = new HashMap<>();

    /**
     * Public accessor for a mob's PowerProfile (for mixins/event hooks).
     */
    public static PowerProfile getActiveProfile(MobEntity mob) {
        return ACTIVE_PROFILES.get(mob);
    }

    /**
     * Called when a mob spawns or enters simulation distance.
     */
    public static void onMobActivated(MobEntity mob, ServerWorld world) {
        if (!ModConfig.getInstance().scalingEnabled) {
            ACTIVE_PROFILES.remove(mob);
            return;
        }
        // Prevent infinite recursion: skip if already processing this mob
        if (ACTIVATION_IN_PROGRESS.contains(mob)) return;
        ACTIVATION_IN_PROGRESS.add(mob);
        try {
            if (ACTIVE_PROFILES.containsKey(mob)) {
                // Even if active, check if we need to update points (e.g. new day)
                // But if maxed, skip.
                PowerProfile profile = ACTIVE_PROFILES.get(mob);
                if (profile.isMaxed) return;
                
                double newDayPoints = calculateDayScalingPoints(world);
                if (newDayPoints > profile.dayScalingPoints) {
                    profile.dayScalingPoints = newDayPoints;
                    profile.totalPoints = profile.dayScalingPoints + profile.killScalingPoints;
                    updateTierAndUpgrades(mob, profile);
                }
                return;
            }
            
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
            profile.archetype = mod.universalmobwar.system.ArchetypeClassifier.detectArchetype(mob);
            profile.categories = mod.universalmobwar.system.ArchetypeClassifier.getMobCategories(mob);
            profile.priorityPath = mod.universalmobwar.system.ArchetypeClassifier.detectPriorityPath(mob);
            ACTIVE_PROFILES.put(mob, profile);
            updateTierAndUpgrades(mob, profile);
            if (ModConfig.getInstance().debugLogging) {
                mod.universalmobwar.UniversalMobWarMod.LOGGER.info("[UMW] Mob activated: {} archetype={} tier={}", mob.getType(), profile.archetype, profile.tierLevel);
            }
        } finally {
            ACTIVATION_IN_PROGRESS.remove(mob);
        }
    }

    /**
     * Called when a mob gets a kill or assist.
     */
    public static void onMobKill(MobEntity mob, LivingEntity victim) {
        if (!ModConfig.getInstance().scalingEnabled) return;
        PowerProfile profile = ACTIVE_PROFILES.get(mob);
        if (profile == null || profile.isMaxed) return;
        double points = calculateKillPoints(mob, victim, profile);
        profile.killScalingPoints += points;
        profile.totalPoints = profile.dayScalingPoints + profile.killScalingPoints;
        updateTierAndUpgrades(mob, profile);
        if (ModConfig.getInstance().debugLogging) {
            mod.universalmobwar.UniversalMobWarMod.LOGGER.info("[UMW] Mob kill: {} archetype={} newPoints={} newTier={}", mob.getType(), profile.archetype, profile.totalPoints, profile.tierLevel);
        }
    }
    /**
     * Cleanup all active profiles (call when scaling is disabled or on world unload).
     */
    public static void cleanupProfiles() {
        ACTIVE_PROFILES.clear();
    }

    /**
     * Calculate day-based scaling points using the user's specific schedule.
     */
    private static double calculateDayScalingPoints(ServerWorld world) {
        long day = world.getTimeOfDay() / 24000L;
        double points = 0;

        // 0-10 days: 0.1 per day
        long daysInPhase1 = Math.min(day, 10);
        points += daysInPhase1 * 0.1;
        if (day <= 10) return points;

        // 10-15 days: 0.5 per day
        long daysInPhase2 = Math.min(day - 10, 5);
        points += daysInPhase2 * 0.5;
        if (day <= 15) return points;

        // 15-20 days: 1 per day
        long daysInPhase3 = Math.min(day - 15, 5);
        points += daysInPhase3 * 1.0;
        if (day <= 20) return points;

        // 20-25 days: 1.5 per day
        long daysInPhase4 = Math.min(day - 20, 5);
        points += daysInPhase4 * 1.5;
        if (day <= 25) return points;

        // 25-30 days: 3 per day
        long daysInPhase5 = Math.min(day - 25, 5);
        points += daysInPhase5 * 3.0;
        if (day <= 30) return points;

        // 30+ days: 5 per day
        long daysInPhase6 = day - 30;
        points += daysInPhase6 * 5.0;

        return points;
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
     * Update tier and apply upgrades based on total points.
     */
    private static void updateTierAndUpgrades(MobEntity mob, PowerProfile profile) {
        // New system: Apply upgrades directly based on points
        mod.universalmobwar.system.UpgradeSystem.applyUpgrades(mob, profile);
        
        // Update tier level for display/logging (approximate based on points)
        profile.tierLevel = calculateTier(profile.totalPoints);
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
     * Get base damage for a mob (attribute or fallback).
     */
    private static double getBaseDamage(MobEntity mob) {
        if (mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            return mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue();
        }
        return 2.0;
    }
}
