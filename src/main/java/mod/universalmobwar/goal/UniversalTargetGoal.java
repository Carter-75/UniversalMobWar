package mod.universalmobwar.goal;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.entity.MobWarlordEntity;
import mod.universalmobwar.system.AllianceSystem;
import mod.universalmobwar.util.TargetingUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

//
public class UniversalTargetGoal extends TrackTargetGoal {

	private final MobEntity mob;
	private final BooleanSupplier modEnabledSupplier;
	private final BooleanSupplier ignoreSameSpeciesSupplier;
	private final BooleanSupplier targetPlayersSupplier;
	private final BooleanSupplier allianceEnabledSupplier;
	private final DoubleSupplier rangeMultiplierSupplier;
	private LivingEntity candidate;
	private long lastAllianceCheck = 0;
	private int updateCooldown = 0; // Staggered update system for performance
	private int allianceCheckInterval = 2000; // Dynamic alliance check interval
	private final long allianceCheckOffset; // UUID-based offset for staggering alliance updates

	public UniversalTargetGoal(
		MobEntity mob,
		BooleanSupplier modEnabledSupplier,
		BooleanSupplier ignoreSameSpeciesSupplier,
		BooleanSupplier targetPlayersSupplier,
		BooleanSupplier allianceEnabledSupplier,
		DoubleSupplier rangeMultiplierSupplier
	) {
		super(mob, false, false);
		this.mob = mob;
		this.modEnabledSupplier = modEnabledSupplier;
		this.ignoreSameSpeciesSupplier = ignoreSameSpeciesSupplier;
		this.targetPlayersSupplier = targetPlayersSupplier;
		this.allianceEnabledSupplier = allianceEnabledSupplier;
		this.rangeMultiplierSupplier = rangeMultiplierSupplier;
		// OPTIMIZATION: Calculate UUID-based offset for staggering alliance updates (0-2000ms)
		this.allianceCheckOffset = Math.abs(mob.getUuid().hashCode()) % 2000L;
	}

	@Override
	public boolean canStart() {
		// OPTIMIZATION: Stagger updates - not all mobs search every tick
		if (updateCooldown > 0) {
			updateCooldown--;
			// Still keep current target if valid
			LivingEntity currentTarget = mob.getTarget();
			if (currentTarget != null && currentTarget.isAlive()) {
				return false;
			}
			return false;
		}
		// Spread updates over 10 ticks using UUID-based offset
		updateCooldown = 10 + (Math.abs(mob.getUuid().hashCode()) % 10);

		// Check if mod is enabled
		if (!modEnabledSupplier.getAsBoolean()) return false;
		if (!(mob.getWorld() instanceof ServerWorld)) return false;
		if (!mob.isAlive()) return false;

		// CRITICAL: Check if this mob is a warlord minion
		UUID masterUuid = MobWarlordEntity.getMasterUuid(mob.getUuid());
		if (masterUuid != null) {
			// This is a minion - clear any invalid targets
			LivingEntity currentTarget = mob.getTarget();
			if (currentTarget != null) {
				// Don't target the warlord master
				if (currentTarget instanceof MobWarlordEntity warlord && warlord.getUuid().equals(masterUuid)) {
					mob.setTarget(null);
					return false;
				}
				// Don't target fellow minions (same master)
				if (currentTarget instanceof MobEntity targetMob) {
					UUID targetMasterUuid = MobWarlordEntity.getMasterUuid(targetMob.getUuid());
					if (targetMasterUuid != null && targetMasterUuid.equals(masterUuid)) {
						mob.setTarget(null);
						return false;
					}
				}
			}
		}

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

		// OPTIMIZATION: Longer cooldown if no target found
		if (this.candidate == null) {
			updateCooldown = 40; // Wait 2 seconds before searching again
		}

		return this.candidate != null;
	}

	@Override
	public void start() {
		if (this.candidate != null) {
			// CRITICAL: Double-check if this mob is a warlord minion before setting target
			UUID masterUuid = MobWarlordEntity.getMasterUuid(mob.getUuid());
			if (masterUuid != null) {
				// This is a minion - validate the candidate
				// Don't target the warlord master
				if (this.candidate instanceof MobWarlordEntity warlord && warlord.getUuid().equals(masterUuid)) {
					this.candidate = null;
					return;
				}
				// Don't target fellow minions (same master)
				if (this.candidate instanceof MobEntity targetMob) {
					UUID targetMasterUuid = MobWarlordEntity.getMasterUuid(targetMob.getUuid());
					if (targetMasterUuid != null && targetMasterUuid.equals(masterUuid)) {
						this.candidate = null;
						return;
					}
				}
			}
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
		// OPTIMIZATION: Dynamic alliance check interval based on combat state with UUID offset
		long currentTime = System.currentTimeMillis();
		long timeSinceLastCheck = currentTime - lastAllianceCheck;
		// OPTIMIZATION: Use offset to stagger alliance updates across all mobs
		if (allianceEnabledSupplier.getAsBoolean() && timeSinceLastCheck > (allianceCheckInterval + allianceCheckOffset)) {
			if (mob.getWorld() instanceof ServerWorld serverWorld) {
				// Check if in active combat (recently attacked)
				boolean inCombat = (currentTime - mob.getLastAttackTime() < 3000);
				// Adjust interval: 2s in combat, 4s when calm
				allianceCheckInterval = inCombat ? 2000 : 4000;
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

