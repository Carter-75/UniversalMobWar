package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.system.EvolutionSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks mob kills for the evolution system.
 */
@Mixin(LivingEntity.class)
public abstract class MobDeathTrackerMixin {
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void universalmobwar$trackKill(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity)(Object)this;

        if (victim.getWorld().isClient()) return;
        if (!(victim instanceof MobEntity)) return;

        // Evolution system (legacy)
        boolean evolutionEnabled = victim.getWorld().getGameRules().getBoolean(UniversalMobWarMod.EVOLUTION_SYSTEM_RULE);
        // Global scaling system (new)
        boolean scalingEnabled = mod.universalmobwar.config.ModConfig.getInstance().scalingEnabled;

        if (!evolutionEnabled && !scalingEnabled) return;

        // Find the killer - only count kills by mobs, not creative players
        if (damageSource.getAttacker() instanceof MobEntity killer) {
            if (evolutionEnabled) {
                EvolutionSystem.onMobKill(killer, victim);
            }
            if (scalingEnabled) {
                mod.universalmobwar.system.GlobalMobScalingSystem.onMobKill(killer, victim);
            }
        }
    }
}

