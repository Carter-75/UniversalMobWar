package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * DEPRECATED: This system has been merged into EvolutionSystem.
 * This class now acts as a bridge to ensure backward compatibility
 * and to redirect calls to the new unified system.
 */
public class GlobalMobScalingSystem {

    /**
     * Redirects to EvolutionSystem.onMobSpawn
     */
    public static void onMobActivated(MobEntity mob, ServerWorld world) {
        EvolutionSystem.onMobSpawn(mob, world);
    }

    /**
     * Redirects to EvolutionSystem.onMobKill
     */
    public static void onMobKill(MobEntity mob, LivingEntity victim) {
        EvolutionSystem.onMobKill(mob, victim);
    }

    /**
     * Cleanup (No-op as EvolutionSystem handles its own cleanup)
     */
    public static void cleanupProfiles() {
        EvolutionSystem.cleanup();
    }
    
    /**
     * Helper to get the active profile for a mob.
     * Used by Mixins.
     */
    public static PowerProfile getActiveProfile(MobEntity mob) {
        return MobWarData.get(mob).getPowerProfile();
    }
}
