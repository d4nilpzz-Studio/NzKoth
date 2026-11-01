package dev.d4nilpzz.nzKoth.koth;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KothManager {

    private final NzKoth plugin;
    private final File folder;
    private final Map<String, Koth> koths = new LinkedHashMap<>();

    public KothManager(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "koths");
    }

    public void loadAll() {
        koths.clear();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getComponentLogger().error("Could not create the koths folder");
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                Koth koth = read(file);
                koths.put(koth.lowerName(), koth);
            } catch (Exception exception) {
                plugin.getComponentLogger().error("Could not load koth from {}", file.getName(), exception);
            }
        }
    }

    public @Nullable Koth get(@NotNull String name) {
        return koths.get(name.toLowerCase(Locale.ROOT));
    }

    public @NotNull Collection<Koth> all() {
        return koths.values();
    }

    public @NotNull List<String> names() {
        List<String> names = new ArrayList<>(koths.size());
        for (Koth koth : koths.values()) {
            names.add(koth.name());
        }
        return names;
    }

    public boolean exists(@NotNull String name) {
        return koths.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public @NotNull Koth create(@NotNull String name, @NotNull KothMode mode) {
        Koth koth = new Koth(name, mode);
        koths.put(koth.lowerName(), koth);
        save(koth);
        return koth;
    }

    public boolean delete(@NotNull String name) {
        Koth removed = koths.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        File file = new File(folder, removed.lowerName() + ".yml");
        return !file.exists() || file.delete();
    }

    public void save(@NotNull Koth koth) {
        File file = new File(folder, koth.lowerName() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", koth.name());
        yaml.set("mode", koth.mode().name());
        yaml.set("capture-time", koth.captureTime().toSeconds());
        yaml.set("max-time", koth.maxTime().toSeconds());
        yaml.set("target-score", koth.targetScore());
        yaml.set("reset-on-leave", koth.resetOnLeave());
        yaml.set("team-mode", koth.teamMode());

        Cuboid region = koth.region();
        if (region != null) {
            yaml.set("region.world", region.worldName());
            yaml.set("region.min", List.of(region.minX(), region.minY(), region.minZ()));
            yaml.set("region.max", List.of(region.maxX(), region.maxY(), region.maxZ()));
        }
        Location spawn = koth.spawn();
        if (spawn != null && spawn.getWorld() != null) {
            yaml.set("spawn.world", spawn.getWorld().getName());
            yaml.set("spawn.x", spawn.getX());
            yaml.set("spawn.y", spawn.getY());
            yaml.set("spawn.z", spawn.getZ());
            yaml.set("spawn.yaw", spawn.getYaw());
            yaml.set("spawn.pitch", spawn.getPitch());
        }

        yaml.set("display.bossbar", koth.bossBarEnabled());
        yaml.set("display.scoreboard", koth.scoreboardEnabled());
        yaml.set("display.broadcast", koth.broadcastEnabled());
        yaml.set("display.view-distance", koth.viewDistance());
        yaml.set("display.same-world-only", koth.sameWorldOnly());

        yaml.set("crown.enabled", koth.crownEnabled());
        yaml.set("crown.material", koth.crownMaterial());
        yaml.set("crown.custom-model-data", koth.crownCustomModelData());
        yaml.set("crown.height", koth.crownHeight());
        yaml.set("crown.spin-ticks", koth.crownSpinTicks());

        yaml.set("rewards.commands", koth.rewards().commands());
        yaml.set("rewards.broadcast-commands", koth.rewards().broadcastCommands());

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save koth '{}'", koth.name(), exception);
        }
    }

    public void saveAll() {
        for (Koth koth : koths.values()) {
            save(koth);
        }
    }

    private @NotNull Koth read(@NotNull File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String name = yaml.getString("name", file.getName().replace(".yml", ""));
        KothMode mode = KothMode.parse(yaml.getString("mode", "CAPTURE"));
        Koth koth = new Koth(name, mode == null ? KothMode.CAPTURE : mode);

        koth.captureTime(Duration.ofSeconds(yaml.getLong("capture-time", 300)));
        koth.maxTime(Duration.ofSeconds(yaml.getLong("max-time", 900)));
        koth.targetScore(yaml.getInt("target-score", 300));
        koth.resetOnLeave(yaml.getBoolean("reset-on-leave", true));
        koth.teamMode(yaml.getBoolean("team-mode", false));

        ConfigurationSection region = yaml.getConfigurationSection("region");
        if (region != null) {
            List<Integer> min = region.getIntegerList("min");
            List<Integer> max = region.getIntegerList("max");
            String world = region.getString("world");
            if (world != null && min.size() == 3 && max.size() == 3) {
                koth.region(new Cuboid(world,
                        min.get(0), min.get(1), min.get(2),
                        max.get(0), max.get(1), max.get(2)));
            }
        }
        ConfigurationSection spawn = yaml.getConfigurationSection("spawn");
        if (spawn != null) {
            String world = spawn.getString("world");
            if (world != null && plugin.getServer().getWorld(world) != null) {
                koth.spawn(new Location(plugin.getServer().getWorld(world),
                        spawn.getDouble("x"), spawn.getDouble("y"), spawn.getDouble("z"),
                        (float) spawn.getDouble("yaw"), (float) spawn.getDouble("pitch")));
            }
        }

        koth.bossBarEnabled(yaml.getBoolean("display.bossbar", true));
        koth.scoreboardEnabled(yaml.getBoolean("display.scoreboard", true));
        koth.broadcastEnabled(yaml.getBoolean("display.broadcast", true));
        koth.viewDistance(yaml.getInt("display.view-distance", -1));
        koth.sameWorldOnly(yaml.getBoolean("display.same-world-only", false));

        koth.crownEnabled(yaml.getBoolean("crown.enabled", true));
        koth.crownMaterial(yaml.getString("crown.material", "GOLDEN_HELMET"));
        koth.crownCustomModelData(yaml.getInt("crown.custom-model-data", 0));
        koth.crownHeight(yaml.getDouble("crown.height", 0.45));
        koth.crownSpinTicks(yaml.getInt("crown.spin-ticks", 40));

        koth.rewards().commands(yaml.getStringList("rewards.commands"));
        koth.rewards().broadcastCommands(yaml.getStringList("rewards.broadcast-commands"));
        return koth;
    }
}
