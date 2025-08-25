package br.com.purplemc.purpleesconde.managers;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import br.com.purplemc.purpleesconde.arena.ArenaState;
import br.com.purplemc.purpleesconde.map.GameMap;
import br.com.purplemc.purpleesconde.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {

    private final PurpleEsconde plugin;
    private final Map<UUID, Long> lastClickTime;
    private final Set<UUID> openInventories;

    public GUIManager(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.lastClickTime = new HashMap<>();
        this.openInventories = new HashSet<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void joinArena(Player player, String mapName) {
        Arena arena = plugin.getArenaManager().getAvailableArena(mapName);
        if (arena == null) {
            GameMap map = plugin.getMapManager().getMap(mapName);
            if (map != null) {
                arena = plugin.getArenaManager().createArena(map);
            }
        }
        if (arena != null) {
            plugin.getArenaManager().addPlayerToArena(player, arena);
        } else {
            player.sendMessage("§cNão foi possível encontrar ou criar uma arena para este mapa.");
        }
    }

    public void openMapSelector(Player player) {
        List<GameMap> maps = plugin.getMapManager().getMaps();
        Inventory inv = Bukkit.createInventory(null, 54, "§8Selecionar mapa - Modo Solo");
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int mapIndex = 0;
        for (int slot : slots) {
            if (mapIndex >= maps.size()) break;
            GameMap map = maps.get(mapIndex);
            boolean isFavorite = plugin.getMapManager().isPlayerFavorite(player, map.getName());
            Material material = isFavorite ? Material.MAP : Material.EMPTY_MAP;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + map.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Salas disponíveis: §a" + getAvailableRooms(map));
            lore.add("§7Em espera: §a" + getWaitingPlayers(map));
            lore.add("§7Jogando: §c" + getPlayingPlayers(map));
            lore.add("");
            if (isFavorite) {
                lore.add("§6★ Mapa Favorito");
            }
            if (plugin.getMapManager().canPlayerSelectMap(player)) {
                lore.add("§aClique esquerdo para jogar");
                if (player.hasPermission("purpleesconde.favorite"))
                    lore.add("§eClique direito para favoritar");
            } else {
                lore.add("§cVocê já selecionou um mapa hoje!");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            mapIndex++;
        }
        inv.setItem(48, ItemUtils.createGUIItem(Material.PAPER, "§aInformações",
                Arrays.asList("§7Clique Direito para Favoritar um mapa", "§7Clique Esquerdo para Selecionar um mapa")));
        inv.setItem(49, ItemUtils.createGUIItem(Material.ARROW, "§cVoltar", null));
        inv.setItem(50, ItemUtils.createGUIItem(Material.DIAMOND, "§bMapa Favorito Aleatório",
                Arrays.asList("§7Clique para jogar em um", "§7mapa favorito aleatório")));
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Esconde Esconde");
        inv.setItem(12, ItemUtils.createGUIItem(Material.ENDER_PEARL, "§aPartida Rápida", Collections.singletonList("§7Clique para entrar em uma partida.")));
        inv.setItem(14, ItemUtils.createGUIItem(Material.SIGN, "§eSelecionar Mapa", Collections.singletonList("§7Clique para escolher um mapa.")));
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    public void openGameMapInfo(Player player, String mapName) {
        GameMap map = plugin.getMapManager().getMap(mapName);
        if (map == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, "§8" + mapName + " - Modo Padrão");
        ItemStack mapItem = new ItemStack(Material.MAP);
        ItemMeta meta = mapItem.getItemMeta();
        meta.setDisplayName("§e" + mapName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Modo Padrão");
        lore.add("");
        lore.add("§7Salas disponíveis: §a" + getAvailableRooms(map));
        lore.add("");
        lore.add("§aClique para Jogar");
        meta.setLore(lore);
        mapItem.setItemMeta(meta);
        inv.setItem(13, mapItem);
        inv.setItem(18, ItemUtils.createGUIItem(Material.ARROW, "§cVoltar", null));
        player.openInventory(inv);
        openInventories.add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        boolean isMapSelector = title.equals("§8Selecionar mapa - Modo Solo");
        boolean isMainMenu = title.equals("§8Esconde Esconde");
        boolean isGameMapInfo = title.contains(" - Modo Padrão") && !title.startsWith("§8Selecionar");

        if (!(isMapSelector || isMainMenu || isGameMapInfo)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        long currentTime = System.currentTimeMillis();
        UUID playerUUID = player.getUniqueId();
        long lastClick = lastClickTime.getOrDefault(playerUUID, 0L);
        if ((currentTime - lastClick) < 500) {
            return;
        }
        lastClickTime.put(playerUUID, currentTime);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (isMapSelector) {
            handleMapSelectorClick(player, item, event.getClick(), event.getRawSlot());
        } else if (isMainMenu) {
            handleMainMenuClick(player, item);
        } else if (isGameMapInfo) {
            handleGameMapInfoClick(player, item);
        }
    }

    private void handleMapSelectorClick(Player player, ItemStack item, ClickType click, int slot) {
        String mapName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName().replace("§e", "") : null;
        if (slot == 49) {
            player.closeInventory();
            openMainMenu(player);
            return;
        } else if (slot == 50) {
            GameMap randomMap = plugin.getMapManager().getRandomFavoriteMap(player);
            if (randomMap != null) {
                player.closeInventory();
                joinArena(player, randomMap.getName());
            } else {
                player.sendMessage("§cVocê não possui mapas favoritos para entrar em um aleatório.");
            }
            return;
        }
        if (item.getType() == Material.MAP || item.getType() == Material.EMPTY_MAP) {
            if (click.isLeftClick()) {
                if (plugin.getMapManager().canPlayerSelectMap(player)) {
                    player.closeInventory();
                    joinArena(player, mapName);
                    plugin.getMapManager().markMapSelected(player);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("map.daily-limit"));
                }
            } else if (click.isRightClick()) {
                if (!player.hasPermission("purpleesconde.favorite")) return;
                if (plugin.getMapManager().isPlayerFavorite(player, mapName)) {
                    plugin.getMapManager().removeFavorite(player, mapName);
                    player.sendMessage("§cMapa '" + mapName + "' removido dos favoritos.");
                } else {
                    plugin.getMapManager().addFavorite(player, mapName);
                    player.sendMessage("§aMapa '" + mapName + "' adicionado aos favoritos.");
                }
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> openMapSelector(player), 5L);
            }
        }
    }

    private void handleMainMenuClick(Player player, ItemStack item) {
        if (item.getType() == Material.ENDER_PEARL) {
            player.closeInventory();
            Arena arena = plugin.getArenaManager().getRandomArena();
            if (arena != null) {
                plugin.getArenaManager().addPlayerToArena(player, arena);
            } else {
                player.sendMessage("§cNão há arenas disponíveis no momento.");
            }
        } else if (item.getType() == Material.SIGN) {
            player.closeInventory();
            openMapSelector(player);
        }
    }

    private void handleGameMapInfoClick(Player player, ItemStack item) {
        if (item.getType() == Material.MAP) {
            String mapName = item.getItemMeta().getDisplayName().replace("§e", "");
            player.closeInventory();
            joinArena(player, mapName);
        } else if (item.getType() == Material.ARROW) {
            player.closeInventory();
            openMapSelector(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (openInventories.contains(event.getWhoClicked().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (openInventories.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
        lastClickTime.remove(event.getPlayer().getUniqueId());
    }

    private int getWaitingPlayers(GameMap map) {
        int count = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName()) &&
                    (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING)) {
                count += arena.getPlayers().size();
            }
        }
        return count;
    }

    private int getPlayingPlayers(GameMap map) {
        int count = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName()) &&
                    arena.getState() == ArenaState.INGAME) {
                count += arena.getPlayers().size();
            }
        }
        return count;
    }

    private int getAvailableRooms(GameMap map) {
        int availableRooms = 0;
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getGameMap() != null && arena.getGameMap().getName().equals(map.getName()) &&
                    !arena.isFull() && arena.getState() != ArenaState.INGAME) {
                availableRooms++;
            }
        }
        return Math.max(1, availableRooms);
    }
}