package mod.universalmobwar.mixin;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

/**
 * Prevent vanilla from assigning random equipment so mobs only earn gear via the scaling system.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityInitEquipmentMixin {

    private static final EnumSet<EquipmentSlot> CLEARED_SLOTS = EnumSet.of(
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    );

    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$disableVanillaEquipment(Random random, LocalDifficulty difficulty, CallbackInfo ci) {
        UniversalMobWarMod.runSafely("MobEntityInitEquipmentMixin#disableVanillaEquipment", () -> {
            if (!ModConfig.getInstance().modEnabled) {
                return;
            }

            MobEntity self = (MobEntity)(Object)this;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!CLEARED_SLOTS.contains(slot)) {
                    continue;
                }
                self.equipStack(slot, ItemStack.EMPTY);
                self.setEquipmentDropChance(slot, 0.0f);
            }

            ci.cancel();
        });
    }
}
