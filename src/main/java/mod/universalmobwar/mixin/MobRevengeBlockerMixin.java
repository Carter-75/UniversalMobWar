package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks same-species retaliation when the gamerule is ON.
 * Uses handleAttack method which is called when an entity is attacked.
 * This prevents same-species mobs from adding each other to their revenge target lists.
 */
@Mixin(Entity.class)
public abstract class MobRevengeBlockerMixin {

	@Inject(method = "handleAttack", at = @At("HEAD"), cancellable = true)
	private void universalmobwar$preventSameSpeciesRevenge(Entity attacker, CallbackInfoReturnable<Boolean> cir) {
		UniversalMobWarMod.runSafely("MobRevengeBlockerMixin#preventSameSpeciesRevenge", () -> {
			Entity self = (Entity)(Object)this;

			// Only apply to mobs in server worlds
			if (self.getWorld().isClient()) return;
			if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
			if (!(self instanceof MobEntity selfMob)) return;
			if (attacker == null) return;

			// Check if ignore same species gamerule is enabled
			boolean ignoreSame = ModConfig.getInstance().ignoreSameSpecies;
			if (!ignoreSame) return;

			// If same species, prevent revenge targeting by returning false (don't handle the attack normally)
			if (attacker.getType() == selfMob.getType()) {
				// Return false to prevent the entity from handling the attack
				// This prevents them from adding each other to revenge target lists
				cir.setReturnValue(false);
			}
		});
	}
}

