package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerChatListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
            plugin.getSpectatorManager().handleSpectatorChat(player, event.getMessage());
            return;
        }

        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena != null) {
            event.getRecipients().clear();
            event.getRecipients().addAll(arena.getPlayers());
        }
    }
}