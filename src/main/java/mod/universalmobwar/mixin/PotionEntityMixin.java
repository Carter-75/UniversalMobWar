package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies ranged potion mastery effects when custom potion projectiles land.
 */
@Mixin(PotionEntity.class)
public abstract class PotionEntityMixin extends ThrownItemEntity {

    protected PotionEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at = @At("TAIL"))
    private void universalmobwar$applyRangedPotionEffects(EntityHitResult hitResult, CallbackInfo ci) {
        if (this.getWorld().isClient()) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof MobEntity mobOwner)) {
            return;
        }

        Entity hit = hitResult.getEntity();
        if (!(hit instanceof LivingEntity livingTarget)) {
            return;
        }

        MobWarData data;
        if (mobOwner instanceof IMobWarDataHolder holder) {
            data = holder.getMobWarData();
        } else {
            data = MobWarData.get(mobOwner);
        }

        if (data == null) {
            return;
        }

        ScalingSystem.applyRangedPotionEffects(mobOwner, data, livingTarget);

        int piercing = ScalingSystem.getPiercingLevel(mobOwner, data);
        if (piercing <= 0) {
            return;
        }

        World world = this.getWorld();
        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class,
            hit.getBoundingBox().expand(2.5),
            entity -> entity != livingTarget && entity != mobOwner && entity.isAlive());

        int applied = 0;
        for (LivingEntity extraTarget : nearby) {
            ScalingSystem.applyRangedPotionEffects(mobOwner, data, extraTarget);
            applied++;
            if (applied >= piercing) {
                break;
            }
        }
    }
}
