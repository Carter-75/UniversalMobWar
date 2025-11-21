package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class InfectiousBiteMixin {

    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void universalmobwar$onAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(target instanceof LivingEntity living)) return;
        
        ZombieEntity zombie = (ZombieEntity)(Object)this;
        if (zombie.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(zombie);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("hunger_attack", 0);
        if (level <= 0) return;
        
        int amplifier = level - 1;
        if (amplifier < 0) amplifier = 0;
        
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, amplifier), zombie);
    }
}
