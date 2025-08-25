package br.com.purplemc.purpleesconde.game;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import br.com.purplemc.purpleesconde.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class Game {

    private final PurpleEsconde plugin;
    private final Arena arena;
    private final Set<Player> seekers;
    private final Set<Player> hiders;
    private final Set<Player> spectators;
    private Player originalSeeker;
    private GameState state;
    private BukkitTask gameTask;
    private int timeLeft;
    private int hidingTime;
    private Scoreboard scoreboard;
    private Team seekersTeam;
    private Team hidersTeam;

    public Game(PurpleEsconde plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.seekers = new HashSet<Player>();
        this.hiders = new HashSet<Player>();
        this.spectators = new HashSet<Player>();
        this.state = GameState.WAITING;
        this.timeLeft = 600;
        this.hidingTime = 30;
    }

    public void start() {
        if (arena.getPlayers().size() < 2) {
            return;
        }
        state = GameState.STARTING;
        selectRoles();
        prepareGame();
        startHidingPhase();
    }

    private void selectRoles() {
        List<Player> players = new ArrayList<Player>(arena.getPlayers());
        Collections.shuffle(players);

        originalSeeker = players.get(0);
        seekers.add(originalSeeker);

        for (int i = 1; i < players.size(); i++) {
            hiders.add(players.get(i));
        }

        MessageUtils.broadcastToArena(arena, plugin.getConfigManager().getMessage("game.roles-selected"));
    }

    private void prepareGame() {
        for (Player player : arena.getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.getInventory().clear();

            if (seekers.contains(player)) {
                equipSeeker(player);
                player.teleport(arena.getGameMap().getSeekerSpawn());
            } else {
                equipHider(player);
                player.teleport(getRandomHiderSpawn());
            }

            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().setGameScoreboard(player, this);
            }
        }
        updateNickAndGlow();
    }

    private void equipSeeker(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getSeekerArmor());
        player.getInventory().setItem(0, ItemUtils.getSeekerWeapon());
        player.getInventory().setItem(1, ItemUtils.getFirework());
    }

    private void equipHider(Player player) {
        player.getInventory().setArmorContents(ItemUtils.getHiderArmor());
        player.getInventory().setItem(0, ItemUtils.getHiderWeapon());
        player.getInventory().setItem(1, ItemUtils.getFirework());
    }

    private void startHidingPhase() {
        state = GameState.HIDING;

        for (Player seeker : seekers) {
            seeker.sendMessage(plugin.getConfigManager().getMessage("game.wait-hiding"));
        }

        for (Player hider : hiders) {
            hider.sendMessage(plugin.getConfigManager().getMessage("game.hide-now"));
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                startGamePhase();
            }
        }, hidingTime * 20L);
    }

    private void startGamePhase() {
        state = GameState.INGAME;

        MessageUtils.broadcastToArena(arena, plugin.getConfigManager().getMessage("game.start"));

        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                timeLeft--;
                updateActionBars();
                if (timeLeft <= 0) {
                    endGame(GameEndReason.TIME_UP);
                } else if (hiders.isEmpty()) {
                    endGame(GameEndReason.ALL_CAUGHT);
                }
            }
        }, 20L, 20L);
    }

    public void onPlayerDeath(Player player) {
        if (hiders.contains(player)) {
            hiders.remove(player);
            seekers.add(player);

            equipSeeker(player);
            player.sendMessage(plugin.getConfigManager().getMessage("game.became-seeker"));

            MessageUtils.broadcastToArena(arena,
                    plugin.getConfigManager().getMessage("game.player-became-seeker")
                            .replace("{player}", player.getName()));

            if (hiders.isEmpty()) {
                endGame(GameEndReason.ALL_CAUGHT);
            }
            updateNickAndGlow();
        } else if (seekers.contains(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    player.spigot().respawn();
                    player.teleport(arena.getGameMap().getSeekerSpawn());
                }
            }, 1L);
        }
    }

    public void addSpectator(Player player) {
        spectators.add(player);
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().setSpectator(player, arena);
        }
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
        if (plugin.getSpectatorManager() != null) {
            plugin.getSpectatorManager().removeSpectator(player);
        }
    }

    private void endGame(GameEndReason reason) {
        state = GameState.ENDING;

        if (gameTask != null) {
            gameTask.cancel();
        }

        String winMessage = "";
        boolean seekersWin = false;
        if (reason == GameEndReason.ALL_CAUGHT) {
            winMessage = plugin.getConfigManager().getMessage("game.seekers-win");
            seekersWin = true;
        } else if (reason == GameEndReason.TIME_UP) {
            winMessage = plugin.getConfigManager().getMessage("game.hiders-win");
        }

        MessageUtils.broadcastToArena(arena, winMessage);

        giveRewards(seekersWin);

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : arena.getPlayers()) {
                    resetNickAndGlow(player);
                    plugin.getArenaManager().sendToMainLobby(player);
                }
                arena.reset();
            }
        }, 100L);
    }

    private void giveRewards(boolean seekersWin) {
        Set<Player> winners;
        Set<Player> losers;
        if (seekersWin) {
            winners = new HashSet<Player>(seekers);
            losers = new HashSet<Player>(hiders);
        } else {
            winners = new HashSet<Player>(hiders);
            losers = new HashSet<Player>(seekers);
        }

        for (Player winner : winners) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(winner, 50);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addWin(winner);
            }
        }
        for (Player loser : losers) {
            if (plugin.getLevelManager() != null) {
                plugin.getLevelManager().giveXP(loser, 10);
            }
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addLoss(loser);
            }
        }
        for (Player player : arena.getPlayers()) {
            if (plugin.getDatabaseManager() != null) {
                plugin.getDatabaseManager().addGame(player.getUniqueId());
            }
        }
    }

    private void updateActionBars() {
        String timeMessage = MessageUtils.formatTime(timeLeft);

        for (Player player : arena.getPlayers()) {
            MessageUtils.sendActionBar(player,
                    plugin.getConfigManager().getMessage("game.actionbar-time")
                            .replace("{time}", timeMessage));
        }
    }

    private Location getRandomHiderSpawn() {
        List<Location> spawns = arena.getGameMap().getHiderSpawns();
        return spawns.get(new Random().nextInt(spawns.size()));
    }

    private void updateNickAndGlow() {
        setupTeams();
        for (Player p : arena.getPlayers()) {
            if (isHider(p)) {
                setHiderVisual(p);
            } else if (isSeeker(p)) {
                setSeekerVisual(p);
            } else {
                resetNickAndGlow(p);
            }
        }
        for (Player hider : hiders) {
            for (Player target : arena.getPlayers()) {
                if (hider.equals(target)) continue;
                target.hidePlayer(hider);
            }
        }
        for (Player seeker : seekers) {
            for (Player target : arena.getPlayers()) {
                if (!seeker.equals(target)) {
                    target.showPlayer(seeker);
                }
            }
        }
    }

    private void setupTeams() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        seekersTeam = scoreboard.getTeam("seekers");
        if (seekersTeam == null) seekersTeam = scoreboard.registerNewTeam("seekers");
        seekersTeam.setPrefix(ChatColor.RED.toString());

        hidersTeam = scoreboard.getTeam("hiders");
        if (hidersTeam == null) hidersTeam = scoreboard.registerNewTeam("hiders");
        hidersTeam.setPrefix(ChatColor.GRAY.toString());

        try {
            hidersTeam.setNameTagVisibility(NameTagVisibility.NEVER);
        } catch (Exception ignored) {}

        Set<String> seekerEntries = new HashSet<String>(seekersTeam.getEntries());
        for (String entry : seekerEntries) {
            seekersTeam.removeEntry(entry);
        }

        Set<String> hiderEntries = new HashSet<String>(hidersTeam.getEntries());
        for (String entry : hiderEntries) {
            hidersTeam.removeEntry(entry);
        }

        for (Player p : seekers) seekersTeam.addEntry(p.getName());
        for (Player p : hiders) hidersTeam.addEntry(p.getName());
        for (Player p : arena.getPlayers()) p.setScoreboard(scoreboard);
    }

    private void setSeekerVisual(Player player) {
        player.setPlayerListName(ChatColor.RED + player.getName());
        player.setDisplayName(ChatColor.RED + player.getName());
    }

    private void setHiderVisual(Player player) {
        player.setPlayerListName(ChatColor.GRAY + "???");
        player.setDisplayName(ChatColor.GRAY + "???");
    }

    private void resetNickAndGlow(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setPlayerListName(player.getName());
        player.setDisplayName(player.getName());
        for (Player target : Bukkit.getOnlinePlayers()) {
            target.showPlayer(player);
        }
    }

    public GameState getState() {
        return state;
    }

    public Set<Player> getSeekers() {
        return seekers;
    }

    public Set<Player> getHiders() {
        return hiders;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public boolean isSeeker(Player player) {
        return seekers.contains(player);
    }

    public boolean isHider(Player player) {
        return hiders.contains(player);
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player);
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public Arena getArena() {
        return arena;
    }
}