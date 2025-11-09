package mod.universalmobwar.mixin;

import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Prevents warlord minions from attacking or damaging their master or fellow minions.
 */
@Mixin(MobEntity.class)
public abstract class WarlordMinionProtectionMixin {
    
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$preventMinionInfighting(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        
        if (target == null) return;
        
        // Check if this mob is a minion using the static map
        UUID masterUuid = MobWarlordEntity.getMasterUuid(self.getUuid());
        if (masterUuid == null) return; // Not a minion
        
        // Don't target the warlord master
        if (target instanceof MobWarlordEntity warlord && warlord.getUuid().equals(masterUuid)) {
            ci.cancel();
            return;
        }
        
        // Don't target fellow minions (same master)
        if (target instanceof MobEntity) {
            UUID targetMasterUuid = MobWarlordEntity.getMasterUuid(target.getUuid());
            if (targetMasterUuid != null && targetMasterUuid.equals(masterUuid)) {
                ci.cancel();
            }
        }
    }
}

/**
 * Prevents minions from damaging their warlord or fellow minions.
 */
@Mixin(LivingEntity.class)
abstract class WarlordDamageProtectionMixin {
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$preventMinionDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity)(Object)this;
        Entity attacker = source.getAttacker();
        
        if (attacker == null || !(attacker instanceof MobEntity attackerMob)) return;
        
        // Check if attacker is a minion using the static map
        UUID masterUuid = MobWarlordEntity.getMasterUuid(attackerMob.getUuid());
        if (masterUuid == null) return; // Not a minion
        
        // Prevent damage to warlord master
        if (victim instanceof MobWarlordEntity warlord && warlord.getUuid().equals(masterUuid)) {
            cir.setReturnValue(false);
            return;
        }
        
        // Prevent damage to fellow minions (same master)
        if (victim instanceof MobEntity) {
            UUID victimMasterUuid = MobWarlordEntity.getMasterUuid(victim.getUuid());
            if (victimMasterUuid != null && victimMasterUuid.equals(masterUuid)) {
                cir.setReturnValue(false);
            }
        }
    }
}

