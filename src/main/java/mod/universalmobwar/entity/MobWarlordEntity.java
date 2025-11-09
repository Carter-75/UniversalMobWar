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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
 * Extends WitchEntity to use the witch model and rendering.
 */
public class MobWarlordEntity extends WitchEntity {
    
    private static final TrackedData<Integer> MINION_COUNT = DataTracker.registerData(MobWarlordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    // Static map to track which mobs are minions of which warlord (thread-safe)
    private static final Map<UUID, UUID> MINION_TO_WARLORD = new java.util.concurrent.ConcurrentHashMap<>();
    
    private final ServerBossBar bossBar;
    private final Set<UUID> minionUuids = new HashSet<>();
    private int summonCooldown = 0;
    private int attackCooldown = 0;
    
    private static final int MAX_MINIONS = 20;
    private static final int SUMMON_COOLDOWN = 100; // 5 seconds
    private static final int ATTACK_COOLDOWN = 40; // 2 seconds
    
    public MobWarlordEntity(EntityType<? extends WitchEntity> entityType, World world) {
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
        // Don't call super - we're completely replacing witch behavior
        // Note: We can't clear() in this API version, but our high-priority goals will override witch's goals
        
        // Add custom warlord goals with highest priorities
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MobWarlordAttackGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        
        // Targeting priorities:
        // 1. Protect minions - attack anyone who hurts them (HIGHEST PRIORITY)
        // 2. Revenge - attack anyone who attacks the warlord
        // 3. Target all players
        // 4. Target all other mobs (except minions)
        this.targetSelector.add(0, new ProtectMinionsGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false,
            entity -> entity instanceof MobEntity mob && !isMinionOf(mob)));
    }
    
    public static DefaultAttributeContainer.Builder createMobWarlordAttributes() {
        return WitchEntity.createWitchAttributes()
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
        
        // Particle effects (skip if world not ready, wait 60 ticks = 3 seconds)
        if (this.age % 20 == 0 && this.age > 60) {
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
                
                // Ensure minion doesn't despawn
                minion.setPersistent();
                
                // Spawn the entity first
                boolean spawned = serverWorld.spawnEntity(minion);
                
                if (spawned) {
                    // AFTER spawning, register as minion in static map
                    MINION_TO_WARLORD.put(minion.getUuid(), this.getUuid());
                    minionUuids.add(minion.getUuid());
                    
                    // Set target to warlord's target
                    if (this.getTarget() != null) {
                        minion.setTarget(this.getTarget());
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
     */
    private void cleanupDeadMinions() {
        if (this.getWorld() == null || !(this.getWorld() instanceof ServerWorld serverWorld)) return;
        if (this.age < 2) return; // Skip during initialization
        
        try {
            minionUuids.removeIf(uuid -> {
                Entity entity = serverWorld.getEntity(uuid);
                boolean isDead = entity == null || !entity.isAlive();
                
                // Clean up from static map if dead
                if (isDead) {
                    MINION_TO_WARLORD.remove(uuid);
                }
                
                return isDead;
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
     * Performs a ranged potion attack with custom effects.
     * 70% chance: Harmful potion for enemies (poison, weakness, slowness, wither)
     * 30% chance: Beneficial potion for minions (strength, speed, resistance, regeneration)
     */
    public void performRangedAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        
        Vec3d vec3d = target.getEyePos().subtract(this.getEyePos());
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        
        // 70% chance for harmful potion, 30% for beneficial (to buff minions)
        boolean harmfulPotion = this.random.nextFloat() < 0.7f;
        
        if (harmfulPotion) {
            // HARMFUL POTION: For players and enemies
            // Multiple debilitating effects
            List<StatusEffectInstance> harmfulEffects = List.of(
                new StatusEffectInstance(StatusEffects.POISON, 200, 1),     // 10 seconds, Poison II
                new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1),   // 15 seconds, Weakness II
                new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1),   // 10 seconds, Slowness II
                new StatusEffectInstance(StatusEffects.WITHER, 100, 0)      // 5 seconds, Wither I
            );
            
            potion.set(DataComponentTypes.POTION_CONTENTS, 
                new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0x330033), harmfulEffects));
            
            // Dark purple particles for harmful potion
            if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
                try {
                    serverWorld.spawnParticles(ParticleTypes.WITCH,
                        this.getX(), this.getY() + 1.5, this.getZ(),
                        10, 0.3, 0.3, 0.3, 0.05);
                } catch (Exception e) {
                    // Ignore particle errors
                }
            }
        } else {
            // BENEFICIAL POTION: For minions (area effect to help allies)
            // Multiple powerful buffs
            List<StatusEffectInstance> beneficialEffects = List.of(
                new StatusEffectInstance(StatusEffects.STRENGTH, 400, 1),      // 20 seconds, Strength II
                new StatusEffectInstance(StatusEffects.SPEED, 400, 1),         // 20 seconds, Speed II
                new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0),    // 20 seconds, Resistance I
                new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0)   // 10 seconds, Regeneration I
            );
            
            potion.set(DataComponentTypes.POTION_CONTENTS,
                new PotionContentsComponent(java.util.Optional.empty(), java.util.Optional.of(0xFF00FF), beneficialEffects));
            
            // Bright purple particles for beneficial potion
            if (this.getWorld() instanceof ServerWorld serverWorld && this.age > 60) {
                try {
                    serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getY() + 1.5, this.getZ(),
                        15, 0.3, 0.3, 0.3, 0.1);
                } catch (Exception e) {
                    // Ignore particle errors
                }
            }
        }
        
        potionEntity.setItem(potion);
        potionEntity.setVelocity(vec3d.x, vec3d.y + vec3d.length() * 0.2, vec3d.z, 0.75f, 8.0f);
        this.getWorld().spawnEntity(potionEntity);
        this.playSound(SoundEvents.ENTITY_WITCH_THROW, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        
        attackCooldown = ATTACK_COOLDOWN;
    }
    
    /**
     * Performs a melee attack with knockback and particles.
     */
    public void performMeleeAttack(LivingEntity target) {
        if (attackCooldown > 0) return;
        
        this.tryAttack(target);
        
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
        if (this.getWorld() == null) return;
        if (this.age < 60) return; // Wait 3 seconds for full initialization
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
                        if (this.age > 60) { // Only if fully initialized (3 seconds)
                            serverWorld.spawnParticles(ParticleTypes.POOF,
                                minion.getX(), minion.getY() + 1.0, minion.getZ(),
                                10, 0.3, 0.3, 0.3, 0.05);
                        }
                    } catch (Exception e) {
                        // Silently ignore particle errors
                    }
                }
                
                // Clean up from static map
                MINION_TO_WARLORD.remove(minionUuid);
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
            // Only check every 10 ticks for performance
            if (checkCooldown > 0) {
                checkCooldown--;
                return false;
            }
            checkCooldown = 10;
            
            // Don't override if warlord is already targeting someone attacking it
            if (this.warlord.getAttacker() != null) {
                return false;
            }
            
            // Check if any minions are being attacked
            if (!(this.warlord.getWorld() instanceof ServerWorld serverWorld)) return false;
            
            for (UUID minionUuid : this.warlord.minionUuids) {
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

