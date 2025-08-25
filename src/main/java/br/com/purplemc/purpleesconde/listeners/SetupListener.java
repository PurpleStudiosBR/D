package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;

public class SetupListener implements Listener {

    private final PurpleEsconde plugin;

    public SetupListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.hasMetadata("pe-setup")) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (player.getItemInHand() == null) return;

        Material item = player.getItemInHand().getType();

        if (item == Material.BLAZE_ROD) {
            event.setCancelled(true);
            player.setMetadata("pe-setup-pos1", new FixedMetadataValue(plugin, event.getClickedBlock().getLocation()));
            player.sendMessage("§aPosição 1 definida: " +
                    event.getClickedBlock().getLocation().getBlockX() + ", " +
                    event.getClickedBlock().getLocation().getBlockY() + ", " +
                    event.getClickedBlock().getLocation().getBlockZ());
        } else if (item == Material.STICK) {
            event.setCancelled(true);
            player.setMetadata("pe-setup-pos2", new FixedMetadataValue(plugin, event.getClickedBlock().getLocation()));
            player.sendMessage("§aPosição 2 definida: " +
                    event.getClickedBlock().getLocation().getBlockX() + ", " +
                    event.getClickedBlock().getLocation().getBlockY() + ", " +
                    event.getClickedBlock().getLocation().getBlockZ());
        } else if (item == Material.NETHER_STAR) {
            event.setCancelled(true);
            player.removeMetadata("pe-setup", plugin);
            player.getInventory().clear();
            player.sendMessage("§aSetup visual finalizado!");
        }
    }
}