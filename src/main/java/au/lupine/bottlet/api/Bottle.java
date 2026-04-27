package au.lupine.bottlet.api;

import au.lupine.bottlet.Bottlet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;

public final class Bottle {

    public static final NamespacedKey BOTTLET_STORED_EXPERIENCE = new NamespacedKey(Bottlet.instance(), "stored_experience");
    public static final NamespacedKey BOTTLET_SHOULD_THROW_BOTTLES = new NamespacedKey(Bottlet.instance(), "should_throw_bottles");

    private Bottle() {}

    public static void give(@NonNull Player player, int quantity) {
        ItemStack bottles = new ItemStack(Material.EXPERIENCE_BOTTLE, quantity);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(bottles);

        remaining.values()
            .forEach(item -> player
                .getWorld()
                .dropItem(player.getLocation(), item)
            );
    }

    public static void give(@NonNull Player player, int experience, int quantity) {
        ItemStack bottles = new ItemStack(Material.EXPERIENCE_BOTTLE, quantity);

        ItemMeta meta = bottles.getItemMeta();
        meta.getPersistentDataContainer().set(
            BOTTLET_STORED_EXPERIENCE,
            PersistentDataType.INTEGER,
            experience
        );

        int level = Experience.level(experience);
        meta.lore(List.of(
            Component.text(level + " level" + (level != 1 ? "s" : ""), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" (" + String.format("%,d", experience) + " experience)", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))
            )
        );

        bottles.setItemMeta(meta);

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(bottles);

        remaining.values()
            .forEach(item -> player
                .getWorld()
                .dropItem(player.getLocation(), item)
            );
    }

    public static int stored(@NonNull ItemStack bottle) {
        if (!(bottle.getType() == Material.EXPERIENCE_BOTTLE)) return 0;

        int defaultAmount = Bottlet.instance().config().root().node("bottle", "default_stored_experience").getInt(10);

        ItemMeta meta = bottle.getItemMeta();
        if (meta == null) return defaultAmount;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Integer amount = pdc.get(BOTTLET_STORED_EXPERIENCE, PersistentDataType.INTEGER);
        if (amount != null) return amount;

        amount = pdc.get(new NamespacedKey("xpmanager", "xpmanager-store-amount"), PersistentDataType.INTEGER);
        if (amount != null) return amount;

        try {
            net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(bottle);
            CustomData data = nms.get(DataComponents.CUSTOM_DATA);

            if (data == null) return defaultAmount;

            return data.copyTag().getInt("StoredBottledExp").orElse(defaultAmount);
        } catch (Throwable throwable) {
            return defaultAmount;
        }
    }

    public static boolean thrown(@NonNull Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        Boolean value = pdc.get(BOTTLET_SHOULD_THROW_BOTTLES, PersistentDataType.BOOLEAN);
        if (value != null) return value;

        return true;
    }
}
