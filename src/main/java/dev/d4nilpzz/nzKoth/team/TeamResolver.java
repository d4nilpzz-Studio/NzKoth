package dev.d4nilpzz.nzKoth.team;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.text.Text;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TeamResolver {

    private final NzKoth plugin;
    private String placeholder = "";
    private TeamDisplay display = TeamDisplay.SUFFIX;
    private String noTeam = "";

    public TeamResolver(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void load() {
        placeholder = plugin.getConfig().getString("teams.placeholder", "");
        noTeam = plugin.getConfig().getString("teams.no-team", "");
        String raw = plugin.getConfig().getString("teams.display", "SUFFIX");
        display = TeamDisplay.parse(raw);
    }

    public @NotNull String teamOf(@NotNull Player player) {
        if (!placeholder.isBlank() && Text.hasPlaceholders()) {
            String resolved = PlainTextComponentSerializer.plainText()
                    .serialize(Text.parse(player, placeholder))
                    .trim();
            if (!resolved.isBlank() && !resolved.equals(placeholder)) {
                return resolved;
            }
        }
        Team vanilla = player.getScoreboard().getEntryTeam(player.getName());
        if (vanilla != null) {
            return vanilla.getName();
        }
        return noTeam;
    }

    public @NotNull String displayNameOf(@NotNull Player player, boolean teamMode) {
        if (!teamMode) {
            return player.getName();
        }
        String team = teamOf(player);
        if (team.isBlank()) {
            return player.getName();
        }
        return switch (display) {
            case REPLACE -> team;
            case SUFFIX -> player.getName() + " [" + team + "]";
            case PREFIX -> "[" + team + "] " + player.getName();
        };
    }

    public @NotNull TeamDisplay display() {
        return display;
    }

    public enum TeamDisplay {

        REPLACE,
        PREFIX,
        SUFFIX;

        static @NotNull TeamDisplay parse(@NotNull String raw) {
            for (TeamDisplay value : values()) {
                if (value.name().equalsIgnoreCase(raw.trim())) {
                    return value;
                }
            }
            return SUFFIX;
        }

        public @NotNull String lowerName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
