package mod.universalmobwar.mixin.mob;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Bee - Neutral mob
 * 
 * Bee from skilltree.txt:
 * - type: neutral
 * - weapon: none
 * - armor: none
 * - shield: false
 * - trees: []
 * 
 * Available upgrades (hostile_and_neutral_potion_effects since neutral):
 * - healing: 5 levels (cost 1-5) -> Regen effects with on-damage triggers
 * - health_boost: 10 levels (cost 2-11) -> +2 to +20 HP
 * - resistance: 3 levels (cost 4, 6, 8) -> Resistance I, II, II+Fire Resist
 * - strength: 4 levels (cost 3, 5, 7, 9) -> Strength I-IV
 * - speed: 3 levels (cost 6, 9, 12) -> Speed I-III
 * - invisibility_on_hit: 5 levels (cost 8, 12, 16, 20, 25) -> Chance-based invis on hit
 * 
 * NO item masteries (no equipment)
 * NO skill trees
 */
@Mixin(BeeEntity.class)
public abstract class BeeMixin {

    // ========== NBT Data Storage ==========
    @Unique
    private static final String NBT_KEY = "UniversalMobWar_Bee";
    
    @Unique
    private int totalPoints = 0;
    
    @Unique
    private int spentPoints = 0;
    
    // Hostile/Neutral potion effect levels
    @Unique
    private int healingLevel = 0;        // 0-5
    
    @Unique
    private int healthBoostLevel = 0;    // 0-10
    
    @Unique
    private int resistanceLevel = 0;     // 0-3
    
    @Unique
    private int strengthLevel = 0;       // 0-4
    
    @Unique
    private int speedLevel = 0;          // 0-3
    
    @Unique
    private int invisOnHitLevel = 0;     // 0-5
    
    // Cooldown tracking for on-damage effects
    @Unique
    private long lastDamageRegenTrigger = 0;
    
    @Unique
    private long lastInvisibilityTrigger = 0;
    
    // Performance tracking - last tick when upgrade was attempted
    @Unique
    private long lastUpdateTick = 0;

    // ========== Point System ==========
    
    @Unique
    private int calculateWorldAgePoints(World world) {
        long worldTime = world.getTime();
        int worldDays = (int) (worldTime / 24000L);
        
        double points = 0.0;
        for (int day = 1; day <= worldDays; day++) {
            if (day <= 10) {
                points += 0.1;
            } else if (day <= 15) {
                points += 0.5;
            } else if (day <= 20) {
                points += 1.0;
            } else if (day <= 25) {
                points += 1.5;
            } else if (day <= 30) {
                points += 3.0;
            } else {
                points += 5.0;
            }
        }
        return (int) points;
    }
    
    @Unique
    private int getBudget() {
        return totalPoints - spentPoints;
    }

    // ========== Upgrade Costs (from skilltree.txt hostile_and_neutral_potion_effects) ==========
    
    @Unique
    private int getHealingCost(int level) {
        // Level 1-5: cost 1, 2, 3, 4, 5
        if (level >= 1 && level <= 5) {
            return level;
        }
        return Integer.MAX_VALUE;
    }
    
    @Unique
    private int getHealthBoostCost(int level) {
        // Level 1-10: cost 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
        if (level >= 1 && level <= 10) {
            return level + 1;
        }
        return Integer.MAX_VALUE;
    }
    
    @Unique
    private int getResistanceCost(int level) {
        // Level 1: 4, Level 2: 6, Level 3: 8
        return switch (level) {
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 8;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getStrengthCost(int level) {
        // Level 1: 3, Level 2: 5, Level 3: 7, Level 4: 9
        return switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 9;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getSpeedCost(int level) {
        // Level 1: 6, Level 2: 9, Level 3: 12
        return switch (level) {
            case 1 -> 6;
            case 2 -> 9;
            case 3 -> 12;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getInvisOnHitCost(int level) {
        // Level 1: 8, Level 2: 12, Level 3: 16, Level 4: 20, Level 5: 25
        return switch (level) {
            case 1 -> 8;
            case 2 -> 12;
            case 3 -> 16;
            case 4 -> 20;
            case 5 -> 25;
            default -> Integer.MAX_VALUE;
        };
    }

    // ========== Spending Logic ==========
    
    @Unique
    private void spendPoints(BeeEntity self) {
        java.util.Random random = new java.util.Random();
        
        while (getBudget() > 0) {
            java.util.List<Runnable> affordableUpgrades = new java.util.ArrayList<>();
            
            // Check healing
            int nextHealLevel = healingLevel + 1;
            if (nextHealLevel <= 5 && getBudget() >= getHealingCost(nextHealLevel)) {
                final int cost = getHealingCost(nextHealLevel);
                affordableUpgrades.add(() -> {
                    healingLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check health boost
            int nextHealthLevel = healthBoostLevel + 1;
            if (nextHealthLevel <= 10 && getBudget() >= getHealthBoostCost(nextHealthLevel)) {
                final int cost = getHealthBoostCost(nextHealthLevel);
                affordableUpgrades.add(() -> {
                    healthBoostLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check resistance
            int nextResistLevel = resistanceLevel + 1;
            if (nextResistLevel <= 3 && getBudget() >= getResistanceCost(nextResistLevel)) {
                final int cost = getResistanceCost(nextResistLevel);
                affordableUpgrades.add(() -> {
                    resistanceLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check strength
            int nextStrengthLevel = strengthLevel + 1;
            if (nextStrengthLevel <= 4 && getBudget() >= getStrengthCost(nextStrengthLevel)) {
                final int cost = getStrengthCost(nextStrengthLevel);
                affordableUpgrades.add(() -> {
                    strengthLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check speed
            int nextSpeedLevel = speedLevel + 1;
            if (nextSpeedLevel <= 3 && getBudget() >= getSpeedCost(nextSpeedLevel)) {
                final int cost = getSpeedCost(nextSpeedLevel);
                affordableUpgrades.add(() -> {
                    speedLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check invisibility on hit
            int nextInvisLevel = invisOnHitLevel + 1;
            if (nextInvisLevel <= 5 && getBudget() >= getInvisOnHitCost(nextInvisLevel)) {
                final int cost = getInvisOnHitCost(nextInvisLevel);
                affordableUpgrades.add(() -> {
                    invisOnHitLevel++;
                    spentPoints += cost;
                });
            }
            
            if (affordableUpgrades.isEmpty()) {
                break;
            }
            
            // 20% chance to save and stop
            if (random.nextDouble() < 0.20) {
                break;
            }
            
            // 80% chance - buy random affordable upgrade
            int index = random.nextInt(affordableUpgrades.size());
            affordableUpgrades.get(index).run();
        }
        
        applyEffects(self);
    }
    
    // ========== Effect Application ==========
    
    @Unique
    private void applyEffects(BeeEntity self) {
        // Healing: Levels 1-2 give permanent regen, 3-5 add on-damage triggers (handled in damage event)
        if (healingLevel >= 1) {
            int regenAmplifier = Math.min(healingLevel - 1, 1); // Regen I or II max for permanent
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION,
                Integer.MAX_VALUE,
                regenAmplifier,
                false, false, true
            ));
        }
        
        // Health Boost: +2 HP per level
        if (healthBoostLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HEALTH_BOOST,
                Integer.MAX_VALUE,
                healthBoostLevel - 1,
                false, false, true
            ));
        }
        
        // Resistance: Level 1-2 give Resistance I/II, Level 3 adds Fire Resistance
        if (resistanceLevel > 0) {
            int resistAmplifier = Math.min(resistanceLevel - 1, 1); // Resistance I or II
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                Integer.MAX_VALUE,
                resistAmplifier,
                false, false, true
            ));
            
            if (resistanceLevel >= 3) {
                self.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE,
                    Integer.MAX_VALUE,
                    0,
                    false, false, true
                ));
            }
        }
        
        // Strength
        if (strengthLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.STRENGTH,
                Integer.MAX_VALUE,
                strengthLevel - 1,
                false, false, true
            ));
        }
        
        // Speed
        if (speedLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                Integer.MAX_VALUE,
                speedLevel - 1,
                false, false, true
            ));
        }
    }

    // ========== NBT Persistence ==========
    
    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound modData = new NbtCompound();
        modData.putInt("totalPoints", totalPoints);
        modData.putInt("spentPoints", spentPoints);
        modData.putInt("healingLevel", healingLevel);
        modData.putInt("healthBoostLevel", healthBoostLevel);
        modData.putInt("resistanceLevel", resistanceLevel);
        modData.putInt("strengthLevel", strengthLevel);
        modData.putInt("speedLevel", speedLevel);
        modData.putInt("invisOnHitLevel", invisOnHitLevel);
        modData.putLong("lastDamageRegenTrigger", lastDamageRegenTrigger);
        modData.putLong("lastInvisibilityTrigger", lastInvisibilityTrigger);
        modData.putLong("lastUpdateTick", lastUpdateTick);
        nbt.put(NBT_KEY, modData);
    }
    
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(NBT_KEY)) {
            NbtCompound modData = nbt.getCompound(NBT_KEY);
            totalPoints = modData.getInt("totalPoints");
            spentPoints = modData.getInt("spentPoints");
            healingLevel = modData.getInt("healingLevel");
            healthBoostLevel = modData.getInt("healthBoostLevel");
            resistanceLevel = modData.getInt("resistanceLevel");
            strengthLevel = modData.getInt("strengthLevel");
            speedLevel = modData.getInt("speedLevel");
            invisOnHitLevel = modData.getInt("invisOnHitLevel");
            lastDamageRegenTrigger = modData.getLong("lastDamageRegenTrigger");
            lastInvisibilityTrigger = modData.getLong("lastInvisibilityTrigger");
            lastUpdateTick = modData.getLong("lastUpdateTick");
            
            applyEffects((BeeEntity) (Object) this);
        }
    }

    // ========== Tick Handling ==========
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        BeeEntity self = (BeeEntity) (Object) this;
        World world = self.getWorld();
        
        if (world.isClient()) {
            return;
        }
        
        long currentTick = world.getTime();
        int newTotalPoints = calculateWorldAgePoints(world);
        
        // Trigger spending: on new points, or if >1 day (24000 ticks) since last attempt
        boolean shouldAttemptSpend = (newTotalPoints > totalPoints) || 
                                      (currentTick - lastUpdateTick > 24000L);
        
        if (newTotalPoints > totalPoints) {
            totalPoints = newTotalPoints;
        }
        
        if (shouldAttemptSpend && getBudget() > 0) {
            lastUpdateTick = currentTick;
            spendPoints(self);
        }
    }
}
