package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.entity.Player;

import java.util.*;

public class LevelManager {

    private final PurpleEsconde plugin;
    private final Map<Player, Integer> playerLevels;
    private final Map<Player, Integer> playerXP;
    private final Map<Player, Integer> playerKills;
    private final NavigableMap<Integer, LevelFormat> levelFormats;
    private final ProgressFormat progressFormat;

    public LevelManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.playerLevels = new HashMap<>();
        this.playerXP = new HashMap<>();
        this.playerKills = new HashMap<>();
        this.levelFormats = new TreeMap<>();
        this.progressFormat = loadProgressFormat();
        loadLevelFormats();
        loadPlayerData();
    }

    private void loadPlayerData() {
        if (plugin.getConfigManager().getPlayers().contains("levels")) {
            for (String playerName : plugin.getConfigManager().getPlayers().getConfigurationSection("levels").getKeys(false)) {
                Player player = plugin.getServer().getPlayerExact(playerName);
                if (player != null) {
                    int level = plugin.getConfigManager().getPlayers().getInt("levels." + playerName + ".level", 1);
                    int xp = plugin.getConfigManager().getPlayers().getInt("levels." + playerName + ".xp", 0);
                    int kills = plugin.getConfigManager().getPlayers().getInt("levels." + playerName + ".kills", 0);

                    playerLevels.put(player, level);
                    playerXP.put(player, xp);
                    playerKills.put(player, kills);
                }
            }
        }
    }

    public void savePlayerData() {
        for (Map.Entry<Player, Integer> entry : playerLevels.entrySet()) {
            String playerName = entry.getKey().getName();
            plugin.getConfigManager().getPlayers().set("levels." + playerName + ".level", entry.getValue());
        }
        for (Map.Entry<Player, Integer> entry : playerXP.entrySet()) {
            String playerName = entry.getKey().getName();
            plugin.getConfigManager().getPlayers().set("levels." + playerName + ".xp", entry.getValue());
        }
        for (Map.Entry<Player, Integer> entry : playerKills.entrySet()) {
            String playerName = entry.getKey().getName();
            plugin.getConfigManager().getPlayers().set("levels." + playerName + ".kills", entry.getValue());
        }
        plugin.getConfigManager().saveConfig(plugin.getConfigManager().getPlayers(), "players.yml");
    }

    public int getPlayerLevel(Player player) {
        return playerLevels.getOrDefault(player, 1);
    }

    public int getPlayerXP(Player player) {
        return playerXP.getOrDefault(player, 0);
    }

    public int getPlayerKills(Player player) {
        return playerKills.getOrDefault(player, 0);
    }

    public void addKill(Player player) {
        playerKills.put(player, getPlayerKills(player) + 1);
        giveXP(player, 150);
        savePlayerData();
    }

    public void addElimination(Player player) {
        giveXP(player, 140);
        savePlayerData();
    }

    public void addWin(Player player) {
        giveXP(player, 250);
        savePlayerData();
    }

    public void addPlayXP(Player player) {
        giveXP(player, 100);
        savePlayerData();
    }

    public void giveXP(Player player, int amount) {
        if (amount <= 0) return;
        int currentLevel = getPlayerLevel(player);
        int currentXP = getPlayerXP(player);
        int xpForCurrentLevel = getXPRequiredForLevel(currentLevel);
        int xpForNextLevel = getXPRequiredForLevel(currentLevel + 1);
        int xpInCurrentLevel = currentXP - xpForCurrentLevel;
        int xpNeededForNext = xpForNextLevel - xpForCurrentLevel;
        int newXP = currentXP + amount;
        int newXpInCurrentLevel = newXP - xpForCurrentLevel;

        if (xpNeededForNext > 0 && newXpInCurrentLevel >= xpNeededForNext) {
            levelUp(player, newXP - xpForNextLevel);
        } else {
            playerXP.put(player, newXP);
            player.sendMessage(plugin.getConfigManager().getMessage("level.xp-gained")
                    .replace("{xp}", String.valueOf(amount)));
        }
        updatePlayerXPBar(player);
        savePlayerData();
    }

    private void levelUp(Player player, int remainingXP) {
        int currentLevel = getPlayerLevel(player);
        int newLevel = currentLevel + 1;
        playerLevels.put(player, newLevel);
        int xpForNewLevel = getXPRequiredForLevel(newLevel);
        playerXP.put(player, xpForNewLevel + Math.max(0, remainingXP));
        player.sendMessage(plugin.getConfigManager().getMessage("level.level-up")
                .replace("{level}", String.valueOf(newLevel)));
        updatePlayerXPBar(player);
    }

    private void updatePlayerXPBar(Player player) {
        int level = getPlayerLevel(player);
        int currentXP = getPlayerXP(player);
        int xpForCurrentLevel = getXPRequiredForLevel(level);
        int xpForNextLevel = getXPRequiredForLevel(level + 1);
        int xpInCurrentLevel = currentXP - xpForCurrentLevel;
        int xpNeededForNext = xpForNextLevel - xpForCurrentLevel;

        float progress = xpNeededForNext > 0 ? Math.max(0, Math.min(1.0f, (float) xpInCurrentLevel / xpNeededForNext)) : 0f;
        player.setLevel(level);
        player.setExp(progress);
    }

    public int getXPRequiredForLevel(int level) {
        for (Map.Entry<Integer, LevelFormat> entry : levelFormats.descendingMap().entrySet()) {
            if (level >= entry.getKey()) {
                return entry.getValue().xp;
            }
        }
        return 0;
    }

    private void loadLevelFormats() {
        levelFormats.clear();
        levelFormats.put(0, new LevelFormat("&7[%nivel%❅]", 0));
        levelFormats.put(1, new LevelFormat("&7[%nivel%❅]", 5000));
        levelFormats.put(10, new LevelFormat("&a[%nivel%❉]", 10000));
        levelFormats.put(20, new LevelFormat("&b[%nivel%✹]", 15000));
        levelFormats.put(30, new LevelFormat("&6[%nivel%❃]", 20000));
        levelFormats.put(40, new LevelFormat("&5[%nivel%☀]", 25000));
        levelFormats.put(50, new LevelFormat("&d[%nivel%✴]", 30000));
        levelFormats.put(60, new LevelFormat("&c[%nivel%✻]", 35000));
        levelFormats.put(70, new LevelFormat("&e[%nivel%✺]", 40000));
        levelFormats.put(80, new LevelFormat("&1[%nivel%❄]", 45000));
        levelFormats.put(90, new LevelFormat("&0[%nivel%✵]", 50000));
        levelFormats.put(100, new LevelFormat("&4[%nivel%✳]", 55000));
        levelFormats.put(200, new LevelFormat("&f[%nivel%✥]", 60000));
        levelFormats.put(300, new LevelFormat("&3[%nivel%✡]", 65000));
        levelFormats.put(400, new LevelFormat("&2[%nivel%✢]", 70000));
        levelFormats.put(500, new LevelFormat("&9[%nivel%✧]", 75000));
        levelFormats.put(600, new LevelFormat("&5[%nivel%✧]", 80000));
        levelFormats.put(700, new LevelFormat("&d[%nivel%❈]", 85000));
        levelFormats.put(800, new LevelFormat("&a[%nivel%✯]", 90000));
        levelFormats.put(900, new LevelFormat("&b[%nivel%❁]", 95000));
        levelFormats.put(1000, new LevelFormat("&0[%nivel%✭]", 100000));
    }

    private ProgressFormat loadProgressFormat() {
        return new ProgressFormat("&f[", "▪", "&a", "&7", 10);
    }

    public String getProgressBar(Player player) {
        int level = getPlayerLevel(player);
        int currentXP = getPlayerXP(player);
        int xpForCurrentLevel = getXPRequiredForLevel(level);
        int xpForNextLevel = getXPRequiredForLevel(level + 1);
        int xpInCurrentLevel = currentXP - xpForCurrentLevel;
        int xpNeededForNext = xpForNextLevel - xpForCurrentLevel;

        float percent = xpNeededForNext > 0 ? Math.max(0, Math.min(1.0f, (float) xpInCurrentLevel / xpNeededForNext)) : 0f;
        int totalChars = progressFormat.numChars;
        int activeChars = Math.round(percent * totalChars);

        StringBuilder bar = new StringBuilder(progressFormat.formato);
        for (int i = 0; i < totalChars; i++) {
            if (i < activeChars && percent > 0) {
                bar.append(progressFormat.ativo).append(progressFormat.charBarra);
            } else {
                bar.append(progressFormat.inativo).append(progressFormat.charBarra);
            }
        }
        bar.append("&f]");
        return bar.toString();
    }

    public String getXPInfo(Player player) {
        int level = getPlayerLevel(player);
        int xp = getPlayerXP(player);
        int xpForCurrentLevel = getXPRequiredForLevel(level);
        int xpForNextLevel = getXPRequiredForLevel(level + 1);
        int xpInCurrentLevel = xp - xpForCurrentLevel;
        int xpNeededForNext = xpForNextLevel - xpForCurrentLevel;
        return xpInCurrentLevel + "/" + xpNeededForNext;
    }

    public String getLevelDisplay(Player player) {
        int level = getPlayerLevel(player);
        LevelFormat fmt = levelFormats.floorEntry(level).getValue();
        return fmt.nome.replace("%nivel%", String.valueOf(level));
    }

    public void initializePlayer(Player player) {
        if (!playerLevels.containsKey(player)) playerLevels.put(player, 1);
        if (!playerXP.containsKey(player)) playerXP.put(player, 0);
        if (!playerKills.containsKey(player)) playerKills.put(player, 0);
        updatePlayerXPBar(player);
    }

    public void removePlayer(Player player) {
        playerLevels.remove(player);
        playerXP.remove(player);
        playerKills.remove(player);
    }

    static class LevelFormat {
        String nome;
        int xp;
        LevelFormat(String nome, int xp) {
            this.nome = nome;
            this.xp = xp;
        }
    }

    static class ProgressFormat {
        String formato;
        String charBarra;
        String ativo;
        String inativo;
        int numChars;
        ProgressFormat(String formato, String charBarra, String ativo, String inativo, int numChars) {
            this.formato = formato;
            this.charBarra = charBarra;
            this.ativo = ativo;
            this.inativo = inativo;
            this.numChars = numChars;
        }
    }
}