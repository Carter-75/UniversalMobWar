package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies scaling-system ranged abilities (piercing, multishot, potion mastery)
 * to every persistent projectile fired by a mob.
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileAbilityMixin extends ProjectileEntity implements ProjectileAbilityBridge {
    @Shadow
    public abstract Entity getOwner();

    @Unique
    private boolean universalmobwar$abilitiesApplied;

    protected PersistentProjectileAbilityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void universalmobwar$setAbilitiesApplied(boolean applied) {
        this.universalmobwar$abilitiesApplied = applied;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void universalmobwar$applyProjectileAbilities(CallbackInfo ci) {
        if (this.getWorld().isClient() || universalmobwar$abilitiesApplied) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof MobEntity mob)) {
            universalmobwar$abilitiesApplied = true;
            return;
        }

        MobWarData data = universalmobwar$getData(mob);
        if (data == null) {
            universalmobwar$abilitiesApplied = true;
            return;
        }

        universalmobwar$abilitiesApplied = true;

        int piercingLevel = Math.max(0, ScalingSystem.getPiercingLevel(mob, data));
        if (piercingLevel > 0) {
            ((PersistentProjectileEntityAccessor) (Object) this).invokeSetPierceLevel((byte) Math.min(127, piercingLevel));
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            LivingEntity target = mob.getTarget();
            int extraProjectiles = ScalingSystem.handleRangedAbilities(mob, data, target, serverWorld.getTime());
            if (extraProjectiles > 0) {
                universalmobwar$spawnExtraProjectiles(serverWorld, mob, data, extraProjectiles);
            }
        }
    }

    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void universalmobwar$applyPotionMastery(EntityHitResult hitResult, CallbackInfo ci) {
        if (this.getWorld().isClient()) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof MobEntity mob)) {
            return;
        }

        Entity hit = hitResult.getEntity();
        if (!(hit instanceof LivingEntity livingTarget)) {
            return;
        }

        MobWarData data = universalmobwar$getData(mob);
        if (data == null) {
            return;
        }

        ScalingSystem.applyRangedPotionEffects(mob, data, livingTarget);
    }

    private void universalmobwar$spawnExtraProjectiles(ServerWorld world, MobEntity mob, MobWarData data, int extraProjectiles) {
        Vec3d currentVelocity = this.getVelocity();
        float speed = (float) currentVelocity.length();
        if (speed < 0.05f) {
            speed = 1.6f;
        }

        for (int i = 0; i < extraProjectiles; i++) {
            Entity duplicateEntity = this.getType().create(world);
            if (!(duplicateEntity instanceof PersistentProjectileEntity extra)) {
                break;
            }

            extra.setOwner(mob);
            extra.setPosition(this.getX(), this.getY(), this.getZ());
            float yawOffset = universalmobwar$getYawOffset(i);
            extra.setVelocity(mob, mob.getPitch(), mob.getYaw() + yawOffset, 0.0f, speed, 1.0f);
            ((PersistentProjectileEntityAccessor) extra).invokeSetPierceLevel(((PersistentProjectileEntityAccessor) (Object) this).invokeGetPierceLevel());
            extra.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            ((ProjectileAbilityBridge) extra).universalmobwar$setAbilitiesApplied(true);
            world.spawnEntity(extra);
        }
    }

    private float universalmobwar$getYawOffset(int index) {
        int pair = index / 2 + 1;
        float base = 6.0f + pair * 2.5f;
        return (index % 2 == 0) ? base : -base;
    }

    private MobWarData universalmobwar$getData(MobEntity mob) {
        if (mob instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get(mob);
    }
}
