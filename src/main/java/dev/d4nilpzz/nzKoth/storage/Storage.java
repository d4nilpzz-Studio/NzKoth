package dev.d4nilpzz.nzKoth.storage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    void initialize();

    void close();

    void addWin(@NotNull UUID playerId, @NotNull String playerName, @NotNull String kothName);

    @NotNull CompletableFuture<Integer> wins(@NotNull UUID playerId);

    @NotNull CompletableFuture<Void> resetWins(@NotNull UUID playerId);

    @NotNull CompletableFuture<List<WinRecord>> topWins(int limit);
}
