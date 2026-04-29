package au.lupine.bottlet.command;

import au.lupine.bottlet.api.Bottle;
import au.lupine.bottlet.api.Experience;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class BottletAdminCommand {

    public static @NonNull LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("bottletadmin")
            .requires(source -> source.getSender().hasPermission("bottlet.command.bottletadmin"))
            .then(Commands.literal("give")
                .requires(source -> source.getSender().hasPermission("bottlet.command.bottletadmin.give"))
                .then(Commands.argument("targets", ArgumentTypes.players())
                    .then(Commands.literal("experience")
                        .then(Commands.argument("experience", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                List<Player> players = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                                    .resolve(context.getSource());

                                int experience = context.getArgument("experience", Integer.class);

                                players.forEach(player -> Bottle.give(player, experience, 1));

                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("bottles", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    List<Player> players = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                                        .resolve(context.getSource());

                                    int experience = context.getArgument("experience", Integer.class);
                                    int bottles = context.getArgument("bottles", Integer.class);

                                    players.forEach(player -> Bottle.give(player, experience, bottles));

                                    return Command.SINGLE_SUCCESS;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("levels")
                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                List<Player> players = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                                    .resolve(context.getSource());

                                int levels = context.getArgument("levels", Integer.class);
                                int experience = Experience.experience(levels);

                                players.forEach(player -> Bottle.give(player, experience, 1));

                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("bottles", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    List<Player> players = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                                        .resolve(context.getSource());

                                    int levels = context.getArgument("levels", Integer.class);

                                    int experience = Experience.experience(levels);
                                    int bottles = context.getArgument("bottles", Integer.class);

                                    players.forEach(player -> Bottle.give(player, experience, bottles));

                                    return Command.SINGLE_SUCCESS;
                                })
                            )
                        )
                    )
                )
            )
            .build();
    }
}
