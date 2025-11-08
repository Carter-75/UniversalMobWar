package mod.universalmobwar.client;

import mod.universalmobwar.data.MobWarData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side visual enhancements for spectator mode.
 * Shows target lines, health bars, and mob labels.
 */
public class MobWarVisuals {
    
    /**
     * Gets display text for a mob showing their level and stats.
     */
    public static Text getMobDisplayName(MobEntity mob) {
        MobWarData data = MobWarData.get(mob);
        int level = data.getLevel();
        
        if (level == 0) {
            return mob.getDisplayName();
        }
        
        // Format: [Lv.5] Zombie ⚔
        net.minecraft.text.MutableText levelText = Text.literal("[Lv." + level + "] ")
            .styled(style -> style.withColor(getLevelColor(level)).withBold(true));
        
        levelText.append(mob.getDisplayName());
        
        // Add weapon indicator if mob has weapon
        if (!mob.getMainHandStack().isEmpty()) {
            levelText.append(Text.literal(" ⚔").styled(style -> style.withColor(Formatting.GOLD)));
        }
        
        return levelText;
    }
    
    /**
     * Gets color based on mob level.
     */
    private static Formatting getLevelColor(int level) {
        if (level >= 50) return Formatting.DARK_RED;
        if (level >= 30) return Formatting.RED;
        if (level >= 20) return Formatting.GOLD;
        if (level >= 10) return Formatting.YELLOW;
        return Formatting.GREEN;
    }
    
    /**
     * Gets status text for a mob (current target info).
     */
    public static Text getMobStatusText(MobEntity mob) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return Text.literal("Idle").styled(style -> style.withColor(Formatting.GRAY));
        }
        
        return Text.literal("→ ")
            .styled(style -> style.withColor(Formatting.RED))
            .append(Text.literal(target.getDisplayName().getString())
                .styled(style -> style.withColor(Formatting.WHITE)));
    }
    
    /**
     * Gets detailed stats text for tooltips.
     */
    public static Text getMobStatsText(MobEntity mob) {
        MobWarData data = MobWarData.get(mob);
        
        return Text.literal("Level: " + data.getLevel())
            .styled(style -> style.withColor(Formatting.YELLOW))
            .append(Text.literal(" | Kills: " + data.getKillCount())
                .styled(style -> style.withColor(Formatting.RED)))
            .append(Text.literal(" | Allies: " + data.getAllies().size())
                .styled(style -> style.withColor(Formatting.AQUA)));
    }
}

