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
        
        // Performance: Only run logic every 20 ticks (1 second)
        if (this.age % 20 == 0) {
            MobEntity mob = (MobEntity)(Object)this;
            
            // Performance: Only upgrade if a player is nearby (within 64 blocks)
            // This prevents processing thousands of mobs in loaded chunks far away
            if (mob.getWorld().getClosestPlayer(mob, 64) == null) return;

            MobWarData data = MobWarData.get(mob);
            
            // Check if mob is fully maxed - if so, skip all upgrade processing
            var profile = data.getPowerProfile();
            if (profile != null && profile.isMaxed) {
                return;
            }
            
            // If we have points to spend
            if (data.getSpentPoints() < data.getSkillPoints()) {
                // Speed up: If we have a LOT of points (e.g. > 100), do multiple steps per tick
                // to avoid taking hours to upgrade high-level mobs.
                double deficit = data.getSkillPoints() - data.getSpentPoints();
                int steps = 1;
                if (deficit > 500) steps = 20;
                else if (deficit > 100) steps = 5;
                
                // Thread-safe upgrade process with safety counter
                synchronized(data) {
                    int safety = 0;
                    for (int i = 0; i < steps && safety < 100; i++, safety++) {
                        if (data.getSpentPoints() >= data.getSkillPoints()) break;
                        UpgradeSystem.performOneStep(mob, data);
                    }
                    
                    if (safety >= 100) {
                        mod.universalmobwar.UniversalMobWarMod.LOGGER.warn(
                            "Upgrade safety limit hit for mob {} (spent: {}, total: {})",
                            mob.getType().getTranslationKey(), data.getSpentPoints(), data.getSkillPoints()
                        );
                    }
                }
                
                MobWarData.save(mob, data);
            }
        }
    }
}
