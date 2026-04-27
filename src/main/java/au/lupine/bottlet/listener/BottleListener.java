package au.lupine.bottlet.listener;

import au.lupine.bottlet.api.Bottle;
import au.lupine.bottlet.api.Experience;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public final class BottleListener implements Listener {

    @EventHandler
    public void on(@NonNull ExpBottleEvent event) {
        ItemStack bottle = event.getEntity().getItem();
        event.setExperience(Bottle.stored(bottle));
    }

    @EventHandler
    public void on(@NonNull PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof ThrownExpBottle thrown)) return;

        // add thrown toggle
        if (Bottle.thrown(event.getPlayer())) return;

        event.setCancelled(true);

        ItemStack bottle = thrown.getItem();

        ItemStack stack = event.getItemStack();
        stack.setAmount(stack.getAmount() - 1);

        int amount = Bottle.stored(bottle);

        Player player = event.getPlayer();
        Experience.change(player, amount);

        Random random = new Random();
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, random.nextFloat(0.55F, 1.25F));
    }
}
