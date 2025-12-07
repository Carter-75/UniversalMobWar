package mod.universalmobwar.mixin;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.UpgradeSystem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobUpgradeTickMixin extends LivingEntity {

    protected MobUpgradeTickMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (this.getWorld().isClient) return;
        
        // Run every 10 ticks (0.5 seconds) to spread out load and make visual progression visible but not too slow
        if (this.age % 10 == 0) {
            MobEntity mob = (MobEntity)(Object)this;
            MobWarData data = MobWarData.get(mob);
            
            // If we have points to spend
            if (data.getSpentPoints() < data.getSkillPoints()) {
                UpgradeSystem.performOneStep(mob, data);
                MobWarData.save(mob, data);
            }
        }
    }
}
