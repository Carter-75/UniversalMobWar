package mod.universalmobwar.mixin;

import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieEntity.class)
public abstract class InfectiousBiteMixin {

    @Inject(method = "onKilledOther", at = @At("RETURN"))
    private void universalmobwar$onKilledOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof VillagerEntity villager) {
            ZombieEntity zombie = (ZombieEntity)(Object)this;
            PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(zombie);
            if (profile != null) {
                int level = profile.specialSkills.getOrDefault("infectious_bite", 0);
                if (level > 0) {
                    // Level 1: 33%, Level 2: 66%, Level 3: 100%
                    float chance = level * 0.33f;
                    if (chance >= 1.0f || zombie.getRandom().nextFloat() < chance) {
                        // Force conversion regardless of difficulty
                        villager.convertTo(net.minecraft.entity.EntityType.ZOMBIE_VILLAGER, false);
                        // We don't cancel, so vanilla might try to convert again?
                        // convertTo handles "isRemoved" check, so calling it twice is safe (second call does nothing).
                    }
                }
            }
        }
    }

    @Inject(method = "tryAttack", at = @At("RETURN"))
    private void universalmobwar$onAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(target instanceof LivingEntity living)) return;
        
        ZombieEntity zombie = (ZombieEntity)(Object)this;
        if (zombie.getWorld().isClient()) return;
        
        PowerProfile profile = mod.universalmobwar.system.GlobalMobScalingSystem.getActiveProfile(zombie);
        if (profile == null) return;
        
        int level = profile.specialSkills.getOrDefault("hunger_attack", 0);
        if (level <= 0) return;
        
        // Progressive hunger: L1=Hunger I (10s), L2=Hunger II (15s), L3=Hunger III (20s)
        int amplifier = level - 1; // 0, 1, 2
        int duration = 200 + (level * 100); // 300, 400, 500 ticks = 15s, 20s, 25s... Wait spec says 10/15/20
        duration = (level == 1) ? 200 : (level == 2) ? 300 : 400; // 10s, 15s, 20s
        
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, duration, amplifier), zombie);
    }
}
