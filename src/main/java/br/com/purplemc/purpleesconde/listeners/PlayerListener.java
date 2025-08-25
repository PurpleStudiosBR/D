package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getArenaManager().sendToMainLobby(player);
        plugin.getScoreboardManager().setLobbyScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isPlayerInArena(player)) {
            plugin.getArenaManager().removePlayerFromArena(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (ItemUtils.isSpecialItem(item)) {
            event.setCancelled(true);
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains("Sair da partida")) {
                if (plugin.getArenaManager().isPlayerInArena(player)) {
                    plugin.getArenaManager().removePlayerFromArena(player);
                } else {
                    plugin.getArenaManager().sendToMainLobby(player);
                }
                player.sendMessage("§cVocê saiu da partida!");
                return;
            }
            if (displayName.contains("Jogar Esconde-Esconde")) {
                plugin.getGameManager().joinRandomGame(player);
            } else if (displayName.contains("Selecionar Mapa")) {
                plugin.getGUIManager().openMapSelector(player);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        if (plugin.getArenaManager().isPlayerInArena(player)) {
            if (!player.hasPermission("purpleesconde.bypass-commands")) {
                boolean allowed = false;
                for (String allowedCmd : plugin.getConfigManager().getAllowedCommands()) {
                    if (command.startsWith(allowedCmd.toLowerCase())) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não pode usar comandos durante a partida!");
                }
            }
        }
    }
}