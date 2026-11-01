package dev.d4nilpzz.nzKoth.koth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum KothMode {

    CAPTURE,
    SCORE;

    public static @Nullable KothMode parse(@NotNull String raw) {
        for (KothMode mode : values()) {
            if (mode.name().equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return null;
    }

    public @NotNull String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
