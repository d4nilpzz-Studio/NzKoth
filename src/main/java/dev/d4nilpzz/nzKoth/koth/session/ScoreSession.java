package dev.d4nilpzz.nzKoth.koth.session;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreSession extends KothSession {

    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final int target;
    private final long maxSeconds;

    public ScoreSession(@NotNull Koth koth) {
        super(koth);
        this.target = Math.max(1, koth.targetScore());
        this.maxSeconds = Math.max(1, koth.maxTime().toSeconds());
    }

    @Override
    public void tick(@NotNull List<Player> inside) {
        if (finished) {
            return;
        }
        contested = inside.size() > 1;
        if (inside.isEmpty() || contested) {
            clearController();
        } else {
            Player holder = inside.getFirst();
            setController(holder);
            names.put(holder.getUniqueId(), holder.getName());
            int points = scores.merge(holder.getUniqueId(), 1, Integer::sum);
            if (points >= target) {
                finish(holder);
                return;
            }
        }
        if (elapsedSeconds() >= maxSeconds) {
            finish(topPlayer());
        }
    }

    private @Nullable Player topPlayer() {
        UUID best = topId();
        return best == null ? null : Bukkit.getPlayer(best);
    }

    public @Nullable UUID topId() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public int topScore() {
        return scores.values().stream().max(Comparator.naturalOrder()).orElse(0);
    }

    public @NotNull String topName() {
        UUID best = topId();
        if (best == null) {
            return "";
        }
        return names.getOrDefault(best, "");
    }

    public int scoreOf(@NotNull UUID playerId) {
        return scores.getOrDefault(playerId, 0);
    }

    public @NotNull List<Map.Entry<UUID, Integer>> ranking() {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());
        return sorted;
    }

    public @NotNull String nameOf(@NotNull UUID playerId) {
        return names.getOrDefault(playerId, "");
    }

    @Override
    protected void finish(@Nullable Player winningPlayer) {
        super.finish(winningPlayer);
        if (winningPlayer == null) {
            UUID best = topId();
            if (best != null) {
                winner = best;
                winnerName = names.getOrDefault(best, "");
            }
        }
    }

    @Override
    public long secondsLeft() {
        return Math.max(0, maxSeconds - elapsedSeconds());
    }

    @Override
    public double progress() {
        return Math.min(1.0, (double) topScore() / (double) target);
    }

    @Override
    public @NotNull String statusLine() {
        if (contested) {
            return "Contested";
        }
        String leader = topName();
        return leader.isEmpty() ? "Uncontrolled" : leader + " (" + topScore() + ")";
    }

    public int target() {
        return target;
    }
}
