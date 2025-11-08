package mod.universalmobwar.goal;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.AllianceSystem;
import mod.universalmobwar.system.EvolutionSystem;
import mod.universalmobwar.util.TargetingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Enhanced targeting goal with alliance and evolution support.
 * Priorities:
 * 1. Continue attacking current target if valid
 * 2. Help allied mobs being attacked
 * 3. Find nearest valid target
 */
public class UniversalTargetGoal extends TrackTargetGoal {

	private final MobEntity mob;
	private final BooleanSupplier modEnabledSupplier;
	private final BooleanSupplier ignoreSameSpeciesSupplier;
	private final BooleanSupplier targetPlayersSupplier;
	private final BooleanSupplier neutralAggressiveSupplier;
	private final BooleanSupplier allianceEnabledSupplier;
	private final BooleanSupplier evolutionEnabledSupplier;
	private final DoubleSupplier rangeMultiplierSupplier;
	private LivingEntity candidate;
	private long lastAllianceCheck = 0;

	public UniversalTargetGoal(
		MobEntity mob, 
		BooleanSupplier modEnabledSupplier, 
		BooleanSupplier ignoreSameSpeciesSupplier,
		BooleanSupplier targetPlayersSupplier,
		BooleanSupplier neutralAggressiveSupplier,
		BooleanSupplier allianceEnabledSupplier,
		BooleanSupplier evolutionEnabledSupplier,
		DoubleSupplier rangeMultiplierSupplier
	) {
		super(mob, false, false);
		this.mob = mob;
		this.modEnabledSupplier = modEnabledSupplier;
		this.ignoreSameSpeciesSupplier = ignoreSameSpeciesSupplier;
		this.targetPlayersSupplier = targetPlayersSupplier;
		this.neutralAggressiveSupplier = neutralAggressiveSupplier;
		this.allianceEnabledSupplier = allianceEnabledSupplier;
		this.evolutionEnabledSupplier = evolutionEnabledSupplier;
		this.rangeMultiplierSupplier = rangeMultiplierSupplier;
	}

	@Override
	public boolean canStart() {
		// Check if mod is enabled
		if (!modEnabledSupplier.getAsBoolean()) return false;

		if (!(mob.getWorld() instanceof ServerWorld serverWorld)) return false;
		if (!mob.isAlive()) return false;

		// Priority 1: If we already have a living target that's still valid, stick with it
		LivingEntity currentTarget = mob.getTarget();
		if (currentTarget != null && currentTarget.isAlive()) {
			MobWarData data = MobWarData.get(mob);
			// Prioritize current target if we've been fighting them recently
			if (data.getCurrentTarget() != null && data.getCurrentTarget().equals(currentTarget.getUuid())) {
				if (data.getTimeSinceTargetChange() < 5000) { // 5 seconds
					return false; // Keep current target
				}
			}
		}

		final boolean ignoreSame = ignoreSameSpeciesSupplier.getAsBoolean();
		final boolean targetPlayers = targetPlayersSupplier.getAsBoolean();
		final boolean allianceEnabled = allianceEnabledSupplier.getAsBoolean();
		final double rangeMultiplier = rangeMultiplierSupplier.getAsDouble();

		double followRange = 16.0;
		EntityAttributeInstance inst = mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
		if (inst != null) followRange = inst.getValue();
		followRange *= rangeMultiplier;

		// Priority 2: Check if we should help an ally
		if (allianceEnabled) {
			LivingEntity friendToHelp = AllianceSystem.findFriendToHelp(mob, followRange);
			if (friendToHelp != null) {
				this.candidate = friendToHelp;
				return true;
			}
		}

		// Priority 3: Find nearest valid target
		this.candidate = TargetingUtil.findNearestValidTarget(mob, followRange, ignoreSame, targetPlayers);
		return this.candidate != null;
	}

	@Override
	public void start() {
		if (this.candidate != null) {
			mob.setTarget(this.candidate);
			
			// Track target change
			MobWarData data = MobWarData.get(mob);
			data.setCurrentTarget(this.candidate.getUuid());
			MobWarData.save(mob, data);
			
			// Update alliances if enabled
			if (allianceEnabledSupplier.getAsBoolean() && mob.getWorld() instanceof ServerWorld serverWorld) {
				AllianceSystem.updateAlliances(mob, serverWorld);
			}
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
		final boolean targetPlayers = targetPlayersSupplier.getAsBoolean();
		
		// Update alliances periodically
		long currentTime = System.currentTimeMillis();
		if (allianceEnabledSupplier.getAsBoolean() && currentTime - lastAllianceCheck > 2000) {
			if (mob.getWorld() instanceof ServerWorld serverWorld) {
				AllianceSystem.updateAlliances(mob, serverWorld);
				AllianceSystem.cleanupExpiredAlliances(mob);
			}
			lastAllianceCheck = currentTime;
		}
		
		return TargetingUtil.isValidTarget(mob, t, ignoreSame, targetPlayers);
	}

	@Override
	public void stop() {
		this.candidate = null;
		
		// Don't clear target tracking - keep it for priority system
		super.stop();
	}
}

