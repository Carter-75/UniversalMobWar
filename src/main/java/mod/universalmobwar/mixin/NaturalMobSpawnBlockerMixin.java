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
     * This mixin blocks ALL natural mob spawns (hostile AND peaceful):
     * - Only blocks mobs that are not spawned by players, eggs, or commands.
     * - Does NOT block mobs spawned by spawn eggs, mod code, or commands.
     */
    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void onSpawnEntity(net.minecraft.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.getInstance().disableNaturalMobSpawns) return;
        if (!(entity instanceof MobEntity)) return;
        
        // Check if mob has special tags indicating it was spawned by player/mod/command
        if (entity.hasCommandTag("umw_player_spawned")) return;
        if (entity.hasCommandTag("umw_horde_reinforcement")) return;
        if (entity.hasCommandTag("umw_summoned")) return;
        if (entity.hasCustomName()) return;
        
        // Block the spawn - this is a natural mob spawn (hostile or peaceful)
        cir.setReturnValue(false);
    }
}
