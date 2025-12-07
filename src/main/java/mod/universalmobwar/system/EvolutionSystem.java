package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.data.PowerProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * THE ULTIMATE PROGRESSION SYSTEM v2.0
 * Implements the complex skill tree system requested by the user.
 */
public class EvolutionSystem {
    
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of("universalmobwar", "health_bonus");

    public static void onMobSpawn(MobEntity mob, ServerWorld world) {
        if (!mod.universalmobwar.config.ModConfig.getInstance().scalingEnabled) return;

        MobWarData data = MobWarData.get(mob);
        
        // Calculate Skill Points based on Day
        long day = world.getTimeOfDay() / 24000L;
        double dayPoints = calculateTotalSkillPoints(day) * mod.universalmobwar.config.ModConfig.getInstance().dayScalingMultiplier;
        
        // Calculate Skill Points based on Kills
        // We assume 1 kill = 1 point * multiplier (configurable)
        // Day based and kill based together
        double killPoints = data.getKillCount() * mod.universalmobwar.config.ModConfig.getInstance().killScalingMultiplier;
        
        double totalPoints = dayPoints + killPoints;
        
        // Update points if total has increased (due to day or kills)
        PowerProfile profile = data.getPowerProfile();
        if (profile == null) {
            profile = new PowerProfile();
        }

        if (totalPoints > data.getSkillPoints() || profile.totalPoints == 0) {
            data.setSkillPoints(totalPoints);
            
            profile.totalPoints = totalPoints;
            // Optional debug logging
            try {
                if (mod.universalmobwar.config.ModConfig.getInstance().debugLogging && mob.getWorld() instanceof ServerWorld sw) {
                    System.out.println("[UMW] Mob: " + mob.getType().getTranslationKey() + " points set to " + totalPoints + " (day="+day+", kills="+data.getKillCount()+")");
                }
            } catch (Exception e) {
                // swallow logging failures
            }
            
            // Initialize base stats if not set
            if (profile.baseHealth == 0) {
                profile.baseHealth = mob.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
                profile.baseDamage = mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                // Default to 0 if attribute missing (e.g. passive mobs)
                if (profile.baseDamage == 0 && mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) == null) {
                    profile.baseDamage = 0;
                }
            }
            
            // Determine Archetype/Categories
            profile.categories = ArchetypeClassifier.getMobCategories(mob);
            profile.archetype = ArchetypeClassifier.detectArchetype(mob);
            
            final PowerProfile finalProfile = profile;
            
            // Apply Upgrades
            // v2.1: We now apply upgrades incrementally in MobEntity.tick() via Mixin
            // This allows for visual progression and "saving" logic.
            // We just set the target points here.
            
            // Initialize profile with base stats if needed
            if (profile.specialSkills.isEmpty()) {
                 // First time init
                 UpgradeSystem.saveStateToProfile(new UpgradeSystem.SimState(), profile);
            }
            
            data.setSkillData(profile.writeNbt());
            MobWarData.save(mob, data);
        }
    }

    public static void onMobKill(MobEntity killer, LivingEntity victim) {
        MobWarData data = MobWarData.get(killer);
        data.addKill();
        MobWarData.save(killer, data);
        
        // Trigger re-evaluation of skills immediately upon kill
        if (killer.getWorld() instanceof ServerWorld serverWorld) {
            onMobSpawn(killer, serverWorld);
        }
    }
    
    private static double calculateTotalSkillPoints(long day) {
        double points = 0;
        
        // Day 0-10: 0.1 per day
        long period1 = Math.min(day, 10);
        points += period1 * 0.1;
        
        // Day 11-15: 0.5 per day
        if (day > 10) {
            long period2 = Math.min(day - 10, 5);
            points += period2 * 0.5;
        }
        
        // Day 16-20: 1.0 per day
        if (day > 15) {
            long period3 = Math.min(day - 15, 5);
            points += period3 * 1.0;
        }
        
        // Day 21-25: 1.5 per day
        if (day > 20) {
            long period4 = Math.min(day - 20, 5);
            points += period4 * 1.5;
        }
        
        // Day 26-30: 3.0 per day
        if (day > 25) {
            long period5 = Math.min(day - 25, 5);
            points += period5 * 3.0;
        }
        
        // Day 31+: 5.0 per day
        if (day > 30) {
            long period6 = day - 30;
            points += period6 * 5.0;
        }
        
        return points;
    }
    
    public static void cleanup() {}
}
