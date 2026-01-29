package mod.universalmobwar.command;
import net.minecraft.util.math.BlockPos;

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

        // Alias command requested: /universalmobwar reload
        dispatcher.register(CommandManager.literal("universalmobwar")
            .then(CommandManager.literal("help")
                .executes(MobWarCommand::executeHelp))
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
            Text.literal("  • /kit [player]")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(" - Max gear kit (self or OP target)")
                    .styled(style -> style.withColor(Formatting.GRAY))), false);
        
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
        // Removed unused variable: BlockPos pos = BlockPos.ofFloored(source.getPosition());
        
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
        ModConfig config = ModConfig.reload();

        source.sendFeedback(() -> Text.literal("Universal Mob War config reloaded from disk.")
            .styled(style -> style.withColor(Formatting.GREEN).withBold(true)), true);

        // Print all config fields (excluding lists for brevity)
        if (config != null) {
            source.sendFeedback(() -> Text.literal("--- Universal Mob War Config ---")
                .styled(style -> style.withColor(Formatting.AQUA)), false);

            source.sendFeedback(() -> Text.literal("modEnabled: ").append(Text.literal(String.valueOf(config.modEnabled)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("targetingEnabled: ").append(Text.literal(String.valueOf(config.targetingEnabled)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("targetPlayers: ").append(Text.literal(String.valueOf(config.targetPlayers)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("ignoreSameSpecies: ").append(Text.literal(String.valueOf(config.ignoreSameSpecies)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("neutralMobsAlwaysAggressive: ").append(Text.literal(String.valueOf(config.neutralMobsAlwaysAggressive)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("disableNaturalMobSpawns: ").append(Text.literal(String.valueOf(config.disableNaturalMobSpawns)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("limitNaturalMobSpawns: ").append(Text.literal(String.valueOf(config.limitNaturalMobSpawns)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("naturalSpawnCapPerSimulationDistance: ").append(Text.literal(String.valueOf(config.naturalSpawnCapPerSimulationDistance)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("rangeMultiplierPercent: ").append(Text.literal(String.valueOf(config.rangeMultiplierPercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("allianceEnabled: ").append(Text.literal(String.valueOf(config.allianceEnabled)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("weakAllianceDurationMs: ").append(Text.literal(String.valueOf(config.weakAllianceDurationMs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("strongAllianceDurationMs: ").append(Text.literal(String.valueOf(config.strongAllianceDurationMs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("allianceBreakChancePercent: ").append(Text.literal(String.valueOf(config.allianceBreakChancePercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("strongAllianceBreakChancePercent: ").append(Text.literal(String.valueOf(config.strongAllianceBreakChancePercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("scalingEnabled: ").append(Text.literal(String.valueOf(config.scalingEnabled)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("dayScalingMultiplierPercent: ").append(Text.literal(String.valueOf(config.dayScalingMultiplierPercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("killScalingMultiplierPercent: ").append(Text.literal(String.valueOf(config.killScalingMultiplierPercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("buyChancePercent: ").append(Text.literal(String.valueOf(config.buyChancePercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("saveChancePercent: ").append(Text.literal(String.valueOf(config.saveChancePercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("maxUpgradeIterations: ").append(Text.literal(String.valueOf(config.maxUpgradeIterations)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("allowBossScaling: ").append(Text.literal(String.valueOf(config.allowBossScaling)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("scaleMobXpDropsWithSpentPoints: ").append(Text.literal(String.valueOf(config.scaleMobXpDropsWithSpentPoints)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("spentPointsPerXpBonusStep: ").append(Text.literal(String.valueOf(config.spentPointsPerXpBonusStep)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("xpBonusPercentPerStep: ").append(Text.literal(String.valueOf(config.xpBonusPercentPerStep)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("manualWorldDayOverride: ").append(Text.literal(String.valueOf(config.manualWorldDayOverride)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("upgradeIntervalTicks: ").append(Text.literal(String.valueOf(config.upgradeIntervalTicks)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("forceSpendAllOnSpawn: ").append(Text.literal(String.valueOf(config.forceSpendAllOnSpawn)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("maxSpawnUpgradeIterations: ").append(Text.literal(String.valueOf(config.maxSpawnUpgradeIterations)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("forceSyncSpawnUpgrade: ").append(Text.literal(String.valueOf(config.forceSyncSpawnUpgrade)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("logSpawnPointSummary: ").append(Text.literal(String.valueOf(config.logSpawnPointSummary)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("allowModdedEnchantments: ").append(Text.literal(String.valueOf(config.allowModdedEnchantments)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordEnabled: ").append(Text.literal(String.valueOf(config.warlordEnabled)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("alwaysSpawnWarlordOnFinalWave: ").append(Text.literal(String.valueOf(config.alwaysSpawnWarlordOnFinalWave)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordSpawnChance: ").append(Text.literal(String.valueOf(config.warlordSpawnChance)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordMinRaidLevel: ").append(Text.literal(String.valueOf(config.warlordMinRaidLevel)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordMinionCount: ").append(Text.literal(String.valueOf(config.warlordMinionCount)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordHealthMultiplierPercent: ").append(Text.literal(String.valueOf(config.warlordHealthMultiplierPercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("warlordDamageMultiplierPercent: ").append(Text.literal(String.valueOf(config.warlordDamageMultiplierPercent)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("performanceMode: ").append(Text.literal(String.valueOf(config.performanceMode)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("enableBatching: ").append(Text.literal(String.valueOf(config.enableBatching)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("enableAsyncTasks: ").append(Text.literal(String.valueOf(config.enableAsyncTasks)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("maxConcurrentUpgradeJobs: ").append(Text.literal(String.valueOf(config.maxConcurrentUpgradeJobs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("upgradeProcessingTimeMs: ").append(Text.literal(String.valueOf(config.upgradeProcessingTimeMs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("targetingCacheMs: ").append(Text.literal(String.valueOf(config.targetingCacheMs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("targetingMaxQueriesPerTick: ").append(Text.literal(String.valueOf(config.targetingMaxQueriesPerTick)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("mobDataSaveDebounceMs: ").append(Text.literal(String.valueOf(config.mobDataSaveDebounceMs)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("cleanupNonPlayerGroundProjectiles: ").append(Text.literal(String.valueOf(config.cleanupNonPlayerGroundProjectiles)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("cleanupNonPlayerGroundProjectilesIntervalSeconds: ").append(Text.literal(String.valueOf(config.cleanupNonPlayerGroundProjectilesIntervalSeconds)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("cleanupNonPlayerGroundProjectilesMinAgeTicks: ").append(Text.literal(String.valueOf(config.cleanupNonPlayerGroundProjectilesMinAgeTicks)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("cleanupNonPlayerGroundProjectilesMaxPerWorldPerRun: ").append(Text.literal(String.valueOf(config.cleanupNonPlayerGroundProjectilesMaxPerWorldPerRun)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("showTargetLines: ").append(Text.literal(String.valueOf(config.showTargetLines)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("showLevelParticles: ").append(Text.literal(String.valueOf(config.showLevelParticles)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("disableParticles: ").append(Text.literal(String.valueOf(config.disableParticles)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("minFpsForVisuals: ").append(Text.literal(String.valueOf(config.minFpsForVisuals)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("maxParticlesPerConnection: ").append(Text.literal(String.valueOf(config.maxParticlesPerConnection)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("maxDrawnMinionConnections: ").append(Text.literal(String.valueOf(config.maxDrawnMinionConnections)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("debugUpgradeLog: ").append(Text.literal(String.valueOf(config.debugUpgradeLog)).styled(s -> s.withColor(Formatting.YELLOW))), false);
            source.sendFeedback(() -> Text.literal("debugLogging: ").append(Text.literal(String.valueOf(config.debugLogging)).styled(s -> s.withColor(Formatting.YELLOW))), false);
        }

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
            Text.literal("[SUCCESS] Next raid will GUARANTEE a Mob Warlord spawn on the final wave!")
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

