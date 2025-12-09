package mod.universalmobwar.command;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Command to force-spawn the Mob Warlord in the next raid.
 */
public class RaidBossSpawnCommand {
    
    private static boolean forceNextRaidBoss = false;
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("mobwar")
            .then(CommandManager.literal("raid")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("forceboss")
                    .executes(RaidBossSpawnCommand::executeForceBoss))
            )
        );
    }
    
    private static int executeForceBoss(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        forceNextRaidBoss = true;
        
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
    
    /**
     * Checks if the next raid should force-spawn the boss.
     */
    public static boolean shouldForceSpawn() {
        if (forceNextRaidBoss) {
            forceNextRaidBoss = false; // Reset after use
            return true;
        }
        return false;
    }
}

