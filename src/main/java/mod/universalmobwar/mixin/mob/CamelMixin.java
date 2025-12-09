package mod.universalmobwar.mixin.mob;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Camel - Passive mob
 * 
 * Camel from skilltree.txt:
 * - type: passive
 * - weapon: none
 * - armor: none
 * - shield: false
 * - trees: []
 * 
 * Available upgrades (passive_potion_effects only, since no equipment):
 * - regeneration: 3 levels (cost 3, 4, 5) -> Permanent Regen I, II, III
 * - resistance: 1 level (cost 5) -> Permanent Resistance I
 * - health_boost: 3 levels (cost 4, 5, 6) -> +2, +4, +6 HP total
 * 
 * NO item masteries (no equipment)
 * NO hostile/neutral potion effects (passive mob)
 * NO skill trees
 */
@Mixin(CamelEntity.class)
public abstract class CamelMixin {

    // ========== NBT Data Storage ==========
    @Unique
    private static final String NBT_KEY = "UniversalMobWar_Camel";
    
    @Unique
    private int totalPoints = 0;
    
    @Unique
    private int spentPoints = 0;
    
    // Passive potion effect levels
    @Unique
    private int regenerationLevel = 0;  // 0-3
    
    @Unique
    private int resistanceLevel = 0;    // 0-1
    
    @Unique
    private int healthBoostLevel = 0;   // 0-3
    
    // Tracking for triggers
    @Unique
    private long lastUpdateTick = 0;

    // ========== Point System ==========
    
    /**
     * Calculate points earned from world age
     * Daily scaling from skilltree.txt:
     * - Days 0-10: 0.1 points/day
     * - Days 11-15: 0.5 points/day
     * - Days 16-20: 1.0 points/day
     * - Days 21-25: 1.5 points/day
     * - Days 26-30: 3.0 points/day
     * - Days 31+: 5.0 points/day
     */
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

    // ========== Upgrade Costs (from skilltree.txt passive_potion_effects) ==========
    
    @Unique
    private int getRegenerationCost(int level) {
        // Level 1: 3, Level 2: 4, Level 3: 5
        return switch (level) {
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            default -> Integer.MAX_VALUE;
        };
    }
    
    @Unique
    private int getResistanceCost(int level) {
        // Level 1: 5 (only 1 level available)
        return level == 1 ? 5 : Integer.MAX_VALUE;
    }
    
    @Unique
    private int getHealthBoostCost(int level) {
        // Level 1: 4, Level 2: 5, Level 3: 6
        return switch (level) {
            case 1 -> 4;
            case 2 -> 5;
            case 3 -> 6;
            default -> Integer.MAX_VALUE;
        };
    }

    // ========== Spending Logic ==========
    
    /**
     * Spending logic from skilltree.txt:
     * - 80% chance to buy an affordable upgrade
     * - 20% chance to save points and stop spending
     * - Continues until save roll succeeds or nothing affordable remains
     */
    @Unique
    private void spendPoints(CamelEntity self) {
        java.util.Random random = new java.util.Random();
        
        while (getBudget() > 0) {
            // Collect all affordable upgrades
            java.util.List<Runnable> affordableUpgrades = new java.util.ArrayList<>();
            
            // Check regeneration (next level)
            int nextRegenLevel = regenerationLevel + 1;
            if (nextRegenLevel <= 3 && getBudget() >= getRegenerationCost(nextRegenLevel)) {
                final int cost = getRegenerationCost(nextRegenLevel);
                affordableUpgrades.add(() -> {
                    regenerationLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check resistance (next level)
            int nextResistLevel = resistanceLevel + 1;
            if (nextResistLevel <= 1 && getBudget() >= getResistanceCost(nextResistLevel)) {
                final int cost = getResistanceCost(nextResistLevel);
                affordableUpgrades.add(() -> {
                    resistanceLevel++;
                    spentPoints += cost;
                });
            }
            
            // Check health boost (next level)
            int nextHealthLevel = healthBoostLevel + 1;
            if (nextHealthLevel <= 3 && getBudget() >= getHealthBoostCost(nextHealthLevel)) {
                final int cost = getHealthBoostCost(nextHealthLevel);
                affordableUpgrades.add(() -> {
                    healthBoostLevel++;
                    spentPoints += cost;
                });
            }
            
            // No affordable upgrades - stop spending
            if (affordableUpgrades.isEmpty()) {
                break;
            }
            
            // 20% chance to save and stop
            if (random.nextDouble() < 0.20) {
                break;
            }
            
            // 80% chance - pick and buy a random affordable upgrade
            int index = random.nextInt(affordableUpgrades.size());
            affordableUpgrades.get(index).run();
        }
        
        // Apply all effects in a single batch
        applyEffects(self);
    }
    
    // ========== Effect Application ==========
    
    @Unique
    private void applyEffects(CamelEntity self) {
        // Apply regeneration based on level
        if (regenerationLevel > 0) {
            // Permanent effect (very long duration, refreshed on tick)
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION,
                Integer.MAX_VALUE,
                regenerationLevel - 1,  // amplifier is 0-indexed
                false, false, true
            ));
        }
        
        // Apply resistance based on level
        if (resistanceLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                Integer.MAX_VALUE,
                resistanceLevel - 1,
                false, false, true
            ));
        }
        
        // Apply health boost based on level (+2 HP per level)
        if (healthBoostLevel > 0) {
            self.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HEALTH_BOOST,
                Integer.MAX_VALUE,
                healthBoostLevel - 1,
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
        modData.putInt("regenerationLevel", regenerationLevel);
        modData.putInt("resistanceLevel", resistanceLevel);
        modData.putInt("healthBoostLevel", healthBoostLevel);
        modData.putLong("lastUpdateTick", lastUpdateTick);
        nbt.put(NBT_KEY, modData);
    }
    
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(NBT_KEY)) {
            NbtCompound modData = nbt.getCompound(NBT_KEY);
            totalPoints = modData.getInt("totalPoints");
            spentPoints = modData.getInt("spentPoints");
            regenerationLevel = modData.getInt("regenerationLevel");
            resistanceLevel = modData.getInt("resistanceLevel");
            healthBoostLevel = modData.getInt("healthBoostLevel");
            lastUpdateTick = modData.getLong("lastUpdateTick");
            
            // Re-apply effects after loading
            applyEffects((CamelEntity) (Object) this);
        }
    }

    // ========== Spawn & Tick Handling ==========
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        CamelEntity self = (CamelEntity) (Object) this;
        World world = self.getWorld();
        
        if (world.isClient()) {
            return;
        }
        
        long currentTick = world.getTime();
        int newTotalPoints = calculateWorldAgePoints(world);
        
        // Spending triggers:
        // 1. Points increased (new day)
        // 2. More than 1 day (24000 ticks) since last update attempt
        boolean shouldSpend = false;
        
        if (newTotalPoints > totalPoints) {
            totalPoints = newTotalPoints;
            shouldSpend = true;
        } else if (currentTick - lastUpdateTick > 24000L) {
            shouldSpend = true;
        }
        
        if (shouldSpend && getBudget() > 0) {
            lastUpdateTick = currentTick;
            spendPoints(self);
        }
    }
}
