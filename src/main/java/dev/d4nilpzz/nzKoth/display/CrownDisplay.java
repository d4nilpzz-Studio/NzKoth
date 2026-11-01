package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CrownDisplay {

    private static final int STEP_TICKS = 4;
    private static final Vector3f SCALE = new Vector3f(0.7f, 0.7f, 0.7f);
    private static final Quaternionf NO_ROTATION = new Quaternionf();

    private final NzKoth plugin;
    private final Map<UUID, Crown> active = new HashMap<>();

    public CrownDisplay(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void show(@NotNull Player player, @NotNull Koth koth, long durationTicks) {
        if (!koth.crownEnabled()) {
            return;
        }
        Material material = Material.matchMaterial(koth.crownMaterial());
        if (material == null || material.isAir()) {
            plugin.getComponentLogger().warn("Unknown crown material '{}' on koth '{}'",
                    koth.crownMaterial(), koth.name());
            return;
        }
        hide(player.getUniqueId());

        ItemStack item = new ItemStack(material);
        if (koth.crownCustomModelData() > 0) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(koth.crownCustomModelData());
            item.setItemMeta(meta);
        }

        float height = (float) koth.crownHeight();
        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, spawned -> {
            spawned.setItemStack(item);
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            spawned.setBillboard(Display.Billboard.FIXED);
            spawned.setTransformation(new Transformation(
                    new Vector3f(0.0f, height, 0.0f), NO_ROTATION, SCALE, NO_ROTATION));
            spawned.setInterpolationDelay(0);
            spawned.setInterpolationDuration(STEP_TICKS);
            spawned.setPersistent(false);
            spawned.setVisibleByDefault(true);
        });

        if (!player.addPassenger(display)) {
            display.remove();
            return;
        }

        double anglePerStep = 2.0 * Math.PI * STEP_TICKS / Math.max(1, koth.crownSpinTicks());
        Crown crown = new Crown(display, height, anglePerStep);
        crown.task = plugin.getServer().getScheduler().runTaskTimer(
                plugin, () -> crown.spin(), STEP_TICKS, STEP_TICKS);
        active.put(player.getUniqueId(), crown);

        if (durationTicks > 0) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin, () -> hide(player.getUniqueId()), durationTicks);
        }
    }

    public void hide(@NotNull UUID playerId) {
        Crown crown = active.remove(playerId);
        if (crown != null) {
            crown.destroy();
        }
    }

    public void hideAll() {
        for (Crown crown : active.values()) {
            crown.destroy();
        }
        active.clear();
    }

    private static final class Crown {

        private final ItemDisplay display;
        private final float height;
        private final double anglePerStep;
        private double angle;
        private BukkitTask task;

        private Crown(ItemDisplay display, float height, double anglePerStep) {
            this.display = display;
            this.height = height;
            this.anglePerStep = anglePerStep;
        }

        private void spin() {
            if (!display.isValid()) {
                destroy();
                return;
            }
            angle = (angle + anglePerStep) % (2.0 * Math.PI);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(STEP_TICKS);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0f, height, 0.0f),
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
