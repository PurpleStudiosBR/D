package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.arena.ArenaState;
import br.com.purplemc.purpleesconde.map.GameMap;
import br.com.purplemc.purpleesconde.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class GameManager {

    private final PurpleEsconde plugin;
    private final Map<Arena, Game> activeGames;
    private BukkitTask cleanupTask;

    public GameManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<Arena, Game>();
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    if (arena.getPlayers().isEmpty() && arena.getGame() == null) {
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                plugin.getArenaManager().removeArena(arena.getId());
                            }
                        });
                    }
                }
            }
        }, 1200L, 1200L);
    }

    public void startGame(Arena arena) {
        if (activeGames.containsKey(arena)) return;
        Game game = new Game(plugin, arena);
        activeGames.put(arena, game);
        game.start();
    }

    public void endGame(Arena arena) {
        Game game = activeGames.remove(arena);
        if (game != null) {
            plugin.getLogger().info("Jogo finalizado na arena: " + arena.getId());
        }
    }

    public Game getGame(Arena arena) {
        return activeGames.get(arena);
    }

    public boolean isGameActive(Arena arena) {
        return activeGames.containsKey(arena);
    }

    public void stopAllGames() {
        for (Arena arena : activeGames.keySet()) {
            arena.cleanup();
        }
        activeGames.clear();
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }

    public void joinGame(Player player, String mapName) {
        if (mapName == null || mapName.isEmpty()) {
            player.sendMessage("§cNome do mapa inválido.");
            return;
        }

        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) {
            player.sendMessage("§cMapa não encontrado: " + mapName);
            plugin.getLogger().warning("Tentativa de entrar em mapa inexistente: " + mapName);
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            plugin.getArenaManager().removePlayerFromArena(player);
        }

        Arena availableArena = plugin.getArenaManager().getAvailableArena(mapName);

        if (availableArena == null) {
            availableArena = plugin.getArenaManager().createArena(map);
            if (availableArena == null) {
                player.sendMessage("§cErro ao criar arena para o mapa: " + mapName);
                return;
            }
        }

        plugin.getArenaManager().addPlayerToArena(player, availableArena);
        player.sendMessage("§aVocê entrou na arena " + mapName + "!");
    }

    public void joinRandomGame(Player player) {
        Arena randomArena = plugin.getArenaManager().getRandomArena();
        if (randomArena == null) {
            player.sendMessage("§cNenhuma arena disponível no momento.");
            return;
        }

        if (plugin.getArenaManager().isPlayerInArena(player)) {
            plugin.getArenaManager().removePlayerFromArena(player);
        }

        plugin.getArenaManager().addPlayerToArena(player, randomArena);
        player.sendMessage("§aVocê entrou em uma partida aleatória!");
    }

    public int getTotalPlayersInGame() {
        int total = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getState() == ArenaState.WAITING ||
                    arena.getState() == ArenaState.STARTING ||
                    arena.getState() == ArenaState.INGAME) {
                total += arena.getPlayers().size();
            }
        }
        return total;
    }

    public int getTotalActiveGames() {
        return activeGames.size();
    }
}