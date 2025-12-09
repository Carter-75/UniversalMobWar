package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Blaze - Hostile mob with ranged abilities
 * All upgrade logic is handled by ScalingSystem reading from mob_configs/blaze.json
 */
@Mixin(BlazeEntity.class)
public abstract class BlazeMixin {

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        BlazeEntity self = (BlazeEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        ScalingSystem.processMobTick((MobEntity) self, world, data);
    }
}
