package br.com.purplemc.purpleesconde.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import java.io.*;

public class WorldUtils {

    public static boolean copyWorld(File source, File target) {
        if (!source.exists()) return false;
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) return false;
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    if (!file.equals("uid.dat") && !file.equals("session.lock")) {
                        if (!copyWorld(new File(source, file), new File(target, file))) return false;
                    }
                }
            }
        } else {
            try (
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(target)
            ) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public static World createArenaWorld(String template, String arenaWorldName, Plugin plugin) {
        File worldContainer = Bukkit.getWorldContainer();
        File templateFolder = new File(worldContainer, template);
        File arenaFolder = new File(worldContainer, arenaWorldName);

        if (arenaFolder.exists()) {
            unloadWorld(arenaWorldName);
            deleteWorld(arenaFolder);
        }

        if (!copyWorld(templateFolder, arenaFolder)) return null;
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv import " + arenaWorldName + " normal");
        }
        return Bukkit.createWorld(new WorldCreator(arenaWorldName));
    }

    public static void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove " + worldName);
            }
        }
    }

    public static boolean deleteWorld(File path) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) for (File file : files) deleteWorld(file);
        }
        return path.delete();
    }
}