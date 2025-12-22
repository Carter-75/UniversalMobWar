package mod.universalmobwar.goal;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;

import java.util.EnumSet;

/**
 * Prevents infinite fights by applying escalating buffs to the attacker
 * and debuffs to the victim as combat drags on.
 */
public class StalemateBreakerGoal extends Goal {

    private final MobEntity mob;
    private static final long STAGE_1_THRESHOLD = 15000; // 15 seconds
    private static final long STAGE_2_THRESHOLD = 30000; // 30 seconds
    private static final long STAGE_3_THRESHOLD = 45000; // 45 seconds

    public StalemateBreakerGoal(MobEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.noneOf(Goal.Control.class)); // Runs in parallel with everything
    }

    @Override
    public boolean canStart() {
        LivingEntity target = mob.getTarget();
        return target instanceof MobEntity && target.isAlive();
    }

    @Override
    public void tick() {
        LivingEntity potentialTarget = mob.getTarget();
        if (!(potentialTarget instanceof MobEntity target) || !target.isAlive()) {
            return; // Players (and other non-mobs) should never trigger stalemate buffs
        }

        MobWarData data = MobWarData.get(mob);
        // Ensure we are tracking the current target
        if (data.getCurrentTarget() == null || !data.getCurrentTarget().equals(target.getUuid())) {
            data.setCurrentTarget(target.getUuid());
            MobWarData.save(mob, data);
            return;
        }

        long combatDuration = data.getTimeSinceTargetChange();

        // Stage 1: Berserk (15s+)
        if (combatDuration > STAGE_1_THRESHOLD) {
            if (!mob.hasStatusEffect(StatusEffects.STRENGTH)) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1)); // Strength II
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));    // Speed II
            }
        }

        // Stage 2: Hyper-Lethality (30s+)
        if (combatDuration > STAGE_2_THRESHOLD) {
            if (!mob.hasStatusEffect(StatusEffects.STRENGTH) || mob.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() < 3) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 3)); // Strength IV
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 2));    // Haste III
            }
        }

        // Stage 3: Sudden Death (45s+)
        if (combatDuration > STAGE_3_THRESHOLD) {
            if (!mob.hasStatusEffect(StatusEffects.STRENGTH) || mob.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() < 9) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 9)); // Strength X (One shot territory)
            }
            // Wither the target
            if (mob.squaredDistanceTo(target) < 16) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 6));
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0)); // No hiding
            }
        }
    }
}
