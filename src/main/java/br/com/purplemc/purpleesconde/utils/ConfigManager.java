package br.com.purplemc.purpleesconde.utils;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final PurpleEsconde plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration maps;
    private FileConfiguration players;
    private FileConfiguration scoreboards;
    private Location mainLobby;
    private final Map<String, String> messageCache;

    public ConfigManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<String, String>();
        loadConfigs();
    }

    private void loadConfigs() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        createConfigFile("messages.yml");
        createConfigFile("maps.yml");
        createConfigFile("players.yml");
        createConfigFile("scoreboards.yml");
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        maps = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "maps.yml"));
        players = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "players.yml"));
        scoreboards = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "scoreboards.yml"));
        loadMainLobby();
        loadMessages();
        setupDefaultConfigs();
    }

    private void createConfigFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception e) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    plugin.getLogger().severe("Erro ao criar arquivo " + fileName + ": " + ex.getMessage());
                }
            }
        }
    }

    private void setupDefaultConfigs() {
        if (!messages.contains("messages")) {
            setupDefaultMessages();
        }
        if (!config.contains("settings")) {
            setupDefaultSettings();
        }
        if (!scoreboards.contains("scoreboard")) {
            setupDefaultScoreboards();
        }
    }

    private void setupDefaultMessages() {
        messages.set("messages.prefix", "§3[EscondeEsconde] ");
        messages.set("messages.no-permission", "§cVocê não tem permissão para isso!");
        messages.set("messages.player-only", "§cApenas jogadores podem usar este comando!");
        messages.set("messages.invalid-args", "§cUso incorreto do comando!");
        messages.set("messages.lobby-set", "§aLobby principal definido!");
        messages.set("messages.arena-created", "§aArena criada com sucesso!");
        messages.set("messages.map-not-found", "§cMapa não encontrado!");
        messages.set("messages.game.joining", "§aEntrando na partida...");
        messages.set("messages.game.full", "§cA partida está cheia!");
        messages.set("messages.game.already-started", "§cA partida já começou!");
        messages.set("messages.game.roles-selected", "§ePapéis selecionados! O jogo começará em breve.");
        messages.set("messages.game.wait-hiding", "§eAguarde enquanto os escondedores se escondem...");
        messages.set("messages.game.hide-now", "§a§lESCONDA-SE! Você tem 30 segundos!");
        messages.set("messages.game.start", "§a§lA CAÇADA COMEÇOU!");
        messages.set("messages.game.became-seeker", "§cVocê morreu e agora é um procurador!");
        messages.set("messages.game.player-became-seeker", "§e{player} §fse tornou um procurador!");
        messages.set("messages.game.seekers-win", "§c§lOS PROCURADORES VENCERAM!");
        messages.set("messages.game.hiders-win", "§a§lOS ESCONDEDORES VENCERAM!");
        messages.set("messages.game.actionbar-time", "§fTempo restante: §a{time}");
        messages.set("messages.game.player-joined", "§e{player} §fentrou na partida! §7({current}/{max})");
        messages.set("messages.game.player-left", "§e{player} §fsaiu da partida! §7({current}/{max})");
        messages.set("messages.game.countdown", "§aA partida começará em §e{time} §asegundos!");
        messages.set("messages.game.countdown-cancelled", "§cContagem regressiva cancelada! Jogadores insuficientes.");
        messages.set("messages.map.favorite-added", "§aMapa §e{map} §aadicionado aos favoritos!");
        messages.set("messages.map.favorite-removed", "§cMapa §e{map} §cremovido dos favoritos!");
        messages.set("messages.spectator.enabled", "§7Modo espectador ativado!");
        messages.set("messages.spectator.disabled", "§7Modo espectador desativado!");
        messages.set("messages.spectator.chat-format", "§7[SPEC] {player}: §f{message}");
        messages.set("messages.level.level-up", "§6§l✦ LEVEL UP! §fVocê alcançou o nível §6{level}§f!");
        messages.set("messages.level.xp-gained", "§a+{xp} XP");
        saveConfig(messages, "messages.yml");
    }

    private void setupDefaultSettings() {
        config.set("settings.max-players-per-arena", 16);
        config.set("settings.min-players-to-start", 2);
        config.set("settings.game-duration", 600);
        config.set("settings.hiding-time", 30);
        config.set("settings.lobby-items", true);
        config.set("settings.allowed-commands", Arrays.asList("/msg", "/tell", "/r"));
        config.set("settings.barrier-material", "BARRIER");
        config.set("settings.barrier-height", 10);
        config.set("database.type", "yaml");
        config.set("database.host", "localhost");
        config.set("database.port", 3306);
        config.set("database.database", "purpleesconde");
        config.set("database.username", "root");
        config.set("database.password", "");
        config.set("database.table", "players");
        plugin.saveConfig();
    }

    private void setupDefaultScoreboards() {
        List<String> waitingLines = Arrays.asList(
                "§7Jogadores: §a{players}/{max}",
                "§7Mapa: §e{map}",
                "",
                "§7Estado: §e{state}",
                "§7Tempo: §a{time}",
                "",
                "§7purple.mc"
        );
        scoreboards.set("scoreboard.waiting.title", "§6§lEsconde Esconde");
        scoreboards.set("scoreboard.waiting.lines", waitingLines);

        List<String> gameLines = Arrays.asList(
                "§7Procuradores: §c{seekers}",
                "§7Escondedores: §a{hiders}",
                "",
                "§7Tempo: §e{time}",
                "§7Mapa: §b{map}",
                "",
                "§7purple.mc"
        );
        scoreboards.set("scoreboard.game.title", "§6§lEsconde Esconde");
        scoreboards.set("scoreboard.game.lines", gameLines);
        saveConfig(scoreboards, "scoreboards.yml");
    }

    private void loadMainLobby() {
        if (config.contains("lobby.world") && config.contains("lobby.x")) {
            String world = config.getString("lobby.world");
            double x = config.getDouble("lobby.x");
            double y = config.getDouble("lobby.y");
            double z = config.getDouble("lobby.z");
            float yaw = (float) config.getDouble("lobby.yaw");
            float pitch = (float) config.getDouble("lobby.pitch");
            if (Bukkit.getWorld(world) != null) {
                mainLobby = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            }
        }
    }

    private void loadMessages() {
        messageCache.clear();
        if (messages.contains("messages")) {
            for (String key : getAllKeys(messages.getConfigurationSection("messages"))) {
                String value = messages.getString("messages." + key);
                if (value != null && !value.trim().isEmpty()) {
                    messageCache.put(key, value.replace("&", "§"));
                }
            }
        }
    }

    private Set<String> getAllKeys(org.bukkit.configuration.ConfigurationSection section) {
        Set<String> keys = new HashSet<String>();
        if (section == null) return keys;

        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                for (String subKey : getAllKeys(section.getConfigurationSection(key))) {
                    keys.add(key + "." + subKey);
                }
            } else {
                keys.add(key);
            }
        }
        return keys;
    }

    public String getMessage(String key) {
        return messageCache.containsKey(key) ? messageCache.get(key) : "§cMensagem não encontrada: " + key;
    }

    public boolean hasMessage(String key) {
        return messageCache.containsKey(key);
    }

    public void setMainLobby(Location location) {
        this.mainLobby = location;
        config.set("lobby.world", location.getWorld().getName());
        config.set("lobby.x", location.getX());
        config.set("lobby.y", location.getY());
        config.set("lobby.z", location.getZ());
        config.set("lobby.yaw", location.getYaw());
        config.set("lobby.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getMainLobby() {
        return mainLobby;
    }

    public int getMaxPlayersPerArena() {
        return config.getInt("settings.max-players-per-arena", 16);
    }

    public int getMinPlayersToStart() {
        return config.getInt("settings.min-players-to-start", 2);
    }

    public int getGameDuration() {
        return config.getInt("settings.game-duration", 600);
    }

    public int getHidingTime() {
        return config.getInt("settings.hiding-time", 30);
    }

    public boolean giveLobbyItems() {
        return config.getBoolean("settings.lobby-items", true);
    }

    public List<String> getAllowedCommands() {
        return config.getStringList("settings.allowed-commands");
    }

    public void giveLobbyItems(Player player) {
        if (!giveLobbyItems()) return;
        player.getInventory().clear();
        ItemStack enderPearl = ItemUtils.createItem(Material.ENDER_PEARL, "§aJogar Esconde-Esconde",
                Arrays.asList("§7Clique para entrar em uma partida!"));
        ItemStack sign = ItemUtils.createItem(Material.SIGN, "§eSelecionar Mapa",
                Arrays.asList("§7Escolha seu mapa favorito!"));
        player.getInventory().setItem(4, enderPearl);
        player.getInventory().setItem(6, sign);
        player.updateInventory();
    }

    public void giveWaitingLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(8, ItemUtils.getWaitingBed());
        player.updateInventory();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getMaps() {
        return maps;
    }

    public FileConfiguration getPlayers() {
        return players;
    }

    public FileConfiguration getScoreboards() {
        return scoreboards;
    }

    public List<String> getScoreboardLines(String type) {
        if (scoreboards.contains("scoreboard." + type + ".lines")) {
            return scoreboards.getStringList("scoreboard." + type + ".lines");
        }
        return Collections.emptyList();
    }

    public String getScoreboardTitle(String type) {
        if (scoreboards.contains("scoreboard." + type + ".title")) {
            return scoreboards.getString("scoreboard." + type + ".title").replace("&", "§");
        }
        return "§aScore";
    }

    public void saveConfig(FileConfiguration config, String fileName) {
        try {
            config.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar " + fileName + ": " + e.getMessage());
        }
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        maps = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "maps.yml"));
        players = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "players.yml"));
        scoreboards = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "scoreboards.yml"));
        loadMainLobby();
        loadMessages();
    }
}