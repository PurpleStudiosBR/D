package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerCommandListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            if (player.hasPermission("purpleesconde.bypass-commands")) {
                return;
            }

            boolean allowed = false;
            for (String allowedCommand : plugin.getConfigManager().getAllowedCommands()) {
                if (command.startsWith(allowedCommand.toLowerCase())) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed && !command.startsWith("/purpleesconde") && !command.startsWith("/ed")) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blocked"));
            }
        }
    }
}