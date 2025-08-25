package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.game.Game;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoreboardManager {

    private final PurpleEsconde plugin;
    private final Map<Player, Scoreboard> playerScoreboards;
    private final Map<Player, String> playerScoreboardTypes;
    private final Set<String> usedBlankLines = new HashSet<>();
    private BukkitTask updateTask;
    private FileConfiguration scoreboardConfig;

    public ScoreboardManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.playerScoreboardTypes = new HashMap<>();
        loadScoreboardConfig();
        startUpdateTask();
    }

    private void loadScoreboardConfig() {
        File scoreboardFile = new File(plugin.getDataFolder(), "scoreboards.yml");
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboards.yml", false);
        }
        this.scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L); // Atualiza a cada segundo
    }

    private void updatePlayerScoreboard(Player player) {
        String currentType = playerScoreboardTypes.get(player);
        if (currentType == null) return;

        Arena arena = plugin.getArenaManager().getPlayerArena(player);

        switch (currentType) {
            case "lobby":
                if (arena == null) {
                    setLobbyScoreboard(player);
                } else {
                    // Player entrou em uma arena, mudar scoreboard
                    setWaitingLobbyScoreboard(player, arena);
                }
                break;
            case "waiting":
                if (arena != null && arena.getGame() == null) {
                    setWaitingLobbyScoreboard(player, arena);
                } else if (arena != null && arena.getGame() != null) {
                    // Jogo começou, mudar para scoreboard do jogo
                    setGameScoreboard(player, arena.getGame());
                } else {
                    // Player saiu da arena, voltar para lobby
                    setLobbyScoreboard(player);
                }
                break;
            case "game":
                if (arena != null && arena.getGame() != null) {
                    setGameScoreboard(player, arena.getGame());
                } else {
                    // Jogo acabou, voltar para lobby
                    setLobbyScoreboard(player);
                }
                break;
        }
    }

    public void setLobbyScoreboard(Player player) {
        List<String> lines = getScoreboardLines("lobby");
        String title = getScoreboardTitle("lobby");

        if (lines == null || lines.isEmpty()) {
            removePlayerScoreboard(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("lobby", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aLobby"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceLobbyPlaceholders(player, line)));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
        playerScoreboardTypes.put(player, "lobby");
    }

    public void setWaitingLobbyScoreboard(Player player, Arena arena) {
        List<String> lines = getScoreboardLines("waiting");
        String title = getScoreboardTitle("waiting");

        if (lines == null || lines.isEmpty()) {
            setLobbyScoreboard(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("waiting", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aWaiting"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceWaitingPlaceholders(player, arena, line)));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
        playerScoreboardTypes.put(player, "waiting");
    }

    public void setGameScoreboard(Player player, Game game) {
        List<String> lines = getScoreboardLines("game");
        String title = getScoreboardTitle("game");

        if (lines == null || lines.isEmpty()) {
            setLobbyScoreboard(player);
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("game", "dummy");
        obj.setDisplayName(colorize(title != null ? title : "§aGame"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        usedBlankLines.clear();
        for (String line : lines) {
            String processedLine = colorize(applyAllPlaceholders(player, replaceGamePlaceholders(player, game, line)));
            processedLine = fixBlankLine(processedLine, score);
            obj.getScore(processedLine).setScore(score--);
        }

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
        playerScoreboardTypes.put(player, "game");
    }

    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        playerScoreboards.clear();
        playerScoreboardTypes.clear();
    }

    public void removePlayerScoreboard(Player player) {
        playerScoreboards.remove(player);
        playerScoreboardTypes.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateWaitingScoreboard(Arena arena) {
        for (Player player : arena.getPlayers()) {
            if ("waiting".equals(playerScoreboardTypes.get(player))) {
                setWaitingLobbyScoreboard(player, arena);
            }
        }
    }

    public void updateGameScoreboard(Game game) {
        for (Player player : game.getArena().getPlayers()) {
            if ("game".equals(playerScoreboardTypes.get(player))) {
                setGameScoreboard(player, game);
            }
        }
    }

    private String replaceLobbyPlaceholders(Player player, String line) {
        if (plugin.getDatabaseManager() == null || plugin.getLevelManager() == null) {
            return line;
        }

        int wins = plugin.getDatabaseManager().getWins(player.getUniqueId());
        int losses = plugin.getDatabaseManager().getLosses(player.getUniqueId());
        int games = plugin.getDatabaseManager().getGames(player.getUniqueId());
        int level = plugin.getLevelManager().getPlayerLevel(player);
        int kills = plugin.getLevelManager().getPlayerKills(player);
        String levelDisplay = plugin.getLevelManager().getLevelDisplay(player);
        String progressBar = plugin.getLevelManager().getProgressBar(player);
        String xp = plugin.getLevelManager().getXPInfo(player);

        return line.replace("{wins}", String.valueOf(wins))
                .replace("{losses}", String.valueOf(losses))
                .replace("{games}", String.valueOf(games))
                .replace("{level}", levelDisplay)
                .replace("{xp_bar}", progressBar)
                .replace("{xp}", xp)
                .replace("{kills}", String.valueOf(kills));
    }

    private String replaceWaitingPlaceholders(Player player, Arena arena, String line) {
        String status;
        if (arena.getPlayers().size() < plugin.getConfigManager().getMinPlayersToStart()) {
            status = "§fStatus: §eEsperando...";
        } else if (arena.getState().name().equalsIgnoreCase("STARTING")) {
            status = "§fStatus: §aInicia em §f" + arena.getCountdown() + "s";
        } else {
            status = "§fStatus: §ePreparando...";
        }

        String mapName = arena.getGameMap() != null ? arena.getGameMap().getName() : "Desconhecido";

        return line.replace("{map}", mapName)
                .replace("{players}", String.valueOf(arena.getPlayers().size()))
                .replace("{max_players}", String.valueOf(plugin.getConfigManager().getMaxPlayersPerArena()))
                .replace("{status}", status);
    }

    private String replaceGamePlaceholders(Player player, Game game, String line) {
        int seekers = game.getSeekers().size();
        int hiders = game.getHiders().size();
        String tempo = formatGameTime(game.getTimeLeft());
        String mapName = game.getArena().getGameMap() != null ? game.getArena().getGameMap().getName() : "Desconhecido";

        return line.replace("{tempo}", tempo)
                .replace("{seekers}", String.valueOf(seekers))
                .replace("{hiders}", String.valueOf(hiders))
                .replace("{map}", mapName);
    }

    private String formatGameTime(int time) {
        int min = time / 60;
        int sec = time % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private String applyAllPlaceholders(Player player, String text) {
        String replaced = text;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                replaced = PlaceholderAPI.setPlaceholders(player, replaced);
            } catch (Exception e) {
                // Ignorar erros do PlaceholderAPI
            }
        }
        return replaced;
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    private String fixBlankLine(String line, int score) {
        if (line == null) line = "";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            String unique = "§" + score + " ";
            while (usedBlankLines.contains(unique)) {
                unique = unique + " ";
            }
            usedBlankLines.add(unique);
            return unique;
        }
        return line;
    }

    public List<String> getScoreboardLines(String type) {
        return scoreboardConfig.getStringList("scoreboard." + type + ".lines");
    }

    public String getScoreboardTitle(String type) {
        return scoreboardConfig.getString("scoreboard." + type + ".title", "§aEsconde Esconde");
    }
}