package au.lupine.bottlet.command;

import au.lupine.bottlet.Bottlet;
import au.lupine.bottlet.api.Bottle;
import au.lupine.bottlet.api.Experience;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;


public final class BottletCommand {

    public static @NonNull LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("bottlet")
            .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet"))
            .executes(context -> {
                context.getSource().getSender().sendMessage(
                    Component.translatable(
                        "bottlet.command.bottlet.feedback",
                        Argument.string("version", Bottlet.instance().getPluginMeta().getVersion())
                    )
                );
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("convert")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.convert") && source.getSender() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    int experience = 0;
                    int bottles = 0;

                    PlayerInventory inventory = player.getInventory();
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (stack == null) continue;

                        if (stack.getType() != Material.EXPERIENCE_BOTTLE) continue;

                        int amount = stack.getAmount();
                        experience += Bottle.stored(stack) * amount;

                        bottles += amount;
                        inventory.setItem(slot, null);
                    }

                    if (bottles == 0) {
                        player.sendMessage(Component.translatable("bottlet.command.bottlet.convert.no_bottles"));
                        return 0;
                    }

                    Bottle.give(player, experience, 1);

                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("get")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.get") && source.getSender() instanceof Player)
                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                    .executes(context -> get(context, context.getArgument("quantity", Integer.class)))
                )
                .then(Commands.literal("max")
                    .executes(context -> {
                        Player player = (Player) context.getSource().getSender();

                        int defaultExperience = Bottlet.instance().config().root().node("bottle", "default_stored_experience").getInt(10);

                        int experience = Experience.experience(player);
                        if (experience < defaultExperience) {
                            player.sendMessage(
                                Component.translatable(
                                    "bottlet.command.bottlet.get.max.insufficient_experience",
                                    Argument.numeric("experience", defaultExperience),
                                    Argument.numeric("current", experience)
                                )
                            );
                            return 0;
                        }

                        int quantity = experience / defaultExperience;

                        return get(context, quantity);
                    })
                )
            )
            .then(Commands.literal("mend")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.mend") && source.getSender() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    ItemStack item = player.getInventory().getItemInMainHand();
                    ItemMeta meta = item.getItemMeta();

                    if (!(meta instanceof Damageable damageable) || !damageable.hasEnchant(Enchantment.MENDING)) {
                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.mend.invalid_item",
                                Argument.component("item", item.effectiveName())
                            )
                        );
                        return 0;
                    }

                    int damage = damageable.getDamage();
                    if (damage == 0) {
                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.mend.item_not_damaged",
                                Argument.component("item", item.effectiveName())
                            )
                        );
                        return 0;
                    }

                    int cost = (int) Math.ceil((double) damage / 2); // https://minecraft.wiki/w/Mending#Usage
                    int current = Experience.experience(player);

                    if (!(current >= cost)) {
                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.mend.insufficient_experience",
                                Argument.numeric("experience", cost),
                                Argument.component("item", item.effectiveName()),
                                Argument.numeric("current", current)
                            )
                        );
                        return 0;
                    }

                    damageable.setDamage(0);
                    item.setItemMeta(damageable);

                    Experience.change(player, -cost);

                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("all")
                    .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.mend.all"))
                    .executes(context -> {
                        Player player = (Player) context.getSource();

                        List<ItemStack> mendable = new ArrayList<>();

                        int damage = 0;
                        for (ItemStack item : player.getInventory()) {
                            if (item == null) continue;

                            ItemMeta meta = item.getItemMeta();

                            if (!(meta instanceof Damageable damageable) || !damageable.hasEnchant(Enchantment.MENDING)) continue;

                            if (damageable.getDamage() == 0) continue;

                            damage += damageable.getDamage();

                            mendable.add(item);
                        }

                        if (damage == 0) {
                            player.sendMessage(Component.translatable("bottlet.command.bottlet.mend.all.items_not_damaged"));
                            return 0;
                        }

                        int cost = (int) Math.ceil((double) damage / 2);
                        int current = Experience.experience(player);

                        if (!(current >= cost)) {
                            player.sendMessage(
                                Component.translatable(
                                    "bottlet.command.bottlet.mend.all.insufficient_experience",
                                    Argument.numeric("experience", cost),
                                    Argument.numeric("count", mendable.size()),
                                    Argument.numeric("current", current)
                                )
                            );
                            return 0;
                        }

                        for (ItemStack item : mendable) {
                            Damageable damageable = (Damageable) item.getItemMeta();

                            damageable.setDamage(0);
                            item.setItemMeta(damageable);
                        }

                        Experience.change(player, -cost);

                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("toggle")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.toggle"))
                .then(Commands.literal("thrown")
                    .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.toggle.thrown") && source.getSender() instanceof Player)
                    .executes(context -> {
                        Player player = (Player) context.getSource().getSender();

                        PersistentDataContainer pdc = player.getPersistentDataContainer();

                        Boolean current = pdc.get(Bottle.BOTTLET_SHOULD_THROW_BOTTLES, PersistentDataType.BOOLEAN);

                        boolean thrown;
                        if (current != null) {
                            thrown = !current;
                        } else {
                            thrown = false;
                        }

                        pdc.set(Bottle.BOTTLET_SHOULD_THROW_BOTTLES, PersistentDataType.BOOLEAN, thrown);

                        if (thrown) {
                            player.sendMessage(Component.translatable("bottlet.command.bottlet.toggle.thrown.feedback.thrown"));
                        } else {
                            player.sendMessage(Component.translatable("bottlet.command.bottlet.toggle.thrown.feedback.not_thrown"));
                        }

                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .build();
    }

    private static int get(@NonNull CommandContext<CommandSourceStack> context, int quantity) {
        Player player = (Player) context.getSource().getSender();

        int experience = quantity * Bottlet.instance().config().root().node("bottle", "default_stored_experience").getInt(10);
        int current = Experience.experience(player);

        if (experience > current || experience <= 0) {
            player.sendMessage(
                Component.translatable(
                    "bottlet.command.bottlet.get.insufficient_experience",
                    Argument.numeric("bottles", quantity),
                    Argument.numeric("experience", experience),
                    Argument.numeric("current", current)
                )
            );
            return 0;
        }

        Experience.change(player, -experience);

        Bottle.give(player, quantity);

        return Command.SINGLE_SUCCESS;
    }
}
