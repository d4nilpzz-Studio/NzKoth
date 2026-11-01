package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.koth.session.ScoreSession;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardService implements Listener {

    private static final String OBJECTIVE_ID = "nzkoth";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final NzKoth plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Scoreboard> previous = new HashMap<>();
    private final Map<UUID, List<String>> lastLines = new HashMap<>();

    public ScoreboardService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void updateAll(@NotNull Collection<KothSession> sessions) {
        if (!plugin.settings().scoreboardsEnabled()) {
            clearAll();
            return;
        }
        Map<UUID, KothSession> assigned = assign(sessions);
        for (Map.Entry<UUID, KothSession> entry : assigned.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                apply(player, entry.getValue());
            }
        }
        for (UUID id : new ArrayList<>(boards.keySet())) {
            if (!assigned.containsKey(id)) {
                restore(id);
            }
        }
    }

    private @NotNull Map<UUID, KothSession> assign(@NotNull Collection<KothSession> sessions) {
        Map<UUID, KothSession> assigned = new HashMap<>();
        for (KothSession session : sessions) {
            Koth koth = session.koth();
            if (!koth.scoreboardEnabled()) {
                continue;
            }
            Cuboid region = koth.region();
            for (Player viewer : AudienceFilter.viewersOf(koth)) {
                KothSession current = assigned.get(viewer.getUniqueId());
                if (current == null || beats(session, current, viewer, region)) {
                    assigned.put(viewer.getUniqueId(), session);
                }
            }
        }
        return assigned;
    }

    private boolean beats(@NotNull KothSession candidate,
                          @NotNull KothSession current,
                          @NotNull Player viewer,
                          Cuboid candidateRegion) {
        if (candidateRegion != null && candidateRegion.contains(viewer.getLocation())) {
            return true;
        }
        Cuboid currentRegion = current.koth().region();
        if (currentRegion != null && currentRegion.contains(viewer.getLocation())) {
            return false;
        }
        return candidate.startedAt() > current.startedAt();
    }

    public void clearAll() {
        for (UUID id : new ArrayList<>(boards.keySet())) {
            restore(id);
        }
        boards.clear();
        previous.clear();
        lastLines.clear();
    }

    private void apply(@NotNull Player player, @NotNull KothSession session) {
        List<String> lines = renderLines(player, session);
        if (lines.isEmpty()) {
            restore(player.getUniqueId());
            return;
        }

        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) {
            previous.put(player.getUniqueId(), player.getScoreboard());
            board = plugin.getServer().getScoreboardManager().getNewScoreboard();
            boards.put(player.getUniqueId(), board);
            player.setScoreboard(board);
        } else if (lines.equals(lastLines.get(player.getUniqueId()))) {
            return;
        }
        lastLines.put(player.getUniqueId(), lines);

        Objective objective = board.getObjective(OBJECTIVE_ID);
        Component title = plugin.messages().get(player, "scoreboard.title",
                Placeholder.unparsed("koth", session.koth().name()));
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_ID, Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(title);
        }
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
    }

    private @NotNull List<String> renderLines(@NotNull Player player, @NotNull KothSession session) {
        Koth koth = session.koth();
        String controller = session.controllerName();
        if (koth.teamMode() && !controller.isEmpty()) {
            Player holder = plugin.getServer().getPlayer(controller);
            if (holder != null) {
                controller = plugin.teams().displayNameOf(holder, true);
            }
        }
        String leader = session instanceof ScoreSession scoreSession ? scoreSession.topName() : controller;
        int topScore = session instanceof ScoreSession scoreSession ? scoreSession.topScore() : 0;

        List<Component> rendered = plugin.messages().list(player, "scoreboard.lines",
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.unparsed("mode", koth.mode().name()),
                Placeholder.unparsed("status", session.statusLine()),
                Placeholder.unparsed("controller", controller.isEmpty() ? "-" : controller),
                Placeholder.unparsed("leader", leader.isEmpty() ? "-" : leader),
                Placeholder.unparsed("score", String.valueOf(topScore)),
                Placeholder.unparsed("time", BossBarService.formatTime(session.secondsLeft())),
                Placeholder.unparsed("progress", String.valueOf(Math.round(session.progress() * 100.0))));

        List<String> lines = new ArrayList<>(rendered.size());
        StringBuilder unique = new StringBuilder();
        for (Component component : rendered) {
            String text = LEGACY.serialize(component);
            if (text.length() > 40) {
                text = text.substring(0, 40);
            }
            lines.add(text + unique);
            unique.append('§').append('r');
        }
        return lines;
    }

    private void restore(@NotNull UUID playerId) {
        boards.remove(playerId);
        lastLines.remove(playerId);
        Scoreboard before = previous.remove(playerId);
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.setScoreboard(before != null
                ? before
                : plugin.getServer().getScoreboardManager().getMainScoreboard());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
        previous.remove(event.getPlayer().getUniqueId());
        lastLines.remove(event.getPlayer().getUniqueId());
    }
}
