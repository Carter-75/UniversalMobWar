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
        if (profile.chosenUpgrades == null) return;
        boolean hasInfect = profile.chosenUpgrades.stream().anyMatch(id -> id.equals("zombie_infect_4"));
        if (!hasInfect) return;
        // Only apply to living targets
        if (target != null && target.isAlive()) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 20, 1)); // 20s, level 2
        }
    }
}
