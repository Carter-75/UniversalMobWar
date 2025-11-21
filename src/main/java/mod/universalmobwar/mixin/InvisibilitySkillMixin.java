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
        
        int time = mob.age;
        
        // Track when invisibility ends
        if (mob.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            profile.specialSkills.put("invis_end_time", time);
            return;
        }
        
        // "only after 1min not invis" (1200 ticks)
        int lastInvis = profile.specialSkills.getOrDefault("invis_end_time", -999999);
        if (time - lastInvis < 1200) return;
        
        int lastTry = profile.specialSkills.getOrDefault("invis_last_try", -999999);
        
        // Use calculated interval from UpgradeSystem if available, else fallback
        int cooldown = profile.specialSkills.getOrDefault("invis_interval_ticks", 0);
        if (cooldown == 0) {
             // Fallback for old profiles or if not set
             double minutes = 11.0 - level; 
             if (minutes < 1.0) minutes = 1.0;
             cooldown = (int)(minutes * 60 * 20);
        }
        
        if (time - lastTry >= cooldown) {
            profile.specialSkills.put("invis_last_try", time);
            
            // 25% Chance
            if (mob.getRandom().nextFloat() < 0.25f) {
                // Duration: 10 seconds
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
            }
        }
    }
}
