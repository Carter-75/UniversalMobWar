package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.entity.Entity;
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

	@Inject(method = "onAttacking", at = @At("HEAD"))
	private void universalmobwar$preventSameSpeciesRevenge(Entity attacker, CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;

		if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
		if (!(self instanceof MobEntity selfMob)) return;
		if (!(attacker instanceof LivingEntity attackerLiving)) return;

		boolean ignoreSame = serverWorld.getServer().getGameRules().getBoolean(UniversalMobWarMod.IGNORE_SAME_SPECIES_RULE);
		if (!ignoreSame) return;

		// If same species, prevent revenge targeting
		if (attackerLiving.getType() == selfMob.getType()) {
			// Do not mark this attacker for revenge
			self.setAttacker(null);
			self.setAttacking(null);
		}
	}
}

