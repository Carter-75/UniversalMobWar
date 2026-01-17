package mod.universalmobwar.mixin;

import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosiveProjectileEntity.class)
public interface ExplosiveProjectileEntityAccessor {
    @Accessor("accelerationPower")
    double universalmobwar$getAccelerationPower();

    @Accessor("accelerationPower")
    void universalmobwar$setAccelerationPower(double value);
}
