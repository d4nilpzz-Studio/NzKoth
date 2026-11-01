package dev.d4nilpzz.nzKoth.storage;

import dev.d4nilpzz.nzKoth.NzKoth;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class YamlStorage implements Storage {

    private final NzKoth plugin;
    private final File file;
    private YamlConfiguration yaml;

    public YamlStorage(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wins.yml");
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
    public void addWin(@NotNull UUID playerId, @NotNull String playerName, @NotNull String kothName) {
        String base = "players." + playerId;
        yaml.set(base + ".name", playerName);
        yaml.set(base + ".wins", yaml.getInt(base + ".wins", 0) + 1);
        yaml.set(base + ".koths." + kothName.toLowerCase(java.util.Locale.ROOT),
                yaml.getInt(base + ".koths." + kothName.toLowerCase(java.util.Locale.ROOT), 0) + 1);
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
