package mod.universalmobwar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mod.universalmobwar.UniversalMobWarMod;
import mod.universalmobwar.config.ModConfig;
import mod.universalmobwar.data.MobWarData;
import mod.universalmobwar.entity.MobWarlordEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
            .then(CommandManager.literal("summon")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("warlord")
                    .executes(MobWarCommand::executeSummonWarlord)))
            .then(CommandManager.literal("raid")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("forceboss")
                    .executes(MobWarCommand::executeForceRaidBoss)))
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
            Text.literal("Boss:").styled(style -> style.withColor(Formatting.DARK_PURPLE).withBold(true)), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar summon warlord")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Summon Mob Warlord boss (OP)")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /mobwar raid forceboss")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Guarantee boss in next raid (OP)")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • /summon universalmobwar:mob_warlord")
                .styled(style -> style.withColor(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" - Summon the Mob Warlord boss")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
        source.sendFeedback(() -> 
            Text.literal("  • Or use Mob Warlord Spawn Egg")
                .styled(style -> style.withColor(Formatting.LIGHT_PURPLE)), false);
        
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
        
        // Get all mob entities in the world
        int count = 0;
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof MobEntity mob) {
                mob.setTarget(null);
                count++;
            }
        }
        
        int finalCount = count;
        source.sendFeedback(() -> 
            Text.literal("Reset targets for " + finalCount + " mobs.")
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
    
    private static int executeSummonWarlord(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Vec3d pos = source.getPosition();
        
        try {
            // Create the Mob Warlord
            MobWarlordEntity warlord = new MobWarlordEntity(UniversalMobWarMod.MOB_WARLORD, world);
            warlord.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0f, 0.0f);
            warlord.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(pos)), SpawnReason.COMMAND, null);
            
            world.spawnEntity(warlord);
            
            source.sendFeedback(() -> 
                Text.literal("Summoned the Mob Warlord boss at ")
                    .styled(style -> style.withColor(Formatting.DARK_PURPLE))
                    .append(Text.literal(String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z))
                        .styled(style -> style.withColor(Formatting.GOLD)))
                    .append(Text.literal("!")
                        .styled(style -> style.withColor(Formatting.DARK_PURPLE))), 
                true
            );
            
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to summon Mob Warlord: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeForceRaidBoss(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // This is a simple flag setter - actual logic is in RaidBossSpawnCommand
        mod.universalmobwar.command.RaidBossSpawnCommand.shouldForceSpawn();
        
        source.sendFeedback(() -> 
            Text.literal("✅ Next raid will GUARANTEE a Mob Warlord spawn on the final wave!")
                .styled(style -> style.withColor(Formatting.GREEN).withBold(true)), 
            true
        );
        
        source.sendFeedback(() -> 
            Text.literal("  Start a raid to summon the boss!")
                .styled(style -> style.withColor(Formatting.GRAY)), 
            false
        );
        
        return 1;
    }
}

