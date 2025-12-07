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
        
        int duration = (2 + level) * 20;
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 0), spider);
    }
}
