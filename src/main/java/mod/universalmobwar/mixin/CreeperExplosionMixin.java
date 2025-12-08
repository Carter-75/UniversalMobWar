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

    @Inject(method = "explode", at = @At("HEAD"))
    private void universalmobwar$onExplode(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity)(Object)this;
        if (creeper.getWorld().isClient()) return;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(creeper);
        if (profile == null) return;

        // Explosion radius scaling (creeper_power)
        int powerLevel = profile.specialSkills.getOrDefault("creeper_power", 0);
        if (powerLevel > 0 && powerLevel <= 5) {
            // Level 1–5: radius 3.0-8.0 progressive
            float radius = 3.0f + ((powerLevel - 1) * 1.25f); // L1=3.0, L2=4.25, L3=5.5, L4=6.75, L5=8.0
            // Set explosion radius (reflection hack, as vanilla field is private)
            try {
                java.lang.reflect.Field field = CreeperEntity.class.getDeclaredField("explosionRadius");
                field.setAccessible(true);
                field.setInt(creeper, (int)radius);
            } catch (Exception e) {
                // ignore
            }
        }

        // Creeper Potion Mastery - Progressive lingering clouds
        int level = profile.specialSkills.getOrDefault("creeper_potion_mastery", 0);
        if (level > 0) {
            // L1: Slowness I (10s)
            // L2: Slowness I (15s) + Weakness I (10s)
            // L3: Slowness II (20s) + Weakness I (15s) + Poison I (10s)
            
            AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(creeper.getWorld(), creeper.getX(), creeper.getY(), creeper.getZ());
            cloud.setRadius(3.0F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(200); // 10s base
            cloud.setRadiusGrowth(-cloud.getRadius() / (float)cloud.getDuration());
            
            if (level == 1) {
                cloud.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0)); // Slowness I 10s
            } else if (level == 2) {
                cloud.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 0)); // Slowness I 15s
                cloud.addEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0)); // Weakness I 10s
            } else if (level >= 3) {
                cloud.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 1)); // Slowness II 20s
                cloud.addEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 0)); // Weakness I 15s
                cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0)); // Poison I 10s
            }
            
            creeper.getWorld().spawnEntity(cloud);
        }
    }
}
