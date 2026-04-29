package au.lupine.bottlet.api;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

/// This static utility class operates upon three representations of a player's "experience".
///
/// The first is a player's raw experience as an integer, this is the number underpinning the next two values.
///
/// The second is a player's level as displayed on their hotbar, this is an integer.
///
/// The third is the player's level, and any progress towards the next integer level, this value is expressed as a double.
///
/// [Experience on the Minecraft wiki](https://minecraft.wiki/w/Experience)
public final class Experience {

    private Experience() {}

    /// @return The experience equivalent to the specified level.
    public static int experience(int level) {
        if (level > 30) return (int) (4.5 * level * level - 162.5 * level + 2220);
        if (level > 15) return (int) (2.5 * level * level - 40.5 * level + 360);
        return level * level + 6 * level;
    }

    /// @return The total experience of the specified player.
    public static int experience(@NonNull Player player) {
        return experience(player.getLevel()) + Math.round(required(player.getLevel()) * player.getExp());
    }

    /// change the total experience of a player by the specified amount.
    public static void change(@NonNull Player player, int experience) {
        experience += experience(player);

        if (experience < 0) experience = 0;

        double progress = progress(experience);
        int level = (int) progress;

        player.setLevel(level);
        player.setExp((float) (progress - level));
    }

    /// @return The level equivalent to the specified experience amount, rounding down any progress towards the next level.
    public static int level(long experience) {
        if (experience > 1395) return (int) ((Math.sqrt(72 * experience - 54215D) + 325) / 18);
        if (experience > 315) return (int) (Math.sqrt(40 * experience - 7839D) / 10 + 8.1);
        if (experience > 0) return (int) (Math.sqrt(experience + 9D) - 3);
        return 0;
    }

    /// @return The experience required to reach the level after the specified level.
    public static int required(int level) {
        if (level >= 30) return level * 9 - 158;
        if (level >= 15) return level * 5 - 38;
        return level * 2 + 7;
    }

    /// @return The level equivalent to the specified experience amount, including any progress towards the next level.
    public static double progress(long xp) {
        int level = level(xp);

        float remainder = xp - (float) experience(level);
        float progress = remainder / required(level);

        return ((double) level) + progress;
    }
}
