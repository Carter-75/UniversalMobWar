package mod.universalmobwar;

import mod.universalmobwar.command.MobWarCommand;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.entity.MobWarlordEntity;
import mod.universalmobwar.goal.StalemateBreakerGoal;
import mod.universalmobwar.goal.UniversalTargetGoal;
import mod.universalmobwar.mixin.MobEntityAccessor;
// Evolution system handled by individual mob mixins
import mod.universalmobwar.system.AllianceSystem;
import mod.universalmobwar.util.TargetingUtil;
import mod.universalmobwar.util.OperationScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class UniversalMobWarMod implements ModInitializer {

	public static final String MODID = "universalmobwar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

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
		// Register and load config
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		ModConfig config = ModConfig.getInstance();
		
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

		// Send welcome message to players when they join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("    UNIVERSAL MOB WAR").styled(style -> style.withColor(Formatting.RED).withBold(true))
					.append(Text.literal(" v2.0 - EVOLUTION UPDATE!").styled(style -> style.withColor(Formatting.YELLOW))),
				false
			);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			
			handler.player.sendMessage(
				Text.literal("⚔ NEW FEATURES:").styled(style -> style.withColor(Formatting.RED).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Evolution System").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Mobs level up, gain stats & equipment!").styled(style -> style.withColor(Formatting.WHITE))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Alliance System").styled(style -> style.withColor(Formatting.AQUA)))
					.append(Text.literal(" - Mobs team up against common enemies!").styled(style -> style.withColor(Formatting.WHITE))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Player Immunity").styled(style -> style.withColor(Formatting.GREEN)))
					.append(Text.literal(" - Toggle to spectate safely!").styled(style -> style.withColor(Formatting.WHITE))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Range Control").styled(style -> style.withColor(Formatting.LIGHT_PURPLE)))
					.append(Text.literal(" - 0.01x to 100x detection range!").styled(style -> style.withColor(Formatting.WHITE))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("Neutral Mob Control").styled(style -> style.withColor(Formatting.GOLD)))
					.append(Text.literal(" - Force passive mobs to fight!").styled(style -> style.withColor(Formatting.WHITE))),
				false
			);
			
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("📋 Quick Commands:").styled(style -> style.withColor(Formatting.AQUA).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("/mobwar help").styled(style -> style.withColor(Formatting.GREEN)))
					.append(Text.literal(" - Full command list").styled(style -> style.withColor(Formatting.DARK_GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("/mobwar stats").styled(style -> style.withColor(Formatting.GREEN)))
					.append(Text.literal(" - View nearby mob levels").styled(style -> style.withColor(Formatting.DARK_GRAY))),
				false
			);
			
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("⚙ Key Settings (config/Mod Menu):").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.DARK_GRAY))
					.append(Text.literal("modEnabled").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Turn mod on/off").styled(style -> style.withColor(Formatting.GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.DARK_GRAY))
					.append(Text.literal("targetPlayers").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Player immunity toggle").styled(style -> style.withColor(Formatting.GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.DARK_GRAY))
					.append(Text.literal("rangeMultiplier").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Scale detection range (0.01x to 100x)").styled(style -> style.withColor(Formatting.GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.DARK_GRAY))
					.append(Text.literal("evolutionSystemEnabled").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Enable leveling system").styled(style -> style.withColor(Formatting.GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.DARK_GRAY))
					.append(Text.literal("allianceSystemEnabled").styled(style -> style.withColor(Formatting.YELLOW)))
					.append(Text.literal(" - Enable alliance system").styled(style -> style.withColor(Formatting.GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("    All options are available in the config file or Mod Menu!").styled(style -> style.withColor(Formatting.DARK_GRAY).withItalic(true)),
				false
			);
			
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("    Watch mobs evolve into warriors! Good luck!").styled(style -> style.withColor(Formatting.WHITE).withItalic(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
		});

		// Attach our targeting goal to every MobEntity when it loads into a server world.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof MobEntity mob)) return;

			// Evolution System - handled by individual mob mixins
			// Each mob's mixin contains its own progression logic

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
				var configSupplier = ModConfig::getInstance;
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

		// OPTIMIZATION: Register cache cleanup for entity query system (every 5 seconds)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 100 == 0) { // Every 5 seconds
				TargetingUtil.cleanupCache();
				OperationScheduler.cleanup(); // Also cleanup operation scheduler
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

}

