package mod.universalmobwar.mixin;

import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Prevents warlord minions from TARGETING their master or fellow minions.
 * NOTE: Minions CAN still damage each other (splash damage, AoE, etc.) but will NEVER retaliate.
 * This ensures friendly fire is possible but minions never intentionally attack each other.
 */
@Mixin(MobEntity.class)
public abstract class WarlordMinionProtectionMixin {
    
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$preventMinionTargeting(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        
        if (target == null) return;
        
        // Check if this mob is a minion using the static map
        UUID masterUuid = MobWarlordEntity.getMasterUuid(self.getUuid());
        if (masterUuid == null) return; // Not a minion
        
        // Don't target the warlord master (no retaliation)
        if (target instanceof MobWarlordEntity warlord && warlord.getUuid().equals(masterUuid)) {
            ci.cancel();
            return;
        }
        
        // Don't target fellow minions (no infighting, even if hit by splash damage)
        if (target instanceof MobEntity) {
            UUID targetMasterUuid = MobWarlordEntity.getMasterUuid(target.getUuid());
            if (targetMasterUuid != null && targetMasterUuid.equals(masterUuid)) {
                ci.cancel();
            }
        }
    }
}
