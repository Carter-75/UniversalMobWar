package mod.universalmobwar.mixin;

import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.IMobWarDataHolder;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.system.ScalingSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Core mixin that handles MobWarData persistence for ALL mobs.
 * 
 * SCALING SYSTEM INTEGRATION:
 * This mixin calls ScalingSystem.processMobTick() which:
 *   1. Loads upgrade config from mob_configs/[mobname].json
 *   2. Calculates points from world age (daily_scaling)
 *   3. Spends points on upgrades (80% buy / 20% save)
 *   4. Applies effects (potions, equipment, abilities)
 * 
 * To add scaling for a new mob:
 *   Just create mob_configs/[mobname].json - no mixin needed!
 */
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
    
    /**
     * SCALING SYSTEM HOOK - Called every tick for ALL mobs
     * ScalingSystem handles everything:
     *   - Checks if mob has JSON config
     *   - Calculates points from world age
     *   - Spends points on upgrades
     *   - Applies effects
     * 
     * Mobs without a JSON config are simply skipped (no scaling).
     */
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void universalmobwar$onMobTick(CallbackInfo ci) {
        if (!ModConfig.getInstance().modEnabled) {
            return;
        }
        MobEntity self = (MobEntity)(Object)this;
        ScalingSystem.processMobTick(self, self.getWorld(), universalMobWarData);
    }

    @Inject(method = "tryAttack(Lnet/minecraft/entity/Entity;)Z", at = @At("TAIL"))
    private void universalmobwar$handleMeleeAbilities(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        MobEntity self = (MobEntity)(Object)this;
        if (universalMobWarData == null) {
            return;
        }

        ScalingSystem.handleMeleeAttackAbilities(self, universalMobWarData, livingTarget, self.getWorld().getTime());
    }
    
    // Strip ALL equipment from every mob immediately on spawn (AFTER vanilla equipment)
    // Using @At("RETURN") ensures we strip equipment AFTER Minecraft adds it in initEquipment()
    @Inject(method = "initialize", at = @At("RETURN"))
    private void universalmobwar$stripAllEquipmentOnSpawn(
        net.minecraft.world.ServerWorldAccess world,
        net.minecraft.world.LocalDifficulty difficulty,
        net.minecraft.entity.SpawnReason spawnReason,
        net.minecraft.entity.EntityData entityData,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.entity.EntityData> cir
    ) {
        MobEntity self = (MobEntity)(Object)this;
        
        // Tag spawn eggs so they aren't blocked by natural spawn blocker
        if (spawnReason == net.minecraft.entity.SpawnReason.SPAWN_EGG) {
            self.addCommandTag("umw_player_spawned");
        }
        
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
