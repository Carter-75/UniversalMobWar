package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CaveSpiderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveSpiderEntity.class)
public abstract class CaveSpiderMixin {

    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void universalmobwar$onAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(target instanceof LivingEntity living)) return;
        
        CaveSpiderEntity spider = (CaveSpiderEntity)(Object)this;
        if (spider.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(spider);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("poison_attack", 0);
        if (level <= 0) return;
        
        int amplifier = 0;
        if (level == 1) amplifier = 1; // Poison 2
        else if (level >= 2) amplifier = 2; // Poison 3
        
        if (amplifier > 0) {
            // Duration? Vanilla uses 7s (Normal) or 15s (Hard).
            // Let's keep vanilla duration logic or just set a fixed one?
            // Vanilla: 7*20 or 15*20.
            // I'll use 7 seconds (140 ticks) as base, maybe scale with difficulty if I want, but fixed is fine.
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 140, amplifier), spider);
        }
    }
}
