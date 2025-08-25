package br.com.purplemc.purpleesconde.database;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DatabaseManager {

    private final PurpleEsconde plugin;
    private File playerFile;
    private YamlConfiguration playerConfig;

    public DatabaseManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        playerFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().warning("Não foi possível criar players.yml");
            }
        }
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
    }

    public void save() {
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Não foi possível salvar players.yml");
        }
    }

    public void addWin(Player player) {
        UUID uuid = player.getUniqueId();
        int wins = getWins(uuid) + 1;
        playerConfig.set("players." + uuid + ".wins", wins);
        addGame(uuid);
        save();
    }

    public void addLoss(Player player) {
        UUID uuid = player.getUniqueId();
        int losses = getLosses(uuid) + 1;
        playerConfig.set("players." + uuid + ".losses", losses);
        addGame(uuid);
        save();
    }

    public void addGame(UUID uuid) {
        int games = getGames(uuid) + 1;
        playerConfig.set("players." + uuid + ".games", games);
    }

    public int getWins(UUID uuid) {
        return playerConfig.getInt("players." + uuid + ".wins", 0);
    }

    public int getLosses(UUID uuid) {
        return playerConfig.getInt("players." + uuid + ".losses", 0);
    }

    public int getGames(UUID uuid) {
        return playerConfig.getInt("players." + uuid + ".games", 0);
    }

    public int getWins(Player player) {
        return getWins(player.getUniqueId());
    }

    public int getLosses(Player player) {
        return getLosses(player.getUniqueId());
    }

    public int getGamesPlayed(Player player) {
        return getGames(player.getUniqueId());
    }
}