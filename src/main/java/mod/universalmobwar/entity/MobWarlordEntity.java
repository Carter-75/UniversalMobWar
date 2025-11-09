package mod.universalmobwar.entity;

import mod.universalmobwar.UniversalMobWarMod;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * The Mob Warlord - A giant witch boss that spawns and commands armies of mobs.
 * All spawned minions are loyal to the warlord and never attack it or each other.
 */
public class MobWarlordEntity extends HostileEntity {
    
    private static final TrackedData<Integer> MINION_COUNT = DataTracker.registerData(MobWarlordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    private final ServerBossBar bossBar;
    private final Set<UUID> minionUuids = new HashSet<>();
    private int summonCooldown = 0;
    private int attackCooldown = 0;
    
    private static final int MAX_MINIONS = 20;
    private static final int SUMMON_COOLDOWN = 100; // 5 seconds
    private static final int ATTACK_COOLDOWN = 40; // 2 seconds
    
    public MobWarlordEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 500; // Massive XP drop
        this.bossBar = new ServerBossBar(
            Text.literal("🔮 Mob Warlord 🔮").styled(style -> style.withColor(Formatting.DARK_PURPLE).withBold(true)),
            BossBar.Color.PURPLE,
            BossBar.Style.NOTCHED_10
        );
    }
    
    @Override
    public Text getName() {
        return Text.literal("Mob Warlord").styled(style -> style.withColor(Formatting.DARK_PURPLE).withBold(true));
    }
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MobWarlordAttackGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false,
            entity -> entity instanceof MobEntity mob && !isMinionOf(mob)));
    }
    
    public static DefaultAttributeContainer.Builder createMobWarlordAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 1500.0) // 750 hearts!
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35) // Normal/fast speed
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
            .add(EntityAttributes.GENERIC_ARMOR, 10.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MINION_COUNT, 0);
    }
    
    @Override
    public void tick() {
        // CRITICAL: Safety checks before calling super.tick()
        // This prevents crashes during chunk loading / world initialization
        if (this.getWorld() == null) return;
        if (this.getWorld().isClient) {
            super.tick();
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            super.tick();
            return;
        }
        
        // Additional safety: ensure entity is fully initialized
        if (this.age < 2) {
            super.tick();
            return; // Skip first few ticks to ensure world is ready
        }
        
        super.tick();
        
        // Update boss bar safely
        try {
            if (this.bossBar != null) {
                this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            }
        } catch (Exception e) {
            // Ignore boss bar errors during initialization
        }
        
        // Cooldowns
        if (summonCooldown > 0) summonCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        
        // Clean up dead minions
        cleanupDeadMinions();
        
        // Auto-summon when low on minions or low health
        if (summonCooldown == 0) {
            int currentMinions = minionUuids.size();
            float healthPercent = this.getHealth() / this.getMaxHealth();
            
            // More aggressive summoning when hurt
            boolean shouldSummon = currentMinions < MAX_MINIONS && (
                currentMinions < 5 || 
                (healthPercent < 0.75f && currentMinions < 10) ||
                (healthPercent < 0.5f && currentMinions < 15)
            );
            
            if (shouldSummon && this.getTarget() != null) {
                summonMinions(healthPercent < 0.5f ? 3 : healthPercent < 0.75f ? 2 : 1);
                summonCooldown = SUMMON_COOLDOWN;
            }
        }
        
        // Particle effects (skip if world not ready)
        if (this.age % 20 == 0 && this.age > 20) {
            spawnParticles();
        }
    }
    
    /**
     * Summons random hostile/neutral mobs to fight for the warlord.
     */
    private void summonMinions(int count) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        // Check if neutral mobs should be aggressive
        boolean neutralAggressive = serverWorld.getGameRules().getBoolean(UniversalMobWarMod.NEUTRAL_MOBS_AGGRESSIVE_RULE);
        
        for (int i = 0; i < count && minionUuids.size() < MAX_MINIONS; i++) {
            EntityType<?> minionType = getRandomMobType(neutralAggressive);
            if (minionType == null) continue;
            
            // Spawn near the warlord
            Vec3d spawnPos = this.getPos().add(
                (this.random.nextDouble() - 0.5) * 6,
                1,
                (this.random.nextDouble() - 0.5) * 6
            );
            
            Entity entity = minionType.create(serverWorld);
            if (entity instanceof MobEntity minion) {
                minion.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, this.random.nextFloat() * 360f, 0);
                minion.initialize(serverWorld, serverWorld.getLocalDifficulty(minion.getBlockPos()), SpawnReason.MOB_SUMMONED, null);
                
                // Mark as minion with NBT
                NbtCompound minionNbt = minion.writeNbt(new NbtCompound());
                minionNbt.putUuid("WarlordMaster", this.getUuid());
                minionNbt.putBoolean("PersistenceRequired", true); // Prevent despawn
                minion.readNbt(minionNbt);
                
                // Set target to warlord's target
                if (this.getTarget() != null) {
                    minion.setTarget(this.getTarget());
                }
                
                // Ensure minion doesn't despawn
                minion.setPersistent();
                
                serverWorld.spawnEntity(minion);
                minionUuids.add(minion.getUuid());
                
                // Spawn effects
                try {
                    serverWorld.spawnParticles(ParticleTypes.PORTAL, 
                        spawnPos.x, spawnPos.y, spawnPos.z, 
                        20, 0.5, 0.5, 0.5, 0.1);
                } catch (Exception e) {
                    // Silently ignore particle errors
                }
                this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);
            }
        }
        
        this.dataTracker.set(MINION_COUNT, minionUuids.size());
    }
    
    /**
     * Gets a random hostile or neutral mob type.
     */
    private EntityType<?> getRandomMobType(boolean includeNeutral) {
        List<EntityType<?>> possibleMobs = new ArrayList<>();
        
        // Hostile mobs (always available)
        possibleMobs.add(EntityType.ZOMBIE);
        possibleMobs.add(EntityType.SKELETON);
        possibleMobs.add(EntityType.CREEPER);
        possibleMobs.add(EntityType.SPIDER);
        possibleMobs.add(EntityType.CAVE_SPIDER);
        possibleMobs.add(EntityType.WITCH);
        possibleMobs.add(EntityType.BLAZE);
        possibleMobs.add(EntityType.ENDERMAN);
        possibleMobs.add(EntityType.ZOMBIFIED_PIGLIN);
        possibleMobs.add(EntityType.PIGLIN);
        possibleMobs.add(EntityType.HOGLIN);
        possibleMobs.add(EntityType.RAVAGER);
        possibleMobs.add(EntityType.VINDICATOR);
        possibleMobs.add(EntityType.PILLAGER);
        possibleMobs.add(EntityType.VEX);
        
        // Neutral mobs (if gamerule is on)
        if (includeNeutral) {
            possibleMobs.add(EntityType.IRON_GOLEM);
            possibleMobs.add(EntityType.WOLF);
            possibleMobs.add(EntityType.POLAR_BEAR);
            possibleMobs.add(EntityType.PANDA);
            possibleMobs.add(EntityType.BEE);
        }
        
        // Could also scan for modded mobs here
        // For now, we'll stick to vanilla
        
        return possibleMobs.get(this.random.nextInt(possibleMobs.size()));
    }
    
    /**
     * Cleans up dead or despawned minions.
     */
    private void cleanupDeadMinions() {
        if (this.getWorld() == null || !(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (this.age < 2) return; // Skip during initialization
        
        try {
            minionUuids.removeIf(uuid -> {
                Entity entity = serverWorld.getEntity(uuid);
                return entity == null || !entity.isAlive();
            });
            
            if (this.dataTracker != null) {
                this.dataTracker.set(MINION_COUNT, minionUuids.size());
            }
        } catch (Exception e) {
            // Silently ignore errors during world loading
        }
    }
    
    /**
     * Checks if a mob is a minion of this warlord.
     */
    public boolean isMinionOf(MobEntity mob) {
        NbtCompound nbt = new NbtCompound();
        mob.writeNbt(nbt);
        return nbt.containsUuid("WarlordMaster") && nbt.getUuid("WarlordMaster").equals(this.getUuid());
    }
    
    /**
     * Performs a ranged potion attack.
     */
    public void performRangedAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        
        Vec3d vec3d = target.getEyePos().subtract(this.getEyePos());
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        
        // Throw harmful potion (using 1.21 component system)
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        potionEntity.setItem(potion);
        
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 1.0f);
        
        attackCooldown = ATTACK_COOLDOWN;
    }
    
    /**
     * Performs a melee attack with knockback.
     */
    public void performMeleeAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        
        this.tryAttack(target);
        
        // Knockback effect
        Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
        target.takeKnockback(2.0, -direction.x, -direction.z);
        
        this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
        attackCooldown = ATTACK_COOLDOWN / 2; // Faster melee cooldown
    }
    
    private void spawnParticles() {
        if (this.getWorld() == null) return;
        if (this.age < 20) return; // Wait for full initialization
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        try {
            // Additional null check for serverWorld's particle manager
            if (serverWorld.getServer() != null && !serverWorld.isClient) {
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                    this.getX(), this.getY() + 2.0, this.getZ(),
                    5, 0.5, 0.5, 0.5, 0.05);
            }
        } catch (Exception e) {
            // Silently ignore particle errors during initialization
        }
    }
    
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        
        try {
            if (this.bossBar != null && player != null) {
                this.bossBar.addPlayer(player);
            }
        } catch (Exception e) {
            // Silently ignore errors during world loading
        }
    }
    
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        
        try {
            if (this.bossBar != null && player != null) {
                this.bossBar.removePlayer(player);
            }
        } catch (Exception e) {
            // Silently ignore errors during world unloading
        }
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        
        long[] minionArray = new long[minionUuids.size() * 2];
        int i = 0;
        for (UUID uuid : minionUuids) {
            minionArray[i++] = uuid.getMostSignificantBits();
            minionArray[i++] = uuid.getLeastSignificantBits();
        }
        nbt.putLongArray("Minions", minionArray);
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        
        if (nbt.contains("Minions")) {
            long[] minionArray = nbt.getLongArray("Minions");
            minionUuids.clear();
            for (int i = 0; i < minionArray.length; i += 2) {
                UUID uuid = new UUID(minionArray[i], minionArray[i + 1]);
                minionUuids.add(uuid);
            }
        }
    }
    
    @Override
    protected void mobTick() {
        super.mobTick();
        
        // Update boss bar safely
        try {
            if (this.bossBar != null && this.age > 2) {
                this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            }
        } catch (Exception e) {
            // Silently ignore errors during initialization
        }
    }
    
    @Override
    public boolean cannotDespawn() {
        return true; // Boss never despawns
    }
    
    @Override
    public boolean isFireImmune() {
        return true; // Boss is fire immune
    }
    
    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        
        // Kill all minions when warlord dies
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            for (UUID minionUuid : new HashSet<>(minionUuids)) {
                Entity entity = serverWorld.getEntity(minionUuid);
                if (entity instanceof LivingEntity minion && minion.isAlive()) {
                    minion.damage(minion.getDamageSources().magic(), Float.MAX_VALUE);
                    
                    // Death particles
                    try {
                        serverWorld.spawnParticles(ParticleTypes.POOF,
                            minion.getX(), minion.getY() + 1.0, minion.getZ(),
                            10, 0.3, 0.3, 0.3, 0.05);
                    } catch (Exception e) {
                        // Silently ignore particle errors
                    }
                }
            }
            minionUuids.clear();
            
            // Victory message
            serverWorld.getPlayers().forEach(player -> {
                player.sendMessage(Text.literal("The Mob Warlord has been defeated! Their army crumbles!")
                    .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
            });
        }
    }
    
    /**
     * Custom AI goal for the Warlord's combat behavior.
     */
    private static class MobWarlordAttackGoal extends Goal {
        private final MobWarlordEntity warlord;
        private LivingEntity target;
        private int updateCountdownTicks;
        
        public MobWarlordAttackGoal(MobWarlordEntity warlord) {
            this.warlord = warlord;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            LivingEntity target = this.warlord.getTarget();
            if (target != null && target.isAlive()) {
                this.target = target;
                return true;
            }
            return false;
        }
        
        @Override
        public void start() {
            this.updateCountdownTicks = 0;
        }
        
        @Override
        public void stop() {
            this.target = null;
        }
        
        @Override
        public void tick() {
            if (this.target == null) return;
            
            this.warlord.getLookControl().lookAt(this.target, 30.0f, 30.0f);
            double distance = this.warlord.squaredDistanceTo(this.target);
            
            --this.updateCountdownTicks;
            
            if (distance <= 6.0) {
                // Melee range
                if (this.updateCountdownTicks <= 0) {
                    this.updateCountdownTicks = 20;
                    this.warlord.performMeleeAttack(this.target);
                }
            } else if (distance <= 256.0) {
                // Ranged attack
                if (this.updateCountdownTicks <= 0) {
                    this.updateCountdownTicks = 40;
                    this.warlord.performRangedAttack(this.target);
                }
                
                // Move closer if too far
                if (distance > 64.0) {
                    this.warlord.getNavigation().startMovingTo(this.target, 1.0);
                }
            }
        }
    }
}

