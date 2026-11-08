package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CrownDisplay {

    private static final int SPIN_STEP_TICKS = 4;
    private static final int TELEPORT_DURATION = 1;
    private static final double MOVE_EPSILON = 0.0001;
    private static final double MAX_LEAD = 1.2;
    private static final Vector3f SCALE = new Vector3f(0.7f, 0.7f, 0.7f);
    private static final Vector3f NO_TRANSLATION = new Vector3f();
    private static final Quaternionf NO_ROTATION = new Quaternionf();
    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);
    private static final String TEAM_PREFIX = "nzk_";

    private final NzKoth plugin;
    private final Map<UUID, Crown> active = new HashMap<>();

    public CrownDisplay(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public static @Nullable ItemStack crownItem(@NotNull Koth koth) {
        boolean packCrown = koth.usesDefaultCrown();
        Material material = packCrown ? Koth.PACK_CROWN_MATERIAL : Material.matchMaterial(koth.crownMaterial());
        if (material == null || material.isAir() || !material.isItem()) {
            return null;
        }
        int modelData = packCrown ? Koth.PACK_CROWN_MODEL_DATA : koth.crownCustomModelData();
        ItemStack item = new ItemStack(material);
        if (modelData > 0 || koth.crownGlint()) {
            ItemMeta meta = item.getItemMeta();
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
            if (koth.crownGlint()) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void show(@NotNull Player player, @NotNull Koth koth, long durationTicks) {
        if (!koth.crownEnabled()) {
            return;
        }
        ItemStack item = crownItem(koth);
        if (item == null) {
            plugin.getComponentLogger().warn("Unknown crown material '{}' on koth '{}'",
                    koth.crownMaterial(), koth.name());
            return;
        }
        hide(player.getUniqueId());

        double height = koth.crownHeight();
        Location spawnAt = anchorOf(player, height);
        ItemDisplay display = player.getWorld().spawn(spawnAt, ItemDisplay.class, spawned -> {
            spawned.setItemStack(item);
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            spawned.setBillboard(Display.Billboard.FIXED);
            spawned.setTransformation(new Transformation(
                    NO_TRANSLATION, NO_ROTATION, SCALE, NO_ROTATION));
            spawned.setInterpolationDelay(0);
            spawned.setInterpolationDuration(SPIN_STEP_TICKS);
            spawned.setTeleportDuration(TELEPORT_DURATION);
            spawned.setPersistent(false);
            spawned.setVisibleByDefault(true);
            if (koth.crownGlow()) {
                spawned.setBrightness(FULL_BRIGHT);
            }
            spawned.setGlowing(koth.crownOutline());
        });

        if (koth.crownOutline()) {
            outlineTeam(koth.crownOutlineColor()).addEntry(display.getUniqueId().toString());
        }

        double anglePerStep = 2.0 * Math.PI * SPIN_STEP_TICKS / Math.max(1, koth.crownSpinTicks());
        Crown crown = new Crown(display, player.getUniqueId(), height, anglePerStep);
        crown.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> follow(crown), 1L, 1L);
        active.put(player.getUniqueId(), crown);

        if (durationTicks > 0) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin, () -> hide(player.getUniqueId()), durationTicks);
        }
    }

    private void follow(@NotNull Crown crown) {
        Player owner = plugin.getServer().getPlayer(crown.playerId);
        if (owner == null || !owner.isOnline() || !crown.display.isValid()) {
            hide(crown.playerId);
            return;
        }
        Location anchor = anchorOf(owner, crown.height);
        Location previous = crown.lastSeen;
        crown.lastSeen = anchor.clone();

        if (previous != null && previous.getWorld().equals(anchor.getWorld())) {
            double dx = anchor.getX() - previous.getX();
            double dy = anchor.getY() - previous.getY();
            double dz = anchor.getZ() - previous.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > MOVE_EPSILON && distance <= MAX_LEAD) {
                anchor.add(dx, dy, dz);
            }
        }

        if (!anchor.getWorld().equals(crown.display.getWorld())
                || crown.display.getLocation().distanceSquared(anchor) > MOVE_EPSILON) {
            crown.display.teleport(anchor);
        }
        crown.tickSpin();
    }

    private @NotNull Team outlineTeam(@NotNull String colorName) {
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase(java.util.Locale.ROOT));
        if (color == null) {
            color = NamedTextColor.GOLD;
        }
        Scoreboard board = plugin.getServer().getScoreboardManager().getMainScoreboard();
        String name = TEAM_PREFIX + color.toString().replace("_", "");
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
        }
        team.color(color);
        return team;
    }

    private void releaseOutline(@NotNull String entry) {
        Scoreboard board = plugin.getServer().getScoreboardManager().getMainScoreboard();
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && team.hasEntry(entry)) {
                team.removeEntry(entry);
            }
        }
    }

    private static @NotNull Location anchorOf(@NotNull Player player, double height) {
        Location anchor = player.getLocation().clone().add(0.0, height, 0.0);
        anchor.setYaw(0.0f);
        anchor.setPitch(0.0f);
        return anchor;
    }

    public void hide(@NotNull UUID playerId) {
        Crown crown = active.remove(playerId);
        if (crown != null) {
            releaseOutline(crown.display.getUniqueId().toString());
            crown.destroy();
        }
    }

    public void hideAll() {
        for (Crown crown : active.values()) {
            releaseOutline(crown.display.getUniqueId().toString());
            crown.destroy();
        }
        active.clear();
    }

    private static final class Crown {

        private final ItemDisplay display;
        private final UUID playerId;
        private final double height;
        private final double anglePerStep;
        private int sinceSpin;
        private double angle;
        private Location lastSeen;
        private BukkitTask task;

        private Crown(ItemDisplay display, UUID playerId, double height, double anglePerStep) {
            this.display = display;
            this.playerId = playerId;
            this.height = height;
            this.anglePerStep = anglePerStep;
        }

        private void tickSpin() {
            if (++sinceSpin < SPIN_STEP_TICKS) {
                return;
            }
            sinceSpin = 0;
            angle = (angle + anglePerStep) % (2.0 * Math.PI);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(SPIN_STEP_TICKS);
            display.setTransformation(new Transformation(
                    NO_TRANSLATION,
                    new Quaternionf().rotateY((float) angle),
                    SCALE,
                    NO_ROTATION));
        }

        private void destroy() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (display.isValid()) {
                display.remove();
            }
        }
    }
}
