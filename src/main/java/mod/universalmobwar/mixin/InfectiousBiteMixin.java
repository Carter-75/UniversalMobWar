package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Hunger to prey when attacked by a mob with the 'infectious_bite' upgrade.
 */
@Mixin(LivingEntity.class)
public abstract class InfectiousBiteMixin {
    @Inject(method = "onAttacking", at = @At("HEAD"))
    private void universalmobwar$applyInfectiousBite(LivingEntity target, CallbackInfo ci) {
        LivingEntity attacker = (LivingEntity)(Object)this;
        if (!(attacker instanceof MobEntity mob)) return;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("hunger_attack", 0);
        if (level <= 0) return;

        // Only apply to living targets
        if (target != null && target.isAlive()) {
            // Level 1 -> Hunger 1 (Amplifier 0)
            // Level 2 -> Hunger 2 (Amplifier 1)
            // Level 3 -> Hunger 3 (Amplifier 2)
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 10 * 20, level - 1)); // 10s duration
        }
    }
}
