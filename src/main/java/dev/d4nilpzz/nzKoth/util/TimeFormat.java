package dev.d4nilpzz.nzKoth.util;

import org.jetbrains.annotations.NotNull;

public final class TimeFormat {

    private TimeFormat() {
    }

    public static @NotNull String clock(long seconds) {
        long safe = Math.max(0, seconds);
        long hours = safe / 3600;
        long minutes = (safe % 3600) / 60;
        long rest = safe % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, rest);
        }
        return String.format("%02d:%02d", minutes, rest);
    }

    public static @NotNull String pretty(long seconds) {
        long safe = Math.max(0, seconds);
        if (safe == 0) {
            return "0s";
        }
        long days = safe / 86400;
        long hours = (safe % 86400) / 3600;
        long minutes = (safe % 3600) / 60;
        long rest = safe % 60;

        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append("d ");
        }
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (minutes > 0) {
            out.append(minutes).append("m ");
        }
        if (rest > 0) {
            out.append(rest).append("s");
        }
        return out.toString().trim();
    }
}
