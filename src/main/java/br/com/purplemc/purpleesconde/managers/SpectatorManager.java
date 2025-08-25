package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpectatorManager {

    private final PurpleEsconde plugin;
    private final Set<Player> spectators;
    private final Map<Player, Arena> spectatorArenas;

    public SpectatorManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.spectators = new HashSet<>();
        this.spectatorArenas = new HashMap<>();
    }

    public void setSpectator(Player player, Arena arena) {
        if (spectators.contains(player)) {
            spectatorArenas.put(player, arena);
            return;
        }
        spectators.add(player);
        spectatorArenas.put(player, arena);

        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), true);

        hideFromPlayers(player, arena);
        setupSpectatorTeam(player);
        giveSpectatorItems(player);

        player.sendMessage(plugin.getConfigManager().getMessage("spectator.enabled"));
    }

    public void removeSpectator(Player player) {
        if (!spectators.contains(player)) {
            return;
        }
        spectators.remove(player);
        Arena arena = spectatorArenas.remove(player);

        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);

        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        showToPlayers(player);
        removeFromSpectatorTeam(player);

        player.getInventory().clear();

        player.sendMessage(plugin.getConfigManager().getMessage("spectator.disabled"));
    }

    private void hideFromPlayers(Player spectator, Arena arena) {
        for (Player player : arena.getPlayers()) {
            if (!spectators.contains(player)) {
                player.hidePlayer(spectator);
            }
        }
        for (Player otherSpectator : spectators) {
            if (!otherSpectator.equals(spectator)) {
                otherSpectator.showPlayer(spectator);
                spectator.showPlayer(otherSpectator);
            }
        }
    }

    private void showToPlayers(Player spectator) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showPlayer(spectator);
        }
    }

    private void setupSpectatorTeam(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }
        Team spectatorTeam = scoreboard.getTeam("spectators");
        if (spectatorTeam == null) {
            spectatorTeam = scoreboard.registerNewTeam("spectators");
        }
        spectatorTeam.setCanSeeFriendlyInvisibles(false);
        spectatorTeam.addEntry(player.getName());
    }

    private void removeFromSpectatorTeam(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard != null) {
            Team spectatorTeam = scoreboard.getTeam("spectators");
            if (spectatorTeam != null) {
                spectatorTeam.removeEntry(player.getName());
            }
        }
    }

    private void giveSpectatorItems(Player player) {
        player.getInventory().clear();
        ItemStack backToLobby = ItemUtils.createItem(Material.BED, "§cVoltar ao Lobby", null);
        ItemStack playAgain = ItemUtils.createItem(Material.PAPER, "§aJogar Novamente", null);
        ItemStack teleportMenu = ItemUtils.createItem(Material.COMPASS, "§bTeleportar para Jogadores", null);

        player.getInventory().setItem(0, backToLobby);
        player.getInventory().setItem(1, playAgain);
        player.getInventory().setItem(4, teleportMenu);

        player.updateInventory();
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player);
    }

    public Arena getSpectatorArena(Player player) {
        return spectatorArenas.get(player);
    }

    public Set<Player> getSpectators() {
        return new HashSet<>(spectators);
    }

    public void handleSpectatorChat(Player player, String message) {
        if (!spectators.contains(player)) {
            return;
        }
        String spectatorMessage = plugin.getConfigManager().getMessage("spectator.chat-format")
                .replace("{player}", player.getName())
                .replace("{message}", message);
        for (Player spectator : spectators) {
            spectator.sendMessage(spectatorMessage);
        }
    }

    public void cleanup() {
        for (Player spectator : new HashSet<>(spectators)) {
            removeSpectator(spectator);
        }
    }
}