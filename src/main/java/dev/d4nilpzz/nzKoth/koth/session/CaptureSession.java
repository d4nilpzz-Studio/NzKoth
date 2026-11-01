package dev.d4nilpzz.nzKoth.koth.session;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CaptureSession extends KothSession {

    private final long totalSeconds;
    private long remaining;

    public CaptureSession(@NotNull Koth koth) {
        super(koth);
        this.totalSeconds = Math.max(1, koth.captureTime().toSeconds());
        this.remaining = totalSeconds;
    }

    @Override
    public void tick(@NotNull List<Player> inside) {
        if (finished) {
            return;
        }
        if (inside.isEmpty()) {
            onNobodyInside();
            return;
        }
        Player holder = inside.getFirst();
        contested = inside.size() > 1;
        if (contested) {
            return;
        }
        if (controller == null || !controller.equals(holder.getUniqueId())) {
            setController(holder);
            remaining = totalSeconds;
        }
        remaining--;
        if (remaining <= 0) {
            remaining = 0;
            finish(holder);
        }
    }

    private void onNobodyInside() {
        contested = false;
        if (controller == null) {
            return;
        }
        if (koth.resetOnLeave()) {
            remaining = totalSeconds;
            clearController();
        }
    }

    @Override
    public long secondsLeft() {
        return remaining;
    }

    @Override
    public double progress() {
        return 1.0 - ((double) remaining / (double) totalSeconds);
    }

    @Override
    public @NotNull String statusLine() {
        if (contested) {
            return "Contested";
        }
        if (controller == null) {
            return "Uncontrolled";
        }
        return controllerName();
    }

    public long totalSeconds() {
        return totalSeconds;
    }
}
