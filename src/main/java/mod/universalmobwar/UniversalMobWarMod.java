package mod.universalmobwar;

import mod.universalmobwar.goal.UniversalTargetGoal;
import mod.universalmobwar.mixin.GameRulesAccessor;
import mod.universalmobwar.mixin.MobEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalMobWarMod implements ModInitializer {

	public static final String MODID = "universalmobwar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	// Gamerule: true = mod is active (default). false = mod is completely disabled.
	public static GameRules.Key<GameRules.BooleanRule> MOD_ENABLED_RULE;

	// Gamerule: true = ignore same-species (default). false = chaos (same-species allowed).
	public static GameRules.Key<GameRules.BooleanRule> IGNORE_SAME_SPECIES_RULE;

	@Override
	public void onInitialize() {
		// Register gamerule to enable/disable the entire mod (default: ON)
		MOD_ENABLED_RULE = GameRulesAccessor.GameRulesInvoker.invokeRegister(
			"universalMobWarEnabled",
			GameRules.Category.MOBS,
			GameRulesAccessor.BooleanRuleInvoker.invokeCreate(true, (server, rule) -> {})
		);

		// Register gamerule for same-species targeting (default: ON = ignore same species)
		IGNORE_SAME_SPECIES_RULE = GameRulesAccessor.GameRulesInvoker.invokeRegister(
			"universalMobWarIgnoreSame",
			GameRules.Category.MOBS,
			GameRulesAccessor.BooleanRuleInvoker.invokeCreate(true, (server, rule) -> {}) // default ON = ignore same-species
		);

		// Send welcome message to players when they join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("    UNIVERSAL MOB WAR").styled(style -> style.withColor(Formatting.RED).withBold(true))
					.append(Text.literal(" - Active!").styled(style -> style.withColor(Formatting.YELLOW))),
				false
			);
			handler.player.sendMessage(
				Text.literal("═══════════════════════════════════════════════════").styled(style -> style.withColor(Formatting.GOLD).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("Every mob attacks different species (including YOU!)").styled(style -> style.withColor(Formatting.WHITE)),
				false
			);
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("Available Commands:").styled(style -> style.withColor(Formatting.AQUA).withBold(true)),
				false
			);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("/gamerule universalMobWarEnabled <true|false>").styled(style -> style.withColor(Formatting.GREEN))),
				false
			);
			handler.player.sendMessage(
				Text.literal("    ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("└─ Enable/disable the entire mod (default: true)").styled(style -> style.withColor(Formatting.DARK_GRAY))),
				false
			);
			handler.player.sendMessage(Text.literal(""), false);
			handler.player.sendMessage(
				Text.literal("  • ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("/gamerule universalMobWarIgnoreSame <true|false>").styled(style -> style.withColor(Formatting.GREEN))),
				false
			);
			handler.player.sendMessage(
				Text.literal("    ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("└─ true = ignore same species (default)").styled(style -> style.withColor(Formatting.DARK_GRAY))),
				false
			);
			handler.player.sendMessage(
				Text.literal("    ").styled(style -> style.withColor(Formatting.GRAY))
					.append(Text.literal("└─ false = TOTAL CHAOS! Same species fight!").styled(style -> style.withColor(Formatting.RED))),
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

			// Check if the mod is enabled
			if (!world.getGameRules().getBoolean(MOD_ENABLED_RULE)) return;

			GoalSelector targetSelector = ((MobEntityAccessor) mob).getTargetSelector();

			// Check if our goal is already added to prevent duplicates on chunk reload
			boolean alreadyHasGoal = targetSelector.getGoals().stream()
				.anyMatch(goal -> goal.getGoal() instanceof UniversalTargetGoal);
			
			if (!alreadyHasGoal) {
				// Priority 2 so it competes well with vanilla targeting without breaking boss-specific logic.
				targetSelector.add(2, new UniversalTargetGoal(mob, 
					() -> world.getGameRules().getBoolean(MOD_ENABLED_RULE),
					() -> world.getGameRules().getBoolean(IGNORE_SAME_SPECIES_RULE)
				));
			}
		});

		LOGGER.info("Universal Mob War initialized successfully!");
	}

	public static boolean isModEnabled(MinecraftServer server) {
		return server.getGameRules().getBoolean(MOD_ENABLED_RULE);
	}

	public static boolean ignoreSameSpecies(MinecraftServer server) {
		return server.getGameRules().getBoolean(IGNORE_SAME_SPECIES_RULE);
	}
}

