package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Armadillo - Passive mob
 * All upgrade logic is handled by ScalingSystem reading from mob_configs/armadillo.json
 */
@Mixin(ArmadilloEntity.class)
public abstract class ArmadilloMixin {

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        ArmadilloEntity self = (ArmadilloEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        ScalingSystem.processMobTick((MobEntity) self, world, data);
    }
}
