package mod.universalmobwar.mixin;

import mod.universalmobwar.bridge.ProjectileAbilityBridge;
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

        // Multishot + extra_shot are handled in ProjectileAbilityMixin for all projectile types.
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

    private MobWarData universalmobwar$getData(MobEntity mob) {
        if (mob instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get(mob);
    }
}
