package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MobXpDropScalingMixin {

    @Inject(method = "getXpToDrop", at = @At("RETURN"), cancellable = true)
    private void universalmobwar$scaleXpDrop(ServerWorld world, @Nullable Entity attacker, CallbackInfoReturnable<Integer> cir) {
        UniversalMobWarMod.runSafely("MobXpDropScalingMixin#scaleXpDrop", () -> {
            ModConfig config = ModConfig.getInstance();
            if (!config.isScalingActive() || !config.scaleMobXpDropsWithSpentPoints) {
                return;
            }

            if (!(attacker instanceof PlayerEntity)) {
                return;
            }

            if (!((Object) this instanceof MobEntity mob)) {
                return;
            }

            MobWarData data;
            if (mob instanceof IMobWarDataHolder holder) {
                data = holder.getMobWarData();
            } else {
                data = MobWarData.get(mob);
            }

            if (data == null) {
                return;
            }

            int baseXp = cir.getReturnValue();
            if (baseXp <= 0) {
                return;
            }

            int stepSize = Math.max(1, config.spentPointsPerXpBonusStep);
            double spentPoints = data.getSpentPoints();
            if (spentPoints <= 0.0) {
                return;
            }

            double bonusPerStep = Math.max(0.0, config.xpBonusPercentPerStep / 100.0);
            double multiplier = 1.0 + (spentPoints / stepSize) * bonusPerStep;

            int scaledXp;
            if (multiplier <= 1.0) {
                scaledXp = baseXp;
            } else {
                double scaled = baseXp * multiplier;
                if (scaled >= Integer.MAX_VALUE) {
                    scaledXp = Integer.MAX_VALUE;
                } else {
                    scaledXp = (int) Math.floor(scaled);
                }
            }

            // Safety: XP to drop should never be negative.
            scaledXp = MathHelper.clamp(scaledXp, 0, Integer.MAX_VALUE);
            if (scaledXp != baseXp) {
                cir.setReturnValue(scaledXp);
            }
        });
    }
}
