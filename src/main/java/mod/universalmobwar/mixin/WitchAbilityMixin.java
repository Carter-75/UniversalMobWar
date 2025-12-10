package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import mod.universalmobwar.system.WitchAbilityHelper;
import mod.universalmobwar.system.WitchAbilityHelper.ThrowStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adds scaling-system special abilities to Witch potion attacks.
 */
@Mixin(WitchEntity.class)
public abstract class WitchAbilityMixin extends MobEntity {

    protected WitchAbilityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "shootAt(Lnet/minecraft/entity/LivingEntity;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean universalmobwar$augmentPotionThrows(World world, Entity entity, LivingEntity target, float pullProgress) {
        if (!(world instanceof ServerWorld serverWorld) || !(entity instanceof PotionEntity potion)) {
            return world.spawnEntity(entity);
        }

        MobEntity witch = (MobEntity) (Object) this;
        if (!(witch instanceof IMobWarDataHolder holder)) {
            return world.spawnEntity(entity);
        }

        MobWarData data = holder.getMobWarData();
        if (data == null) {
            return world.spawnEntity(entity);
        }

        ItemStack resolvedStack = WitchAbilityHelper.resolvePotionStack(witch, data, potion.getStack().copy(), witch.getRandom());
        potion.setItem(resolvedStack);

        ThrowStats stats = WitchAbilityHelper.resolveThrowStats(witch, data);
        WitchAbilityHelper.configureTrajectory(potion, witch, target, stats, 0.0f);

        int extraProjectiles = ScalingSystem.handleRangedAbilities(witch, data, target, serverWorld.getTime());
        boolean spawned = serverWorld.spawnEntity(potion);
        WitchAbilityHelper.spawnAdditionalShots(serverWorld, witch, resolvedStack, target, stats, extraProjectiles);
        return spawned;
    }
}
