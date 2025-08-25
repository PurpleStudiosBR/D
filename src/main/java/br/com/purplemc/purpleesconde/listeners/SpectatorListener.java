package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class SpectatorListener implements Listener {

    private final PurpleEsconde plugin;

    public SpectatorListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }
}