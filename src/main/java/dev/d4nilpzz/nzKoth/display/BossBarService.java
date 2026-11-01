package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.text.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BossBarService {

    private final NzKoth plugin;
    private final Map<String, BossBar> bars = new HashMap<>();
    private final Map<String, Set<UUID>> shownTo = new HashMap<>();

    public BossBarService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void update(@NotNull KothSession session, @NotNull List<Player> viewers) {
        Koth koth = session.koth();
        if (!plugin.settings().bossBarsEnabled() || !koth.bossBarEnabled()) {
            return;
        }
        BossBar bar = bars.computeIfAbsent(koth.lowerName(), key ->
                BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 1.0f,
                        BossBar.Color.RED, BossBar.Overlay.PROGRESS));

        bar.name(Text.parse(plugin.messages().raw("bossbar"),
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.unparsed("status", session.statusLine()),
                Placeholder.unparsed("time", formatTime(session.secondsLeft()))));
        bar.progress(clamp(session.progress()));

        Set<UUID> current = shownTo.computeIfAbsent(koth.lowerName(), key -> new HashSet<>());
        Set<UUID> next = new HashSet<>();
        for (Player viewer : viewers) {
            next.add(viewer.getUniqueId());
            if (current.add(viewer.getUniqueId())) {
                viewer.showBossBar(bar);
            }
        }
        current.removeIf(id -> {
            if (next.contains(id)) {
                return false;
            }
            Player gone = plugin.getServer().getPlayer(id);
            if (gone != null) {
                gone.hideBossBar(bar);
            }
            return true;
        });
    }

    public void remove(@NotNull Koth koth) {
        BossBar bar = bars.remove(koth.lowerName());
        Set<UUID> viewers = shownTo.remove(koth.lowerName());
        if (bar == null || viewers == null) {
            return;
        }
        for (UUID id : viewers) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }

    public void removeAll() {
        for (Map.Entry<String, BossBar> entry : bars.entrySet()) {
            Set<UUID> viewers = shownTo.get(entry.getKey());
            if (viewers == null) {
                continue;
            }
            for (UUID id : viewers) {
                Player player = plugin.getServer().getPlayer(id);
                if (player != null) {
                    player.hideBossBar(entry.getValue());
                }
            }
        }
        bars.clear();
        shownTo.clear();
    }

    private static float clamp(double value) {
        return (float) Math.max(0.0, Math.min(1.0, value));
    }

    public static @NotNull String formatTime(long seconds) {
        long minutes = seconds / 60;
        long rest = seconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }
}
