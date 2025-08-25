package br.com.purplemc.purpleesconde.api;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PurpleEscondeAPI {

    private final PurpleEsconde plugin;

    public PurpleEscondeAPI(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerInGame(Player player) {
        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        return arena != null && arena.getGame() != null;
    }

    public boolean isPlayerSeeker(Player player) {
        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null || arena.getGame() == null) return false;
        return arena.getGame().isSeeker(player);
    }

    public boolean isPlayerHider(Player player) {
        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null || arena.getGame() == null) return false;
        return arena.getGame().isHider(player);
    }

    public boolean isPlayerSpectator(Player player) {
        return plugin.getSpectatorManager().isSpectator(player);
    }

    public Arena getPlayerArena(Player player) {
        return plugin.getArenaManager().getPlayerArena(player);
    }

    public Game getPlayerGame(Player player) {
        Arena arena = getPlayerArena(player);
        return arena != null ? arena.getGame() : null;
    }

    public Collection<Arena> getAllArenas() {
        return plugin.getArenaManager().getArenas();
    }

    public void addPlayerToRandomArena(Player player) {
        Arena arena = plugin.getArenaManager().getRandomArena();
        if (arena != null) {
            plugin.getArenaManager().addPlayerToArena(player, arena);
        }
    }

    public void removePlayerFromArena(Player player) {
        plugin.getArenaManager().removePlayerFromArena(player);
    }

    public int getPlayerLevel(Player player) {
        return plugin.getLevelManager().getPlayerLevel(player);
    }

    public int getPlayerXP(Player player) {
        return plugin.getLevelManager().getPlayerXP(player);
    }

    public void givePlayerXP(Player player, int amount) {
        plugin.getLevelManager().giveXP(player, amount);
    }
}