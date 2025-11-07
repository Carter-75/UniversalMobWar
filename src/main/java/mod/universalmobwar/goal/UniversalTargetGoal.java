package mod.universalmobwar.goal;

import mod.universalmobwar.util.TargetingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.function.BooleanSupplier;

/**
 * Finds and sets the nearest valid target according to rules:
 * - living only
 * - not self
 * - Survival players allowed; creative/spectator ignored
 * - if ignoreSameSpecies = true => target.type != self.type
 * If nothing valid is found, goal does nothing and vanilla AI continues.
 */
public class UniversalTargetGoal extends TrackTargetGoal {

	private final MobEntity mob;
	private final BooleanSupplier modEnabledSupplier;
	private final BooleanSupplier ignoreSameSpeciesSupplier;
	private LivingEntity candidate;

	public UniversalTargetGoal(MobEntity mob, BooleanSupplier modEnabledSupplier, BooleanSupplier ignoreSameSpeciesSupplier) {
		super(mob, false, false);
		this.mob = mob;
		this.modEnabledSupplier = modEnabledSupplier;
		this.ignoreSameSpeciesSupplier = ignoreSameSpeciesSupplier;
	}

	@Override
	public boolean canStart() {
		// Check if mod is enabled
		if (!modEnabledSupplier.getAsBoolean()) return false;

		if (!(mob.getWorld() instanceof ServerWorld)) return false;
		if (!mob.isAlive()) return false;

		// If we already have a living target, let shouldContinue handle it
		if (mob.getTarget() != null && mob.getTarget().isAlive()) return false;

		final boolean ignoreSame = ignoreSameSpeciesSupplier.getAsBoolean();

		double followRange = 16.0;
		EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
		if (inst != null) followRange = inst.getValue();

		this.candidate = TargetingUtil.findNearestValidTarget(mob, followRange, ignoreSame);
		return this.candidate != null;
	}

	@Override
	public void start() {
		if (this.candidate != null) {
			mob.setTarget(this.candidate);
		}
		super.start();
	}

	@Override
	public boolean shouldContinue() {
		// Check if mod is still enabled
		if (!modEnabledSupplier.getAsBoolean()) return false;

		LivingEntity t = mob.getTarget();
		if (t == null || !t.isAlive()) return false;

		final boolean ignoreSame = ignoreSameSpeciesSupplier.getAsBoolean();
		return TargetingUtil.isValidTarget(mob, t, ignoreSame);
	}

	@Override
	public void stop() {
		this.candidate = null;
		super.stop();
	}
}

