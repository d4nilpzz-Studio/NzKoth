package dev.d4nilpzz.nzKoth.hook;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.display.BossBarService;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.koth.session.ScoreSession;
import dev.d4nilpzz.nzKoth.storage.WinRecord;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class NzKothExpansion extends PlaceholderExpansion {

    private final NzKoth plugin;

    public NzKothExpansion(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nzkoth";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        String query = params.toLowerCase(Locale.ROOT);

        switch (query) {
            case "version" -> {
                return plugin.getPluginMeta().getVersion();
            }
            case "running" -> {
                return String.valueOf(plugin.sessions().runningCount());
            }
            case "total" -> {
                return String.valueOf(plugin.koths().all().size());
            }
            case "wins" -> {
                if (player == null) {
                    return "0";
                }
                return String.valueOf(plugin.storage().wins(player.getUniqueId()).getNow(0));
            }
            default -> {
            }
        }

        if (query.startsWith("top_")) {
            return topPlaceholder(query.substring(4));
        }

        int split = query.lastIndexOf('_');
        while (split > 0) {
            String kothName = query.substring(0, split);
            String field = query.substring(split + 1);
            Koth koth = plugin.koths().get(kothName);
            if (koth != null) {
                return resolve(koth, field);
            }
            split = query.lastIndexOf('_', split - 1);
        }
        return null;
    }

    private @Nullable String topPlaceholder(@NotNull String query) {
        int lastSplit = query.lastIndexOf('_');
        if (lastSplit <= 0) {
            return null;
        }
        String field = query.substring(lastSplit + 1);
        String head = query.substring(0, lastSplit);
        int positionSplit = head.lastIndexOf('_');
        if (positionSplit <= 0) {
            return null;
        }
        String boardId = head.substring(0, positionSplit);
        int position;
        try {
            position = Integer.parseInt(head.substring(positionSplit + 1));
        } catch (NumberFormatException exception) {
            return null;
        }
        if (position < 1) {
            return null;
        }
        List<WinRecord> records = plugin.leaderboards().top(boardId, position);
        if (records.size() < position) {
            return switch (field) {
                case "name", "player" -> plugin.messages().raw("leaderboard.none-name");
                case "wins", "value" -> "0";
                default -> null;
            };
        }
        WinRecord record = records.get(position - 1);
        return switch (field) {
            case "name", "player" -> record.playerName();
            case "wins", "value" -> String.valueOf(record.wins());
            default -> null;
        };
    }

    private @Nullable String resolve(@NotNull Koth koth, @NotNull String field) {
        KothSession session = plugin.sessions().session(koth.name());
        return switch (field) {
            case "name" -> koth.name();
            case "mode" -> koth.mode().name();
            case "active", "running" -> String.valueOf(session != null);
            case "status" -> session == null ? "stopped" : session.statusLine();
            case "controller", "capturer" -> session == null ? "" : session.controllerName();
            case "contested" -> String.valueOf(session != null && session.contested());
            case "time", "timeleft" -> session == null ? "00:00"
                    : BossBarService.formatTime(session.secondsLeft());
            case "seconds" -> session == null ? "0" : String.valueOf(session.secondsLeft());
            case "progress" -> session == null ? "0"
                    : String.valueOf(Math.round(session.progress() * 100.0));
            case "score" -> session instanceof ScoreSession score
                    ? String.valueOf(score.topScore()) : "0";
            case "leader" -> session instanceof ScoreSession score ? score.topName() : "";
            case "target" -> String.valueOf(koth.targetScore());
            default -> null;
        };
    }
}
