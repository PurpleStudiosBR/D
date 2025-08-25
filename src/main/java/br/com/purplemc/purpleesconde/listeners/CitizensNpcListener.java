package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;

public class CitizensNpcListener implements Listener {

    private final PurpleEsconde plugin;

    public CitizensNpcListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        plugin.getGUIManager().openMainMenu(player);
    }
}