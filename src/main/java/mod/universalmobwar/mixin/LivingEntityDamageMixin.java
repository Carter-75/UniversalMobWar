package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles damage-triggered scaling abilities on the LivingEntity level so all mobs stay compatible
 * even when Mojang changes MobEntity's overrides.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage", at = @At("TAIL"))
    private void universalmobwar$handleDamageAbilities(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        if (!((Object) this instanceof MobEntity mobEntity)) {
            return;
        }

        MobWarData data = ((IMobWarDataHolder) mobEntity).getMobWarData();
        if (data == null) {
            return;
        }

        long currentTick = mobEntity.getWorld().getTime();
        ScalingSystem.handleDamageAbilities(mobEntity, data, currentTick);
        if (mobEntity.getWorld() instanceof ServerWorld serverWorld) {
            ScalingSystem.handleHordeSummon(mobEntity, data, serverWorld, currentTick);
        }
    }
}
