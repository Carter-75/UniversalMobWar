package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Breeze - Hostile mob with ranged abilities
 * All upgrade logic is handled by ScalingSystem reading from mob_configs/breeze.json
 */
@Mixin(BreezeEntity.class)
public abstract class BreezeMixin {

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        BreezeEntity self = (BreezeEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        ScalingSystem.processMobTick((MobEntity) self, world, data);
    }
}
