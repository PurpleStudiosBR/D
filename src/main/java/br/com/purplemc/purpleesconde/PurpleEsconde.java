package br.com.purplemc.purpleesconde;

import br.com.purplemc.purpleesconde.api.PurpleEscondeAPI;
import br.com.purplemc.purpleesconde.commands.PurpleEscondeCommand;
import br.com.purplemc.purpleesconde.database.DatabaseManager;
import br.com.purplemc.purpleesconde.listeners.CitizensNpcListener;
import br.com.purplemc.purpleesconde.listeners.PlayerChatListener;
import br.com.purplemc.purpleesconde.listeners.PlayerCommandListener;
import br.com.purplemc.purpleesconde.listeners.PlayerDamageListener;
import br.com.purplemc.purpleesconde.listeners.PlayerDeathListener;
import br.com.purplemc.purpleesconde.listeners.PlayerInteractListener;
import br.com.purplemc.purpleesconde.listeners.PlayerJoinListener;
import br.com.purplemc.purpleesconde.listeners.PlayerQuitListener;
import br.com.purplemc.purpleesconde.listeners.BlockListener;
import br.com.purplemc.purpleesconde.listeners.PlayerListener;
import br.com.purplemc.purpleesconde.listeners.SpectatorListener;
import br.com.purplemc.purpleesconde.listeners.SetupListener;
import br.com.purplemc.purpleesconde.managers.ArenaManager;
import br.com.purplemc.purpleesconde.managers.GameManager;
import br.com.purplemc.purpleesconde.managers.GUIManager;
import br.com.purplemc.purpleesconde.managers.LevelManager;
import br.com.purplemc.purpleesconde.managers.MapManager;
import br.com.purplemc.purpleesconde.managers.ScoreboardManager;
import br.com.purplemc.purpleesconde.managers.SpectatorManager;
import br.com.purplemc.purpleesconde.utils.ConfigManager;
import br.com.purplemc.purpleesconde.placeholders.EscondePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PurpleEsconde extends JavaPlugin {

    private static PurpleEsconde instance;
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private MapManager mapManager;
    private GUIManager guiManager;
    private SpectatorManager spectatorManager;
    private ScoreboardManager scoreboardManager;
    private LevelManager levelManager;
    private DatabaseManager databaseManager;
    private PurpleEscondeAPI api;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Iniciando PurpleEsconde...");

        try {
            // Ordem correta de inicialização - dependências primeiro
            this.configManager = new ConfigManager(this);
            this.mapManager = new MapManager(this);
            this.databaseManager = new DatabaseManager(this);
            this.levelManager = new LevelManager(this);
            this.spectatorManager = new SpectatorManager(this);
            this.scoreboardManager = new ScoreboardManager(this);
            this.arenaManager = new ArenaManager(this); // MapManager já existe
            this.gameManager = new GameManager(this);
            this.guiManager = new GUIManager(this);
            this.api = new PurpleEscondeAPI(this);

            registerCommands();
            registerListeners();
            registerPlaceholders();

            getLogger().info("PurpleEsconde carregado com sucesso!");

        } catch (Exception e) {
            getLogger().severe("Erro ao carregar o plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Desabilitando PurpleEsconde...");

        if (gameManager != null) {
            gameManager.stopAllGames();
        }

        if (arenaManager != null) {
            arenaManager.cleanup();
        }

        if (levelManager != null) {
            levelManager.savePlayerData();
        }

        if (databaseManager != null) {
            databaseManager.save();
        }

        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
        }

        getLogger().info("PurpleEsconde desabilitado!");
    }

    private void registerCommands() {
        getCommand("purpleesconde").setExecutor(new PurpleEscondeCommand(this));
        getCommand("ed").setExecutor(new PurpleEscondeCommand(this));
        getCommand("edbuild").setExecutor(new PurpleEscondeCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this, guiManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCommandListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SpectatorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SetupListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            Bukkit.getPluginManager().registerEvents(new CitizensNpcListener(this), this);
            getLogger().info("Citizens integration enabled!");
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new EscondePlaceholder(this).register();
                getLogger().info("PlaceholderAPI integration enabled!");
            } catch (Exception e) {
                getLogger().warning("Erro ao registrar placeholders: " + e.getMessage());
            }
        }
    }

    public static PurpleEsconde getInstance() {
        return instance;
    }

    // Getters
    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public MapManager getMapManager() { return mapManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PurpleEscondeAPI getAPI() { return api; }
}