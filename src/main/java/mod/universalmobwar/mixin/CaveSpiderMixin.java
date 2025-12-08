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
        // Poison Mastery 1-5: Poison I (7s) -> Poison II (20s) + Wither I
        // Level 1: Poison I (7s)
        // Level 2: Poison I (14s)
        // Level 3: Poison II (14s)
        // Level 4: Poison II (20s)
        // Level 5: Poison II (20s) + Wither I (10s)
        if (level == 1) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 140, 0), spider); // 7s Poison I
        } else if (level == 2) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 280, 0), spider); // 14s Poison I
        } else if (level == 3) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 280, 1), spider); // 14s Poison II
        } else if (level == 4) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 400, 1), spider); // 20s Poison II
        } else if (level >= 5) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 400, 1), spider); // 20s Poison II
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200, 0), spider); // 10s Wither I
        }
    }
}
