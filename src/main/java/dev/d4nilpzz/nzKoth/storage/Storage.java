package dev.d4nilpzz.nzKoth.storage;

import dev.d4nilpzz.nzKoth.leaderboard.Leaderboard;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    @NotNull String name();

    void initialize() throws Exception;

    void close();

    boolean shared();

    void addWin(@NotNull UUID playerId, @NotNull String playerName, @NotNull String kothName);

    @NotNull CompletableFuture<Integer> wins(@NotNull UUID playerId);

    @NotNull CompletableFuture<Void> resetWins(@NotNull UUID playerId);

    @NotNull CompletableFuture<List<WinRecord>> topWins(int limit);

    @NotNull List<BoardSnapshot> loadBoards();

    void createBoard(@NotNull String boardId, @NotNull Instant periodStart);

    void recordBoardWin(@NotNull String boardId, @NotNull UUID playerId, @NotNull String playerName,
                        @NotNull String team);

    void rollBoard(@NotNull String boardId, @NotNull Instant previousStart, @NotNull Instant newStart);

    void resetBoardPlayer(@NotNull UUID playerId);

    void saveBoards(@NotNull Collection<Leaderboard> boards);
}
