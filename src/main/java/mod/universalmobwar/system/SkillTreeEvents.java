package mod.universalmobwar.system;

import mod.universalmobwar.data.MobWarData;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;

public class SkillTreeEvents {

    public static void register() {
        // Handle Melee Attacks (Zombie Hunger)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // This event is for PLAYERS attacking. We need mob attacks.
            // Fabric doesn't have a generic "MobAttackEvent".
            // We might need to rely on Mixins for this if we want it perfect.
            // However, we can check this in a different way or use a mixin.
            return ActionResult.PASS;
        });
    }

    // Called from Mixin or Event
    public static void onMobAttack(LivingEntity attacker, Entity target) {
        if (attacker.getWorld().isClient) return;
        if (!(attacker instanceof MobEntity mob)) return;
        if (!(target instanceof LivingEntity livingTarget)) return;

        MobWarData data = MobWarData.get(mob);
        
        // Zombie Tree: Infectious Bite
        int zLevel = data.getSkillData().getInt("z_level");
        if (zLevel > 0) {
            // Level 1: Hunger I (10s)
            // Level 2: Hunger II (10s)
            // Level 3: Hunger III (10s)
            int amplifier = zLevel - 1;
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, amplifier));
        }
    }

    // Called when a mob is hurt
    public static void onMobHurt(LivingEntity entity) {
        if (entity.getWorld().isClient) return;
        if (!(entity instanceof MobEntity mob)) return;
        
        MobWarData data = MobWarData.get(mob);
        
        // Zombie Tree: Horde Summon
        // Logic: 1(25% 1 extra) > 2(50%) > 3(75% 1 else 2) ...
        // This is complex logic from the txt. Let's implement a simplified version for stability first.
        // "summons 1-3 extra... 25% chance..."
        // We need to prevent infinite loops (summoned mobs summoning more).
        // We can check if the mob has a "summoned" tag or just limit it.
        
        if (data.getSkillData().contains("z_horde_summon")) { // We didn't add this key in EvolutionSystem yet
             // Placeholder for now
        }
    }

    // Called when a projectile is spawned
    public static void onProjectileSpawn(Entity entity) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof PersistentProjectileEntity arrow) {
            Entity owner = arrow.getOwner();
            if (owner instanceof MobEntity mob) {
                MobWarData data = MobWarData.get(mob);
                int proLevel = data.getSkillData().getInt("pro_level");
                
                // Piercing
                if (proLevel >= 1) {
                    arrow.setPierceLevel((byte) proLevel);
                }
                
                // Multishot (Simulated)
                // If level > 1, spawn extra arrows with slight spread
                if (proLevel >= 2) {
                    // Prevent infinite recursion if we spawn arrows that trigger this event
                    // But we are in the event handler, so spawning new ones might trigger it again.
                    // We should be careful.
                }
            }
        }
    }
}
