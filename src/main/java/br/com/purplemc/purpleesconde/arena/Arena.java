package br.com.purplemc.purpleesconde.arena;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.game.Game;
import br.com.purplemc.purpleesconde.map.GameMap;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import br.com.purplemc.purpleesconde.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Arena {

    private final PurpleEsconde plugin;
    private final String id;
    private final GameMap gameMap;
    private final Set<Player> players;
    private final Map<Player, Double> seekerChances;
    private ArenaState state;
    private Game currentGame;
    private BukkitTask startTask;
    private int countdown;
    private final int maxPlayers;

    public Arena(PurpleEsconde plugin, String id, GameMap gameMap) {
        this.plugin = plugin;
        this.id = id;
        this.gameMap = gameMap;
        this.players = new HashSet<Player>();
        this.seekerChances = new HashMap<Player, Double>();
        this.state = ArenaState.WAITING;
        this.maxPlayers = plugin.getConfigManager().getMaxPlayersPerArena();
        this.countdown = 30;
    }

    public void addPlayer(Player player) {
        if (players.size() >= maxPlayers || state == ArenaState.INGAME) {
            return;
        }

        players.add(player);
        updateSeekerChances();

        player.teleport(gameMap.getWaitingLobby());

        if (player.hasMetadata("pe-build") || player.hasPermission("purpleesconde.build")) {
            player.setGameMode(GameMode.CREATIVE);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
        }

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFlying(false);
        player.setAllowFlight(false);

        plugin.getConfigManager().giveWaitingLobbyItems(player);

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().setWaitingLobbyScoreboard(player, this);
        }

        MessageUtils.broadcastToArena(this, plugin.getConfigManager().getMessage("game.player-joined")
                .replace("{player}", player.getName())
                .replace("{current}", String.valueOf(players.size()))
                .replace("{max}", String.valueOf(maxPlayers)));

        updateActionBars();
        checkStartConditions();
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateWaitingScoreboard(this);
        }
    }

    public void removePlayer(Player player) {
        if (!players.contains(player)) {
            return;
        }

        players.remove(player);
        seekerChances.remove(player);

        if (currentGame != null) {
            if (currentGame.isSpectator(player)) {
                currentGame.removeSpectator(player);
            }
        }

        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removePlayerScoreboard(player);
        }

        if (startTask != null && players.size() < plugin.getConfigManager().getMinPlayersToStart()) {
            startTask.cancel();
            startTask = null;
            countdown = 30;
            state = ArenaState.WAITING;

            MessageUtils.broadcastToArena(this, plugin.getConfigManager().getMessage("game.countdown-cancelled"));
        }

        updateSeekerChances();
        updateActionBars();
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateWaitingScoreboard(this);
        }

        MessageUtils.broadcastToArena(this, plugin.getConfigManager().getMessage("game.player-left")
                .replace("{player}", player.getName())
                .replace("{current}", String.valueOf(players.size()))
                .replace("{max}", String.valueOf(maxPlayers)));
    }

    private void updateSeekerChances() {
        seekerChances.clear();

        if (players.isEmpty()) return;

        double seekerChance = 1.0 / players.size();

        for (Player player : players) {
            seekerChances.put(player, seekerChance);
        }
    }

    private void updateActionBars() {
        for (Player player : players) {
            Double chance = seekerChances.get(player);
            if (chance != null) {
                int seekerPercent = (int) (chance * 100);
                int hiderPercent = 100 - seekerPercent;

                String message = "§cProcurador: " + seekerPercent + "% §8| §aEscondedor: " + hiderPercent + "%";
                MessageUtils.sendActionBar(player, message);
            }
        }
    }

    private void checkStartConditions() {
        if (players.size() >= plugin.getConfigManager().getMinPlayersToStart() &&
                state == ArenaState.WAITING && startTask == null) {
            startCountdown();
        }
    }

    private void startCountdown() {
        state = ArenaState.STARTING;
        countdown = 30;
        startTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().updateWaitingScoreboard(Arena.this);
                }
                if (countdown <= 0) {
                    startGame();
                    return;
                }
                if (countdown <= 5 || countdown % 10 == 0) {
                    MessageUtils.broadcastToArena(Arena.this, plugin.getConfigManager().getMessage("game.countdown")
                            .replace("{time}", String.valueOf(countdown)));
                }
                countdown--;
            }
        }, 0L, 20L);
    }

    private void startGame() {
        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }

        if (players.size() < plugin.getConfigManager().getMinPlayersToStart()) {
            state = ArenaState.WAITING;
            countdown = 30;
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updateWaitingScoreboard(this);
            }
            return;
        }

        state = ArenaState.INGAME;
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateWaitingScoreboard(this);
        }
        currentGame = new Game(plugin, this);
        currentGame.start();
    }

    public void reset() {
        state = ArenaState.WAITING;
        countdown = 30;

        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }

        if (currentGame != null) {
            currentGame = null;
        }

        players.clear();
        seekerChances.clear();
    }

    public void cleanup() {
        for (Player player : new HashSet<Player>(players)) {
            plugin.getArenaManager().sendToMainLobby(player);
        }
        reset();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean canStart() {
        return players.size() >= plugin.getConfigManager().getMinPlayersToStart();
    }

    public Set<Player> getWaitingPlayers() {
        if (state == ArenaState.WAITING || state == ArenaState.STARTING) {
            return new HashSet<Player>(players);
        }
        return new HashSet<Player>();
    }

    public String getId() {
        return id;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public Set<Player> getPlayers() {
        return new HashSet<Player>(players);
    }

    public ArenaState getState() {
        return state;
    }

    public Game getGame() {
        return currentGame;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public double getSeekerChance(Player player) {
        Double chance = seekerChances.get(player);
        return chance != null ? chance : 0.0;
    }

    public int getCountdown() {
        return countdown;
    }
}