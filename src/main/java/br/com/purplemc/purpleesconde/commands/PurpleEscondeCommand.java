package br.com.purplemc.purpleesconde.commands;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.map.GameMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import java.util.*;

public class PurpleEscondeCommand implements CommandExecutor {

    private final PurpleEsconde plugin;
    private final Map<Player, MapSetup> setups;

    public PurpleEscondeCommand(PurpleEsconde plugin) {
        this.plugin = plugin;
        this.setups = new HashMap<Player, MapSetup>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("build")) return handleBuild(player);
        if (sub.equals("setlobby")) return handleSetLobby(player);
        if (sub.equals("setupvisual")) return handleSetupVisual(player);
        if (sub.equals("createmap")) return handleCreateMap(player, args);
        if (sub.equals("setwaiting")) return handleSetWaiting(player);
        if (sub.equals("setseeker")) return handleSetSeekerSpawn(player);
        if (sub.equals("addhider")) return handleAddHiderSpawn(player);
        if (sub.equals("setbarrier")) return handleSetBarrier(player);
        if (sub.equals("finishmap")) return handleFinishMap(player);
        if (sub.equals("cancelmap")) return handleCancelMap(player);
        if (sub.equals("statusmap")) return handleStatusMap(player);
        if (sub.equals("reloadmaps")) return handleReloadMaps(player);
        if (sub.equals("clonemap")) return handleCloneMap(player, args);
        if (sub.equals("clonearena")) return handleCloneArena(player, args);
        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§lPurpleEsconde - Setup de Mapas");
        player.sendMessage("§e/purpleesconde build §7- Ativa/desativa modo construção");
        player.sendMessage("§e/purpleesconde setlobby §7- Define o lobby principal");
        player.sendMessage("§e/purpleesconde createmap <nome> §7- Começa setup de novo mapa");
        player.sendMessage("§e/purpleesconde setwaiting §7- Define lobby de espera");
        player.sendMessage("§e/purpleesconde setseeker §7- Define spawn do procurador");
        player.sendMessage("§e/purpleesconde addhider §7- Adiciona spawn de escondedor");
        player.sendMessage("§e/purpleesconde setbarrier §7- Define área da barreira (use pos1 e pos2 visualmente)");
        player.sendMessage("§e/purpleesconde statusmap §7- Mostra status do setup atual");
        player.sendMessage("§e/purpleesconde finishmap §7- Finaliza e salva o mapa (só funciona se tudo estiver setado)");
        player.sendMessage("§e/purpleesconde cancelmap §7- Cancela o setup do mapa atual");
        player.sendMessage("§e/purpleesconde reloadmaps §7- Recarrega todos os mapas do plugin");
        player.sendMessage("§e/purpleesconde clonemap <original> <novo> §7- Clona um mapa para novo nome");
        player.sendMessage("§e/purpleesconde clonearena <mapa> <quantidade> §7- Aumenta a quantidade de salas para o mapa");
    }

    private boolean handleBuild(Player player) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (player.hasMetadata("pe-build")) {
            player.removeMetadata("pe-build", plugin);
            player.sendMessage("§cModo de construção desativado!");
        } else {
            player.setMetadata("pe-build", new FixedMetadataValue(plugin, true));
            player.sendMessage("§aModo de construção ativado!");
        }
        return true;
    }

    private boolean handleSetLobby(Player player) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        plugin.getConfigManager().setMainLobby(player.getLocation());
        player.sendMessage("§aLobby principal setado!");
        return true;
    }

    private boolean handleSetupVisual(Player player) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        player.setMetadata("pe-setup", new FixedMetadataValue(plugin, true));
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.BLAZE_ROD));
        player.getInventory().setItem(1, new ItemStack(Material.STICK));
        player.getInventory().setItem(8, new ItemStack(Material.NETHER_STAR));
        player.sendMessage("§eItens de setup visual recebidos: §6Blaze Rod (Pos1), §6Stick (Pos2), §bNether Star (Finalizar)");
        player.sendMessage("§7Use as ferramentas para definir a área da barreira visualmente.");
        return true;
    }

    private boolean handleCreateMap(Player player, String[] args) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cUso: /purpleesconde createmap <nome>");
            return true;
        }
        if (setups.containsKey(player)) {
            player.sendMessage("§cVocê já está criando um mapa! Finalize ou cancele antes de iniciar outro.");
            return true;
        }
        String name = args[1];
        if (plugin.getMapManager().getMap(name) != null) {
            player.sendMessage("§cJá existe um mapa com esse nome!");
            return true;
        }
        setups.put(player, new MapSetup(name));
        player.sendMessage("§aIniciado setup do mapa: §e" + name);
        player.sendMessage("§7Ordem correta: §eLobby de espera → Spawn do procurador → Spawns de escondedor → Área da barreira → Finalizar");
        player.sendMessage("§7Use os comandos na ordem acima.");
        return true;
    }

    private boolean handleSetWaiting(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        if (setup.waitingLobby != null) {
            player.sendMessage("§cLobby de espera já setado!");
            return true;
        }
        setup.waitingLobby = player.getLocation().clone();
        player.sendMessage("§aLobby de espera setado!");
        return true;
    }

    private boolean handleSetSeekerSpawn(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        if (setup.waitingLobby == null) {
            player.sendMessage("§cDefina o lobby de espera primeiro!");
            return true;
        }
        if (setup.seekerSpawn != null) {
            player.sendMessage("§cSpawn do procurador já setado!");
            return true;
        }
        setup.seekerSpawn = player.getLocation().clone();
        player.sendMessage("§aSpawn do procurador setado!");
        return true;
    }

    private boolean handleAddHiderSpawn(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        if (setup.waitingLobby == null || setup.seekerSpawn == null) {
            player.sendMessage("§cDefina o lobby de espera e o spawn do procurador antes!");
            return true;
        }
        setup.hiderSpawns.add(player.getLocation().clone());
        player.sendMessage("§aSpawn de escondedor adicionado! Total: " + setup.hiderSpawns.size());
        return true;
    }

    private boolean handleSetBarrier(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        if (setup.waitingLobby == null || setup.seekerSpawn == null || setup.hiderSpawns.isEmpty()) {
            player.sendMessage("§cDefina o lobby de espera, spawn do procurador e pelo menos um spawn de escondedor antes!");
            return true;
        }
        Location pos1 = null, pos2 = null;
        if (player.hasMetadata("pe-setup-pos1") && player.hasMetadata("pe-setup-pos2")) {
            pos1 = (Location) player.getMetadata("pe-setup-pos1").get(0).value();
            pos2 = (Location) player.getMetadata("pe-setup-pos2").get(0).value();
        }
        if (pos1 == null || pos2 == null) {
            player.sendMessage("§cUse o setup visual para definir as posições 1 e 2 (Blaze Rod e Stick)!");
            player.sendMessage("§eOu use: /purpleesconde setupvisual");
            return true;
        }
        if (!pos1.getWorld().equals(pos2.getWorld()) || !pos1.getWorld().equals(player.getWorld())) {
            player.sendMessage("§cAs posições devem estar no mesmo mundo!");
            return true;
        }
        setup.barrierMin = pos1.clone();
        setup.barrierMax = pos2.clone();
        player.sendMessage("§aÁrea da barreira definida!");
        return true;
    }

    private boolean handleFinishMap(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        if (!setup.isComplete()) {
            player.sendMessage("§cVocê precisa setar todos os itens obrigatórios antes de finalizar:");
            player.sendMessage(setup.missingFields());
            return true;
        }
        try {
            plugin.getMapManager().createMap(
                    setup.name,
                    player.getWorld(),
                    setup.waitingLobby,
                    setup.seekerSpawn,
                    setup.hiderSpawns,
                    setup.barrierMin,
                    setup.barrierMax
            );
            player.sendMessage("§aMapa §e" + setup.name + " §acriado com sucesso!");
            setups.remove(player);
        } catch (Exception e) {
            player.sendMessage("§cErro ao criar mapa: " + e.getMessage());
            plugin.getLogger().severe("Erro ao criar mapa " + setup.name + ": " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleCancelMap(Player player) {
        if (!setups.containsKey(player)) {
            player.sendMessage("§cNenhum setup de mapa em andamento!");
            return true;
        }
        setups.remove(player);
        player.sendMessage("§cSetup de mapa cancelado.");
        return true;
    }

    private boolean handleStatusMap(Player player) {
        MapSetup setup = getCurrentSetup(player);
        if (setup == null) return true;
        player.sendMessage(setup.status());
        return true;
    }

    private boolean handleReloadMaps(Player player) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        try {
            plugin.getMapManager().reloadMaps();
            player.sendMessage("§aTodos os mapas recarregados! Total: " + plugin.getMapManager().getMaps().size());
        } catch (Exception e) {
            player.sendMessage("§cErro ao recarregar mapas: " + e.getMessage());
            plugin.getLogger().severe("Erro ao recarregar mapas: " + e.getMessage());
        }
        return true;
    }

    private boolean handleCloneMap(Player player, String[] args) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("§cUso: /purpleesconde clonemap <original> <novo>");
            return true;
        }
        String src = args[1], dst = args[2];
        if (plugin.getMapManager().getMap(src) == null) {
            player.sendMessage("§cMapa original não encontrado: " + src);
            return true;
        }
        if (plugin.getMapManager().getMap(dst) != null) {
            player.sendMessage("§cJá existe um mapa com o nome: " + dst);
            return true;
        }
        try {
            plugin.getMapManager().cloneMap(src, dst);
            player.sendMessage("§aMapa clonado: §e" + src + " §apara §e" + dst);
        } catch (Exception e) {
            player.sendMessage("§cErro ao clonar mapa: " + e.getMessage());
            plugin.getLogger().severe("Erro ao clonar mapa: " + e.getMessage());
        }
        return true;
    }

    private boolean handleCloneArena(Player player, String[] args) {
        if (!player.hasPermission("purpleesconde.admin")) {
            player.sendMessage("§cSem permissão.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("§cUso: /purpleesconde clonearena <mapa> <quantidade>");
            return true;
        }
        String mapName = args[1];
        int quantity;
        try {
            quantity = Integer.parseInt(args[2]);
            if (quantity <= 0 || quantity > 50) {
                player.sendMessage("§cQuantidade deve ser entre 1 e 50!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cQuantidade deve ser um número!");
            return true;
        }

        if (plugin.getMapManager().getMap(mapName) == null) {
            player.sendMessage("§cMapa não encontrado: " + mapName);
            return true;
        }

        plugin.getArenaManager().addArenas(mapName, quantity);
        player.sendMessage("§aQuantidade de salas para o mapa §e" + mapName + " §aatualizada para §e" + quantity + "§a!");
        return true;
    }

    private MapSetup getCurrentSetup(Player player) {
        MapSetup setup = setups.get(player);
        if (setup == null) {
            player.sendMessage("§cVocê não está criando nenhum mapa. Use /purpleesconde createmap <nome>.");
            return null;
        }
        return setup;
    }

    private static class MapSetup {
        final String name;
        Location waitingLobby;
        Location seekerSpawn;
        List<Location> hiderSpawns;
        Location barrierMin;
        Location barrierMax;

        MapSetup(String name) {
            this.name = name;
            this.hiderSpawns = new ArrayList<Location>();
        }

        boolean isComplete() {
            return waitingLobby != null && seekerSpawn != null && !hiderSpawns.isEmpty() && barrierMin != null && barrierMax != null;
        }

        String missingFields() {
            List<String> missing = new ArrayList<String>();
            if (waitingLobby == null) missing.add("§b- Lobby de espera");
            if (seekerSpawn == null) missing.add("§b- Spawn do procurador");
            if (hiderSpawns.isEmpty()) missing.add("§b- Pelo menos 1 spawn de escondedor");
            if (barrierMin == null || barrierMax == null) missing.add("§b- Área da barreira (pos1/pos2)");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < missing.size(); i++) {
                sb.append(missing.get(i));
                if (i < missing.size() - 1) sb.append("\n");
            }
            return sb.toString();
        }

        String status() {
            StringBuilder sb = new StringBuilder("§eStatus do setup do mapa: §a" + name + "\n");
            sb.append(waitingLobby != null ? "§a✓ " : "§c✗ ").append("Lobby de espera\n");
            sb.append(seekerSpawn != null ? "§a✓ " : "§c✗ ").append("Spawn do procurador\n");
            sb.append(!hiderSpawns.isEmpty() ? "§a✓ " : "§c✗ ").append("Spawns de escondedor: ").append(hiderSpawns.size()).append("\n");
            sb.append(barrierMin != null && barrierMax != null ? "§a✓ " : "§c✗ ").append("Área da barreira");
            return sb.toString();
        }
    }
}