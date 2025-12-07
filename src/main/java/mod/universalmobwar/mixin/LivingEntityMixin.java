package mod.universalmobwar.mixin;

import mod.universalmobwar.system.SkillTreeEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onAttacking", at = @At("HEAD"))
    private void onAttacking(Entity target, CallbackInfo ci) {
        SkillTreeEvents.onMobAttack((LivingEntity) (Object) this, target);
    }
    
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            SkillTreeEvents.onMobHurt((LivingEntity) (Object) this);
        }
    }
}
