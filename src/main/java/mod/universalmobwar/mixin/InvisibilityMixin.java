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
public abstract class InvisibilityMixin {

    private int invisCooldown = 0;
    private int timeSinceLastInvis = 0;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void universalmobwar$onTick(CallbackInfo ci) {
        MobEntity mob = (MobEntity)(Object)this;
        if (mob.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("invis_mastery", 0);
        if (level <= 0) return;
        
        if (mob.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            timeSinceLastInvis = 0;
            return;
        } else {
            timeSinceLastInvis++;
        }
        
        // "only after 1min not invis" -> 1200 ticks
        if (timeSinceLastInvis < 1200) return;
        
        if (invisCooldown > 0) {
            invisCooldown--;
            return;
        }
        
        // Cooldown based on level (tries every X min)
        // 1: 10m, 2: 9m, ... 9: 2m, 10: 1m? No, prompt says:
        // 10min > 9 > 8 > 7 > 6 > 5 > 4 > 3 > 2 > 1 > .5 > .25
        // Wait, that's 12 levels? Or 1-10 maps to these?
        // Prompt: "invis 1 ... tries every 10min>9>8>7>6>5>4>3>2>1>.5>.25"
        // This list has 12 items. But level is 1-10.
        // Let's map 1->10m, 10->0.25m.
        // 1: 10m
        // 2: 9m
        // 3: 8m
        // 4: 7m
        // 5: 6m
        // 6: 5m
        // 7: 4m
        // 8: 3m
        // 9: 2m
        // 10: 1m (or 0.25m?)
        // Let's assume linear interpolation or just switch.
        
        double minutes = 10.0;
        if (level >= 10) minutes = 0.25;
        else if (level == 9) minutes = 1.0; // Jump?
        else minutes = 10.0 - (level - 1); // 1->10, 2->9, ... 9->2
        
        // Set cooldown
        invisCooldown = (int)(minutes * 60 * 20);
        
        // 25% chance
        if (mob.getRandom().nextFloat() < 0.25f) {
            // 10 sec max
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
        }
    }
}
