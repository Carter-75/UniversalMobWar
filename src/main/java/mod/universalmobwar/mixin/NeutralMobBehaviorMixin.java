package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes neutral mobs (those with Angerable interface) always aggressive when the gamerule is enabled.
 * Affects: Enderman, Zombie Piglin, Wolves, Pandas, Polar Bears, Bees, etc.
 */
@Mixin(MobEntity.class)
public abstract class NeutralMobBehaviorMixin {
    
    /**
     * Keeps neutral mobs aggressive by maintaining their anger when the gamerule is on.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void universalmobwar$forceAggressive(CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        
        if (self.getWorld().isClient()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        
        // Only affect mobs with anger system (neutral mobs)
        if (!(self instanceof Angerable angerable)) return;
        
        boolean neutralAggressive = serverWorld.getGameRules().getBoolean(UniversalMobWarMod.NEUTRAL_MOBS_AGGRESSIVE_RULE);
        if (!neutralAggressive) return;
        
        // Keep anger high when they have a target
        if (angerable.getAngryAt() == null && self.getTarget() != null) {
            // Set anger target to current target
            angerable.setAngryAt(self.getTarget().getUuid());
            angerable.setAngerTime(600); // 30 seconds
        }
    }
}

