package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks Creeper-specific scaling abilities for explosion power and lingering clouds.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperAbilityMixin extends HostileEntity {

    protected CreeperAbilityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void universalmobwar$boostExplosionRadius(CallbackInfo ci) {
        if (!(this.getWorld() instanceof ServerWorld)) {
            return;
        }

        MobWarData data = universalmobwar$getData();
        if (data == null) {
            return;
        }

        NbtCompound skillData = data.getSkillData();
        if (skillData.getInt("ability_creeper_power") <= 0) {
            return;
        }

        float configuredRadius = ScalingSystem.getCreeperExplosionRadius((MobEntity) (Object) this, data);
        CreeperEntityAccessor accessor = (CreeperEntityAccessor) this;
        int upgradedRadius = Math.max(accessor.universalmobwar$getExplosionRadius(), Math.round(configuredRadius));
        accessor.universalmobwar$setExplosionRadius(upgradedRadius);
    }

    @Inject(method = "explode", at = @At("TAIL"))
    private void universalmobwar$spawnPotionCloud(CallbackInfo ci) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        MobWarData data = universalmobwar$getData();
        if (data == null) {
            return;
        }

        if (data.getSkillData().getInt("ability_creeper_potion_cloud") <= 0) {
            return;
        }

        BlockPos pos = this.getBlockPos();
        ScalingSystem.spawnCreeperPotionCloud((MobEntity) (Object) this, data, serverWorld, pos);
    }

    @Unique
    private MobWarData universalmobwar$getData() {
        if (this instanceof IMobWarDataHolder holder) {
            return holder.getMobWarData();
        }
        return MobWarData.get((MobEntity) (Object) this);
    }
}
