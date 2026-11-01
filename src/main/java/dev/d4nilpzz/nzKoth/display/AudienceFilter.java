package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AudienceFilter {

    private AudienceFilter() {
    }

    public static @NotNull List<Player> viewersOf(@NotNull Koth koth) {
        Cuboid region = koth.region();
        List<Player> viewers = new ArrayList<>();
        if (region == null) {
            viewers.addAll(Bukkit.getOnlinePlayers());
            return viewers;
        }
        Location center = region.center();
        int distance = koth.viewDistance();
        double squared = distance > 0 ? (double) distance * distance : -1.0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSee(player, koth, center, squared)) {
                continue;
            }
            viewers.add(player);
        }
        return viewers;
    }

    public static boolean canSee(@NotNull Player player,
                                 @NotNull Koth koth,
                                 @NotNull Location center,
                                 double squaredDistance) {
        boolean sameWorld = player.getWorld().equals(center.getWorld());
        if (koth.sameWorldOnly() && !sameWorld) {
            return false;
        }
        if (squaredDistance < 0) {
            return true;
        }
        if (!sameWorld) {
            return false;
        }
        return player.getLocation().distanceSquared(center) <= squaredDistance;
    }
}
