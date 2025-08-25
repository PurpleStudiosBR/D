package br.com.purplemc.purpleesconde.listeners;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final PurpleEsconde plugin;

    public PlayerDamageListener(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Arena arena = plugin.getArenaManager().getPlayerArena(player);

        if (arena != null) {
            if (arena.getGame() == null) {
                event.setCancelled(true);
                return;
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                    event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
                    event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                event.setCancelled(true);
                return;
            }
        }

        if (plugin.getSpectatorManager().isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        if (plugin.getSpectatorManager().isSpectator(damager) || plugin.getSpectatorManager().isSpectator(victim)) {
            event.setCancelled(true);
            return;
        }

        Arena arena = plugin.getArenaManager().getPlayerArena(victim);
        if (arena == null || arena.getGame() == null) {
            if (plugin.getArenaManager().isPlayerInArena(victim)) {
                event.setCancelled(true);
            }
            return;
        }

        Game game = arena.getGame();

        if (game.isSeeker(victim) && game.isSeeker(damager)) {
            event.setCancelled(true);
        } else if (game.isHider(victim) && game.isHider(damager)) {
            event.setCancelled(true);
        }
    }
}