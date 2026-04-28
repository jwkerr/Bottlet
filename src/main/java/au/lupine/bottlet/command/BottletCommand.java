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
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
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
                        player.sendMessage(Component.translatable("bottlet.command.bottlet.convert.feedback.no_bottles"));
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
                                    "bottlet.command.bottlet.get.max.feedback.insufficient_experience",
                                    Argument.string("experience", Bottlet.pretty(defaultExperience)),
                                    Argument.string("current", Bottlet.pretty(defaultExperience))
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
                                "bottlet.command.bottlet.mend.feedback.invalid_item",
                                Argument.component("item", item.effectiveName().colorIfAbsent(NamedTextColor.RED))
                            )
                        );
                        return 0;
                    }

                    int damage = damageable.getDamage();
                    if (damage == 0) {
                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.mend.feedback.item_not_damaged",
                                Argument.component("item", item.effectiveName().colorIfAbsent(NamedTextColor.RED))
                            )
                        );
                        return 0;
                    }

                    int cost = (int) Math.ceil((double) damage / 2); // https://minecraft.wiki/w/Mending#Usage
                    int current = Experience.experience(player);

                    if (!(current >= cost)) {
                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.mend.feedback.insufficient_experience",
                                Argument.string("experience", Bottlet.pretty(cost)),
                                Argument.component("item", item.effectiveName()),
                                Argument.string("current", Bottlet.pretty(current))
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
                        Player player = (Player) context.getSource().getSender();

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
                            player.sendMessage(Component.translatable("bottlet.command.bottlet.mend.all.feedback.items_not_damaged"));
                            return 0;
                        }

                        int cost = (int) Math.ceil((double) damage / 2);
                        int current = Experience.experience(player);

                        if (!(current >= cost)) {
                            player.sendMessage(
                                Component.translatable(
                                    "bottlet.command.bottlet.mend.all.feedback.insufficient_experience",
                                    Argument.string("experience", Bottlet.pretty(cost)),
                                    Argument.string("current", Bottlet.pretty(current))
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
            .then(Commands.literal("stats")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.stats") && source.getSender() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    int level = player.getLevel();

                    int currentExperience = Experience.experience(player);
                    int goalExperience = Experience.experience(level + 1);

                    int remaining = goalExperience - currentExperience;
                    int remainingBottles = (int) Math.ceil(remaining / Bottlet.instance().config().root().node("bottle", "default_store_experience").getDouble(10));

                    player.sendMessage(
                        Component.translatable(
                            "bottlet.command.bottlet.stats.feedback",
                            Argument.string("level", Bottlet.pretty(level)),
                            Argument.tagResolver(Formatter.choice("level_count", level)),
                            Argument.string("experience", Bottlet.pretty(currentExperience)),
                            Argument.string("remaining", Bottlet.pretty(remaining)),
                            Argument.string("bottles", Bottlet.pretty(remainingBottles)),
                            Argument.tagResolver(Formatter.choice("bottle_count", remainingBottles))
                        )
                    );

                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("target", ArgumentTypes.player())
                    .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.stats.other"))
                    .executes(context -> {
                        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class)
                            .resolve(context.getSource())
                            .getFirst();

                        int level = target.getLevel();

                        int currentExperience = Experience.experience(target);
                        int goalExperience = Experience.experience(level + 1);

                        int remaining = goalExperience - currentExperience;
                        int remainingBottles = (int) Math.ceil(remaining / Bottlet.instance().config().root().node("bottle", "default_store_experience").getDouble(10));

                        Player player = (Player) context.getSource().getSender();

                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.stats.other.feedback",
                                Argument.string("name", target.getName()),
                                Argument.string("level", Bottlet.pretty(level)),
                                Argument.tagResolver(Formatter.choice("level_count", level)),
                                Argument.string("experience", Bottlet.pretty(currentExperience)),
                                Argument.string("remaining", Bottlet.pretty(remaining)),
                                Argument.string("bottles", Bottlet.pretty(remainingBottles)),
                                Argument.tagResolver(Formatter.choice("bottle_count", remainingBottles))
                            )
                        );

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
            .then(Commands.literal("until")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottlet.until") && source.getSender() instanceof Player)
                .then(Commands.argument("goal", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        Player player = (Player) context.getSource().getSender();

                        int current = player.getLevel();
                        int goal = context.getArgument("goal", Integer.class);

                        if (goal < current) {
                            player.sendMessage(
                                Component.translatable(
                                    "bottlet.command.bottlet.until.feedback.less_than_current",
                                    Argument.string("goal", Bottlet.pretty(goal)),
                                    Argument.string("current", Bottlet.pretty(current)))
                            );
                            return 0;
                        }

                        if (goal == current) {
                            player.sendMessage(
                                Component.translatable(
                                    "bottlet.command.bottlet.until.feedback.equal_to_current",
                                    Argument.string("goal", Bottlet.pretty(goal))
                                )
                            );
                            return 0;
                        }

                        int currentExperience = Experience.experience(player);
                        int goalExperience = Experience.experience(goal);

                        int remaining = goalExperience - currentExperience;
                        int requiredBottles = (int) Math.ceil(remaining / Bottlet.instance().config().root().node("bottle", "default_store_experience").getDouble(10));

                        player.sendMessage(
                            Component.translatable(
                                "bottlet.command.bottlet.until.feedback",
                                Argument.string("remaining", Bottlet.pretty(remaining)),
                                Argument.string("bottles", Bottlet.pretty(requiredBottles)),
                                Argument.tagResolver(Formatter.choice("bottle_count", requiredBottles)),
                                Argument.string("goal", Bottlet.pretty(goal))
                            )
                        );

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
                    "bottlet.command.bottlet.get.feedback.insufficient_experience",
                    Argument.string("bottles", Bottlet.pretty(quantity)),
                    Argument.tagResolver(Formatter.choice("bottle_count", quantity)),
                    Argument.string("experience", Bottlet.pretty(experience)),
                    Argument.string("current", Bottlet.pretty(current))
                )
            );
            return 0;
        }

        Experience.change(player, -experience);

        Bottle.give(player, quantity);

        return Command.SINGLE_SUCCESS;
    }
}
