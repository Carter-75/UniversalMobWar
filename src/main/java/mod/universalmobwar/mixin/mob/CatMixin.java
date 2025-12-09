package mod.universalmobwar.mixin.mob;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Standalone mixin for Cat - Passive mob
 * Uses MobWarData for persistence
 */
@Mixin(CatEntity.class)
public abstract class CatMixin {

    @Unique private static final String DATA_KEY = "cat_upgrades";
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
        CatEntity self = (CatEntity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;
        
        long currentTick = world.getTime();
        if (currentTick - lastProcessTick < 100) return;
        lastProcessTick = currentTick;
        
        MobWarData data = MobWarData.get((MobEntity) self);
        NbtCompound skillData = data.getSkillData();
        
        if (!skillData.contains(DATA_KEY)) {
            NbtCompound upgrades = new NbtCompound();
            upgrades.putInt("regeneration", 0);
            upgrades.putInt("resistance", 0);
            upgrades.putInt("health_boost", 0);
            skillData.put(DATA_KEY, upgrades);
        }
        
        int totalPoints = calculateWorldAgePoints(world);
        int budget = totalPoints - (int) data.getSpentPoints();
        NbtCompound upgrades = skillData.getCompound(DATA_KEY);
        
        if (budget > 0) {
            java.util.Random random = new java.util.Random();
            while (budget > 0) {
                java.util.List<Runnable> affordable = new java.util.ArrayList<>();
                
                int regenLvl = upgrades.getInt("regeneration");
                if (regenLvl < 3 && budget >= 3 + regenLvl) {
                    int cost = 3 + regenLvl;
                    affordable.add(() -> { upgrades.putInt("regeneration", regenLvl + 1); data.setSpentPoints(data.getSpentPoints() + cost); });
                }
                int resLvl = upgrades.getInt("resistance");
                if (resLvl < 1 && budget >= 5) {
                    affordable.add(() -> { upgrades.putInt("resistance", 1); data.setSpentPoints(data.getSpentPoints() + 5); });
                }
                int hpLvl = upgrades.getInt("health_boost");
                if (hpLvl < 3 && budget >= 4 + hpLvl) {
                    int cost = 4 + hpLvl;
                    affordable.add(() -> { upgrades.putInt("health_boost", hpLvl + 1); data.setSpentPoints(data.getSpentPoints() + cost); });
                }
                
                if (affordable.isEmpty() || random.nextDouble() < 0.20) break;
                affordable.get(random.nextInt(affordable.size())).run();
                budget = totalPoints - (int) data.getSpentPoints();
            }
        }
        
        skillData.put(DATA_KEY, upgrades);
        data.setSkillData(skillData);
        MobWarData.save((MobEntity) self, data);
        
        int regen = upgrades.getInt("regeneration");
        int resist = upgrades.getInt("resistance");
        int hp = upgrades.getInt("health_boost");
        if (regen > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, regen - 1, false, false, true));
        if (resist > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0, false, false, true));
        if (hp > 0) self.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 200, hp - 1, false, false, true));
    }
}
