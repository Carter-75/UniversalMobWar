package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.entity.mob.*;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes neutral mobs always aggressive when the gamerule is enabled.
 * Affects: Enderman, Zombie Piglin, Iron Golem, Wolves, Pandas, Polar Bears, etc.
 */
@Mixin({
    EndermanEntity.class,
    ZombifiedPiglinEntity.class,
    IronGolemEntity.class,
    WolfEntity.class,
    PandaEntity.class,
    PolarBearEntity.class,
    SpiderEntity.class,
    CaveSpiderEntity.class
})
public abstract class NeutralMobBehaviorMixin {
    
    /**
     * Prevents neutral mobs from becoming passive if the gamerule is on.
     * This keeps them in "always aggressive" mode.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void universalmobwar$forceAggressive(CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        
        if (self.getWorld().isClient()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        
        boolean neutralAggressive = serverWorld.getGameRules().getBoolean(UniversalMobWarMod.NEUTRAL_MOBS_AGGRESSIVE_RULE);
        if (!neutralAggressive) return;
        
        // For mobs with anger tracking, keep anger high
        if (self instanceof Angerable angerable) {
            if (angerable.getAngryAt() == null && self.getTarget() != null) {
                // Set anger target to current target
                angerable.setAngryAt(self.getTarget().getUuid());
                angerable.setAngerTime(600); // 30 seconds
            }
        }
    }
}

