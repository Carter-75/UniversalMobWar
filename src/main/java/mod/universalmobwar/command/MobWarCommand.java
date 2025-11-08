package mod.universalmobwar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Provides /mobwar commands for managing the mod.
 */
public class MobWarCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("mobwar")
            .then(CommandManager.literal("help")
                .executes(MobWarCommand::executeHelp))
            .then(CommandManager.literal("stats")
                .executes(MobWarCommand::executeStats))
            .then(CommandManager.literal("reset")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(MobWarCommand::executeReset))
            .then(CommandManager.literal("reload")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(MobWarCommand::executeReload))
            .executes(MobWarCommand::executeHelp)
        );
    }
    
    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> 
            Text.literal("═══════════════════════════════════════════════════")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
        source.sendFeedback(() -> 
            Text.literal("    UNIVERSAL MOB WAR - Commands")
                .styled(style -> style.withColor(Formatting.RED).withBold(true)), false);
        source.sendFeedback(() -> 
            Text.literal("═══════════════════════════════════════════════════")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
        
        source.sendFeedback(() -> 
            Text.literal("Commands:").styled(style -> style.withColor(Formatting.AQUA).withBold(true)), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar help")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Show this help message")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar stats")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Show nearby mob statistics")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar reset")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Clear all mob targets (OP)")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar reload")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Reload config file (OP)")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> Text.literal(""), false);
        source.sendFeedback(() -> 
            Text.literal("Game Rules:").styled(style -> style.withColor(Formatting.AQUA).withBold(true)), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarEnabled <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarIgnoreSame <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarTargetPlayers <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarNeutralAggressive <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarAlliances <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarEvolution <true|false>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("  • /gamerule universalMobWarRangeMultiplier <1-10000>")
                .styled(style -> style.withColor(Formatting.YELLOW)), false);
        source.sendFeedback(() -> 
            Text.literal("    (1=0.01x, 100=1.0x, 10000=100.0x range)")
                .styled(style -> style.withColor(Formatting.DARK_GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("═══════════════════════════════════════════════════")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
        
        return 1;
    }
    
    private static int executeStats(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        
        // Find mobs within 50 blocks
        List<MobEntity> nearbyMobs = world.getEntitiesByClass(
            MobEntity.class,
            source.getPlayer().getBoundingBox().expand(50),
            mob -> mob.isAlive()
        );
        
        if (nearbyMobs.isEmpty()) {
            source.sendFeedback(() -> 
                Text.literal("No mobs found within 50 blocks.")
                    .styled(style -> style.withColor(Formatting.YELLOW)), false);
            return 0;
        }
        
        source.sendFeedback(() -> 
            Text.literal("═══ Nearby Mob Statistics ═══")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
        
        int highestLevel = 0;
        MobEntity topMob = null;
        
        for (MobEntity mob : nearbyMobs) {
            MobWarData data = MobWarData.get(mob);
            if (data.getLevel() > highestLevel) {
                highestLevel = data.getLevel();
                topMob = mob;
            }
        }
        
        source.sendFeedback(() -> 
            Text.literal("Total Mobs: ")
                .styled(style -> style.withColor(Formatting.AQUA))
                .append(Text.literal(String.valueOf(nearbyMobs.size()))
                    .styled(style -> style.withColor(Formatting.WHITE))), false);
        
        if (topMob != null) {
            MobEntity finalTopMob = topMob;
            int finalHighestLevel = highestLevel;
            MobWarData topData = MobWarData.get(topMob);
            
            source.sendFeedback(() -> 
                Text.literal("Strongest Mob: ")
                    .styled(style -> style.withColor(Formatting.AQUA))
                    .append(Text.literal(finalTopMob.getType().getName().getString())
                        .styled(style -> style.withColor(Formatting.RED).withBold(true)))
                    .append(Text.literal(" (Level " + finalHighestLevel + ", " + topData.getKillCount() + " kills)")
                        .styled(style -> style.withColor(Formatting.YELLOW))), false);
        }
        
        // Count mobs with levels
        long veteranMobs = nearbyMobs.stream()
            .filter(mob -> MobWarData.get(mob).getLevel() >= 10)
            .count();
        
        if (veteranMobs > 0) {
            long finalVeteranMobs = veteranMobs;
            source.sendFeedback(() -> 
                Text.literal("Veteran Mobs (Lv10+): ")
                    .styled(style -> style.withColor(Formatting.AQUA))
                    .append(Text.literal(String.valueOf(finalVeteranMobs))
                        .styled(style -> style.withColor(Formatting.RED))), false);
        }
        
        source.sendFeedback(() -> 
            Text.literal("═══════════════════════════")
                .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);
        
        return 1;
    }
    
    private static int executeReset(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        
        List<MobEntity> allMobs = world.getEntitiesByClass(
            MobEntity.class,
            world.getBoundingBox(),
            mob -> true
        );
        
        for (MobEntity mob : allMobs) {
            mob.setTarget(null);
        }
        
        int count = allMobs.size();
        source.sendFeedback(() -> 
            Text.literal("Reset targets for " + count + " mobs.")
                .styled(style -> style.withColor(Formatting.GREEN)), true);
        
        return count;
    }
    
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        ModConfig config = ModConfig.getInstance();
        config.save(); // This will create a new config if it doesn't exist
        
        source.sendFeedback(() -> 
            Text.literal("Configuration reloaded successfully!")
                .styled(style -> style.withColor(Formatting.GREEN)), true);
        
        return 1;
    }
}

