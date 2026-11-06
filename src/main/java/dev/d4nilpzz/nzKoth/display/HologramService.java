package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.text.Text;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HologramService {

    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);
    private static final double MOVE_EPSILON = 0.01;

    private record Style(@NotNull Display.Billboard billboard,
                         @NotNull TextDisplay.TextAlignment alignment,
                         float scale,
                         boolean shadow,
                         boolean seeThrough,
                         @Nullable Color background,
                         int lineWidth,
                         float viewRange,
                         boolean fullBright,
                         double height) {
    }

    private final NzKoth plugin;
    private final Map<String, TextDisplay> displays = new HashMap<>();
    private BukkitTask task;
    private Style style;

    public HologramService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        style = readStyle(plugin.getConfig().getConfigurationSection("hologram"));
        long period = Math.max(1L, plugin.getConfig().getLong("hologram.update-ticks", 20L));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    public void removeAll() {
        for (TextDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
    }

    private void update() {
        Set<String> shown = new HashSet<>();
        if (plugin.getConfig().getBoolean("hologram.enabled", true)) {
            for (Koth koth : plugin.koths().all()) {
                if (render(koth)) {
                    shown.add(koth.lowerName());
                }
            }
        }
        displays.entrySet().removeIf(entry -> {
            if (shown.contains(entry.getKey())) {
                return false;
            }
            if (entry.getValue().isValid()) {
                entry.getValue().remove();
            }
            return true;
        });
    }

    private boolean render(@NotNull Koth koth) {
        if (!koth.hologramEnabled()) {
            return false;
        }
        Location anchor = anchor(koth);
        if (anchor == null) {
            return false;
        }
        KothSession session = plugin.sessions().session(koth.name());
        List<String> lines;
        TagResolver resolver;
        if (session != null) {
            lines = plugin.getConfig().getStringList("hologram.lines." + KothPlaceholders.modeKey(koth));
            resolver = KothPlaceholders.active(plugin, session, null);
        } else {
            if (!plugin.getConfig().getBoolean("hologram.idle.enabled", true)) {
                return false;
            }
            lines = plugin.getConfig().getStringList("hologram.idle.lines");
            resolver = KothPlaceholders.idle(plugin, koth);
        }
        if (lines.isEmpty()) {
            return false;
        }

        Component text = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                text = text.append(Component.newline());
            }
            text = text.append(Text.parse(null, lines.get(i), resolver));
        }

        TextDisplay display = displays.get(koth.lowerName());
        if (display != null && display.isValid() && !display.getWorld().equals(anchor.getWorld())) {
            display.remove();
            display = null;
        }
        if (display == null || !display.isValid()) {
            Component initial = text;
            display = anchor.getWorld().spawn(anchor, TextDisplay.class, spawned -> {
                spawned.setPersistent(false);
                applyStyle(spawned);
                spawned.text(initial);
            });
            displays.put(koth.lowerName(), display);
            return true;
        }
        if (display.getLocation().distanceSquared(anchor) > MOVE_EPSILON) {
            display.teleport(anchor);
        }
        applyStyle(display);
        display.text(text);
        return true;
    }

    private @Nullable Location anchor(@NotNull Koth koth) {
        Cuboid region = koth.region();
        if (region == null) {
            return null;
        }
        World world = plugin.getServer().getWorld(region.worldName());
        if (world == null) {
            return null;
        }
        Location center = region.center();
        int blockX = center.getBlockX();
        int blockZ = center.getBlockZ();
        if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return null;
        }
        int top = region.minY();
        for (int y = region.maxY(); y >= region.minY(); y--) {
            if (world.getBlockAt(blockX, y, blockZ).getType().isSolid()) {
                top = y + 1;
                break;
            }
        }
        return new Location(world, center.getX(), top + style.height(), center.getZ());
    }

    private void applyStyle(@NotNull TextDisplay display) {
        display.setBillboard(style.billboard());
        display.setAlignment(style.alignment());
        display.setShadowed(style.shadow());
        display.setSeeThrough(style.seeThrough());
        display.setLineWidth(style.lineWidth());
        display.setViewRange(style.viewRange());
        if (style.background() == null) {
            display.setDefaultBackground(true);
        } else {
            display.setDefaultBackground(false);
            display.setBackgroundColor(style.background());
        }
        display.setBrightness(style.fullBright() ? FULL_BRIGHT : null);
        Transformation current = display.getTransformation();
        Vector3f scale = new Vector3f(style.scale(), style.scale(), style.scale());
        if (!current.getScale().equals(scale)) {
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), scale, new Quaternionf()));
        }
    }

    private @NotNull Style readStyle(@Nullable ConfigurationSection section) {
        if (section == null) {
            return new Style(Display.Billboard.CENTER, TextDisplay.TextAlignment.CENTER, 1.0f, true, false,
                    Color.fromARGB(0x40, 0, 0, 0), 200, 1.0f, true, 1.0);
        }
        return new Style(
                parse(Display.Billboard.class, section.getString("billboard"), Display.Billboard.CENTER),
                parse(TextDisplay.TextAlignment.class, section.getString("alignment"), TextDisplay.TextAlignment.CENTER),
                (float) Math.max(0.1, section.getDouble("scale", 1.0)),
                section.getBoolean("shadow", true),
                section.getBoolean("see-through", false),
                color(section.getString("background", "#40000000")),
                Math.max(1, section.getInt("line-width", 200)),
                (float) Math.max(0.1, section.getDouble("view-range", 1.0)),
                section.getBoolean("full-bright", true),
                section.getDouble("height", 1.0));
    }

    private @Nullable Color color(@Nullable String raw) {
        if (raw == null || raw.equalsIgnoreCase("default")) {
            return null;
        }
        if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("transparent")) {
            return Color.fromARGB(0, 0, 0, 0);
        }
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            long value = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                value |= 0xFF000000L;
            }
            return Color.fromARGB((int) value);
        } catch (NumberFormatException exception) {
            plugin.getComponentLogger().warn("Invalid hologram background '{}', using the default one", raw);
            return null;
        }
    }

    private static <E extends Enum<E>> @NotNull E parse(@NotNull Class<E> type, @Nullable String raw, @NotNull E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
