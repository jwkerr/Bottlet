package au.lupine.bottlet;

import au.lupine.bottlet.base.Plugin;
import au.lupine.bottlet.command.BottletCommand;
import au.lupine.bottlet.listener.BottleListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public final class Bottlet extends Plugin {

    private static Bottlet instance;

    @Override
    public void load() {
        instance = this;

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(BottletCommand.build(), List.of("bottle", "b", "xp", "exp", "experience", "xpm", "xpmanager"));
        });
    }

    @Override
    public void enable() {
        listeners(
            new BottleListener()
        );
    }

    @Override
    public @NonNull Map<String, Object> nodes() {
        return Map.of(
            "bottle", Map.of(
                "default_stored_experience", 10
            )
        );
    }

    public static @NonNull Bottlet instance() {
        return instance;
    }

    public static @NonNull String pretty(int number) {
        return String.format("%,d", number);
    }
}
