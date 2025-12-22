package mod.universalmobwar.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implements the /kit command that hands out a maxed gear loadout using the config's top tiers.
 */
public final class KitCommand {

    private KitCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("kit")
            .executes(context -> giveKit(context, context.getSource().getPlayerOrThrow()))
            .then(CommandManager.argument("player", EntityArgumentType.player())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> giveKit(context, EntityArgumentType.getPlayer(context, "player"))))
        );
    }

    private static int giveKit(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) throws CommandSyntaxException {
        Registry<Enchantment> enchantRegistry = context.getSource().getServer().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        List<ItemStack> kitItems = buildKit(enchantRegistry);
        kitItems.forEach(target::giveItemStack);

        target.sendMessage(Text.literal("You received the Universal Arsenal kit!")
            .styled(style -> style.withColor(Formatting.GOLD).withBold(true)), false);

        ServerCommandSource source = context.getSource();
        if (source.getEntity() == null || source.getEntity() != target) {
            String targetName = target.getName().getString();
            source.sendFeedback(() -> Text.literal("Gave the Universal kit to ")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(targetName).styled(style -> style.withColor(Formatting.YELLOW).withBold(true))), true);
        }

        return kitItems.size();
    }

    private static List<ItemStack> buildKit(Registry<Enchantment> enchantRegistry) {
        List<ItemStack> kit = new ArrayList<>();

        kit.add(armorPiece(Items.NETHERITE_HELMET, stack -> {
            add(stack, enchantRegistry, Enchantments.PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.FIRE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.BLAST_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.PROJECTILE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.THORNS, 3);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
            add(stack, enchantRegistry, Enchantments.AQUA_AFFINITY, 1);
            add(stack, enchantRegistry, Enchantments.RESPIRATION, 3);
        }));

        kit.add(armorPiece(Items.NETHERITE_CHESTPLATE, stack -> {
            add(stack, enchantRegistry, Enchantments.PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.FIRE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.BLAST_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.PROJECTILE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.THORNS, 3);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        kit.add(armorPiece(Items.NETHERITE_LEGGINGS, stack -> {
            add(stack, enchantRegistry, Enchantments.PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.FIRE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.BLAST_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.PROJECTILE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.THORNS, 3);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
            add(stack, enchantRegistry, Enchantments.SWIFT_SNEAK, 3);
        }));

        kit.add(armorPiece(Items.NETHERITE_BOOTS, stack -> {
            add(stack, enchantRegistry, Enchantments.PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.FIRE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.BLAST_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.PROJECTILE_PROTECTION, 4);
            add(stack, enchantRegistry, Enchantments.THORNS, 3);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
            add(stack, enchantRegistry, Enchantments.FEATHER_FALLING, 4);
            add(stack, enchantRegistry, Enchantments.DEPTH_STRIDER, 3);
            add(stack, enchantRegistry, Enchantments.SOUL_SPEED, 3);
            add(stack, enchantRegistry, Enchantments.FROST_WALKER, 2);
        }));

        kit.add(weapon(Items.NETHERITE_SWORD, stack -> {
            add(stack, enchantRegistry, Enchantments.SHARPNESS, 5);
            add(stack, enchantRegistry, Enchantments.FIRE_ASPECT, 2);
            add(stack, enchantRegistry, Enchantments.KNOCKBACK, 2);
            add(stack, enchantRegistry, Enchantments.LOOTING, 3);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        kit.add(weapon(Items.TRIDENT, stack -> {
            add(stack, enchantRegistry, Enchantments.IMPALING, 5);
            add(stack, enchantRegistry, Enchantments.LOYALTY, 3);
            add(stack, enchantRegistry, Enchantments.RIPTIDE, 3);
            add(stack, enchantRegistry, Enchantments.CHANNELING, 1);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        kit.add(weapon(Items.CROSSBOW, stack -> {
            add(stack, enchantRegistry, Enchantments.QUICK_CHARGE, 3);
            add(stack, enchantRegistry, Enchantments.PIERCING, 4);
            add(stack, enchantRegistry, Enchantments.MULTISHOT, 1);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        kit.add(weapon(Items.BOW, stack -> {
            add(stack, enchantRegistry, Enchantments.POWER, 5);
            add(stack, enchantRegistry, Enchantments.PUNCH, 2);
            add(stack, enchantRegistry, Enchantments.FLAME, 1);
            add(stack, enchantRegistry, Enchantments.INFINITY, 1);
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        kit.add(weapon(Items.SHIELD, stack -> {
            add(stack, enchantRegistry, Enchantments.UNBREAKING, 3);
            add(stack, enchantRegistry, Enchantments.MENDING, 1);
        }));

        ItemStack arrow = new ItemStack(Items.ARROW);
        arrow.setCount(1);
        kit.add(arrow);

        kit.add(new ItemStack(Items.TOTEM_OF_UNDYING));

        return kit;
    }

    private static ItemStack armorPiece(Item item, Consumer<ItemStack> enchantments) {
        return withEnchantments(item, enchantments);
    }

    private static ItemStack weapon(Item item, Consumer<ItemStack> enchantments) {
        return withEnchantments(item, enchantments);
    }

    private static ItemStack withEnchantments(Item item, Consumer<ItemStack> enchantments) {
        ItemStack stack = new ItemStack(item);
        enchantments.accept(stack);
        return stack;
    }

    private static void add(ItemStack stack, Registry<Enchantment> enchantRegistry, RegistryKey<Enchantment> enchantmentKey, int level) {
        RegistryEntry<Enchantment> entry = enchantRegistry
            .getEntry(enchantmentKey)
            .orElseThrow(() -> new IllegalStateException("Missing enchantment: " + enchantmentKey.getValue()));
        stack.addEnchantment(entry, level);
    }
}
