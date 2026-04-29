package au.lupine.bottlet.util;

import io.papermc.paper.dialog.Dialog;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public final class ExperienceDialog {

    // TODO: add interface for getting bottles
    public void open(@NonNull Player player) {
        Dialog.create(builder -> builder.empty()
            .base()
        );
    }
}
