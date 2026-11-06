package dev.d4nilpzz.nzKoth.koth;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KothManager {

    private static final String FLOW_PREFIX = "__nzkoth_flow_";
    private static final String NO_WORLD = "";

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
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Koth koth = read(file, yaml);
                koths.put(koth.lowerName(), koth);
                if (!yaml.isConfigurationSection("settings")) {
                    plugin.getComponentLogger().info("Converted koth '{}' to the new file format", koth.name());
                    save(koth);
                }
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
        Map<String, String> flows = new LinkedHashMap<>();

        yaml.set("name", koth.name());

        yaml.set("settings.display-name", koth.displayName());
        yaml.set("settings.mode", koth.mode().name());
        yaml.set("settings.capture-time", koth.captureTime().toSeconds());
        yaml.set("settings.max-time", koth.maxTime().toSeconds());
        yaml.set("settings.target-score", koth.targetScore());
        yaml.set("settings.reset-on-leave", koth.resetOnLeave());
        yaml.set("settings.team-mode", koth.teamMode());

        Cuboid region = koth.region();
        yaml.set("region.world", region == null ? NO_WORLD : region.worldName());
        flow(yaml, flows, "region.p1", region == null
                ? new double[]{0, 0, 0}
                : new double[]{region.minX(), region.minY(), region.minZ()});
        flow(yaml, flows, "region.p2", region == null
                ? new double[]{0, 0, 0}
                : new double[]{region.maxX(), region.maxY(), region.maxZ()});

        Location spawn = koth.spawn();
        boolean hasSpawn = spawn != null && spawn.getWorld() != null;
        yaml.set("spawn.world", hasSpawn ? spawn.getWorld().getName() : NO_WORLD);
        flow(yaml, flows, "spawn.location", hasSpawn
                ? new double[]{spawn.getX(), spawn.getY(), spawn.getZ()}
                : new double[]{0, 0, 0}, true);
        yaml.setComments("spawn.location", List.of(
                "Location for the teleport command",
                "Format    x      y      z"));

        yaml.set("display.bossbar", koth.bossBarEnabled());
        yaml.set("display.scoreboard", koth.scoreboardEnabled());
        yaml.set("display.hologram", koth.hologramEnabled());
        yaml.setComments("display.hologram", List.of(
                "Hologram is placed at the center of the koth at the highest position 1 block above"));
        yaml.set("display.broadcast", koth.broadcastEnabled());
        yaml.set("display.view-distance", koth.viewDistance());
        yaml.set("display.same-world-only", koth.sameWorldOnly());

        yaml.set("crown.enabled", koth.crownEnabled());
        yaml.set("crown.material", koth.crownMaterial());
        yaml.set("crown.custom-model-data", koth.crownCustomModelData());
        yaml.set("crown.height", koth.crownHeight());
        yaml.set("crown.spin-ticks", koth.crownSpinTicks());
        yaml.set("crown.glow", koth.crownGlow());
        yaml.set("crown.glint", koth.crownGlint());
        yaml.set("crown.outline", koth.crownOutline());
        yaml.set("crown.outline-color", koth.crownOutlineColor());

        yaml.set("rewards.commands", koth.rewards().commands());
        yaml.set("rewards.broadcast-commands", koth.rewards().broadcastCommands());
        yaml.createSection("rewards.items");
        List<ItemReward> items = koth.rewards().items();
        for (int index = 0; index < items.size(); index++) {
            ItemReward reward = items.get(index);
            String path = "rewards.items." + index;
            yaml.set(path + ".item", reward.item());
            yaml.set(path + ".chance", reward.chance());
            yaml.set(path + ".min-amount", reward.minAmount());
            yaml.set(path + ".max-amount", reward.maxAmount());
            if (reward.message() != null) {
                yaml.set(path + ".message", reward.message());
            }
        }

        try {
            Files.writeString(file.toPath(), format(yaml.saveToString(), flows), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save koth '{}'", koth.name(), exception);
        }
    }

    public void saveAll() {
        for (Koth koth : koths.values()) {
            save(koth);
        }
    }

    private static void flow(@NotNull YamlConfiguration yaml, @NotNull Map<String, String> flows,
                             @NotNull String path, double @NotNull [] values) {
        flow(yaml, flows, path, values, false);
    }

    private static void flow(@NotNull YamlConfiguration yaml, @NotNull Map<String, String> flows,
                             @NotNull String path, double @NotNull [] values, boolean padded) {
        String token = FLOW_PREFIX + flows.size();
        StringBuilder out = new StringBuilder(padded ? "[ " : "[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append('"').append(Math.round(values[i] * 100.0) / 100.0).append('"');
        }
        flows.put(token, out.append(padded ? " ]" : "]").toString());
        yaml.set(path, token);
    }

    private static @NotNull String format(@NotNull String raw, @NotNull Map<String, String> flows) {
        String text = raw;
        for (Map.Entry<String, String> entry : flows.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        StringBuilder out = new StringBuilder(text.length() + 64);
        boolean first = true;
        for (String line : text.split("\n", -1)) {
            boolean topLevel = !line.isEmpty() && Character.isLetter(line.charAt(0));
            if (topLevel && !first) {
                out.append('\n');
            }
            if (topLevel) {
                first = false;
            }
            out.append(line).append('\n');
        }
        return out.toString().stripTrailing() + "\n";
    }

    private @NotNull Koth read(@NotNull File file, @NotNull YamlConfiguration yaml) {
        String settings = yaml.isConfigurationSection("settings") ? "settings." : "";
        String name = yaml.getString("name", file.getName().replace(".yml", ""));
        KothMode mode = KothMode.parse(yaml.getString(settings + "mode", "CAPTURE"));
        Koth koth = new Koth(name, mode == null ? KothMode.CAPTURE : mode);
        koth.displayName(yaml.getString(settings + "display-name", name));

        koth.captureTime(Duration.ofSeconds(yaml.getLong(settings + "capture-time", 300)));
        koth.maxTime(Duration.ofSeconds(yaml.getLong(settings + "max-time", 900)));
        koth.targetScore(yaml.getInt(settings + "target-score", 300));
        koth.resetOnLeave(yaml.getBoolean(settings + "reset-on-leave", true));
        koth.teamMode(yaml.getBoolean(settings + "team-mode", false));

        ConfigurationSection region = yaml.getConfigurationSection("region");
        if (region != null) {
            String world = region.getString("world", NO_WORLD);
            double[] p1 = coordinates(region, region.contains("p1") ? "p1" : "min");
            double[] p2 = coordinates(region, region.contains("p2") ? "p2" : "max");
            if (!world.isBlank() && p1 != null && p2 != null) {
                koth.region(new Cuboid(world,
                        (int) Math.floor(p1[0]), (int) Math.floor(p1[1]), (int) Math.floor(p1[2]),
                        (int) Math.floor(p2[0]), (int) Math.floor(p2[1]), (int) Math.floor(p2[2])));
            }
        }
        ConfigurationSection spawn = yaml.getConfigurationSection("spawn");
        if (spawn != null) {
            String worldName = spawn.getString("world", NO_WORLD);
            World world = worldName.isBlank() ? null : plugin.getServer().getWorld(worldName);
            if (world != null) {
                double[] at = spawn.contains("location") ? coordinates(spawn, "location")
                        : new double[]{spawn.getDouble("x"), spawn.getDouble("y"), spawn.getDouble("z")};
                if (at != null) {
                    koth.spawn(new Location(world, at[0], at[1], at[2],
                            (float) spawn.getDouble("yaw"), (float) spawn.getDouble("pitch")));
                }
            }
        }

        koth.bossBarEnabled(yaml.getBoolean("display.bossbar", true));
        koth.scoreboardEnabled(yaml.getBoolean("display.scoreboard", true));
        koth.hologramEnabled(yaml.getBoolean("display.hologram", true));
        koth.broadcastEnabled(yaml.getBoolean("display.broadcast", true));
        koth.viewDistance(yaml.getInt("display.view-distance", -1));
        koth.sameWorldOnly(yaml.getBoolean("display.same-world-only", false));

        koth.crownEnabled(yaml.getBoolean("crown.enabled", true));
        koth.crownMaterial(yaml.getString("crown.material", "GOLDEN_HELMET"));
        koth.crownCustomModelData(yaml.getInt("crown.custom-model-data", 1002));
        double crownHeight = yaml.getDouble("crown.height", 2.2);
        if (crownHeight < 1.0) {
            plugin.getComponentLogger().warn(
                    "Koth '{}' had crown height {} measured from the old mount point; using 2.2 instead",
                    name, crownHeight);
            crownHeight = 2.2;
        }
        koth.crownHeight(crownHeight);
        koth.crownSpinTicks(yaml.getInt("crown.spin-ticks", 40));
        koth.crownGlow(yaml.getBoolean("crown.glow", true));
        koth.crownGlint(yaml.getBoolean("crown.glint", false));
        koth.crownOutline(yaml.getBoolean("crown.outline", true));
        koth.crownOutlineColor(yaml.getString("crown.outline-color", "gold"));

        koth.rewards().commands(yaml.getStringList("rewards.commands"));
        koth.rewards().broadcastCommands(yaml.getStringList("rewards.broadcast-commands"));
        koth.rewards().items(readItems(name, yaml.getConfigurationSection("rewards.items")));
        return koth;
    }

    private static double @Nullable [] coordinates(@NotNull ConfigurationSection section, @NotNull String key) {
        List<?> raw = section.getList(key);
        if (raw == null || raw.size() < 3) {
            return null;
        }
        double[] out = new double[3];
        try {
            for (int i = 0; i < 3; i++) {
                out[i] = Double.parseDouble(String.valueOf(raw.get(i)).trim());
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        return out;
    }

    private @NotNull List<ItemReward> readItems(@NotNull String name, @Nullable ConfigurationSection section) {
        List<ItemReward> out = new ArrayList<>();
        if (section == null) {
            return out;
        }
        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort(Comparator.comparingInt(key -> {
            try {
                return Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                return Integer.MAX_VALUE;
            }
        }));
        for (String key : keys) {
            ItemStack stack = section.getItemStack(key + ".item");
            if (stack == null || stack.getType().isAir()) {
                plugin.getComponentLogger().warn("Koth '{}' has an invalid item reward '{}', skipping it", name, key);
                continue;
            }
            out.add(new ItemReward(stack,
                    section.getDouble(key + ".chance", ItemReward.MAX_CHANCE),
                    section.getInt(key + ".min-amount", 1),
                    section.getInt(key + ".max-amount", 1),
                    section.getString(key + ".message")));
        }
        return out;
    }
}
