package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerQuitListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            plugin.getArenaManager().removePlayerFromArena(player);
        }

        if (plugin.getSpectatorManager().isSpectator(player)) {
            plugin.getSpectatorManager().removeSpectator(player);
        }

        plugin.getScoreboardManager().removePlayerScoreboard(player);
    }
}