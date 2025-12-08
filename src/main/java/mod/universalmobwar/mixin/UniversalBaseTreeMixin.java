package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles universal base tree effects: speed, strength, healing burst, invisibility on damage.
 */
@Mixin(MobEntity.class)
public abstract class UniversalBaseTreeMixin {

    // Healing burst on hit (levels 3-5)
    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void universalmobwar$onAttack(net.minecraft.entity.Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        MobEntity mob = (MobEntity)(Object)this;
        if (mob.getWorld().isClient()) return;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        int healingLevel = profile.specialSkills.getOrDefault("healing", 0);
        if (healingLevel >= 3) {
            // Level 3+: chance for burst regen on hit, with cooldown
            int burstLevel = healingLevel - 2; // 1=level3, 2=level4, 3=level5
            int[] chances = {20, 40, 80}; // %
            int[] durations = {200, 200, 400}; // ticks (10s, 10s, 20s)
            int[] amplifiers = {2, 3, 4}; // Regen III, IV, V
            int[] cooldowns = {1200, 1200, 1200}; // 60s shared cooldown
            int idx = Math.min(burstLevel-1, 2);
            int chance = chances[idx];
            int duration = durations[idx];
            int amplifier = amplifiers[idx];
            int cooldown = cooldowns[idx];
            int now = mob.age;
            int last = profile.specialSkills.getOrDefault("healing_burst_last", -999999);
            if (now - last >= cooldown) {
                if (mob.getRandom().nextInt(100) < chance) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, amplifier));
                    profile.specialSkills.put("healing_burst_last", now);
                }
            }
        }
    }

    // Invisibility on damage (with cooldown and chance per level)
    @Inject(method = "damage", at = @At("RETURN"))
    private void universalmobwar$onDamage(net.minecraft.entity.damage.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        MobEntity mob = (MobEntity)(Object)this;
        if (mob.getWorld().isClient()) return;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        int level = profile.specialSkills.getOrDefault("invis_mastery", 0);
        if (level <= 0) return;
        // Spec: 5/20/40/60/80% chance, 5/8/12/16/20s, 60s cooldown
        int[] chances = {5, 20, 40, 60, 80};
        int[] durations = {100, 160, 240, 320, 400}; // ticks
        int cooldown = 1200; // 60s
        int idx = Math.min(level-1, 4);
        int chance = chances[idx];
        int duration = durations[idx];
        int now = mob.age;
        int last = profile.specialSkills.getOrDefault("invis_burst_last", -999999);
        if (now - last >= cooldown) {
            if (mob.getRandom().nextInt(100) < chance) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration, 0));
                profile.specialSkills.put("invis_burst_last", now);
            }
        }
    }

    // Speed and Strength status effects (permanent, based on upgrade level)
    @Inject(method = "tick", at = @At("TAIL"))
    private void universalmobwar$applyBaseEffects(CallbackInfo ci) {
        MobEntity mob = (MobEntity)(Object)this;
        if (mob.getWorld().isClient()) return;
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(mob);
        if (profile == null) return;
        // Speed
        int speedLevel = profile.specialSkills.getOrDefault("speed", 0);
        if (speedLevel > 0) {
            int amplifier = speedLevel - 1;
            if (!mob.hasStatusEffect(StatusEffects.SPEED) || mob.getStatusEffect(StatusEffects.SPEED).getAmplifier() != amplifier) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 220, amplifier, true, false));
            }
        }
        // Strength
        int strengthLevel = profile.specialSkills.getOrDefault("strength", 0);
        if (strengthLevel > 0) {
            int amplifier = strengthLevel - 1;
            if (!mob.hasStatusEffect(StatusEffects.STRENGTH) || mob.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() != amplifier) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 220, amplifier, true, false));
            }
        }
    }
}