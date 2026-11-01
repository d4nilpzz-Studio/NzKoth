package dev.d4nilpzz.nzKoth.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Cuboid {

    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Cuboid(@NotNull String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static @NotNull Cuboid between(@NotNull Location a, @NotNull Location b) {
        World world = a.getWorld();
        if (world == null || !world.equals(b.getWorld())) {
            throw new IllegalArgumentException("Both corners must be in the same world");
        }
        return new Cuboid(world.getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    public @NotNull String worldName() {
        return worldName;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public boolean contains(@Nullable Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null || !world.getName().equals(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean contains(@NotNull Entity entity) {
        return contains(entity.getLocation());
    }

    public @NotNull Location center() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        return new Location(world,
                (minX + maxX) / 2.0 + 0.5,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0 + 0.5);
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    @Override
    public String toString() {
        return worldName + ':' + minX + ',' + minY + ',' + minZ + " -> " + maxX + ',' + maxY + ',' + maxZ;
    }
}
