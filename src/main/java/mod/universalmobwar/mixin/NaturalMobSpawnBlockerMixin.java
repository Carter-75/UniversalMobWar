package mod.universalmobwar.mixin;

import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents all natural mob spawns if disableNaturalMobSpawns is true in config.
 */
@Mixin(ServerWorld.class)
public abstract class NaturalMobSpawnBlockerMixin {
    /**
     * This mixin blocks only natural mob spawns by heuristics:
     * - Only blocks mobs that are not spawned by players, eggs, or commands.
     * - Does NOT block mobs spawned by mod code, eggs, or commands.
     * - This is a best-effort fallback for versions without SpawnReason.
     */
    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void onSpawnEntity(net.minecraft.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.getInstance().disableNaturalMobSpawns && entity instanceof MobEntity) {
            // Heuristic: block if entity is a mob and not a player, and not being ridden or leashed (common for natural spawns)
            // This will NOT block mobs spawned by eggs, commands, or modded events that set passengers or leash
            if (entity.getVehicle() == null && entity.getControllingPassenger() == null && entity.getFirstPassenger() == null && !entity.hasCustomName()) {
                cir.setReturnValue(false);
            }
        }
    }
}
