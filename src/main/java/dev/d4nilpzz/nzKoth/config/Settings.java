package dev.d4nilpzz.nzKoth.config;

import dev.d4nilpzz.nzKoth.NzKoth;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class Settings {

    private final NzKoth plugin;

    private String storageType = "YAML";
    private String jdbcUrl = "";
    private String username = "";
    private String password = "";
    private String tablePrefix = "nzkoth_";

    private long crownDurationTicks = 200L;
    private String wandMaterial = "GOLDEN_AXE";
    private int wandCustomModelData = 0;
    private boolean bossBarsEnabled = true;
    private boolean scoreboardsEnabled = true;
    private boolean broadcastsEnabled = true;

    public Settings(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        storageType = config.getString("storage.type", "YAML").toUpperCase(Locale.ROOT);
        jdbcUrl = config.getString("storage.jdbc-url", "");
        username = config.getString("storage.username", "");
        password = config.getString("storage.password", "");
        tablePrefix = config.getString("storage.table-prefix", "nzkoth_");

        crownDurationTicks = config.getLong("crown.duration-ticks", 200L);
        wandMaterial = config.getString("wand.material", "GOLDEN_AXE");
        wandCustomModelData = config.getInt("wand.custom-model-data", 0);

        bossBarsEnabled = config.getBoolean("modules.bossbars", true);
        scoreboardsEnabled = config.getBoolean("modules.scoreboards", true);
        broadcastsEnabled = config.getBoolean("modules.broadcasts", true);
    }

    public @NotNull String storageType() {
        return storageType;
    }

    public @NotNull String jdbcUrl() {
        return jdbcUrl;
    }

    public @NotNull String username() {
        return username;
    }

    public @NotNull String password() {
        return password;
    }

    public @NotNull String tablePrefix() {
        return tablePrefix;
    }

    public long crownDurationTicks() {
        return crownDurationTicks;
    }

    public @NotNull String wandMaterial() {
        return wandMaterial;
    }

    public int wandCustomModelData() {
        return wandCustomModelData;
    }

    public boolean bossBarsEnabled() {
        return bossBarsEnabled;
    }

    public boolean scoreboardsEnabled() {
        return scoreboardsEnabled;
    }

    public boolean broadcastsEnabled() {
        return broadcastsEnabled;
    }
}
