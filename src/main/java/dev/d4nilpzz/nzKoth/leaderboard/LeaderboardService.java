package dev.d4nilpzz.nzKoth.leaderboard;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.storage.WinRecord;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LeaderboardService {

    private static final Pattern DURATION = Pattern.compile("^(\\d+)([mhdw])$");

    private final NzKoth plugin;
    private final File file;
    private final Map<String, Leaderboard> boards = new ConcurrentHashMap<>();
    private BukkitTask task;
    private volatile boolean dirty;

    public LeaderboardService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    public void load() {
        boards.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("boards");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                Duration period = periodOf(id);
                if (period == null) {
                    continue;
                }
                Leaderboard board = new Leaderboard(id, period,
                        Instant.ofEpochSecond(section.getLong("period-start", Instant.now().getEpochSecond())));
                ConfigurationSection teams = section.getConfigurationSection("teams");
                if (teams != null) {
                    for (String key : teams.getKeys(false)) {
                        board.loadTeam(key, teams.getInt(key, 0));
                    }
                }
                ConfigurationSection entries = section.getConfigurationSection("entries");
                if (entries != null) {
                    for (String key : entries.getKeys(false)) {
                        try {
                            board.load(UUID.fromString(key),
                                    entries.getString(key + ".name", key),
                                    entries.getInt(key + ".wins", 0));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                boards.put(id, board);
            }
        }
        for (String id : plugin.getConfig().getStringList("leaderboards.preload")) {
            board(id.toLowerCase(Locale.ROOT));
        }
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::tick, 200L, 200L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        save();
    }

    private void tick() {
        Instant now = Instant.now();
        for (Leaderboard board : boards.values()) {
            if (board.hasExpired(now)) {
                board.roll(now);
                dirty = true;
            }
        }
        if (dirty) {
            dirty = false;
            save();
        }
    }

    public void record(@NotNull UUID playerId, @NotNull String playerName, @NotNull String team) {
        for (Leaderboard board : boards.values()) {
            board.record(playerId, playerName);
            board.recordTeam(team);
        }
        dirty = true;
    }

    public @NotNull List<Map.Entry<String, Integer>> topTeams(@NotNull String boardId, int limit) {
        Leaderboard board = board(boardId);
        return board == null ? List.of() : board.topTeams(limit);
    }

    public int teamWins(@NotNull String boardId, @NotNull String team) {
        Leaderboard board = board(boardId);
        return board == null ? 0 : board.teamWinsOf(team);
    }

    public int playerWins(@NotNull String boardId, @NotNull UUID playerId) {
        Leaderboard board = board(boardId);
        return board == null ? 0 : board.winsOf(playerId);
    }

    public void reset(@NotNull UUID playerId) {
        for (Leaderboard board : boards.values()) {
            board.reset(playerId);
        }
        dirty = true;
    }

    public @Nullable Leaderboard board(@NotNull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Leaderboard existing = boards.get(key);
        if (existing != null) {
            return existing;
        }
        Duration period = periodOf(key);
        if (period == null) {
            return null;
        }
        Leaderboard created = new Leaderboard(key, period, alignedStart(period));
        boards.put(key, created);
        dirty = true;
        return created;
    }

    public @NotNull List<WinRecord> top(@NotNull String boardId, int limit) {
        Leaderboard board = board(boardId);
        return board == null ? List.of() : board.top(limit);
    }

    public @NotNull List<String> ids() {
        return new ArrayList<>(boards.keySet());
    }

    private static @NotNull Instant alignedStart(@NotNull Duration period) {
        Instant now = Instant.now();
        if (period.isZero()) {
            return now;
        }
        long seconds = period.getSeconds();
        return Instant.ofEpochSecond(now.getEpochSecond() / seconds * seconds);
    }

    public static @Nullable Duration periodOf(@NotNull String id) {
        switch (id) {
            case "alltime", "total", "global" -> {
                return Duration.ZERO;
            }
            case "hourly" -> {
                return Duration.ofHours(1);
            }
            case "daily" -> {
                return Duration.ofDays(1);
            }
            case "weekly" -> {
                return Duration.ofDays(7);
            }
            case "monthly" -> {
                return Duration.ofDays(30);
            }
            default -> {
            }
        }
        Matcher matcher = DURATION.matcher(id);
        if (!matcher.matches()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        if (amount <= 0) {
            return null;
        }
        return switch (matcher.group(2)) {
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(amount * 7);
            default -> null;
        };
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Leaderboard board : boards.values()) {
            String base = "boards." + board.id();
            yaml.set(base + ".period-start", board.periodStart().getEpochSecond());
            for (Map.Entry<UUID, Integer> entry : board.raw().entrySet()) {
                String path = base + ".entries." + entry.getKey();
                yaml.set(path + ".name", board.rawNames().getOrDefault(entry.getKey(), ""));
                yaml.set(path + ".wins", entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : board.rawTeams().entrySet()) {
                yaml.set(base + ".teams." + entry.getKey(), entry.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save leaderboards.yml", exception);
        }
    }
}
