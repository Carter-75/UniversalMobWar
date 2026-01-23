package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.entity.MobWarlordEntity;
import mod.universalmobwar.util.SummonerTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * UNIVERSAL summoner-summoned protection system.
 * Prevents ALL summoned mobs from targeting their summoners.
 * Works for:
 * - Warlord minions (can't target warlord or fellow minions)
 * - Evoker's Vexes (can't target the evoker)
 * - Illusioner's duplicates (can't target the illusioner)
 * - Any modded mob that summons others
 * 
 * NOTE: Summoned mobs CAN still take splash damage/AoE damage but will NEVER retaliate.
 */
@Mixin(MobEntity.class)
public abstract class WarlordMinionProtectionMixin {
    
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$preventSummonedTargeting(LivingEntity target, CallbackInfo ci) {
        UniversalMobWarMod.runSafely("WarlordMinionProtectionMixin#preventSummonedTargeting", () -> {
            MobEntity self = (MobEntity)(Object)this;
            
            if (target == null) return;
            
            // === WARLORD MINION PROTECTION ===
            // Check if this mob is a warlord minion using the static map
            UUID warlordMasterUuid = MobWarlordEntity.getMasterUuid(self.getUuid());
            if (warlordMasterUuid != null) {
                // Don't target the warlord master (no retaliation)
                if (target instanceof MobWarlordEntity warlord && warlord.getUuid().equals(warlordMasterUuid)) {
                    ci.cancel();
                    return;
                }
                
                // Don't target fellow minions (no infighting, even if hit by splash damage)
                if (target instanceof MobEntity) {
                    UUID targetMasterUuid = MobWarlordEntity.getMasterUuid(target.getUuid());
                    if (targetMasterUuid != null && targetMasterUuid.equals(warlordMasterUuid)) {
                        ci.cancel();
                        return;
                    }
                }
            }
            
            // === UNIVERSAL SUMMONER PROTECTION ===
            // Check if this mob was summoned by ANY mob (Evoker, Illusioner, modded mobs, etc.)
            UUID summonerUuid = SummonerTracker.getSummoner(self.getUuid());
            if (summonerUuid != null && target.getUuid().equals(summonerUuid)) {
                // This summoned mob is trying to target its summoner - prevent it!
                ci.cancel();
            }
        });
    }
}
