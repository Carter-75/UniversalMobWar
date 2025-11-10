package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks same-species retaliation when the gamerule is ON.
 * Damage still applies normally. We just prevent setting the attacker as a revenge target.
 */
@Mixin(LivingEntity.class)
public abstract class MobRevengeBlockerMixin {

	@Inject(method = "setAttacker", at = @At("HEAD"), cancellable = true)
	private void universalmobwar$preventSameSpeciesRevenge(LivingEntity attacker, CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;

		if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
		if (!(self instanceof MobEntity selfMob)) return;
		if (attacker == null) return;

		boolean ignoreSame = serverWorld.getServer().getGameRules().getBoolean(UniversalMobWarMod.IGNORE_SAME_SPECIES_RULE);
		if (!ignoreSame) return;

		// If same species, prevent revenge targeting by cancelling the setAttacker call
		if (attacker.getType() == selfMob.getType()) {
			// Cancel setting this attacker - prevents revenge targeting
			ci.cancel();
		}
	}
}

