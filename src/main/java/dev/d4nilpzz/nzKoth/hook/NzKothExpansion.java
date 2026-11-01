package dev.d4nilpzz.nzKoth.hook;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.session.CaptureSession;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.koth.session.ScoreSession;
import dev.d4nilpzz.nzKoth.storage.WinRecord;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import dev.d4nilpzz.nzKoth.util.TimeFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NzKothExpansion extends PlaceholderExpansion {

    private static final String ALL_TIME = "alltime";
    private static final String FORMATTED = "_formatted";

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
            case "player_wins", "wins" -> {
                return player == null ? "0"
                        : String.valueOf(plugin.leaderboards().playerWins(ALL_TIME, player.getUniqueId()));
            }
            case "team_wins" -> {
                return String.valueOf(plugin.leaderboards().teamWins(ALL_TIME, teamOf(player)));
            }
            case "next" -> {
                return seconds(plugin.schedulers().secondsUntilNext());
            }
            case "next_formatted" -> {
                return formatted(plugin.schedulers().secondsUntilNext());
            }
            default -> {
            }
        }

        if (query.startsWith("top_")) {
            return topPlaceholder(query.substring(4));
        }
        if (query.startsWith("next_")) {
            String rest = query.substring(5);
            boolean pretty = rest.endsWith(FORMATTED);
            String id = pretty ? rest.substring(0, rest.length() - FORMATTED.length()) : rest;
            long value = plugin.schedulers().secondsUntilSchedule(id);
            return pretty ? formatted(value) : seconds(value);
        }

        int split = query.lastIndexOf('_');
        while (split > 0) {
            Koth koth = plugin.koths().get(query.substring(0, split));
            if (koth != null) {
                String resolved = resolve(koth, query.substring(split + 1), player);
                if (resolved != null) {
                    return resolved;
                }
            }
            split = query.lastIndexOf('_', split - 1);
        }
        return null;
    }

    private @Nullable String resolve(@NotNull Koth koth, @NotNull String field, @Nullable OfflinePlayer player) {
        KothSession session = plugin.sessions().session(koth.name());
        Cuboid region = koth.region();
        Location center = region == null ? null : region.center();

        switch (field) {
            case "active", "running" -> {
                return String.valueOf(session != null);
            }
            case "name" -> {
                return koth.name();
            }
            case "displayname" -> {
                return koth.displayName();
            }
            case "mode" -> {
                return koth.mode().name();
            }
            case "world" -> {
                return region == null ? "" : region.worldName();
            }
            case "x" -> {
                return center == null ? "0" : String.valueOf((int) Math.floor(center.getX()));
            }
            case "y" -> {
                return center == null ? "0" : String.valueOf((int) Math.floor(center.getY()));
            }
            case "z" -> {
                return center == null ? "0" : String.valueOf((int) Math.floor(center.getZ()));
            }
            case "next" -> {
                return seconds(plugin.schedulers().secondsUntilNext(koth.name()));
            }
            case "next_formatted" -> {
                return formatted(plugin.schedulers().secondsUntilNext(koth.name()));
            }
            case "status" -> {
                return session == null ? "stopped" : session.statusLine();
            }
            case "contested" -> {
                return String.valueOf(session != null && session.contested());
            }
            case "capturer" -> {
                return capturerDisplay(koth, session);
            }
            case "capturer_name" -> {
                return session == null ? "" : session.controllerName();
            }
            case "capturer_team", "capturer_team_name" -> {
                return capturerTeam(session);
            }
            case "maxtime" -> {
                return String.valueOf(koth.maxTime().toSeconds());
            }
            case "maxtime_formatted" -> {
                return TimeFormat.pretty(koth.maxTime().toSeconds());
            }
            case "maxtimeleft" -> {
                return String.valueOf(maxTimeLeft(koth, session));
            }
            case "maxtimeleft_formatted" -> {
                return TimeFormat.pretty(maxTimeLeft(koth, session));
            }
            case "timetaken" -> {
                return String.valueOf(session == null ? 0 : session.elapsedSeconds());
            }
            case "timetaken_formatted" -> {
                return TimeFormat.pretty(session == null ? 0 : session.elapsedSeconds());
            }
            case "time" -> {
                return String.valueOf(koth.captureTime().toSeconds());
            }
            case "time_formatted" -> {
                return TimeFormat.pretty(koth.captureTime().toSeconds());
            }
            case "timeleft" -> {
                return String.valueOf(session == null ? 0 : session.secondsLeft());
            }
            case "timeleft_formatted" -> {
                return TimeFormat.pretty(session == null ? 0 : session.secondsLeft());
            }
            case "timeleft_clock" -> {
                return TimeFormat.clock(session == null ? 0 : session.secondsLeft());
            }
            case "progress" -> {
                return session == null ? "0" : String.valueOf(Math.round(session.progress() * 100.0));
            }
            case "target" -> {
                return String.valueOf(koth.targetScore());
            }
            case "score" -> {
                if (!(session instanceof ScoreSession score) || player == null) {
                    return "0";
                }
                return String.valueOf(score.scoreOf(player.getUniqueId()));
            }
            case "leader" -> {
                return session instanceof ScoreSession score ? score.topName() : capturerDisplay(koth, session);
            }
            default -> {
            }
        }
        if (field.startsWith("score_")) {
            return scorePlacement(session, field.substring(6));
        }
        return null;
    }

    private @Nullable String scorePlacement(@Nullable KothSession session, @NotNull String rest) {
        int split = rest.lastIndexOf('_');
        if (split <= 0) {
            return null;
        }
        String what = rest.substring(split + 1);
        int placement;
        try {
            placement = Integer.parseInt(rest.substring(0, split));
        } catch (NumberFormatException exception) {
            return null;
        }
        if (placement < 1 || !(session instanceof ScoreSession score)) {
            return what.equals("points") ? "0" : "";
        }
        List<Map.Entry<UUID, Integer>> ranking = score.ranking();
        if (ranking.size() < placement) {
            return what.equals("points") ? "0" : "";
        }
        Map.Entry<UUID, Integer> entry = ranking.get(placement - 1);
        return switch (what) {
            case "player", "name" -> score.nameOf(entry.getKey());
            case "points", "score" -> String.valueOf(entry.getValue());
            default -> null;
        };
    }

    private @Nullable String topPlaceholder(@NotNull String query) {
        String[] parts = query.split("_");
        if (parts.length < 3) {
            return null;
        }
        String what = parts[parts.length - 1];
        String subject = parts[parts.length - 2];
        if (!subject.equals("player") && !subject.equals("team")) {
            return null;
        }
        int placement;
        try {
            placement = Integer.parseInt(parts[parts.length - 3]);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (placement < 1) {
            return null;
        }
        String board = parts.length == 3
                ? ALL_TIME
                : String.join("_", java.util.Arrays.copyOfRange(parts, 0, parts.length - 3));

        if (subject.equals("team")) {
            List<Map.Entry<String, Integer>> teams = plugin.leaderboards().topTeams(board, placement);
            if (teams.size() < placement) {
                return what.equals("wins") ? "0" : none();
            }
            Map.Entry<String, Integer> entry = teams.get(placement - 1);
            return switch (what) {
                case "name" -> entry.getKey();
                case "wins" -> String.valueOf(entry.getValue());
                default -> null;
            };
        }

        List<WinRecord> records = plugin.leaderboards().top(board, placement);
        if (records.size() < placement) {
            return what.equals("wins") ? "0" : none();
        }
        WinRecord record = records.get(placement - 1);
        return switch (what) {
            case "name" -> record.playerName();
            case "wins" -> String.valueOf(record.wins());
            default -> null;
        };
    }

    private @NotNull String capturerDisplay(@NotNull Koth koth, @Nullable KothSession session) {
        if (session == null || session.controller() == null) {
            return "";
        }
        Player online = plugin.getServer().getPlayer(session.controller());
        if (online == null) {
            return session.controllerName();
        }
        return plugin.teams().displayNameOf(online, koth.teamMode());
    }

    private @NotNull String capturerTeam(@Nullable KothSession session) {
        if (session == null || session.controller() == null) {
            return "";
        }
        Player online = plugin.getServer().getPlayer(session.controller());
        return online == null ? "" : plugin.teams().teamOf(online);
    }

    private @NotNull String teamOf(@Nullable OfflinePlayer player) {
        if (player == null) {
            return "";
        }
        Player online = player.getPlayer();
        return online == null ? "" : plugin.teams().teamOf(online);
    }

    private static long maxTimeLeft(@NotNull Koth koth, @Nullable KothSession session) {
        if (session == null) {
            return koth.maxTime().toSeconds();
        }
        if (session instanceof CaptureSession) {
            return Math.max(0, koth.maxTime().toSeconds() - session.elapsedSeconds());
        }
        return session.secondsLeft();
    }

    private @NotNull String none() {
        return plugin.messages().raw("leaderboard.none-name");
    }

    private static @NotNull String seconds(long value) {
        return value < 0 ? "0" : String.valueOf(value);
    }

    private static @NotNull String formatted(long value) {
        return value < 0 ? "-" : TimeFormat.pretty(value);
    }
}
