package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wires Enderman-specific scaling abilities (shadow_step + void_grasp).
 */
@Mixin(EndermanEntity.class)
public abstract class EndermanAbilityMixin extends HostileEntity {

    @Unique
    private BlockPos universalmobwar$lastTeleportFrom = BlockPos.ORIGIN;

    protected EndermanAbilityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "teleportTo(DDD)Z", at = @At("HEAD"))
    private void universalmobwar$rememberTeleportOrigin(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        this.universalmobwar$lastTeleportFrom = this.getBlockPos();
    }

    @Inject(method = "teleportTo(DDD)Z", at = @At("RETURN"))
    private void universalmobwar$triggerShadowStep(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        MobWarData data = universalmobwar$getData();
        if (data == null) {
            return;
        }
        ScalingSystem.handleShadowStep((MobEntity) (Object) this, data, serverWorld, universalmobwar$lastTeleportFrom, serverWorld.getTime());
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void universalmobwar$handleVoidGrasp(CallbackInfo ci) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        // Run roughly every 10 ticks per Enderman to avoid constant scanning.
        if ((serverWorld.getTime() + this.getId()) % 10 != 0) {
            return;
        }
        MobWarData data = universalmobwar$getData();
        if (data == null) {
            return;
        }
        ScalingSystem.handleVoidGrasp((MobEntity) (Object) this, data, serverWorld, serverWorld.getTime());
    }

    private MobWarData universalmobwar$getData() {
        if (this instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get((MobEntity) (Object) this);
    }
}
