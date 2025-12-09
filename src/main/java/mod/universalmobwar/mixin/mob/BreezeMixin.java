package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Breeze - Hostile mob
 * Uses MobWarData for persistence
 */
@Mixin(BreezeEntity.class)
public abstract class BreezeMixin {

    @Unique private static final String DATA_KEY = "breeze_upgrades";
    @Unique private long lastProcessTick = 0;

    @Unique
    private int calculateWorldAgePoints(World world) {
        int worldDays = (int) (world.getTime() / 24000L);
        double points = 0.0;
        for (int day = 1; day <= worldDays; day++) {
            if (day <= 10) points += 0.1;
            else if (day <= 15) points += 0.5;
            else if (day <= 20) points += 1.0;
            else if (day <= 25) points += 1.5;
            else if (day <= 30) points += 3.0;
            else points += 5.0;
        }
        return (int) points;
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        BreezeEntity self = (BreezeEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        long currentTick = world.getTime();
        if (currentTick - lastProcessTick < 100) return;
        lastProcessTick = currentTick;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        NbtCompound skillData = data.getSkillData();
        
        if (!skillData.contains(DATA_KEY)) {
            NbtCompound upgrades = new NbtCompound();
            upgrades.putInt("healing", 0);
            upgrades.putInt("health_boost", 0);
            upgrades.putInt("resistance", 0);
            upgrades.putInt("strength", 0);
            upgrades.putInt("speed", 0);
            skillData.put(DATA_KEY, upgrades);
        }
        
        int totalPoints = calculateWorldAgePoints(world);
        int budget = totalPoints - (int) data.getSpentPoints();
        NbtCompound upgrades = skillData.getCompound(DATA_KEY);
        
        if (budget > 0) {
            java.util.Random random = new java.util.Random();
            while (budget > 0) {
                java.util.List<Runnable> affordable = new java.util.ArrayList<>();
                
                int healLvl = upgrades.getInt("healing");
                if (healLvl < 5 && budget >= healLvl + 1) {
                    int cost = healLvl + 1;
                    affordable.add(() -> { upgrades.putInt("healing", healLvl + 1); data.setSpentPoints(data.getSpentPoints() + cost); });
                }
                int hpLvl = upgrades.getInt("health_boost");
                if (hpLvl < 10 && budget >= hpLvl + 2) {
                    int cost = hpLvl + 2;
                    affordable.add(() -> { upgrades.putInt("health_boost", hpLvl + 1); data.setSpentPoints(data.getSpentPoints() + cost); });
                }
                int resLvl = upgrades.getInt("resistance");
                int resCost = resLvl == 0 ? 4 : (resLvl == 1 ? 6 : (resLvl == 2 ? 8 : Integer.MAX_VALUE));
                if (resLvl < 3 && budget >= resCost) {
                    affordable.add(() -> { upgrades.putInt("resistance", resLvl + 1); data.setSpentPoints(data.getSpentPoints() + resCost); });
                }
                int strLvl = upgrades.getInt("strength");
                int strCost = strLvl == 0 ? 3 : (strLvl == 1 ? 5 : (strLvl == 2 ? 7 : (strLvl == 3 ? 9 : Integer.MAX_VALUE)));
                if (strLvl < 4 && budget >= strCost) {
                    affordable.add(() -> { upgrades.putInt("strength", strLvl + 1); data.setSpentPoints(data.getSpentPoints() + strCost); });
                }
                int spdLvl = upgrades.getInt("speed");
                int spdCost = spdLvl == 0 ? 6 : (spdLvl == 1 ? 9 : (spdLvl == 2 ? 12 : Integer.MAX_VALUE));
                if (spdLvl < 3 && budget >= spdCost) {
                    affordable.add(() -> { upgrades.putInt("speed", spdLvl + 1); data.setSpentPoints(data.getSpentPoints() + spdCost); });
                }
                
                if (affordable.isEmpty() || random.nextDouble() < 0.20) break;
                affordable.get(random.nextInt(affordable.size())).run();
                budget = totalPoints - (int) data.getSpentPoints();
            }
        }
        
        skillData.put(DATA_KEY, upgrades);
        data.setSkillData(skillData);
        MobWarData.save((MobEntity) self, data);
        
        int heal = upgrades.getInt("healing");
        int hp = upgrades.getInt("health_boost");
        int res = upgrades.getInt("resistance");
        int str = upgrades.getInt("strength");
        int spd = upgrades.getInt("speed");
        
        if (heal > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, Math.min(heal - 1, 1), false, false, true));
        if (hp > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 200, hp - 1, false, false, true));
        if (res > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, Math.min(res - 1, 1), false, false, true));
        if (str > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, str - 1, false, false, true));
        if (spd > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, spd - 1, false, false, true));
    }
}
