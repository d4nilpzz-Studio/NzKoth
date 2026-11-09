package dev.d4nilpzz.nzKoth.storage;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BoardSnapshot(@NotNull String id, @NotNull Instant periodStart,
                            @NotNull List<WinRecord> entries, @NotNull Map<String, Integer> teams) {
}
