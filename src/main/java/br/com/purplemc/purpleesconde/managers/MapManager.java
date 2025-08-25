package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.map.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MapManager {

    private final PurpleEsconde plugin;
    private final Map<String, GameMap> maps;
    private final Map<UUID, Set<String>> playerFavorites;
    private final Map<UUID, LocalDate> lastMapSelection;

    public MapManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.maps = new LinkedHashMap<String, GameMap>();
        this.playerFavorites = new HashMap<UUID, Set<String>>();
        this.lastMapSelection = new HashMap<UUID, LocalDate>();
        reloadMaps();
        loadPlayerData();
    }

    public void reloadMaps() {
        maps.clear();
        plugin.getConfigManager().getMaps().options().copyDefaults(true);
        ConfigurationSection mapsSection = plugin.getConfigManager().getMaps().getConfigurationSection("maps");
        if (mapsSection == null) {
            plugin.getLogger().info("Seção 'maps' não encontrada no arquivo maps.yml, criando estrutura padrão...");
            plugin.getConfigManager().getMaps().set("maps.exemplo.world", "world");
            plugin.getConfigManager().getMaps().set("maps.exemplo.waiting-lobby.x", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.waiting-lobby.y", 64.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.waiting-lobby.z", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.waiting-lobby.yaw", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.waiting-lobby.pitch", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.seeker-spawn.x", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.seeker-spawn.y", 64.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.seeker-spawn.z", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.seeker-spawn.yaw", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.seeker-spawn.pitch", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.hider-spawns.0.x", 10.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.hider-spawns.0.y", 64.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.hider-spawns.0.z", 10.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.hider-spawns.0.yaw", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.hider-spawns.0.pitch", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.min.x", -50.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.min.y", 0.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.min.z", -50.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.max.x", 50.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.max.y", 100.0);
            plugin.getConfigManager().getMaps().set("maps.exemplo.barrier.max.z", 50.0);
            plugin.getConfigManager().saveConfig(plugin.getConfigManager().getMaps(), "maps.yml");
            mapsSection = plugin.getConfigManager().getMaps().getConfigurationSection("maps");
        }

        if (mapsSection != null) {
            for (String mapName : mapsSection.getKeys(false)) {
                try {
                    GameMap gameMap = loadMapFromConfig(mapName);
                    if (gameMap != null) {
                        maps.put(mapName, gameMap);
                        plugin.getLogger().info("Mapa carregado: " + mapName);
                    } else {
                        plugin.getLogger().warning("Falha ao carregar mapa: " + mapName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao carregar mapa " + mapName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        plugin.getLogger().info("Total de mapas carregados: " + maps.size());
    }

    private GameMap loadMapFromConfig(String mapName) {
        try {
            ConfigurationSection mapSection = plugin.getConfigManager().getMaps().getConfigurationSection("maps." + mapName);
            if (mapSection == null) {
                plugin.getLogger().warning("Seção do mapa não encontrada: " + mapName);
                return null;
            }

            String worldName = mapSection.getString("world");
            if (worldName == null) {
                plugin.getLogger().warning("Nome do mundo não especificado para o mapa: " + mapName);
                return null;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Mundo não encontrado: " + worldName + " para o mapa: " + mapName);
                return null;
            }

            Location waitingLobby = getLocationFromConfig(mapSection, "waiting-lobby", world);
            if (waitingLobby == null) {
                plugin.getLogger().warning("Lobby de espera não configurado para o mapa: " + mapName);
                return null;
            }

            Location seekerSpawn = getLocationFromConfig(mapSection, "seeker-spawn", world);
            if (seekerSpawn == null) {
                plugin.getLogger().warning("Spawn do procurador não configurado para o mapa: " + mapName);
                return null;
            }

            List<Location> hiderSpawns = new ArrayList<Location>();
            ConfigurationSection hidersSection = mapSection.getConfigurationSection("hider-spawns");
            if (hidersSection != null) {
                for (String key : hidersSection.getKeys(false)) {
                    Location spawn = getLocationFromConfig(hidersSection, key, world);
                    if (spawn != null) {
                        hiderSpawns.add(spawn);
                    }
                }
            }

            if (hiderSpawns.isEmpty()) {
                plugin.getLogger().warning("Nenhum spawn de escondedor configurado para o mapa: " + mapName);
                return null;
            }

            Location barrierMin = getLocationFromConfig(mapSection, "barrier.min", world);
            Location barrierMax = getLocationFromConfig(mapSection, "barrier.max", world);

            return new GameMap(mapName, world, waitingLobby, seekerSpawn, hiderSpawns, barrierMin, barrierMax);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar configuração do mapa " + mapName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Location getLocationFromConfig(ConfigurationSection section, String path, World world) {
        try {
            if (!section.contains(path + ".x")) return null;
            double x = section.getDouble(path + ".x");
            double y = section.getDouble(path + ".y");
            double z = section.getDouble(path + ".z");
            float yaw = (float) section.getDouble(path + ".yaw", 0);
            float pitch = (float) section.getDouble(path + ".pitch", 0);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar localização " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void loadPlayerData() {
        try {
            ConfigurationSection playersSection = plugin.getConfigManager().getPlayers().getConfigurationSection("players");
            if (playersSection == null) return;

            for (String playerKey : playersSection.getKeys(false)) {
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(playerKey);
                } catch (Exception e) {
                    Player player = Bukkit.getPlayerExact(playerKey);
                    if (player != null) {
                        uuid = player.getUniqueId();
                    }
                }
                if (uuid == null) continue;

                List<String> favorites = playersSection.getStringList(playerKey + ".favorites");
                if (!favorites.isEmpty()) {
                    playerFavorites.put(uuid, new HashSet<String>(favorites));
                }

                String lastSelection = playersSection.getString(playerKey + ".last-selection");
                if (lastSelection != null) {
                    try {
                        LocalDate date = LocalDate.parse(lastSelection);
                        lastMapSelection.put(uuid, date);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar dados dos jogadores: " + e.getMessage());
        }
    }

    public void savePlayerData() {
        try {
            for (Map.Entry<UUID, Set<String>> entry : playerFavorites.entrySet()) {
                String playerKey = entry.getKey().toString();
                plugin.getConfigManager().getPlayers().set("players." + playerKey + ".favorites", new ArrayList<String>(entry.getValue()));
            }

            for (Map.Entry<UUID, LocalDate> entry : lastMapSelection.entrySet()) {
                String playerKey = entry.getKey().toString();
                plugin.getConfigManager().getPlayers().set("players." + playerKey + ".last-selection", entry.getValue().toString());
            }

            plugin.getConfigManager().saveConfig(plugin.getConfigManager().getPlayers(), "players.yml");
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao salvar dados dos jogadores: " + e.getMessage());
        }
    }

    public GameMap getMap(String name) {
        return maps.get(name);
    }

    public List<GameMap> getMaps() {
        return new ArrayList<GameMap>(maps.values());
    }

    public GameMap getRandomMap() {
        if (maps.isEmpty()) return null;
        List<GameMap> mapList = new ArrayList<GameMap>(maps.values());
        return mapList.get(new Random().nextInt(mapList.size()));
    }

    public GameMap getRandomFavoriteMap(Player player) {
        Set<String> favorites = playerFavorites.get(player.getUniqueId());
        if (favorites == null || favorites.isEmpty()) return null;
        List<GameMap> favoriteMaps = new ArrayList<GameMap>();
        for (String mapName : favorites) {
            GameMap map = maps.get(mapName);
            if (map != null) {
                favoriteMaps.add(map);
            }
        }
        if (favoriteMaps.isEmpty()) return null;
        return favoriteMaps.get(new Random().nextInt(favoriteMaps.size()));
    }

    public void addFavorite(Player player, String mapName) {
        if (!maps.containsKey(mapName)) return;
        Set<String> favorites = playerFavorites.get(player.getUniqueId());
        if (favorites == null) {
            favorites = new HashSet<String>();
            playerFavorites.put(player.getUniqueId(), favorites);
        }
        favorites.add(mapName);
        savePlayerData();
    }

    public void removeFavorite(Player player, String mapName) {
        Set<String> favorites = playerFavorites.get(player.getUniqueId());
        if (favorites != null && favorites.remove(mapName)) {
            savePlayerData();
        }
    }

    public boolean isPlayerFavorite(Player player, String mapName) {
        Set<String> favorites = playerFavorites.get(player.getUniqueId());
        return favorites != null && favorites.contains(mapName);
    }

    public boolean hasPlayerFavorites(Player player) {
        Set<String> favorites = playerFavorites.get(player.getUniqueId());
        return favorites != null && !favorites.isEmpty();
    }

    public boolean canPlayerSelectMap(Player player) {
        if (player.hasPermission("purpleesconde.unlimited-maps")) return true;
        LocalDate lastSelection = lastMapSelection.get(player.getUniqueId());
        return lastSelection == null || !lastSelection.equals(LocalDate.now());
    }

    public void markMapSelected(Player player) {
        lastMapSelection.put(player.getUniqueId(), LocalDate.now());
        savePlayerData();
    }

    public void createMap(String name, World world, Location waitingLobby, Location seekerSpawn,
                          List<Location> hiderSpawns, Location barrierMin, Location barrierMax) {
        try {
            String path = "maps." + name;
            plugin.getConfigManager().getMaps().set(path + ".world", world.getName());
            saveLocationToConfig(waitingLobby, path + ".waiting-lobby");
            saveLocationToConfig(seekerSpawn, path + ".seeker-spawn");

            for (int i = 0; i < hiderSpawns.size(); i++) {
                saveLocationToConfig(hiderSpawns.get(i), path + ".hider-spawns." + i);
            }

            if (barrierMin != null) saveLocationToConfig(barrierMin, path + ".barrier.min");
            if (barrierMax != null) saveLocationToConfig(barrierMax, path + ".barrier.max");

            plugin.getConfigManager().saveConfig(plugin.getConfigManager().getMaps(), "maps.yml");
            reloadMaps();

            plugin.getLogger().info("Mapa criado com sucesso: " + name);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao criar mapa " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void cloneMap(String sourceName, String newName) {
        GameMap src = getMap(sourceName);
        if (src == null) return;
        createMap(newName, src.getWorld(), src.getWaitingLobby(), src.getSeekerSpawn(),
                new ArrayList<Location>(src.getHiderSpawns()), src.getBarrierMin(), src.getBarrierMax());
    }

    public List<GameMap> cloneArena(String mapName, int quantity) {
        GameMap original = getMap(mapName);
        if (original == null) return Collections.emptyList();
        List<GameMap> created = new ArrayList<GameMap>();
        String base = mapName;
        int nextNum = 1;
        Set<String> existing = new HashSet<String>(maps.keySet());
        for (int i = 0; i < quantity; i++) {
            String nextName;
            while (true) {
                nextName = base + "_clone" + nextNum;
                nextNum++;
                if (!existing.contains(nextName)) break;
            }
            createMap(nextName, original.getWorld(), original.getWaitingLobby(), original.getSeekerSpawn(),
                    new ArrayList<Location>(original.getHiderSpawns()), original.getBarrierMin(), original.getBarrierMax());
            GameMap clone = getMap(nextName);
            if (clone != null) {
                created.add(clone);
                existing.add(nextName);
            }
        }
        plugin.getConfigManager().saveConfig(plugin.getConfigManager().getMaps(), "maps.yml");
        reloadMaps();
        return created;
    }

    private void saveLocationToConfig(Location location, String path) {
        if (location == null) return;
        plugin.getConfigManager().getMaps().set(path + ".x", location.getX());
        plugin.getConfigManager().getMaps().set(path + ".y", location.getY());
        plugin.getConfigManager().getMaps().set(path + ".z", location.getZ());
        plugin.getConfigManager().getMaps().set(path + ".yaw", location.getYaw());
        plugin.getConfigManager().getMaps().set(path + ".pitch", location.getPitch());
    }
}