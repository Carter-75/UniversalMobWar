package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies scaling-system ranged abilities (multishot + extra_shot follow-up cycles)
 * to any projectile fired by a mob.
 */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileAbilityMixin {

    @Unique
    private boolean universalmobwar$abilitiesApplied;

    @Inject(method = "tick", at = @At("HEAD"))
    private void universalmobwar$applyProjectileAbilities(CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient() || universalmobwar$abilitiesApplied) {
            return;
        }

        universalmobwar$abilitiesApplied = true;

        if (self.getCommandTags().contains(ScalingSystem.MULTISHOT_CHILD_TAG)
            || self.getCommandTags().contains(ScalingSystem.EXTRA_SHOT_CHILD_TAG)) {
            return;
        }

        Entity owner = self.getOwner();
        if (!(owner instanceof MobEntity mob)) {
            return;
        }

        MobWarData data = universalmobwar$getData(mob);
        if (data == null) {
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        LivingEntity target = mob.getTarget();

        // Extra-shot follow-up cycles (scheduled between natural cycles).
        long serverTick = serverWorld.getServer().getTicks();
        ScalingSystem.onMobFiredProjectileForExtraShot(mob, data, self, serverWorld, serverTick);

        // Multishot extra projectiles (immediate duplicates).
        int extraProjectiles = ScalingSystem.handleRangedAbilities(mob, data, target, serverTick);
        if (extraProjectiles <= 0) {
            return;
        }

        universalmobwar$spawnMultishotProjectiles(serverWorld, mob, data, self, extraProjectiles);
    }

    @Unique
    private void universalmobwar$spawnMultishotProjectiles(ServerWorld world, MobEntity mob, MobWarData data, ProjectileEntity original, int extraProjectiles) {
        final boolean isExplosive = original instanceof ExplosiveProjectileEntity;

        double baseSpeed;
        Vec3d baseDir;

        if (isExplosive) {
            double accel = 0.1;
            try {
                accel = ((ExplosiveProjectileEntityAccessor) (Object) original).universalmobwar$getAccelerationPower();
            } catch (Throwable ignored) {
            }
            baseSpeed = Math.max(0.05, accel);
            Vec3d v = original.getVelocity();
            baseDir = v.lengthSquared() > 1.0E-6 ? v.normalize() : mob.getRotationVec(1.0F);
        } else {
            Vec3d v = original.getVelocity();
            baseSpeed = Math.max(0.05, v.length());
            baseDir = v.lengthSquared() > 1.0E-6 ? v.normalize() : mob.getRotationVec(1.0F);
        }

        // Use small variance: ~5-10% speed and small angle jitter.
        // This keeps it basically the same shot with slight imperfection.
        final float maxAngleDeg = 4.0f;
        final double minSpeedMul = 0.90;
        final double maxSpeedMul = 1.10;

        int piercingLevel = Math.max(0, ScalingSystem.getPiercingLevel(mob, data));

        for (int i = 0; i < extraProjectiles; i++) {
            Entity duplicate = original.getType().create(world);
            if (!(duplicate instanceof ProjectileEntity projectile)) {
                continue;
            }

            projectile.setOwner(mob);
            projectile.setPosition(original.getX(), original.getY(), original.getZ());
            projectile.addCommandTag(ScalingSystem.MULTISHOT_CHILD_TAG);

            if (original instanceof ThrownItemEntity thrown && projectile instanceof ThrownItemEntity thrownCopy) {
                thrownCopy.setItem(thrown.getStack().copy());
            }

            Vec3d dirJittered = universalmobwar$jitterDirection(baseDir, mob.getRandom().nextFloat() * 2f - 1f, mob.getRandom().nextFloat() * 2f - 1f, maxAngleDeg);
            double speedMul = minSpeedMul + mob.getRandom().nextDouble() * (maxSpeedMul - minSpeedMul);
            double speed = baseSpeed * speedMul;
            Vec3d velocity = dirJittered.multiply(speed);
            projectile.setVelocity(velocity);

            if (projectile instanceof ExplosiveProjectileEntity explosiveCopy) {
                try {
                    ((ExplosiveProjectileEntityAccessor) (Object) explosiveCopy).universalmobwar$setAccelerationPower(speed);
                } catch (Throwable ignored) {
                }
            }

            if (projectile instanceof PersistentProjectileEntity persistentCopy) {
                persistentCopy.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                if (piercingLevel > 0) {
                    ((PersistentProjectileEntityAccessor) (Object) persistentCopy).invokeSetPierceLevel((byte) Math.min(127, piercingLevel));
                }
            }

            world.spawnEntity(projectile);
        }
    }

    @Unique
    private static Vec3d universalmobwar$jitterDirection(Vec3d baseDir, float yawRand, float pitchRand, float maxAngleDeg) {
        // Convert small yaw/pitch offsets (degrees) into a perturbed direction.
        double yaw = Math.toRadians(yawRand * maxAngleDeg);
        double pitch = Math.toRadians(pitchRand * maxAngleDeg);

        // Apply yaw around Y axis.
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        Vec3d yawed = new Vec3d(
            baseDir.x * cosYaw - baseDir.z * sinYaw,
            baseDir.y,
            baseDir.x * sinYaw + baseDir.z * cosYaw
        );

        // Apply pitch around X axis.
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        Vec3d pitched = new Vec3d(
            yawed.x,
            yawed.y * cosPitch - yawed.z * sinPitch,
            yawed.y * sinPitch + yawed.z * cosPitch
        );

        return pitched.lengthSquared() > 1.0E-6 ? pitched.normalize() : baseDir;
    }

    @Unique
    private MobWarData universalmobwar$getData(MobEntity mob) {
        if (mob instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get(mob);
    }
}
