package dev.d4nilpzz.nzKoth.config;

import dev.d4nilpzz.nzKoth.NzKoth;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class Settings {

    private final NzKoth plugin;

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

        crownDurationTicks = config.getLong("crown.duration-ticks", 200L);
        wandMaterial = config.getString("wand.material", "GOLDEN_AXE");
        wandCustomModelData = config.getInt("wand.custom-model-data", 0);

        bossBarsEnabled = config.getBoolean("modules.bossbars", true);
        scoreboardsEnabled = config.getBoolean("modules.scoreboards", true);
        broadcastsEnabled = config.getBoolean("modules.broadcasts", true);
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
