package dev.d4nilpzz.nzKoth.koth.session;

import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class KothSession {

    protected final Koth koth;
    protected final long startedAt = System.currentTimeMillis();
    protected UUID controller;
    protected String controllerName;
    protected boolean contested;
    protected boolean finished;
    protected UUID winner;
    protected String winnerName;

    protected KothSession(@NotNull Koth koth) {
        this.koth = koth;
    }

    public @NotNull Koth koth() {
        return koth;
    }

    public long startedAt() {
        return startedAt;
    }

    public long elapsedSeconds() {
        return (System.currentTimeMillis() - startedAt) / 1000L;
    }

    public @Nullable UUID controller() {
        return controller;
    }

    public @NotNull String controllerName() {
        return controllerName == null ? "" : controllerName;
    }

    public boolean contested() {
        return contested;
    }

    public boolean finished() {
        return finished;
    }

    public @Nullable UUID winner() {
        return winner;
    }

    public @NotNull String winnerName() {
        return winnerName == null ? "" : winnerName;
    }

    public abstract void tick(@NotNull List<Player> inside);

    public abstract long secondsLeft();

    public abstract double progress();

    public abstract @NotNull String statusLine();

    protected void finish(@Nullable Player winningPlayer) {
        finished = true;
        if (winningPlayer != null) {
            winner = winningPlayer.getUniqueId();
            winnerName = winningPlayer.getName();
        }
    }

    protected void clearController() {
        controller = null;
        controllerName = null;
    }

    protected void setController(@NotNull Player player) {
        controller = player.getUniqueId();
        controllerName = player.getName();
    }

    public static @NotNull List<Player> playersInside(@NotNull Koth koth) {
        List<Player> inside = new ArrayList<>();
        Cuboid region = koth.region();
        if (region == null) {
            return inside;
        }
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }
            if (region.contains(player.getLocation())) {
                inside.add(player);
            }
        }
        return inside;
    }
}
