package dev.d4nilpzz.nzKoth;

import dev.d4nilpzz.nzKoth.command.KothCommands;
import dev.d4nilpzz.nzKoth.command.KothParameterType;
import dev.d4nilpzz.nzKoth.config.Messages;
import dev.d4nilpzz.nzKoth.config.Settings;
import dev.d4nilpzz.nzKoth.display.BossBarService;
import dev.d4nilpzz.nzKoth.display.CrownDisplay;
import dev.d4nilpzz.nzKoth.display.HologramService;
import dev.d4nilpzz.nzKoth.display.ItemRewardEditor;
import dev.d4nilpzz.nzKoth.display.KothEditor;
import dev.d4nilpzz.nzKoth.display.RewardsEditor;
import dev.d4nilpzz.nzKoth.display.menu.ChatInput;
import dev.d4nilpzz.nzKoth.display.menu.MenuListener;
import dev.d4nilpzz.nzKoth.display.ScoreboardService;
import dev.d4nilpzz.nzKoth.hook.PapiHook;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothManager;
import dev.d4nilpzz.nzKoth.koth.session.SessionManager;
import dev.d4nilpzz.nzKoth.leaderboard.LeaderboardService;
import dev.d4nilpzz.nzKoth.scheduler.SchedulerEditor;
import dev.d4nilpzz.nzKoth.scheduler.SchedulerService;
import dev.d4nilpzz.nzKoth.storage.Storage;
import dev.d4nilpzz.nzKoth.storage.YamlStorage;
import dev.d4nilpzz.nzKoth.team.TeamResolver;
import dev.d4nilpzz.nzKoth.text.Text;
import dev.d4nilpzz.nzKoth.wand.StarterService;
import dev.d4nilpzz.nzKoth.wand.WandService;
import dev.d4nilpzz.nzKoth.webhook.WebhookService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public final class NzKoth extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private KothManager koths;
    private SessionManager sessions;
    private BossBarService bossBars;
    private ScoreboardService scoreboards;
    private CrownDisplay crowns;
    private KothEditor editor;
    private RewardsEditor rewardsEditor;
    private ItemRewardEditor itemRewardEditor;
    private ChatInput chatInput;
    private HologramService holograms;
    private SchedulerService schedulers;
    private SchedulerEditor schedulerEditor;
    private LeaderboardService leaderboards;
    private WebhookService webhooks;
    private TeamResolver teams;
    private WandService wands;
    private StarterService starters;
    private Storage storage;
    private Lamp<BukkitCommandActor> lamp;
    private boolean placeholderApi;

    @Override
    public void onEnable() {
        settings = new Settings(this);
        settings.load();

        messages = new Messages(this);
        messages.load();

        storage = new YamlStorage(this);
        storage.initialize();

        koths = new KothManager(this);
        koths.loadAll();

        teams = new TeamResolver(this);
        teams.load();

        webhooks = new WebhookService(this);
        webhooks.load();

        leaderboards = new LeaderboardService(this);
        leaderboards.load();
        leaderboards.start();

        bossBars = new BossBarService(this);
        scoreboards = new ScoreboardService(this);
        crowns = new CrownDisplay(this);
        editor = new KothEditor(this);
        chatInput = new ChatInput(this);
        rewardsEditor = new RewardsEditor(this);
        itemRewardEditor = new ItemRewardEditor(this);
        wands = new WandService(this);
        starters = new StarterService(this);

        sessions = new SessionManager(this);
        sessions.startTicking();

        schedulers = new SchedulerService(this);
        schedulers.load();
        schedulers.start();
        schedulerEditor = new SchedulerEditor(this);

        holograms = new HologramService(this);
        holograms.start();

        getServer().getPluginManager().registerEvents(wands, this);
        getServer().getPluginManager().registerEvents(starters, this);
        getServer().getPluginManager().registerEvents(editor, this);
        getServer().getPluginManager().registerEvents(chatInput, this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(schedulerEditor, this);
        getServer().getPluginManager().registerEvents(scoreboards, this);

        lamp = BukkitLamp.builder(this)
                .parameterTypes(builder -> builder.addParameterType(Koth.class, new KothParameterType(koths)))
                .build();
        lamp.register(new KothCommands(this));

        placeholderApi = PapiHook.install(this);
        getComponentLogger().info(Text.parse(
                "<green>NzKoth enabled<gray>: <white><koths><gray> koths, <white><schedules><gray> schedulers, PlaceholderAPI <white><papi_state>",
                Placeholder.unparsed("koths", String.valueOf(koths.all().size())),
                Placeholder.unparsed("schedules", String.valueOf(schedulers.size())),
                Placeholder.unparsed("papi_state", placeholderApi ? "hooked" : "absent")));
    }

    @Override
    public void onDisable() {
        if (schedulers != null) {
            schedulers.shutdown();
        }
        if (sessions != null) {
            sessions.shutdown();
        }
        if (crowns != null) {
            crowns.hideAll();
        }
        if (holograms != null) {
            holograms.stop();
        }
        if (bossBars != null) {
            bossBars.removeAll();
        }
        if (scoreboards != null) {
            scoreboards.clearAll();
        }
        if (leaderboards != null) {
            leaderboards.shutdown();
        }
        if (koths != null) {
            koths.saveAll();
        }
        if (storage != null) {
            storage.close();
        }
        if (placeholderApi) {
            PapiHook.uninstall();
        }
    }

    public void reloadEverything() {
        sessions.stopAll();
        settings.load();
        messages.load();
        koths.loadAll();
        teams.load();
        webhooks.load();
        leaderboards.load();
        schedulers.load();
        holograms.start();
    }

    public @NotNull Settings settings() {
        return settings;
    }

    public @NotNull Messages messages() {
        return messages;
    }

    public @NotNull KothManager koths() {
        return koths;
    }

    public @NotNull SessionManager sessions() {
        return sessions;
    }

    public @NotNull BossBarService bossBars() {
        return bossBars;
    }

    public @NotNull ScoreboardService scoreboards() {
        return scoreboards;
    }

    public @NotNull CrownDisplay crowns() {
        return crowns;
    }

    public @NotNull KothEditor editor() {
        return editor;
    }

    public @NotNull RewardsEditor rewardsEditor() {
        return rewardsEditor;
    }

    public @NotNull ItemRewardEditor itemRewardEditor() {
        return itemRewardEditor;
    }

    public @NotNull HologramService holograms() {
        return holograms;
    }

    public @NotNull ChatInput chatInput() {
        return chatInput;
    }

    public @NotNull SchedulerService schedulers() {
        return schedulers;
    }

    public @NotNull SchedulerEditor schedulerEditor() {
        return schedulerEditor;
    }

    public @NotNull LeaderboardService leaderboards() {
        return leaderboards;
    }

    public @NotNull WebhookService webhooks() {
        return webhooks;
    }

    public @NotNull TeamResolver teams() {
        return teams;
    }

    public @NotNull WandService wands() {
        return wands;
    }

    public @NotNull StarterService starters() {
        return starters;
    }

    public @NotNull Storage storage() {
        return storage;
    }

    public boolean hasPlaceholderApi() {
        return placeholderApi;
    }
}
