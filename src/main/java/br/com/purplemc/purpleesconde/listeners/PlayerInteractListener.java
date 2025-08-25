package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import br.com.purplemc.purpleesconde.managers.GUIManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final PurpleEsconde plugin;
    private final GUIManager guiManager;

    public PlayerInteractListener(PurpleEsconde plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);

        if (item.getType() == Material.ENDER_PEARL) {
            guiManager.openMainMenu(player);
        } else if (item.getType() == Material.SIGN) {
            guiManager.openMapSelector(player);
        } else if (item.getType() == Material.BED) {
            if (plugin.getArenaManager().isPlayerInArena(player)) {
                plugin.getArenaManager().removePlayerFromArena(player);
            } else {
                plugin.getArenaManager().sendToMainLobby(player);
            }
        } else if (item.getType() == Material.FIREWORK) {
            Arena arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena != null && arena.getGame() != null) {
                Game game = arena.getGame();
                if (game.isHider(player) || game.isSeeker(player)) {
                    event.setCancelled(false);
                }
            }
        } else {
            event.setCancelled(false);
        }
    }
}