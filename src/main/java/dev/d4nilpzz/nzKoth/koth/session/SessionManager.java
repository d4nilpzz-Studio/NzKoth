package dev.d4nilpzz.nzKoth.koth.session;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.api.event.KothCaptureStartEvent;
import dev.d4nilpzz.nzKoth.api.event.KothCaptureStopEvent;
import dev.d4nilpzz.nzKoth.api.event.KothEndEvent;
import dev.d4nilpzz.nzKoth.api.event.KothStartEvent;
import dev.d4nilpzz.nzKoth.display.AudienceFilter;
import dev.d4nilpzz.nzKoth.display.BossBarService;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothMode;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {

    private final NzKoth plugin;
    private final Map<String, KothSession> running = new LinkedHashMap<>();
    private BukkitTask task;

    public SessionManager(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void startTicking() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        running.clear();
        plugin.scoreboards().clearAll();
    }

    public boolean isRunning(@NotNull Koth koth) {
        return running.containsKey(koth.lowerName());
    }

    public @Nullable KothSession session(@NotNull String kothName) {
        return running.get(kothName.toLowerCase(Locale.ROOT));
    }

    public @NotNull Collection<KothSession> sessions() {
        return running.values();
    }

    public int runningCount() {
        return running.size();
    }

    public boolean start(@NotNull Koth koth) {
        if (!koth.isConfigured() || isRunning(koth)) {
            return false;
        }
        KothSession session = koth.mode() == KothMode.SCORE
                ? new ScoreSession(koth)
                : new CaptureSession(koth);
        running.put(koth.lowerName(), session);
        Bukkit.getPluginManager().callEvent(new KothStartEvent(koth));
        broadcast(koth, "start", AudienceFilter.viewersOf(koth),
                Placeholder.unparsed("koth", koth.name()));
        plugin.webhooks().send("start", koth, null);
        return true;
    }

    public boolean stop(@NotNull Koth koth) {
        KothSession session = running.remove(koth.lowerName());
        if (session == null) {
            return false;
        }
        cleanUpDisplays(koth);
        Bukkit.getPluginManager().callEvent(new KothEndEvent(koth, null));
        broadcast(koth, "stop", AudienceFilter.viewersOf(koth),
                Placeholder.unparsed("koth", koth.name()));
        plugin.webhooks().send("stop", koth, null);
        return true;
    }

    public int stopAll() {
        int stopped = 0;
        for (KothSession session : new ArrayList<>(running.values())) {
            if (stop(session.koth())) {
                stopped++;
            }
        }
        plugin.scoreboards().clearAll();
        return stopped;
    }

    private void tick() {
        if (running.isEmpty()) {
            return;
        }
        for (KothSession session : new ArrayList<>(running.values())) {
            Koth koth = session.koth();
            UUID previousController = session.controller();
            boolean wasContested = session.contested();

            session.tick(KothSession.playersInside(koth));

            List<Player> viewers = AudienceFilter.viewersOf(koth);
            announceTransitions(session, koth, previousController, wasContested, viewers);

            plugin.bossBars().update(session, viewers);

            if (session.finished()) {
                complete(session, viewers);
            }
        }
        plugin.scoreboards().updateAll(running.values());
    }

    private void announceTransitions(@NotNull KothSession session,
                                     @NotNull Koth koth,
                                     @Nullable UUID previousController,
                                     boolean wasContested,
                                     @NotNull List<Player> viewers) {
        UUID controller = session.controller();
        if (controller != null && !controller.equals(previousController)) {
            Player player = plugin.getServer().getPlayer(controller);
            if (player != null) {
                Bukkit.getPluginManager().callEvent(new KothCaptureStartEvent(koth, player));
                String shown = plugin.teams().displayNameOf(player, koth.teamMode());
                broadcast(koth, "capture-start", viewers,
                        Placeholder.unparsed("koth", koth.name()),
                        Placeholder.unparsed("player", shown));
                plugin.webhooks().send("capture-start", koth, shown);
            }
        } else if (controller == null && previousController != null) {
            Bukkit.getPluginManager().callEvent(new KothCaptureStopEvent(koth));
            broadcast(koth, "capture-stop", viewers, Placeholder.unparsed("koth", koth.name()));
            plugin.webhooks().send("capture-stop", koth, null);
        }
        if (session.contested() && !wasContested) {
            broadcast(koth, "contested", viewers, Placeholder.unparsed("koth", koth.name()));
        }
    }

    private void complete(@NotNull KothSession session, @NotNull List<Player> viewers) {
        Koth koth = session.koth();
        running.remove(koth.lowerName());
        cleanUpDisplays(koth);

        UUID winnerId = session.winner();
        OfflinePlayer winner = winnerId == null ? null : plugin.getServer().getOfflinePlayer(winnerId);
        Bukkit.getPluginManager().callEvent(new KothEndEvent(koth, winner));

        if (winnerId == null) {
            broadcast(koth, "end-no-winner", viewers, Placeholder.unparsed("koth", koth.name()));
            plugin.webhooks().send("end", koth, null);
            return;
        }

        Player online = plugin.getServer().getPlayer(winnerId);
        String shown = online != null
                ? plugin.teams().displayNameOf(online, koth.teamMode())
                : session.winnerName();

        broadcast(koth, "win", viewers,
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.unparsed("player", shown));
        plugin.webhooks().send("capture", koth, shown);

        if (online != null) {
            plugin.crowns().show(online, koth, plugin.settings().crownDurationTicks());
            runRewards(koth, online);
        }
        plugin.storage().addWin(winnerId, session.winnerName(), koth.name());
        String winnerTeam = online == null ? "" : plugin.teams().teamOf(online);
        plugin.leaderboards().record(winnerId, session.winnerName(), winnerTeam);
    }

    private void cleanUpDisplays(@NotNull Koth koth) {
        plugin.bossBars().remove(koth);
    }

    private void runRewards(@NotNull Koth koth, @NotNull Player winner) {
        for (String command : koth.rewards().commands()) {
            dispatch(command, winner.getName());
        }
        for (String command : koth.rewards().broadcastCommands()) {
            dispatch(command, winner.getName());
        }
    }

    private void dispatch(@NotNull String rawCommand, @NotNull String playerName) {
        String command = rawCommand.replace("%player%", playerName).replace("{player}", playerName);
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }

    private void broadcast(@NotNull Koth koth,
                           @NotNull String key,
                           @NotNull List<Player> viewers,
                           @NotNull TagResolver... resolvers) {
        if (!plugin.settings().broadcastsEnabled() || !koth.broadcastEnabled()) {
            return;
        }
        for (Player viewer : viewers) {
            viewer.sendMessage(plugin.messages().get(viewer, "broadcast." + key, resolvers));
        }
    }

    public @NotNull String formatTime(long seconds) {
        return BossBarService.formatTime(seconds);
    }
}
