package mod.universalmobwar.system;

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
}
