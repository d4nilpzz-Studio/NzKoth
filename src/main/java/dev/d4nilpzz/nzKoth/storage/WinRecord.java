package dev.d4nilpzz.nzKoth.storage;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record WinRecord(@NotNull UUID playerId, @NotNull String playerName, int wins) {
}
