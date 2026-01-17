package mod.universalmobwar;

import mod.universalmobwar.command.KitCommand;
import mod.universalmobwar.command.MobWarCommand;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.entity.MobWarlordEntity;
import mod.universalmobwar.goal.StalemateBreakerGoal;
import mod.universalmobwar.goal.UniversalTargetGoal;
import mod.universalmobwar.mixin.GameRulesAccessor;
import mod.universalmobwar.mixin.MobEntityAccessor;
import mod.universalmobwar.net.UmwRequiredClientPayload;
import mod.universalmobwar.net.UmwServerEnchantCompat;
// Evolution system handled globally via MobDataMixin + ScalingSystem
import mod.universalmobwar.system.AllianceSystem;
import mod.universalmobwar.system.NaturalSpawnLimiter;
import mod.universalmobwar.system.EntityCleanupSystem;
import mod.universalmobwar.system.ScalingSystem;
import mod.universalmobwar.util.TargetingUtil;
import mod.universalmobwar.util.OperationScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class UniversalMobWarMod implements ModInitializer {

	public static final String MODID = "universalmobwar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static final GameRules.Key<GameRules.BooleanRule> IGNORE_SAME_SPECIES_RULE = registerBooleanRule(
		"universalmobwarIgnoreSameSpecies",
		GameRules.Category.MOBS,
		true,
		(server, rule) -> {
			ModConfig config = ModConfig.getInstance();
			if (config.ignoreSameSpecies != rule.get()) {
				config.ignoreSameSpecies = rule.get();
				ModConfig.save();
			}
		}
	);

	public static final GameRules.Key<GameRules.BooleanRule> NEUTRAL_MOBS_AGGRESSIVE_RULE = registerBooleanRule(
		"universalmobwarNeutralMobsAggressive",
		GameRules.Category.MOBS,
		false,
		(server, rule) -> {
			ModConfig config = ModConfig.getInstance();
			if (config.neutralMobsAlwaysAggressive != rule.get()) {
				config.neutralMobsAlwaysAggressive = rule.get();
				ModConfig.save();
			}
		}
	);

	// Mob Warlord Boss Entity (uses HostileEntity for Iris Shaders compatibility)
	public static final EntityType<MobWarlordEntity> MOB_WARLORD = Registry.register(
		Registries.ENTITY_TYPE,
		Identifier.of(MODID, "mob_warlord"),
		EntityType.Builder.create(MobWarlordEntity::new, SpawnGroup.MONSTER)
			.dimensions(1.2f, 3.6f) // Large boss size (1.2m wide x 3.6m tall)
			.maxTrackingRange(64)
			.build()
	);
	
	// Mob Warlord Spawn Egg (same colors as witch egg)
	public static final Item MOB_WARLORD_SPAWN_EGG = Registry.register(
		Registries.ITEM,
		Identifier.of(MODID, "mob_warlord_spawn_egg"),
		new SpawnEggItem(MOB_WARLORD, 0x334E4C, 0x51A03E, new Item.Settings())
	);

	@Override
	public void onInitialize() {
		// Register an empty S2C payload solely so the server can verify the client has this mod.
		// This makes "server has mod, client doesn't" a hard-fail join (as requested).
		try {
			PayloadTypeRegistry.playS2C().register(UmwRequiredClientPayload.ID, UmwRequiredClientPayload.CODEC);
		} catch (IllegalArgumentException alreadyRegistered) {
			// Tolerate double-init in dev / hot reload scenarios.
		}

		// Register and load config
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ModConfig config = ModConfig.getInstance();

		UmwServerEnchantCompat.init();
		ServerLifecycleEvents.SERVER_STARTED.register(UniversalMobWarMod::syncGameRulesWithConfig);
		
		// Register Skill Tree Events (Projectiles, etc.)
		// SkillTreeEvents.register();
		
		// Register Mob Warlord attributes
		FabricDefaultAttributeRegistry.register(MOB_WARLORD, MobWarlordEntity.createMobWarlordAttributes());
		
		// Add spawn egg to creative inventory
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
			content.add(MOB_WARLORD_SPAWN_EGG);
		});
		
		// Register commands
		CommandRegistrationCallback.EVENT.register(MobWarCommand::register);
		CommandRegistrationCallback.EVENT.register(KitCommand::register);

		// Send welcome message to players when they join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// If the client doesn't have this mod installed, they won't advertise our payload,
			// and we reject the connection with a clear message.
			if (!ServerPlayNetworking.canSend(handler.player, UmwRequiredClientPayload.ID)) {
				handler.disconnect(Text.literal("Universal Mob War is required to join this server."));
				return;
			}

			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("    UNIVERSAL MOB WAR").styled(style -> style.withColor(Formatting.RED).withBold(true))
					,
				false
			);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
		});

		// Attach our targeting goal to every MobEntity when it loads into a server world.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			// Track naturally spawned mobs for spawn limiting.
			if (world instanceof ServerWorld serverWorld && entity instanceof MobEntity mob) {
				NaturalSpawnLimiter.onMobLoaded(serverWorld, mob.getCommandTags().contains("umw_natural_spawned"));
			}

			if (!(entity instanceof MobEntity mob)) return;

			// Evolution System now handled globally via MobDataMixin + ScalingSystem
			// Each mob automatically inherits progression logic from its JSON config

			// Check if the mod is enabled
			if (!ModConfig.getInstance().isTargetingActive()) return;

			// Check if this mob type is excluded
			String mobId = mob.getType().getTranslationKey();
			if (ModConfig.getInstance().isMobExcluded(mobId)) return;

			GoalSelector targetSelector = ((MobEntityAccessor) mob).getTargetSelector();
			GoalSelector goalSelector = ((MobEntityAccessor) mob).getGoalSelector();

			// Check if our goal is already added to prevent duplicates on chunk reload
			boolean alreadyHasGoal = targetSelector.getGoals().stream()
				.anyMatch(goal -> goal.getGoal() instanceof UniversalTargetGoal);

			if (!alreadyHasGoal) {
				Supplier<ModConfig> configSupplier = ModConfig::getInstance;
				// Add targeting goal
				targetSelector.add(2, new UniversalTargetGoal(
					mob,
					() -> configSupplier.get().isTargetingActive(),
					() -> configSupplier.get().ignoreSameSpecies,
					() -> configSupplier.get().targetPlayers,
					() -> configSupplier.get().isAllianceActive(),
					() -> configSupplier.get().getRangeMultiplier()
				));
				
				// Add stalemate breaker goal (Priority 0 to ensure it always runs)
				goalSelector.add(0, new StalemateBreakerGoal(mob));
			}
		});

		ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
			if (world instanceof ServerWorld serverWorld && entity instanceof MobEntity mob) {
				NaturalSpawnLimiter.onMobUnloaded(serverWorld, mob.getCommandTags().contains("umw_natural_spawned"));
			}
		});

		// OPTIMIZATION: Register cache cleanup for entity query system (every 5 seconds)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Execute scheduled extra_shot follow-up cycles (projectiles only).
			ScalingSystem.processExtraShotQueue(server);

			if (server.getTicks() % 100 == 0) { // Every 5 seconds
				TargetingUtil.cleanupCache();
				OperationScheduler.cleanup(); // Also cleanup operation scheduler
			}

			// PERFORMANCE: Clean up non-player ground projectiles (arrows/tridents) on an interval.
			int intervalSeconds = Math.max(5, ModConfig.getInstance().cleanupNonPlayerGroundProjectilesIntervalSeconds);
			int intervalTicks = intervalSeconds * 20;
			if (server.getTicks() % intervalTicks == 0) {
				EntityCleanupSystem.cleanupNonPlayerGroundProjectiles(server);
			}
			
			// MEMORY LEAK FIX: Clean up dead mob UUIDs from AllianceSystem (every 60 seconds)
			if (server.getTicks() % 1200 == 0) {
				for (ServerWorld world : server.getWorlds()) {
					AllianceSystem.cleanupDeadMobs(world);
				}
			}
		});

		LOGGER.info("Universal Mob War initialized successfully!");
	}

	private static GameRules.Key<GameRules.BooleanRule> registerBooleanRule(
		String name,
		GameRules.Category category,
		boolean defaultValue,
		BiConsumer<MinecraftServer, GameRules.BooleanRule> changeCallback
	) {
		return GameRulesAccessor.GameRulesInvoker.invokeRegister(
			name,
			category,
			GameRulesAccessor.BooleanRuleInvoker.invokeCreate(defaultValue, changeCallback)
		);
	}

	private static void syncGameRulesWithConfig(MinecraftServer server) {
		ModConfig config = ModConfig.getInstance();
		GameRules.BooleanRule ignoreSameRule = server.getGameRules().get(IGNORE_SAME_SPECIES_RULE);
		if (ignoreSameRule.get() != config.ignoreSameSpecies) {
			ignoreSameRule.set(config.ignoreSameSpecies, server);
		}
		GameRules.BooleanRule neutralAggressiveRule = server.getGameRules().get(NEUTRAL_MOBS_AGGRESSIVE_RULE);
		if (neutralAggressiveRule.get() != config.neutralMobsAlwaysAggressive) {
			neutralAggressiveRule.set(config.neutralMobsAlwaysAggressive, server);
		}
	}

}

