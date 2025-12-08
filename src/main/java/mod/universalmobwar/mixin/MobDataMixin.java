package mod.universalmobwar.mixin;

import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobDataMixin extends LivingEntity implements IMobWarDataHolder {

    @Unique
    private MobWarData universalMobWarData = new MobWarData();

    protected MobDataMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public MobWarData getMobWarData() {
        return universalMobWarData;
    }

    @Override
    public void setMobWarData(MobWarData data) {
        this.universalMobWarData = data;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        if (universalMobWarData != null) {
            nbt.put("UniversalMobWarData", universalMobWarData.writeNbt());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("UniversalMobWarData")) {
            universalMobWarData = new MobWarData();
            universalMobWarData.readNbt(nbt.getCompound("UniversalMobWarData"));
        }
    }
    
        // Strip ALL equipment from every mob immediately on spawn (before upgrades)
        @Inject(method = "initialize", at = @At("HEAD"))
        private void universalmobwar$stripAllEquipmentOnSpawn(
            net.minecraft.world.ServerWorldAccess world,
            net.minecraft.world.LocalDifficulty difficulty,
            net.minecraft.entity.SpawnReason spawnReason,
            net.minecraft.entity.EntityData entityData,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.entity.EntityData> cir
        ) {
            MobEntity self = (MobEntity)(Object)this;
            // Remove armor (head, chest, legs, boots)
            self.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.item.ItemStack.EMPTY);
            self.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.item.ItemStack.EMPTY);
            self.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.item.ItemStack.EMPTY);
            self.equipStack(net.minecraft.entity.EquipmentSlot.FEET, net.minecraft.item.ItemStack.EMPTY);
            // Remove weapons/tools
            self.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, net.minecraft.item.ItemStack.EMPTY);
            self.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, net.minecraft.item.ItemStack.EMPTY);
        }
}
