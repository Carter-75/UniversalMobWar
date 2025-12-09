package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Cat - Passive mob
 * All upgrade logic is handled by ScalingSystem reading from mob_configs/cat.json
 */
@Mixin(CatEntity.class)
public abstract class CatMixin {

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        CatEntity self = (CatEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        ScalingSystem.processMobTick((MobEntity) self, world, data);
    }
}
