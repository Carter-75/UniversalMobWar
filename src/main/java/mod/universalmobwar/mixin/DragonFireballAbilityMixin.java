package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Augments Ender Dragon fireballs with void bombardment scaling effects.
 */
@Mixin(DragonFireballEntity.class)
public abstract class DragonFireballAbilityMixin extends ExplosiveProjectileEntity {

    protected DragonFireballAbilityMixin(EntityType<? extends DragonFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/math/Vec3d;)V", at = @At("TAIL"))
    private void universalmobwar$configureBombardment(World world, LivingEntity owner, Vec3d direction, CallbackInfo ci) {
        UniversalMobWarMod.runSafely("DragonFireballAbilityMixin#configureBombardment", () -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(owner instanceof EnderDragonEntity dragon)) {
                return;
            }

            MobWarData data = universalmobwar$getData(dragon);
            if (!universalmobwar$hasVoidBombardment(data)) {
                return;
            }

            ScalingSystem.handleVoidBombardment(dragon, data, serverWorld, (DragonFireballEntity) (Object) this, serverWorld.getTime());
        });
    }

    @Inject(method = "onCollision", at = @At("TAIL"))
    private void universalmobwar$applyBombardmentEffects(HitResult hitResult, CallbackInfo ci) {
        UniversalMobWarMod.runSafely("DragonFireballAbilityMixin#applyBombardmentEffects", () -> {
            if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
                return;
            }

            MobEntity owner = universalmobwar$getOwner();
            if (owner == null) {
                return;
            }

            MobWarData data = universalmobwar$getData(owner);
            if (!universalmobwar$hasVoidBombardment(data)) {
                return;
            }

            LivingEntity directHit = null;
            if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
                directHit = living;
            }

            Vec3d impactPos = hitResult.getPos();
            ScalingSystem.applyVoidBombardmentEffects(owner, data, serverWorld, impactPos, directHit);
        });
    }

    @Unique
    private boolean universalmobwar$hasVoidBombardment(MobWarData data) {
        return data != null && data.getSkillData().getInt("ability_void_bombardment") > 0;
    }

    @Unique
    private MobWarData universalmobwar$getData(MobEntity mob) {
        if (mob instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get(mob);
    }

    @Unique
    private MobEntity universalmobwar$getOwner() {
        Entity owner = this.getOwner();
        return owner instanceof MobEntity mob ? mob : null;
    }
}
