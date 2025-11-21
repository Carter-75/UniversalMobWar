package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperExplosionMixin {

    @Inject(method = "explode", at = @At("TAIL"))
    private void universalmobwar$onExplode(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity)(Object)this;
        if (creeper.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(creeper);
        if (profile == null) return;
        
        int chance = profile.specialSkills.getOrDefault("creeper_potion_chance", 0);
        if (chance <= 0) return;
        
        // Chance check (0-100)
        if (creeper.getRandom().nextInt(100) < chance) {
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(creeper.getWorld(), creeper.getX(), creeper.getY(), creeper.getZ());
            cloud.setRadius(2.5F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(cloud.getDuration() / 2);
            cloud.setRadiusGrowth(-cloud.getRadius() / (float)cloud.getDuration());
            
            // Pick random effect
            // weakness, slowness, poison, harming, slowness II, blindness, nausea, wither
            int pick = creeper.getRandom().nextInt(8);
            StatusEffectInstance effect = switch (pick) {
                case 0 -> new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0);
                case 1 -> new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0);
                case 2 -> new StatusEffectInstance(StatusEffects.POISON, 200, 0);
                case 3 -> new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 0);
                case 4 -> new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1); // Slowness II
                case 5 -> new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0);
                case 6 -> new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0);
                case 7 -> new StatusEffectInstance(StatusEffects.WITHER, 200, 0);
                default -> new StatusEffectInstance(StatusEffects.POISON, 200, 0);
            };
            
            cloud.addEffect(effect);
            creeper.getWorld().spawnEntity(cloud);
        }
    }
}
