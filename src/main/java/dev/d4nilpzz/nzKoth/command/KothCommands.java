package dev.d4nilpzz.nzKoth.command;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothMode;
import dev.d4nilpzz.nzKoth.koth.session.KothSession;
import dev.d4nilpzz.nzKoth.storage.WinRecord;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("nzkoth")
@CommandPermission("nzkoth.admin")
public final class KothCommands {

    private final NzKoth plugin;

    public KothCommands(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    @Subcommand("wand")
    public void wand(@NotNull BukkitCommandActor actor) {
        Player player = actor.requirePlayer();
        player.getInventory().addItem(plugin.wands().createWand());
        actor.reply(plugin.messages().get(player, "command.wand-given"));
    }

    @Subcommand("create")
    public void create(@NotNull BukkitCommandActor actor,
                       @NotNull String koth,
                       @NotNull KothMode mode) {
        Player player = actor.requirePlayer();
        if (plugin.koths().exists(koth)) {
            actor.reply(plugin.messages().get(player, "command.already-exists",
                    Placeholder.unparsed("koth", koth)));
            return;
        }
        Cuboid selection = plugin.wands().selectionOf(player);
        if (selection == null) {
            actor.reply(plugin.messages().get(player, "wand.none"));
            return;
        }
        Koth created = plugin.koths().create(koth, mode);
        created.region(selection);
        created.spawn(player.getLocation());
        plugin.koths().save(created);
        plugin.wands().clear(player.getUniqueId());
        actor.reply(plugin.messages().get(player, "command.created",
                Placeholder.unparsed("koth", created.name()),
                Placeholder.unparsed("mode", mode.name()),
                Placeholder.unparsed("blocks", String.valueOf(selection.volume()))));
    }

    @Subcommand("delete")
    public void delete(@NotNull BukkitCommandActor actor, @NotNull Koth koth) {
        plugin.sessions().stop(koth);
        plugin.koths().delete(koth.name());
        actor.reply(plugin.messages().get("command.deleted",
                Placeholder.unparsed("koth", koth.name())));
    }

    @Subcommand("start")
    public void start(@NotNull BukkitCommandActor actor, @NotNull Koth koth) {
        if (!koth.isConfigured()) {
            actor.reply(plugin.messages().get("command.not-configured",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        if (!plugin.sessions().start(koth)) {
            actor.reply(plugin.messages().get("command.already-running",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        actor.reply(plugin.messages().get("command.started",
                Placeholder.unparsed("koth", koth.name())));
    }

    @Subcommand("stop")
    public void stop(@NotNull BukkitCommandActor actor, @NotNull Koth koth) {
        if (!plugin.sessions().stop(koth)) {
            actor.reply(plugin.messages().get("command.not-running",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        actor.reply(plugin.messages().get("command.stopped",
                Placeholder.unparsed("koth", koth.name())));
    }

    @Subcommand("stopall")
    public void stopAll(@NotNull BukkitCommandActor actor) {
        int stopped = plugin.sessions().stopAll();
        actor.reply(plugin.messages().get("command.stopped-all",
                Placeholder.unparsed("amount", String.valueOf(stopped))));
    }

    @Subcommand("tp")
    public void teleport(@NotNull BukkitCommandActor actor, @NotNull Koth koth) {
        Player player = actor.requirePlayer();
        Location target = koth.spawn();
        if (target == null || target.getWorld() == null) {
            actor.reply(plugin.messages().get(player, "command.not-configured",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        player.teleportAsync(target);
        actor.reply(plugin.messages().get(player, "command.teleported",
                Placeholder.unparsed("koth", koth.name())));
    }

    @Subcommand("list")
    public void list(@NotNull BukkitCommandActor actor) {
        if (plugin.koths().all().isEmpty()) {
            actor.reply(plugin.messages().get("command.list-empty"));
            return;
        }
        actor.reply(plugin.messages().get("command.list-header",
                Placeholder.unparsed("amount", String.valueOf(plugin.koths().all().size()))));
        for (Koth koth : plugin.koths().all()) {
            KothSession session = plugin.sessions().session(koth.name());
            actor.reply(plugin.messages().get("command.list-entry",
                    Placeholder.unparsed("koth", koth.name()),
                    Placeholder.unparsed("mode", koth.mode().name()),
                    Placeholder.unparsed("state", session == null ? "stopped" : "running"),
                    Placeholder.unparsed("region", koth.isConfigured() ? "ok" : "missing")));
        }
    }

    @Subcommand("givestarter")
    public void giveStarter(@NotNull BukkitCommandActor actor,
                            @NotNull Koth koth,
                            @NotNull Player player,
                            @Optional @Default("1") int amount) {
        player.getInventory().addItem(plugin.starters().create(koth, Math.max(1, amount)));
        actor.reply(plugin.messages().get("command.starter-given",
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("amount", String.valueOf(Math.max(1, amount)))));
    }

    @Subcommand("reload")
    public void reload(@NotNull BukkitCommandActor actor) {
        plugin.reloadEverything();
        actor.reply(plugin.messages().get("command.reloaded"));
    }

    @Subcommand("resetwins")
    public void resetWins(@NotNull BukkitCommandActor actor, @NotNull OfflinePlayer player) {
        plugin.storage().resetWins(player.getUniqueId());
        plugin.leaderboards().reset(player.getUniqueId());
        actor.reply(plugin.messages().get("command.wins-reset",
                Placeholder.unparsed("player", player.getName() == null ? "?" : player.getName())));
    }

    @Subcommand("editor")
    public void editor(@NotNull BukkitCommandActor actor, @NotNull Koth koth) {
        Player player = actor.requirePlayer();
        plugin.editor().open(player, koth);
    }

    @Subcommand("schedulers")
    public void schedulers(@NotNull BukkitCommandActor actor) {
        plugin.schedulerEditor().open(actor.requirePlayer());
    }

    @Subcommand("top")
    public void top(@NotNull BukkitCommandActor actor, @Optional @Default("alltime") String board) {
        List<WinRecord> records = plugin.leaderboards().top(board, 10);
        if (records.isEmpty()) {
            actor.reply(plugin.messages().get("leaderboard.empty",
                    Placeholder.unparsed("board", board)));
            return;
        }
        actor.reply(plugin.messages().get("leaderboard.header",
                Placeholder.unparsed("board", board)));
        int position = 1;
        for (WinRecord record : records) {
            actor.reply(plugin.messages().get("leaderboard.entry",
                    Placeholder.unparsed("position", String.valueOf(position++)),
                    Placeholder.unparsed("player", record.playerName()),
                    Placeholder.unparsed("wins", String.valueOf(record.wins()))));
        }
    }
}
