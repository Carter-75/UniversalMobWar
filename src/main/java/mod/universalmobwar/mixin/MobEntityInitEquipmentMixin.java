package mod.universalmobwar.mixin;

import mod.universalmobwar.config.ModConfig;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent vanilla from assigning random equipment so mobs only earn gear via the scaling system.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityInitEquipmentMixin {

    @Inject(method = "initEquipment", at = @At("HEAD"), cancellable = true)
    private void universalmobwar$disableVanillaEquipment(ServerWorldAccess world, LocalDifficulty difficulty, CallbackInfo ci) {
        if (!ModConfig.getInstance().modEnabled) {
            return;
        }

        MobEntity self = (MobEntity)(Object)this;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                self.equipStack(slot, ItemStack.EMPTY);
                self.setEquipmentDropChance(slot, 0.0f);
            }
        }

        ci.cancel();
    }
}
