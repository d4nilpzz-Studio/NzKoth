package dev.d4nilpzz.nzKoth.leaderboard;

import dev.d4nilpzz.nzKoth.storage.WinRecord;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Leaderboard {

    private final String id;
    private final Duration period;
    private final Map<UUID, Integer> wins = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private Instant periodStart;

    public Leaderboard(@NotNull String id, @NotNull Duration period, @NotNull Instant periodStart) {
        this.id = id;
        this.period = period;
        this.periodStart = periodStart;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Duration period() {
        return period;
    }

    public @NotNull Instant periodStart() {
        return periodStart;
    }

    public boolean isAllTime() {
        return period.isZero();
    }

    public @NotNull Instant periodEnd() {
        return isAllTime() ? Instant.MAX : periodStart.plus(period);
    }

    public boolean hasExpired(@NotNull Instant now) {
        return !isAllTime() && !now.isBefore(periodEnd());
    }

    public void roll(@NotNull Instant now) {
        wins.clear();
        names.clear();
        if (isAllTime()) {
            return;
        }
        long seconds = period.getSeconds();
        long elapsed = now.getEpochSecond() - periodStart.getEpochSecond();
        long skipped = Math.max(1, elapsed / seconds);
        periodStart = periodStart.plusSeconds(skipped * seconds);
    }

    public void record(@NotNull UUID playerId, @NotNull String playerName) {
        wins.merge(playerId, 1, Integer::sum);
        names.put(playerId, playerName);
    }

    public void reset(@NotNull UUID playerId) {
        wins.remove(playerId);
        names.remove(playerId);
    }

    public void clear() {
        wins.clear();
        names.clear();
    }

    public int winsOf(@NotNull UUID playerId) {
        return wins.getOrDefault(playerId, 0);
    }

    public @NotNull List<WinRecord> top(int limit) {
        List<WinRecord> records = new ArrayList<>(wins.size());
        for (Map.Entry<UUID, Integer> entry : wins.entrySet()) {
            records.add(new WinRecord(entry.getKey(),
                    names.getOrDefault(entry.getKey(), ""),
                    entry.getValue()));
        }
        records.sort(Comparator.comparingInt(WinRecord::wins).reversed()
                .thenComparing(WinRecord::playerName));
        return records.size() > limit ? new ArrayList<>(records.subList(0, limit)) : records;
    }

    public @NotNull Map<UUID, Integer> raw() {
        return wins;
    }

    public @NotNull Map<UUID, String> rawNames() {
        return names;
    }

    public void load(@NotNull UUID playerId, @NotNull String playerName, int amount) {
        wins.put(playerId, amount);
        names.put(playerId, playerName);
    }

    public void periodStart(@NotNull Instant value) {
        this.periodStart = value;
    }
}
