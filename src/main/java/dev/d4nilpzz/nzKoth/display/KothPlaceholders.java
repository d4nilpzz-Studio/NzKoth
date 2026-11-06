package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothMode;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.koth.session.ScoreSession;
import dev.d4nilpzz.nzKoth.util.TimeFormat;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class KothPlaceholders {

    private KothPlaceholders() {
    }

    public static @NotNull String modeKey(@NotNull Koth koth) {
        return koth.mode() == KothMode.SCORE ? "score" : "capture";
    }

    public static @NotNull TagResolver idle(@NotNull NzKoth plugin, @NotNull Koth koth) {
        long next = plugin.schedulers().secondsUntilNext(koth.name());
        String nextText = next < 0
                ? plugin.getConfig().getString("hologram.not-scheduled", "Not scheduled")
                : TimeFormat.pretty(next);
        return TagResolver.resolver(
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.parsed("display-name", koth.displayName()),
                Placeholder.unparsed("mode", koth.mode().name()),
                Placeholder.unparsed("next", nextText),
                Placeholder.unparsed("capture-time", TimeFormat.clock(koth.captureTime().toSeconds())),
                Placeholder.unparsed("target", String.valueOf(koth.targetScore())));
    }

    public static @NotNull TagResolver active(@NotNull NzKoth plugin, @NotNull KothSession session,
                                              @Nullable Player viewer) {
        Koth koth = session.koth();
        String controller = session.controllerName();
        if (koth.teamMode() && !controller.isEmpty()) {
            Player holder = plugin.getServer().getPlayer(controller);
            if (holder != null) {
                controller = plugin.teams().displayNameOf(holder, true);
            }
        }
        String none = plugin.getConfig().getString("placeholders.none", "-");
        String leader = session instanceof ScoreSession score ? score.topName() : controller;
        int topScore = session instanceof ScoreSession score ? score.topScore() : 0;
        long percent = Math.round(Math.max(0, Math.min(1, session.progress())) * 100.0);

        List<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.unparsed("koth", koth.name()));
        resolvers.add(Placeholder.parsed("display-name", koth.displayName()));
        resolvers.add(Placeholder.unparsed("mode", koth.mode().name()));
        resolvers.add(Placeholder.unparsed("status", session.statusLine()));
        resolvers.add(Placeholder.unparsed("controller", controller.isEmpty() ? none : controller));
        resolvers.add(Placeholder.unparsed("leader", leader.isEmpty() ? none : leader));
        resolvers.add(Placeholder.unparsed("score", String.valueOf(topScore)));
        resolvers.add(Placeholder.unparsed("target", String.valueOf(koth.targetScore())));
        resolvers.add(Placeholder.unparsed("time", TimeFormat.clock(session.secondsLeft())));
        resolvers.add(Placeholder.unparsed("progress", String.valueOf(percent)));
        resolvers.add(Placeholder.parsed("bar", bar(plugin.getConfig().getConfigurationSection("progress-bar"),
                session.progress())));
        resolvers.add(Placeholder.unparsed("viewer", viewer == null ? "" : viewer.getName()));
        return TagResolver.resolver(resolvers);
    }

    private static @NotNull String bar(@Nullable ConfigurationSection section, double progress) {
        int length = section == null ? 20 : Math.max(1, section.getInt("length", 20));
        String symbol = section == null ? "|" : section.getString("symbol", "|");
        String filled = section == null ? "<green>" : section.getString("filled", "<green>");
        String empty = section == null ? "<dark_gray>" : section.getString("empty", "<dark_gray>");
        int done = (int) Math.round(Math.max(0, Math.min(1, progress)) * length);
        return filled + symbol.repeat(done) + empty + symbol.repeat(length - done);
    }
}
