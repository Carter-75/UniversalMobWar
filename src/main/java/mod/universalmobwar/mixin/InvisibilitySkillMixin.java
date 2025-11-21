package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class InvisibilitySkillMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void universalmobwar$tickInvis(CallbackInfo ci) {
        MobEntity mob = (MobEntity)(Object)this;
        if (mob.getWorld().isClient()) return;
        
        // Run check every second (20 ticks) to save perf
        if (mob.age % 20 != 0) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("invis_mastery", 0);
        if (level <= 0) return;
        
        long time = mob.getWorld().getTime();
        long lastTry = profile.specialSkills.getOrDefault("invis_last_try", 0);
        
        // Calculate Cooldown (in ticks)
        // 10min -> 1min mapping
        // Level 1: 10m = 12000 ticks
        // Level 10: 1m = 1200 ticks
        // Linear interpolation? Or step?
        // User list: 10, 9, 8, 7, 6, 5, 4, 3, 2, 1.
        double minutes = 11.0 - level; 
        if (minutes < 1.0) minutes = 1.0;
        long cooldown = (long)(minutes * 60 * 20);
        
        if (time - lastTry >= cooldown) {
            profile.specialSkills.put("invis_last_try", (int)time); // Cast to int might overflow eventually but ok for logic
            
            // 25% Chance
            if (mob.getRandom().nextFloat() < 0.25f) {
                // Duration: 10 seconds
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
            }
        }
    }
}
