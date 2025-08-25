package br.com.purplemc.purpleesconde.placeholders;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import br.com.purplemc.purpleesconde.arena.Arena;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class EscondePlaceholder extends PlaceholderExpansion {

    private final PurpleEsconde plugin;

    public EscondePlaceholder(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "esconde";
    }

    @Override
    public @NotNull String getAuthor() {
        return "PurpleStudiosBR";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (identifier.equalsIgnoreCase("jogando")) {
            int total = 0;
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getState().name().equalsIgnoreCase("WAITING") ||
                        arena.getState().name().equalsIgnoreCase("STARTING") ||
                        arena.getState().name().equalsIgnoreCase("INGAME")) {
                    total += arena.getPlayers().size();
                }
            }
            return String.valueOf(total);
        }
        return null;
    }
}