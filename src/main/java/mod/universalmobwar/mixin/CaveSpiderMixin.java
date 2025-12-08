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
        // Level 1: Poison I (7s), Level 2: Poison II (20s), Level 3+: Poison II (20s) + Wither I (5s)
        if (level == 1) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 140, 0), spider); // 7s Poison I
        } else if (level == 2) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 400, 1), spider); // 20s Poison II
        } else if (level >= 3) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 400, 1), spider); // 20s Poison II
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0), spider); // 5s Wither I
        }
    }
}
