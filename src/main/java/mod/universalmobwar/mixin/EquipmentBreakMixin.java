package mod.universalmobwar.mixin;

import mod.universalmobwar.system.UpgradeSystem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class EquipmentBreakMixin {

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);
    @Shadow public abstract void equipStack(EquipmentSlot slot, ItemStack stack);

    @Inject(method = "sendEquipmentBreakStatus", at = @At("HEAD"))
    private void onEquipmentBreak(ItemStack stack, EquipmentSlot slot, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Only apply to Mobs (not players)
        if (!(entity instanceof MobEntity)) return;
        if (stack.isEmpty()) return;
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        // Check if it's a tier item and downgrade if possible
        ItemStack replacement = getDowngradedItem(itemId);
        if (replacement != null) {
            // We need to schedule the replacement because the current stack is about to be set to empty/shrunk by the caller
            // However, sendEquipmentBreakStatus is usually called BEFORE the stack is actually removed/shrunk in some contexts,
            // or as a callback. If we set it now, the caller might overwrite it.
            // But usually, the caller (ItemStack.damage) will shrink the stack AFTER this callback.
            // So if we replace it here, the caller might shrink OUR new stack.
            // Wait, ItemStack.damage(amount, entity, slot, breakCallback)
            // 1. Decrement damage.
            // 2. If damage > max:
            //    a. Call breakCallback (this method)
            //    b. Shrink stack (count--)
            //    c. Set damage to 0
            // So if we replace the stack in the slot NOW, the `this` stack in ItemStack.damage is still the OLD stack object.
            // But `entity.equipStack` changes the reference in the inventory.
            // The `ItemStack.damage` method is operating on the OLD stack object reference.
            // So if we equip a NEW stack, the old stack continues to die (count--), but it's no longer in the inventory.
            // So the new stack should be safe!
            this.equipStack(slot, replacement);
        }
    }
    
    private ItemStack getDowngradedItem(String itemId) {
        // Check Swords
        ItemStack sword = checkTierList(itemId, UpgradeSystem.SWORD_TIERS);
        if (sword != null) return sword;
        
        // Check Gold Swords (Piglins)
        ItemStack goldSword = checkTierList(itemId, UpgradeSystem.GOLD_SWORD_TIERS);
        if (goldSword != null) return goldSword;
        
        // Check Axes
        ItemStack axe = checkTierList(itemId, UpgradeSystem.AXE_TIERS);
        if (axe != null) return axe;
        
        // Check Gold Axes (Brutes)
        ItemStack goldAxe = checkTierList(itemId, UpgradeSystem.GOLD_AXE_TIERS);
        if (goldAxe != null) return goldAxe;
        
        // Check Armor
        ItemStack helm = checkTierList(itemId, UpgradeSystem.HELMET_TIERS);
        if (helm != null) return helm;
        
        ItemStack chest = checkTierList(itemId, UpgradeSystem.CHEST_TIERS);
        if (chest != null) return chest;
        
        ItemStack legs = checkTierList(itemId, UpgradeSystem.LEGS_TIERS);
        if (legs != null) return legs;
        
        ItemStack boots = checkTierList(itemId, UpgradeSystem.BOOTS_TIERS);
        if (boots != null) return boots;
        
        return null;
    }
    
    private ItemStack checkTierList(String id, List<String> tiers) {
        int index = tiers.indexOf(id);
        if (index > 0) {
            // Found, return previous tier
            String prevId = tiers.get(index - 1);
            return new ItemStack(Registries.ITEM.get(Identifier.of(prevId.split(":")[0], prevId.split(":")[1])));
        }
        return null;
    }
}
