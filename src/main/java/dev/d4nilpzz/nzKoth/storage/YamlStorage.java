package dev.d4nilpzz.nzKoth.storage;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.leaderboard.Leaderboard;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class YamlStorage implements Storage {

    private final NzKoth plugin;
    private final File file;
    private final File boardsFile;
    private YamlConfiguration yaml;

    public YamlStorage(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wins.yml");
        this.boardsFile = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    @Override
    public @NotNull String name() {
        return "YAML";
    }

    @Override
    public void initialize() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public boolean shared() {
        return false;
    }

    @Override
    public void addWin(@NotNull UUID playerId, @NotNull String playerName, @NotNull String kothName) {
        String base = "players." + playerId;
        String kothPath = base + ".koths." + kothName.toLowerCase(Locale.ROOT);
        yaml.set(base + ".name", playerName);
        yaml.set(base + ".wins", yaml.getInt(base + ".wins", 0) + 1);
        yaml.set(kothPath, yaml.getInt(kothPath, 0) + 1);
        flushLater();
    }

    @Override
    public @NotNull CompletableFuture<Integer> wins(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(yaml.getInt("players." + playerId + ".wins", 0));
    }

    @Override
    public @NotNull CompletableFuture<Void> resetWins(@NotNull UUID playerId) {
        yaml.set("players." + playerId, null);
        flushLater();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<List<WinRecord>> topWins(int limit) {
        List<WinRecord> records = new ArrayList<>();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return CompletableFuture.completedFuture(records);
        }
        for (String key : players.getKeys(false)) {
            try {
                records.add(new WinRecord(
                        UUID.fromString(key),
                        players.getString(key + ".name", key),
                        players.getInt(key + ".wins", 0)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        records.sort(Comparator.comparingInt(WinRecord::wins).reversed());
        return CompletableFuture.completedFuture(
                records.size() > limit ? new ArrayList<>(records.subList(0, limit)) : records);
    }

    @Override
    public @NotNull List<BoardSnapshot> loadBoards() {
        List<BoardSnapshot> snapshots = new ArrayList<>();
        YamlConfiguration boards = YamlConfiguration.loadConfiguration(boardsFile);
        ConfigurationSection root = boards.getConfigurationSection("boards");
        if (root == null) {
            return snapshots;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Instant start = Instant.ofEpochSecond(section.getLong("period-start", Instant.now().getEpochSecond()));
            List<WinRecord> entries = new ArrayList<>();
            ConfigurationSection entrySection = section.getConfigurationSection("entries");
            if (entrySection != null) {
                for (String key : entrySection.getKeys(false)) {
                    try {
                        entries.add(new WinRecord(UUID.fromString(key),
                                entrySection.getString(key + ".name", key),
                                entrySection.getInt(key + ".wins", 0)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            Map<String, Integer> teams = new HashMap<>();
            ConfigurationSection teamSection = section.getConfigurationSection("teams");
            if (teamSection != null) {
                for (String key : teamSection.getKeys(false)) {
                    teams.put(key, teamSection.getInt(key, 0));
                }
            }
            snapshots.add(new BoardSnapshot(id, start, entries, teams));
        }
        return snapshots;
    }

    @Override
    public void createBoard(@NotNull String boardId, @NotNull Instant periodStart) {
    }

    @Override
    public void recordBoardWin(@NotNull String boardId, @NotNull UUID playerId, @NotNull String playerName,
                               @NotNull String team) {
    }

    @Override
    public void rollBoard(@NotNull String boardId, @NotNull Instant previousStart, @NotNull Instant newStart) {
    }

    @Override
    public void resetBoardPlayer(@NotNull UUID playerId) {
    }

    @Override
    public synchronized void saveBoards(@NotNull Collection<Leaderboard> boards) {
        YamlConfiguration out = new YamlConfiguration();
        for (Leaderboard board : boards) {
            String base = "boards." + board.id();
            out.set(base + ".period-start", board.periodStart().getEpochSecond());
            for (Map.Entry<UUID, Integer> entry : board.raw().entrySet()) {
                String path = base + ".entries." + entry.getKey();
                out.set(path + ".name", board.rawNames().getOrDefault(entry.getKey(), ""));
                out.set(path + ".wins", entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : board.rawTeams().entrySet()) {
                out.set(base + ".teams." + entry.getKey(), entry.getValue());
            }
        }
        try {
            out.save(boardsFile);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save leaderboards.yml", exception);
        }
    }

    private void flushLater() {
        String snapshot = yaml.saveToString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(snapshot));
    }

    private void flush() {
        write(yaml.saveToString());
    }

    private synchronized void write(@NotNull String contents) {
        try {
            Files.writeString(file.toPath(), contents, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save wins.yml", exception);
        }
    }
}
