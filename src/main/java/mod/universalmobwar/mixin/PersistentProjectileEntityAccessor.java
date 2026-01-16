package mod.universalmobwar.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
    @Accessor("inGround")
    boolean universalmobwar$isInGround();

    @Invoker("setPierceLevel")
    void invokeSetPierceLevel(byte pierceLevel);
    
    @Invoker("getPierceLevel")
    byte invokeGetPierceLevel();
}
