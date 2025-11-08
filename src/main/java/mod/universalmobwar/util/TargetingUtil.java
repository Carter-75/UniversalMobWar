package mod.universalmobwar.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class TargetingUtil {

	private TargetingUtil() {}

	public static LivingEntity findNearestValidTarget(MobEntity self, double range, boolean ignoreSameSpecies, boolean targetPlayers) {
		Box box = self.getBoundingBox().expand(range, range, range);
		Predicate<LivingEntity> filter = target -> isValidTarget(self, target, ignoreSameSpecies, targetPlayers);

		List<LivingEntity> list = self.getWorld().getEntitiesByClass(LivingEntity.class, box, filter::test);
		if (list.isEmpty()) return null;

		list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(self)));
		return list.getFirst();
	}

	public static boolean isValidTarget(MobEntity self, LivingEntity target, boolean ignoreSameSpecies, boolean targetPlayers) {
		if (target == null || target == self) return false;
		if (!target.isAlive()) return false;

		// Basic visibility helps reduce erratic chasing; remove if you want through-walls aggro.
		if (!self.canSee(target)) return false;

		// Players: check if player targeting is enabled
		if (target instanceof ServerPlayerEntity player) {
			if (player.isSpectator() || player.isCreative()) return false;
			if (!targetPlayers) return false; // New: respect player immunity toggle
		}

		// Same-species filter (default ON)
		if (ignoreSameSpecies && target.getType() == self.getType()) return false;

		return true;
	}
}

