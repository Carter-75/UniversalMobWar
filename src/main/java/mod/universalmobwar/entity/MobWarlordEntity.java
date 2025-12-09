package mod.universalmobwar.entity;

import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.mixin.MobEntityAccessor;
import mod.universalmobwar.util.SummonerTracker;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Angerable;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import mod.universalmobwar.config.ModConfig;
import java.util.ArrayList;

import java.util.*;

/**
 * WARLORD SYSTEM - Independent Module
 * 
 * The Mob Warlord - A giant boss that summons and commands armies of mobs.
 * All spawned minions are loyal to the warlord and never attack it or each other.
 * Uses HostileEntity for maximum compatibility with Iris Shaders and rendering mods.
 * 
 * This system works independently and can be enabled/disabled via:
 * - Config: warlordEnabled
 * 
 * Configurable options:
 * - warlordSpawnChance (default 25%)
 * - warlordMinRaidLevel (default 3)
 * - warlordMinionCount (default 20)
 * - warlordHealthMultiplierPercent (default 300%)
 * - warlordDamageMultiplierPercent (default 200%)
 * 
 * Does NOT depend on: Targeting, Alliance, or Scaling systems
 */
public class MobWarlordEntity extends HostileEntity {
    
    private static final TrackedData<Integer> MINION_COUNT = DataTracker.registerData(MobWarlordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_RAID_BOSS = DataTracker.registerData(MobWarlordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Static map to track which mobs are minions of which warlord (thread-safe)
    private static final Map<UUID, UUID> MINION_TO_WARLORD = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track betrayers - minions that attacked other minions
    private final Set<UUID> betrayers = new HashSet<>();
    
    private final ServerBossBar bossBar;
    private final Set<UUID> minionUuids = new HashSet<>();
    private int attackCooldown = 0;
    
    // OPTIMIZATION: UUID-based tick offsets for staggering operations across multiple Warlords
    private final int particleOffset; // 0-30 tick offset for particle drawing
    private final int cleanupOffset; // 0-100 tick offset for cleanup cycles
    private final int validationOffset; // 0-60 tick offset for validation
    private final int summonOffset; // 0-40 tick offset for summoning checks
    
    // Get max minions from config (default 20)
    private static int getMaxMinions() {
        return ModConfig.getInstance().warlordMinionCount;
    }
    private static final int SUMMON_COOLDOWN = 40; // 2 seconds - FASTER ally spawning!
    private static final int ATTACK_COOLDOWN = 40; // 2 seconds
    private static final int CLEANUP_COOLDOWN = 100; // 5 seconds - performance optimization for large modpacks
    private static final int PARTICLE_COOLDOWN = 30; // 1.5 seconds - optimized particle connections
    
    public MobWarlordEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 500; // Massive XP drop
        this.bossBar = new ServerBossBar(
            Text.literal("🔮 Mob Warlord 🔮").styled(style -> style.withColor(Formatting.DARK_PURPLE).withBold(true)),
            BossBar.Color.PURPLE,
            BossBar.Style.NOTCHED_10
        );
        // FIX: Start with summon cooldown at 0 so boss summons immediately after initialization
        // (Field removed in cleanup; if cooldown logic is needed, re-add as appropriate)
        
        // OPTIMIZATION: Calculate UUID-based offsets for staggering operations
        int hash = Math.abs(this.getUuid().hashCode());
        this.particleOffset = hash % 30;
        this.cleanupOffset = hash % 100;
        this.validationOffset = hash % 60;
        this.summonOffset = hash % 40;
    }
    
    @Override
    public Text getName() {
        return Text.literal("Mob Warlord").styled(style -> style.withColor(Formatting.DARK_PURPLE).withBold(true));
    }
    
    @Override
    protected void initGoals() {
        // Don't call super - we're completely replacing witch behavior
        // Note: We can't clear() in this API version, but our high-priority goals will override witch's goals
        
        // Add custom warlord goals with highest priorities
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MobWarlordAttackGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        
        // Targeting priorities (context-aware based on raid status):
        // 0. Protect minions - attack anyone who hurts them (HIGHEST PRIORITY)
        // 1. Revenge - attack anyone who attacks the warlord (except minions - friendly fire forgiven)
        // 2. AGGRESSIVE: Target ANY hostile/neutral mob that's angry at ANYTHING (NEW!)
        // 3. RAID MODE: Villagers (priority 2) → Iron Golems (priority 3) → Players (priority 5)
        // 3. NORMAL MODE: Players (priority 3) → Other mobs (priority 4)
        this.targetSelector.add(0, new ProtectMinionsGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this) {
            @Override
            public boolean canStart() {
                // Don't retaliate against our own minions (forgive friendly fire)
                LivingEntity attacker = this.mob.getAttacker();
                if (attacker instanceof MobEntity attackerMob && isMinionOf(attackerMob)) {
                    return false;
                }
                return super.canStart();
            }
        });
        
        // PRIORITY 2: Target OTHER WARLORDS FIRST in chaos mode (kill boss = kill all minions!)
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, MobWarlordEntity.class, 1, true, false,
            entity -> {
                if (entity == this) return false; // Don't target self
                
                // Only target other warlords in chaos mode
                boolean ignoreSame = entity.getWorld().getGameRules().getBoolean(UniversalMobWarMod.IGNORE_SAME_SPECIES_RULE);
                return !ignoreSame; // Only in chaos mode
            }
        ));
        
        // PRIORITY 3: RAID-SPECIFIC TARGETING - Villagers (when in raid)
        this.targetSelector.add(3, new RaidAwareTargetGoal<>(this, net.minecraft.entity.passive.VillagerEntity.class, true));
        
        // PRIORITY 4: RAID-SPECIFIC TARGETING - Iron Golems (when in raid)
        this.targetSelector.add(4, new RaidAwareTargetGoal<>(this, net.minecraft.entity.passive.IronGolemEntity.class, true));
        
        // PRIORITY 5: PLAYERS - Always target players aggressively
        this.targetSelector.add(5, new ActiveTargetGoal<PlayerEntity>(this, PlayerEntity.class, 1, true, false, null));
        
        // PRIORITY 6: ALL hostile mobs (including enemy minions, but NOT other warlords)
        this.targetSelector.add(6, new ActiveTargetGoal<>(this, HostileEntity.class, 1, true, false,
            entity -> {
                if (!(entity instanceof MobEntity mob)) return false;
                if (isMinionOf(mob)) return false; // Never target own minions
                if (entity instanceof MobWarlordEntity) return false; // Warlords handled by priority 2
                
                return true;
            }
        ));
        
        // PRIORITY 7: Passive mobs and animals (lowest priority)
        this.targetSelector.add(7, new ActiveTargetGoal<net.minecraft.entity.passive.AnimalEntity>(this, net.minecraft.entity.passive.AnimalEntity.class, 10, true, false, null));
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
        builder.add(IS_RAID_BOSS, false);
    }
    
    /**
     * Marks this warlord as spawned during a raid.
     */
    public void setRaidBoss(boolean isRaidBoss) {
        this.dataTracker.set(IS_RAID_BOSS, isRaidBoss);
    }
    
    /**
     * Checks if this warlord was spawned during a raid.
     */
    public boolean isRaidBoss() {
        return this.dataTracker.get(IS_RAID_BOSS);
    }
    
    /**
     * Marks a minion as a betrayer (attacked other minions).
     */
    public void markBetrayer(UUID minionUuid) {
        if (minionUuids.contains(minionUuid)) {
            betrayers.add(minionUuid);
            // Remove from minion list so they can be targeted
            MINION_TO_WARLORD.remove(minionUuid);
        }
    }
    
    /**
     * Checks if a minion is a betrayer.
     */
    public boolean isBetrayer(UUID minionUuid) {
        return betrayers.contains(minionUuid);
    }
    
    @Override
    public void tick() {
        // CRITICAL: Safety checks before calling super.tick()
        // This prevents crashes during chunk loading / world initialization
        if (this.getWorld() == null) return;
        
        // Client-side rendering: wrap in try-catch for Iris Shaders compatibility
        if (this.getWorld().isClient) {
            try {
                super.tick();
            } catch (Exception e) {
                // Silently ignore rendering errors (Iris Shaders compatibility)
            }
            return;
        }
        
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            try {
                super.tick();
            } catch (Exception e) {
                // Silently ignore errors
            }
            return;
        }
        
        // Additional safety: ensure entity is fully initialized
        // Wait longer (40 ticks = 2 seconds) to ensure all rendering systems are ready
        if (this.age < 40) {
            try {
                super.tick();
            } catch (Exception e) {
                // Silently ignore initialization errors
            }
            return; // Skip first 2 seconds to ensure world/rendering is ready
        }
        
        // Wrap super.tick() to catch any particle/rendering errors from WitchEntity
        try {
            super.tick();
        } catch (Exception e) {
            // Silently ignore witch particle errors
        }
        
        // Update boss bar safely
        try {
            if (this.bossBar != null) {
                this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            }
        } catch (Exception e) {
            // Ignore boss bar errors during initialization
        }
        
        // Cooldowns
        if (attackCooldown > 0) attackCooldown--;
        
        // OPTIMIZED: Clean up dead minions with UUID offset (spreads multiple Warlords over 100 ticks)
        if ((this.age + cleanupOffset) % CLEANUP_COOLDOWN == 0 && this.age > 60) {
            cleanupDeadMinions();
        }
        
        // OPTIMIZED: Draw particle connections with UUID offset (spreads multiple Warlords over 30 ticks)
        if ((this.age + particleOffset) % PARTICLE_COOLDOWN == 0 && this.age > 60) {
            drawParticleConnections();
        }
        
        // OPTIMIZED: Auto-summon with UUID offset (spreads summon checks across 40 ticks)
        if ((this.age + summonOffset) % SUMMON_COOLDOWN == 0 && this.age > 60) {
            int currentMinions = minionUuids.size();
            float healthPercent = this.getHealth() / this.getMaxHealth();
            
            // More aggressive summoning when hurt
            boolean shouldSummon = currentMinions < getMaxMinions() && (
                currentMinions < 5 || 
                (healthPercent < 0.75f && currentMinions < 10) ||
                (healthPercent < 0.5f && currentMinions < 15)
            );
            
            // Boss summons when there's a target (combat situation)
            if (shouldSummon && this.getTarget() != null) {
                int toSummon = healthPercent < 0.5f ? 3 : healthPercent < 0.75f ? 2 : 1;
                UniversalMobWarMod.LOGGER.debug("Mob Warlord summoning {} minions (current: {}, health: {}%, target: {})", 
                    toSummon, currentMinions, (int)(healthPercent * 100), this.getTarget().getName().getString());
                summonMinions(toSummon);
            }
        }
        
        // DEBUG: Log if boss has no target but should be looking for one
        if (this.age % 100 == 0 && this.age > 60) { // Every 5 seconds
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget == null && serverWorld != null) {
                // Count nearby hostile entities
                List<LivingEntity> nearbyHostiles = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    this.getBoundingBox().expand(64.0),
                    entity -> entity != this && entity.isAlive() && 
                             !(entity instanceof MobWarlordEntity) &&
                             !(entity instanceof MobEntity mob && isMinionOf(mob))
                );
                UniversalMobWarMod.LOGGER.debug("Mob Warlord has NO TARGET! {} potential targets nearby within 64 blocks", 
                    nearbyHostiles.size());
            }
        }
        
        // MOB CONVERSION: When boss kills a mob, there's a 50% chance to convert it to a minion
        // This is handled in the damage method, not here
        
        // Particle effects (skip if world not ready, wait 60 ticks = 3 seconds)
        if (this.age % 20 == 0 && this.age > 60) {
            spawnParticles();
        }
        
        // OPTIMIZED: Periodically validate minion targets with UUID offset (spreads validation across 60 ticks)
        if ((this.age + validationOffset) % 60 == 0 && this.age > 60) {
            validateMinionTargets();
        }
        
        // FIX: Make minions copy boss's target every 2 seconds for coordinated attacks
        if (this.age % 40 == 0 && this.age > 60) {
            LivingEntity bossTarget = this.getTarget();
            if (bossTarget != null && bossTarget.isAlive() && serverWorld != null) {
                for (UUID minionUuid : minionUuids) {
                    try {
                        Entity entity = serverWorld.getEntity(minionUuid);
                        if (entity instanceof MobEntity minion && minion.isAlive()) {
                            // Only set target if minion doesn't already have one
                            if (minion.getTarget() == null || !minion.getTarget().isAlive()) {
                                minion.setTarget(bossTarget);
                            }
                        }
                    } catch (Exception e) {
                        // Skip problematic minions
                    }
                }
            }
        }
    }
    
    /**
     * Validates minion targets and clears invalid ones.
     * OPTIMIZED: Only checks 3 random minions per cycle to spread load.
     * Also detects betrayal (minions attacking other minions).
     */
    private void validateMinionTargets() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        // OPTIMIZATION: Only validate 3 random minions per cycle
        List<UUID> minionsList = new ArrayList<>(minionUuids);
        Collections.shuffle(minionsList);
        int checksThisCycle = Math.min(3, minionsList.size());
        
        try {
            for (int i = 0; i < checksThisCycle; i++) {
                UUID minionUuid = minionsList.get(i);
                Entity entity = serverWorld.getEntity(minionUuid);
                if (entity instanceof MobEntity minion && minion.isAlive()) {
                    LivingEntity target = minion.getTarget();
                    
                    if (target != null) {
                        boolean invalidTarget = false;
                        
                        // Check if targeting the warlord (INVALID)
                        if (target == this) {
                            invalidTarget = true;
                        }
                        
                        // Check if targeting another minion (BETRAYAL DETECTION)
                        if (target instanceof MobEntity targetMob) {
                            UUID targetMasterUuid = MINION_TO_WARLORD.get(targetMob.getUuid());
                            if (targetMasterUuid != null && targetMasterUuid.equals(this.getUuid())) {
                                // BETRAYAL! Mark this minion as traitor
                                markBetrayer(minionUuid);
                                invalidTarget = false; // Allow targeting - they're now an enemy
                                
                                // Send message to nearby players
                                serverWorld.getPlayers().forEach(player -> {
                                    if (player.squaredDistanceTo(minion) <= 1024) // 32 block range
                                        player.sendMessage(
                                            Text.literal("⚔ A minion has betrayed the Warlord! ⚔")
                                                .styled(style -> style.withColor(Formatting.RED).withBold(true)),
                                            true // Action bar
                                        );
                                });
                            }
                        }
                        
                        // Clear invalid target (but not betrayers - they can fight)
                        if (invalidTarget) {
                            minion.setTarget(null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }
    
    /**
     * Draws particle connections from the warlord to all minions.
     * OPTIMIZED: Reduced particle density for better performance.
     * Shows purple/dark purple lines so players can see who is allied.
     */
    private void drawParticleConnections() {
        if (ModConfig.getInstance().disableParticles) return; // Performance optimization
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (this.age < 60) return; // Wait for full initialization
        
        try {
            Vec3d bossPos = this.getPos().add(0, this.getHeight() / 2, 0); // Center of boss
            
            // OPTIMIZATION: Limit connections drawn based on minion count
            int drawnCount = 0;
            int maxDraws = Math.min(10, minionUuids.size()); // Reduced from 15 to 10
            
            for (UUID minionUuid : minionUuids) {
                if (drawnCount++ >= maxDraws) break;
                
                try {
                    Entity entity = serverWorld.getEntity(minionUuid);
                    if (entity instanceof MobEntity minion && minion.isAlive()) {
                        Vec3d minionPos = minion.getPos().add(0, minion.getHeight() / 2, 0);
                        
                        // OPTIMIZATION: Skip particles if minion is very close
                        double distance = minionPos.distanceTo(bossPos);
                        if (distance < 3.0) continue; // Don't draw particles for minions within 3 blocks
                        
                        // Check if betrayer - different color
                        boolean isBetrayer = betrayers.contains(minionUuid);
                        
                        // Draw particle line from boss to minion
                        Vec3d direction = minionPos.subtract(bossPos);
                        Vec3d step = direction.normalize().multiply(1.5); // OPTIMIZED: Particle every 1.5 blocks (was 1.0)
                        
                        // OPTIMIZATION: Fewer particles per connection, scale with minion count
                        int maxParticles = minionUuids.size() > 10 ? 5 : 10; // Reduced from 8/15
                        int particleCount = Math.min((int)(distance / 1.5), maxParticles);
                        for (int i = 0; i < particleCount; i++) {
                            Vec3d particlePos = bossPos.add(step.multiply(i));
                            
                            // Choose particle type based on status
                            if (isBetrayer) {
                                // RED particles for betrayers
                                serverWorld.spawnParticles(
                                    ParticleTypes.ANGRY_VILLAGER,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0, 0, 0, 0
                                );
                            } else {
                                // PURPLE particles for loyal minions
                                serverWorld.spawnParticles(
                                    ParticleTypes.PORTAL,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0, 0, 0, 0
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip problematic minions
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }
    
    /**
     * Converts a killed mob into a minion instead of letting it die.
     * Called when the boss deals a killing blow to a mob.
     * 50% chance to convert the mob into a loyal minion.
     */
    public void tryConvertKilledMob(MobEntity killedMob) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (minionUuids.size() >= getMaxMinions()) return;
        if (killedMob == null || !killedMob.isAlive()) return;
        
        // CRITICAL: Never convert other Mob Warlords (prevents infinite loops and conflicts)
        if (killedMob instanceof MobWarlordEntity) return;
        
        // CRITICAL: Never convert another warlord's minions (prevents ownership conflicts and goal duplication)
        UUID existingMaster = MINION_TO_WARLORD.get(killedMob.getUuid());
        if (existingMaster != null && !existingMaster.equals(this.getUuid())) {
            // This mob belongs to another warlord - can't steal them!
            return;
        }
        
        // 50% chance to convert
        if (this.random.nextFloat() > 0.5f) return;
        
        // Heal the mob instead of letting it die
        killedMob.setHealth(killedMob.getMaxHealth());
        
        // Add to minion tracking
        MINION_TO_WARLORD.put(killedMob.getUuid(), this.getUuid());
        minionUuids.add(killedMob.getUuid());
        
        // Register with universal summoner tracker
        SummonerTracker.registerSummoned(killedMob.getUuid(), this.getUuid());
        
        // Make minion persistent
        killedMob.setPersistent();
        
        // Add tethering goal with HIGHEST priority - keeps minion within 14 blocks of warlord
        ((MobEntityAccessor) killedMob).getGoalSelector().add(0, new StayNearWarlordGoal(killedMob, this));
        
        // Make sure the converted mob will actually attack enemies
        // The UniversalTargetGoal should already be on them, but if not, their normal AI will work
        // Clear any fear/fleeing states
        killedMob.setTarget(null);
        killedMob.setAttacker(null);
        
        // Force the mob to be aggressive by setting it to angry state if it's angerable
        if (killedMob instanceof Angerable angerable) {
            angerable.setAttacker(null); // Clear any anger at the boss
            angerable.stopAnger(); // Reset anger
        }
        
        LivingEntity warlordTarget = this.getTarget();
        if (warlordTarget != null && warlordTarget.isAlive() && warlordTarget != killedMob) {
            // Validate target isn't another minion
            boolean targetIsMinion = false;
            if (warlordTarget instanceof MobEntity targetMob) {
                UUID targetMasterUuid = MINION_TO_WARLORD.get(targetMob.getUuid());
                targetIsMinion = (targetMasterUuid != null && targetMasterUuid.equals(this.getUuid()));
            }
            
            if (!targetIsMinion) {
                killedMob.setTarget(warlordTarget);
            }
        }
        
        // OPTIMIZED: Conversion particles - reduced count for better performance on kill events
        try {
            if (this.age > 60) {
                serverWorld.spawnParticles(ParticleTypes.SOUL,
                    killedMob.getX(), killedMob.getY() + killedMob.getHeight() / 2, killedMob.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1); // Reduced from 50 to 20
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    killedMob.getX(), killedMob.getY() + killedMob.getHeight() / 2, killedMob.getZ(),
                    15, 0.5, 0.5, 0.5, 0.5); // Reduced from 30 to 15
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                    killedMob.getX(), killedMob.getY() + killedMob.getHeight() / 2, killedMob.getZ(),
                    10, 0.3, 0.3, 0.3, 0.3); // Reduced from 20 to 10
            }
        } catch (Exception e) {
            // Ignore particle errors
        }
        
        this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
        this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f);
        
        this.dataTracker.set(MINION_COUNT, minionUuids.size());
        
        //noinspection deprecation
        UniversalMobWarMod.LOGGER.debug("Mob Warlord converted {} into a minion! (UUID: {})", 
            net.minecraft.registry.Registries.ENTITY_TYPE.getId(killedMob.getType()), 
            killedMob.getUuid());
    }
    
    /**
     * Summons random hostile/neutral mobs to fight for the warlord.
     */
    private void summonMinions(int count) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        // Check if neutral mobs should be aggressive
        boolean neutralAggressive = serverWorld.getGameRules().getBoolean(UniversalMobWarMod.NEUTRAL_MOBS_AGGRESSIVE_RULE);
        
        for (int i = 0; i < count && minionUuids.size() < getMaxMinions(); i++) {
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
                
                // Ensure minion doesn't despawn
                minion.setPersistent();
                
                // Spawn the entity first
                boolean spawned = serverWorld.spawnEntity(minion);
                
                if (spawned) {
                    // AFTER spawning, register as minion in static map
                    MINION_TO_WARLORD.put(minion.getUuid(), this.getUuid());
                    minionUuids.add(minion.getUuid());
                    
                    // ALSO register with universal summoner tracker
                    SummonerTracker.registerSummoned(minion.getUuid(), this.getUuid());
                    
                    // Add tethering goal with HIGHEST priority - keeps minion within 14 blocks of warlord
                    ((MobEntityAccessor) minion).getGoalSelector().add(0, new StayNearWarlordGoal(minion, this));
                    
                    // Special AI for creepers - make them avoid allies when exploding
                    if (minion instanceof net.minecraft.entity.mob.CreeperEntity creeper) {
                        // Add smart creeper goal that checks for allies before exploding
                        ((MobEntityAccessor) creeper).getGoalSelector().add(1, new SmartCreeperGoal(creeper, this));
                    }
                    
                    // Set target to warlord's target (validate it's not another minion or the warlord)
                    LivingEntity warlordTarget = this.getTarget();
                    if (warlordTarget != null && warlordTarget.isAlive()) {
                        // Don't target other minions
                        boolean targetIsMinion = false;
                        if (warlordTarget instanceof MobEntity targetMob) {
                            UUID targetMasterUuid = MINION_TO_WARLORD.get(targetMob.getUuid());
                            targetIsMinion = (targetMasterUuid != null && targetMasterUuid.equals(this.getUuid()));
                        }
                        
                        // Only set valid targets
                        if (!targetIsMinion) {
                            minion.setTarget(warlordTarget);
                        }
                    }
                    
                    // Spawn effects (after initialization delay for Iris Shaders compatibility)
                    try {
                        if (this.age > 60) { // Wait 3 seconds to ensure rendering is ready
                            serverWorld.spawnParticles(ParticleTypes.PORTAL, 
                                spawnPos.x, spawnPos.y, spawnPos.z, 
                                20, 0.5, 0.5, 0.5, 0.1);
                        }
                    } catch (Exception e) {
                        // Silently ignore particle errors
                    }
                    this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);
                }
            }
        }
        
        this.dataTracker.set(MINION_COUNT, minionUuids.size());
    }
    
    /**
     * Gets a random hostile or neutral mob type for the Warlord to summon.
     * 
     * HOSTILE MOBS (22 types):
     * - Common: Zombie, Skeleton, Creeper, Spider, Cave Spider
     * - Magical: Witch, Blaze, Enderman, Vex, Evoker
     * - Nether: Zombified Piglin, Piglin, Hoglin, Piglin Brute, Wither Skeleton
     * - Illagers: Vindicator, Pillager, Ravager, Illusioner
     * - Other: Stray, Husk, Drowned
     * 
     * NEUTRAL MOBS (5 types, when universalMobWarNeutralAggressive is enabled):
     * - Iron Golem, Wolf, Polar Bear, Panda, Bee
     */
    private EntityType<?> getRandomMobType(boolean includeNeutral) {
        List<EntityType<?>> possibleMobs = new ArrayList<>();
        
        // HOSTILE MOBS (always available)
        // Common undead
        possibleMobs.add(EntityType.ZOMBIE);
        possibleMobs.add(EntityType.SKELETON);
        possibleMobs.add(EntityType.HUSK);
        possibleMobs.add(EntityType.STRAY);
        possibleMobs.add(EntityType.DROWNED);
        possibleMobs.add(EntityType.WITHER_SKELETON);
        
        // Common monsters
        possibleMobs.add(EntityType.CREEPER);
        possibleMobs.add(EntityType.SPIDER);
        possibleMobs.add(EntityType.CAVE_SPIDER);
        
        // Magical mobs
        possibleMobs.add(EntityType.WITCH);
        possibleMobs.add(EntityType.BLAZE);
        possibleMobs.add(EntityType.ENDERMAN);
        possibleMobs.add(EntityType.VEX);          // Flying, phases through walls!
        possibleMobs.add(EntityType.EVOKER);       // Summons vexes, powerful caster
        
        // Nether mobs
        possibleMobs.add(EntityType.ZOMBIFIED_PIGLIN);
        possibleMobs.add(EntityType.PIGLIN);
        possibleMobs.add(EntityType.PIGLIN_BRUTE);
        possibleMobs.add(EntityType.HOGLIN);
        
        // Illagers (raid mobs)
        possibleMobs.add(EntityType.VINDICATOR);
        possibleMobs.add(EntityType.PILLAGER);
        possibleMobs.add(EntityType.RAVAGER);      // Tank, powerful charge attack
        
        // NEUTRAL MOBS (only if gamerule enables neutral aggression)
        if (includeNeutral) {
            possibleMobs.add(EntityType.IRON_GOLEM);  // Very tanky ally
            possibleMobs.add(EntityType.WOLF);
            possibleMobs.add(EntityType.POLAR_BEAR);
            possibleMobs.add(EntityType.PANDA);
            possibleMobs.add(EntityType.BEE);
        }
        
        // TODO: Future enhancement - scan for modded mobs dynamically
        
        return possibleMobs.get(this.random.nextInt(possibleMobs.size()));
    }
    
    /**
     * Cleans up dead or despawned minions.
     * Only runs every 5 seconds to avoid performance issues with large modpacks.
     */
    private void cleanupDeadMinions() {
        if (this.getWorld() == null || !(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (this.age < 60) return; // Skip during initialization (3 seconds)
        
        // OPTIMIZATION: Skip cleanup if no minions
        if (minionUuids.isEmpty()) {
            return;
        }
        
        try {
            // Performance optimization: Create a list to avoid concurrent modification
            List<UUID> toRemove = new ArrayList<>();
            
            // Limit how many we check at once for large modpacks
            int checked = 0;
            int maxChecks = 10; // Check max 10 minions per cleanup cycle
            
            for (UUID uuid : minionUuids) {
                if (checked++ >= maxChecks) break;
                
                try {
                    Entity entity = serverWorld.getEntity(uuid);
                    boolean isDead = entity == null || !entity.isAlive();
                    
                    if (isDead) {
                        toRemove.add(uuid);
                        MINION_TO_WARLORD.remove(uuid);
                    }
                } catch (Exception e) {
                    // Skip problematic UUIDs
                    toRemove.add(uuid); // Remove problematic minions to prevent future errors
                }
            }
            
            // Remove all dead minions
            minionUuids.removeAll(toRemove);
            
            // Update data tracker safely
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
        UUID masterUuid = MINION_TO_WARLORD.get(mob.getUuid());
        return masterUuid != null && masterUuid.equals(this.getUuid());
    }
    
    /**
     * Static helper to check if a mob is a minion of any warlord.
     */
    public static boolean isMinion(UUID mobUuid) {
        return MINION_TO_WARLORD.containsKey(mobUuid);
    }
    
    /**
     * Static helper to get the warlord UUID for a minion.
     */
    public static UUID getMasterUuid(UUID minionUuid) {
        return MINION_TO_WARLORD.get(minionUuid);
    }
    
    /**
     * Checks if a mob is a raid mob (pillagers, vindicators, ravagers, etc.).
     * Used to avoid targeting raid allies when boss is spawned in a raid.
     */

    
    /**
     * Finds a low-health minion that needs healing.
     * Returns null if no minions need healing.
     */
    private MobEntity findLowHealthMinion() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return null;
        
        MobEntity lowestHealthMinion = null;
        float lowestHealthPercent = 1.0f;
        
        for (UUID minionUuid : minionUuids) {
            try {
                Entity entity = serverWorld.getEntity(minionUuid);
                if (entity instanceof MobEntity minion && minion.isAlive()) {
                    float healthPercent = minion.getHealth() / minion.getMaxHealth();
                    
                    // Find minion with lowest health below 50%
                    if (healthPercent < 0.5f && healthPercent < lowestHealthPercent) {
                        lowestHealthPercent = healthPercent;
                        lowestHealthMinion = minion;
                    }
                }
            } catch (Exception e) {
                // Skip problematic minions
            }
        }
        
        return lowestHealthMinion;
    }
    
    /**
     * Checks if there are minions near the target that would be hit by splash damage.
     */
    private boolean minionsNearTarget(LivingEntity target, double radius) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return false;
        
        List<MobEntity> nearbyMobs = serverWorld.getEntitiesByClass(
            MobEntity.class,
            target.getBoundingBox().expand(radius),
            mob -> mob != target && isMinionOf(mob)
        );
        
        return !nearbyMobs.isEmpty();
    }
    
    /**
     * Smart ranged attack system with dynamic decision making.
     * Priority:
     * 0. SELF-HEALING - heal the boss if health is low (HIGHEST PRIORITY)
     * 1. Self-defense - attack current target if being attacked
     * 2. Heal critical minions (below 50% health)
     * 3. Attack enemies (avoiding friendly fire)
     * 4. Buff healthy minions
     */
    public void performRangedAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        // PRIORITY 0: SELF-HEALING - Boss heals itself when health is below 70%
        float healthPercent = this.getHealth() / this.getMaxHealth();
        if (healthPercent < 0.7f) { // Changed back to 0.7 - boss should heal more often!
            // Throw a super healing potion at self
            UniversalMobWarMod.LOGGER.debug("Mob Warlord self-healing! Health: {}% ({}/{})", 
                (int)(healthPercent * 100), (int)this.getHealth(), (int)this.getMaxHealth());
            throwSuperHealingPotion(this);
            attackCooldown = ATTACK_COOLDOWN / 2; // Shorter cooldown for self-healing
            return;
        }
        
        // PRIORITY 1: Self-defense - if boss has a target (being attacked), deal with threat first
        if (target != null && target.isAlive()) {
            // Check if minions would be hit by splash damage (4 block radius)
            boolean minionsInSplashRange = minionsNearTarget(target, 4.0);
            
            if (minionsInSplashRange) {
                // Don't throw harmful potion if it would hit minions
                // Try melee instead or skip this attack
                if (this.squaredDistanceTo(target) <= 6.0) {
                    // Close enough for melee, let melee goal handle it
                    return;
                } else {
                    // Too far for melee, skip this harmful potion to avoid friendly fire
                    attackCooldown = ATTACK_COOLDOWN / 2; // Shorter cooldown to try again soon
                    return;
                }
            }
            
            // Safe to throw harmful potion at target
            throwHarmfulPotion(target);
            attackCooldown = ATTACK_COOLDOWN;
            return;
        }
        
        // PRIORITY 2: Heal low-health minions
        MobEntity lowHealthMinion = findLowHealthMinion();
        if (lowHealthMinion != null) {
            double distanceToMinion = this.squaredDistanceTo(lowHealthMinion);
            
            // Check if we're in range to throw healing potion
            if (distanceToMinion <= 256.0) // 16 block range
                {
                // Check if enemies are nearby that would also get healed
                List<LivingEntity> nearbyEnemies = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    lowHealthMinion.getBoundingBox().expand(4.0),
                    entity -> entity != this && entity != lowHealthMinion && 
                             !(entity instanceof MobEntity mob && isMinionOf(mob)) &&
                             entity.isAlive()
                );
                
                if (nearbyEnemies.isEmpty()) {
                    // Safe to heal - no enemies will benefit
                    throwHealingPotion(lowHealthMinion);
                    attackCooldown = ATTACK_COOLDOWN;
                    return;
                } else {
                    // Enemies nearby - don't heal yet, focus on combat
                    // Fall through to buff minions or do nothing
                }
            } else {
                // Out of range - navigation will be handled by combat goal
                // For now, just skip
            }
        }
        
        // PRIORITY 3: Buff minions (if we have target but can't safely attack)
        // Find nearest minion to buff
        MobEntity nearestMinion = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (UUID minionUuid : minionUuids) {
            try {
                Entity entity = serverWorld.getEntity(minionUuid);
                if (entity instanceof MobEntity minion && minion.isAlive()) {
                    double distance = this.squaredDistanceTo(minion);
                    if (distance < nearestDistance && distance <= 256.0) {
                        nearestDistance = distance;
                        nearestMinion = minion;
                    }
                }
            } catch (Exception e) {
                // Skip problematic minions
            }
        }
        
        if (nearestMinion != null) {
            // Check if enemies would also get buffed
            List<LivingEntity> nearbyEnemies = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                nearestMinion.getBoundingBox().expand(4.0),
                entity -> entity != this && !(entity instanceof MobEntity mob && isMinionOf(mob)) && entity.isAlive()
            );
            
            if (nearbyEnemies.isEmpty()) {
                // Safe to buff - no enemies will benefit
                throwBuffPotion(nearestMinion);
                attackCooldown = ATTACK_COOLDOWN * 2; // Longer cooldown for buffs
                return;
            }
        }
        
        // If we can't do anything safely, just wait
        attackCooldown = ATTACK_COOLDOWN / 2;
    }
    
    /**
     * Throws a harmful potion at an enemy.
     */
    private void throwHarmfulPotion(LivingEntity target) {
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        
        List<StatusEffectInstance> harmfulEffects = List.of(
            new StatusEffectInstance(StatusEffects.POISON, 200, 1),     // 10 seconds, Poison II
            new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1),   // 15 seconds, Weakness II
            new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1),   // 10 seconds, Slowness II
            new StatusEffectInstance(StatusEffects.WITHER, 100, 0)      // 5 seconds, Wither I
        );
        
        potion.set(DataComponentTypes.POTION_CONTENTS, 
            new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0x330033), harmfulEffects));
        
        Vec3d vec3d = target.getEyePos().subtract(this.getEyePos());
        potionEntity.setItem(potion);
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        
        // Dark purple particles
        if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
            try {
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
    }
    
    /**
     * Throws a SUPER healing potion at the boss itself or critical minions.
     * Much stronger than regular healing potions.
     */
    private void throwSuperHealingPotion(LivingEntity target) {
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        
        List<StatusEffectInstance> superHealingEffects = List.of(
            new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 3),      // Instant Health IV (8 hearts)
            new StatusEffectInstance(StatusEffects.REGENERATION, 400, 2),      // 20 seconds, Regeneration III
            new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 2),        // 30 seconds, Resistance III
            new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 2)         // 30 seconds, Absorption III (6 extra hearts)
        );
        
        potion.set(DataComponentTypes.POTION_CONTENTS,
            new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0xFFD700), superHealingEffects)); // Golden
        
        Vec3d vec3d = target.getEyePos().subtract(this.getEyePos());
        potionEntity.setItem(potion);
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        
        // Golden healing particles
        if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
            try {
                serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    20, 0.5, 0.5, 0.5, 0.2);
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        this.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f); // Epic healing sound
    }
    
    /**
     * Throws a healing potion at a low-health minion.
     */
    private void throwHealingPotion(MobEntity minion) {
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        
        List<StatusEffectInstance> healingEffects = List.of(
            new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 1),     // Instant Health II
            new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1),      // 10 seconds, Regeneration II
            new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 1)         // 15 seconds, Resistance II
        );
        
        potion.set(DataComponentTypes.POTION_CONTENTS,
            new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0xFF1493), healingEffects)); // Deep pink
        
        Vec3d vec3d = minion.getEyePos().subtract(this.getEyePos());
        potionEntity.setItem(potion);
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        
        // Pink/healing particles
        if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
            try {
                serverWorld.spawnParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 1.2f); // Higher pitch for healing
    }
    
    /**
     * Throws a buff potion at minions.
     */
    private void throwBuffPotion(MobEntity minion) {
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        
        List<StatusEffectInstance> buffEffects = List.of(
            new StatusEffectInstance(StatusEffects.STRENGTH, 400, 1),      // 20 seconds, Strength II
            new StatusEffectInstance(StatusEffects.SPEED, 400, 1),         // 20 seconds, Speed II
            new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0)     // 20 seconds, Resistance I
        );
        
        potion.set(DataComponentTypes.POTION_CONTENTS,
            new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0xFF00FF), buffEffects)); // Bright purple
        
        Vec3d vec3d = minion.getEyePos().subtract(this.getEyePos());
        potionEntity.setItem(potion);
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        
        // Purple particles
        if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
            try {
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    15, 0.3, 0.3, 0.3, 0.1);
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 1.0f);
    }
    
    /**
     * Performs a melee attack with knockback and particles.
     * Also handles mob conversion on killing blows.
     */
    public void performMeleeAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        
        // Check if this attack would kill the target (mob only, not players)
        boolean wouldKill = false;
        if (target instanceof MobEntity mobTarget && !(target instanceof MobWarlordEntity)) {
            float damage = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            wouldKill = (mobTarget.getHealth() <= damage);
        }
        
        // Perform the attack
        this.tryAttack(target);
        
        // If we would have killed a mob, try to convert it
        if (wouldKill && target instanceof MobEntity mobTarget && mobTarget.isAlive()) {
            tryConvertKilledMob(mobTarget);
        }
        
        // Knockback effect
        Vec3d direction = target.getPos().subtract(this.getPos()).normalize();
        target.takeKnockback(2.0, -direction.x, -direction.z);
        
        // Attack particles (purple magic burst)
        if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
            try {
                Vec3d targetPos = target.getPos();
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                    targetPos.x, targetPos.y + 1.0, targetPos.z,
                    20, 0.5, 0.5, 0.5, 0.1);
                serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH,
                    targetPos.x, targetPos.y + 1.0, targetPos.z,
                    10, 0.3, 0.3, 0.3, 0.05);
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
        this.playSound(SoundEvents.ENTITY_WITHER_HURT, 0.5f, 1.5f); // Magical sound effect
        attackCooldown = ATTACK_COOLDOWN / 2; // Faster melee cooldown
    }
    
    private void spawnParticles() {
        if (ModConfig.getInstance().disableParticles) return; // Performance optimization
        if (this.getWorld() == null) return;
        if (this.age < 60) return; // Wait 3 seconds for full initialization
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        
        try {
            // Additional null check for serverWorld's particle manager
            if (serverWorld.getServer() != null && !serverWorld.isClient) {
                serverWorld.spawnParticles(ParticleTypes.WITCH,
                    this.getX(), this.getY() + 2.0, this.getZ(),
                    2, 0.5, 0.5, 0.5, 0.05); // Reduced from 5 to 2
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
        
        // Save betrayers
        long[] betrayerArray = new long[betrayers.size() * 2];
        int j = 0;
        for (UUID uuid : betrayers) {
            betrayerArray[j++] = uuid.getMostSignificantBits();
            betrayerArray[j++] = uuid.getLeastSignificantBits();
        }
        nbt.putLongArray("Betrayers", betrayerArray);
        
        // Save raid status
        nbt.putBoolean("IsRaidBoss", this.isRaidBoss());
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
        
        // Load betrayers
        if (nbt.contains("Betrayers")) {
            long[] betrayerArray = nbt.getLongArray("Betrayers");
            betrayers.clear();
            for (int i = 0; i < betrayerArray.length; i += 2) {
                UUID uuid = new UUID(betrayerArray[i], betrayerArray[i + 1]);
                betrayers.add(uuid);
            }
        }
        
        // Load raid status
        if (nbt.contains("IsRaidBoss")) {
            this.setRaidBoss(nbt.getBoolean("IsRaidBoss"));
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
        
        // OPTIMIZED: Stagger minion deaths to prevent lag spike (5 per tick, cascading death effect)
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            List<UUID> minionList = new ArrayList<>(minionUuids);
            
            // Schedule staggered minion deaths
            for (int i = 0; i < minionList.size(); i++) {
                final UUID minionUuid = minionList.get(i);
                final int delay = i / 5; // 5 minions per tick (0.25 seconds total for 20 minions)
                
                // Schedule death after delay ticks
                serverWorld.getServer().execute(() -> {
                    scheduleMinionDeath(serverWorld, minionUuid, delay);
                });
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
     * Helper method to schedule minion death after a delay.
     */
    private void scheduleMinionDeath(ServerWorld serverWorld, UUID minionUuid, int delayTicks) {
        if (delayTicks == 0) {
            killMinion(serverWorld, minionUuid);
        } else {
            // Schedule for later
            serverWorld.getServer().execute(() -> {
                try {
                    Thread.sleep(delayTicks * 50L); // 50ms per tick
                    killMinion(serverWorld, minionUuid);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    /**
     * Kills a minion with particles.
     */
    private void killMinion(ServerWorld serverWorld, UUID minionUuid) {
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
        
        // Clean up from static map
        MINION_TO_WARLORD.remove(minionUuid);
    }
    
    /**
     * Custom AI goal for protecting minions.
     * The Warlord will target anyone who attacks its minions.
     */
    private static class ProtectMinionsGoal extends Goal {
        private final MobWarlordEntity warlord;
        private LivingEntity attacker;
        private int checkCooldown;
        
        public ProtectMinionsGoal(MobWarlordEntity warlord) {
            this.warlord = warlord;
            this.checkCooldown = 0;
        }
        
        @Override
        public boolean canStart() {
            // Only check every 20 ticks (1 second) for performance with large modpacks
            if (checkCooldown > 0) {
                checkCooldown--;
                return false;
            }
            checkCooldown = 20;
            
            // Don't override if warlord is already targeting someone attacking it
            if (this.warlord.getAttacker() != null) {
                return false;
            }
            
            // Check if any minions are being attacked
            if (!(this.warlord.getWorld() instanceof ServerWorld serverWorld)) return false;
            
            // Performance optimization: limit how many minions we check at once
            int checkedCount = 0;
            int maxChecks = 5; // Only check 5 minions per check cycle
            
            for (UUID minionUuid : this.warlord.minionUuids) {
                if (checkedCount++ >= maxChecks) break; // Stop after checking max minions
                
                try {
                    Entity entity = serverWorld.getEntity(minionUuid);
                    if (entity instanceof MobEntity minion && minion.isAlive()) {
                        LivingEntity minionAttacker = minion.getAttacker();
                        
                        // Found someone attacking our minion!
                        if (minionAttacker != null && minionAttacker.isAlive() && minionAttacker != this.warlord) {
                            // Don't attack other minions
                            if (minionAttacker instanceof MobEntity && this.warlord.isMinionOf((MobEntity) minionAttacker)) {
                                continue;
                            }
                            
                            this.attacker = minionAttacker;
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Skip problematic minions
                    continue;
                }
            }
            
            return false;
        }
        
        @Override
        public void start() {
            this.warlord.setTarget(this.attacker);
            
            // Play angry sound
            this.warlord.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
            
            // Spawn angry particles
            if (this.warlord.getWorld() instanceof ServerWorld serverWorld && this.warlord.age > 60) {
                try {
                    serverWorld.spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                        this.warlord.getX(), this.warlord.getY() + 2.5, this.warlord.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0);
                } catch (Exception e) {
                    // Ignore particle errors
                }
            }
        }
        
        @Override
        public boolean shouldContinue() {
            return this.attacker != null && this.attacker.isAlive();
        }
        
        @Override
        public void stop() {
            this.attacker = null;
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
                // CRITICAL: Never target our own minions
                if (target instanceof MobEntity targetMob && this.warlord.isMinionOf(targetMob)) {
                    this.warlord.setTarget(null);
                    return false;
                }
                
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
    
    /**
     * Smart AI for creeper minions that avoids exploding near allies.
     * Creepers will only explode if there are 2 or fewer allies nearby.
     */
    private static class SmartCreeperGoal extends Goal {
        private final net.minecraft.entity.mob.CreeperEntity creeper;
        private final MobWarlordEntity warlord;
        private int checkCooldown = 0;
        
        public SmartCreeperGoal(net.minecraft.entity.mob.CreeperEntity creeper, MobWarlordEntity warlord) {
            this.creeper = creeper;
            this.warlord = warlord;
        }
        
        @Override
        public boolean canStart() {
            // Check every 10 ticks
            if (checkCooldown > 0) {
                checkCooldown--;
                return false;
            }
            checkCooldown = 10;
            
            // Check if creeper is about to explode (fuse time > 0)
            if (this.creeper.getFuseSpeed() > 0) {
                // Count nearby allies (within explosion radius of 3 blocks)
                if (!(this.creeper.getWorld() instanceof ServerWorld serverWorld)) return false;
                
                List<MobEntity> nearbyAllies = serverWorld.getEntitiesByClass(
                    MobEntity.class,
                    this.creeper.getBoundingBox().expand(4.0), // Slightly larger than explosion radius
                    mob -> mob != this.creeper && (this.warlord.isMinionOf(mob) || mob == this.warlord)
                );
                
                // If more than 2 allies nearby, cancel explosion
                if (nearbyAllies.size() > 2) {
                    // Force stop fuse (set to not powered/ignited state)
                    try {
                        // We can't directly stop the fuse in the API, but we can make the creeper flee
                        LivingEntity target = this.creeper.getTarget();
                        if (target != null) {
                            // Move away from target to avoid explosion near allies
                            Vec3d awayDirection = this.creeper.getPos().subtract(target.getPos()).normalize();
                            Vec3d fleePos = this.creeper.getPos().add(awayDirection.multiply(5.0));
                            this.creeper.getNavigation().startMovingTo(fleePos.x, fleePos.y, fleePos.z, 1.5);
                            return true;
                        }
                    } catch (Exception e) {
                        // Silently handle errors
                    }
                }
            }
            
            return false;
        }
        
        @Override
        public boolean shouldContinue() {
            // Continue fleeing for a bit
            return this.creeper.getNavigation().isFollowingPath();
        }
    }
    
    /**
     * Raid-aware targeting goal that only activates when boss is in raid mode.
     * Used for prioritizing villagers and iron golems during raids.
     */
    private static class RaidAwareTargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
        private final MobWarlordEntity warlord;
        
        public RaidAwareTargetGoal(MobWarlordEntity warlord, Class<T> targetClass, boolean checkVisibility) {
            super(warlord, targetClass, checkVisibility);
            this.warlord = warlord;
        }
        
        @Override
        public boolean canStart() {
            // Only target villagers/golems if in raid mode
            if (!this.warlord.isRaidBoss()) {
                return false;
            }
            
            return super.canStart();
        }
    }
    
    /**
     * Tethering goal that keeps minions within 14 blocks of the Mob Warlord.
     * If a minion strays too far, they will return to the boss regardless of enemy targets.
     * This prevents minions from wandering off and getting separated from their master.
     */
    public static class StayNearWarlordGoal extends Goal {
        private final MobEntity minion;
        private final MobWarlordEntity warlord;
        private static final double RETURN_DISTANCE = 14.0; // Minions start returning at this distance
        private int checkCooldown = 0;
        
        public StayNearWarlordGoal(MobEntity minion, MobWarlordEntity warlord) {
            this.minion = minion;
            this.warlord = warlord;
            this.setControls(EnumSet.of(Control.MOVE, Control.TARGET));
        }
        
        @Override
        public boolean canStart() {
            // Check every 20 ticks (1 second) for performance
            if (checkCooldown > 0) {
                checkCooldown--;
                return false;
            }
            checkCooldown = 20;
            
            // Check if warlord is still alive
            if (!this.warlord.isAlive()) {
                return false;
            }
            
            // Check if minion is too far from warlord
            double distance = this.minion.squaredDistanceTo(this.warlord);
            return distance > (RETURN_DISTANCE * RETURN_DISTANCE);
        }
        
        @Override
        public boolean shouldContinue() {
            // Continue until we're close enough to the warlord
            if (!this.warlord.isAlive()) return false;
            
            double distance = this.minion.squaredDistanceTo(this.warlord);
            // Stop when within 12 blocks to prevent bouncing
            return distance > (12.0 * 12.0);
        }
        
        @Override
        public void start() {
            // Clear current target to focus on returning
            this.minion.setTarget(null);
            // Navigate back to warlord
            this.minion.getNavigation().startMovingTo(this.warlord, 1.2); // Slightly faster return speed
        }
        
        @Override
        public void tick() {
            // Keep navigating to warlord's current position
            if (this.minion.age % 10 == 0) { // Update path every 0.5 seconds
                this.minion.getNavigation().startMovingTo(this.warlord, 1.2);
            }
            
            // Make minion look at the warlord while returning
            this.minion.getLookControl().lookAt(this.warlord, 30.0f, 30.0f);
        }
        
        @Override
        public void stop() {
            // Stop navigating
            this.minion.getNavigation().stop();
        }
    }
}

