package br.com.purplemc.purpleesconde.utils;

import br.com.purplemc.purpleesconde.arena.Arena;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ServiceLoader;

public class MessageUtils {

    public static void sendActionBar(Player player, String message) {
        player.sendMessage(applyPlaceholders(player, message));
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        try {
            player.sendTitle(title, subtitle);
        } catch (Exception e) {
            if (title != null) player.sendMessage(title);
            if (subtitle != null) player.sendMessage(subtitle);
        }
    }

    public static void broadcastToArena(Arena arena, String message) {
        if (message == null || message.isEmpty()) return;
        for (Player player : arena.getPlayers()) {
            player.sendMessage(applyPlaceholders(player, message));
        }
    }

    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public static String colorize(String text) {
        return text.replace("&", "§");
    }

    public static String stripColor(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public static String replaceVariables(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    public static String applyPlaceholders(Player player, String message) {
        if (message == null) return "";
        String processed = message;
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }
        for (PlaceholderProvider provider : PlaceholderProviderLoader.getProviders()) {
            processed = provider.setPlaceholders(player, processed);
        }
        return processed;
    }

    public interface PlaceholderProvider {
        String setPlaceholders(Player player, String message);
    }

    public static class PlaceholderProviderLoader {
        private static final Iterable<PlaceholderProvider> providers;
        static {
            ServiceLoader<PlaceholderProvider> loader = ServiceLoader.load(PlaceholderProvider.class);
            providers = loader;
        }
        public static Iterable<PlaceholderProvider> getProviders() {
            return providers;
        }
    }
}