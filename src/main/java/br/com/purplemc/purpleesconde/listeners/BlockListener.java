package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final PurpleEsconde plugin;

    public BlockListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.hasMetadata("pe-build") || player.hasPermission("purpleesconde.build")) {
            event.setCancelled(false);
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.hasMetadata("pe-build") || player.hasPermission("purpleesconde.build")) {
            event.setCancelled(false);
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }
}