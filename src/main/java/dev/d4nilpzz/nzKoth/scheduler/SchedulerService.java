package dev.d4nilpzz.nzKoth.scheduler;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.util.CronExpression;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class SchedulerService {

    private final NzKoth plugin;
    private final File file;
    private final Map<String, Schedule> schedules = new LinkedHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private LocalDateTime lastEvaluated;
    private BukkitTask task;

    public SchedulerService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "schedulers.yml");
    }

    public void load() {
        schedules.clear();
        if (!file.exists()) {
            plugin.saveResource("schedulers.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("schedulers");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String expression = section.getString("cron", "");
            if (!CronExpression.isValid(expression)) {
                plugin.getComponentLogger().warn(
                        "Scheduler '{}' has an invalid cron expression '{}' and was skipped", id, expression);
                continue;
            }
            ScheduleAction action = ScheduleAction.parse(section.getString("action", "START"));
            schedules.put(id, new Schedule(id,
                    section.getString("koth", ""),
                    CronExpression.parse(expression),
                    action == null ? ScheduleAction.START : action,
                    section.getBoolean("enabled", true)));
            try {
                nextId.updateAndGet(current -> Math.max(current, Integer.parseInt(id) + 1));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Schedule schedule : schedules.values()) {
            String base = "schedulers." + schedule.id();
            yaml.set(base + ".koth", schedule.kothName());
            yaml.set(base + ".cron", schedule.cron().raw());
            yaml.set(base + ".action", schedule.action().name());
            yaml.set(base + ".enabled", schedule.enabled());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getComponentLogger().error("Could not save schedulers.yml", exception);
        }
    }

    public void start() {
        if (task != null) {
            return;
        }
        lastEvaluated = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (now.equals(lastEvaluated)) {
            return;
        }
        lastEvaluated = now;
        for (Schedule schedule : new ArrayList<>(schedules.values())) {
            if (!schedule.enabled() || !schedule.cron().matches(now)) {
                continue;
            }
            run(schedule);
        }
    }

    private void run(@NotNull Schedule schedule) {
        Koth koth = plugin.koths().get(schedule.kothName());
        if (koth == null) {
            plugin.getComponentLogger().warn("Scheduler '{}' points at unknown koth '{}'",
                    schedule.id(), schedule.kothName());
            return;
        }
        boolean applied = schedule.action() == ScheduleAction.START
                ? plugin.sessions().start(koth)
                : plugin.sessions().stop(koth);
        plugin.getComponentLogger().info("Scheduler #{} fired {} on '{}' ({})",
                schedule.id(), schedule.action(), koth.name(),
                applied ? "applied" : "no change");
    }

    public @NotNull Schedule add(@NotNull String kothName,
                                 @NotNull CronExpression cron,
                                 @NotNull ScheduleAction action) {
        String id = String.valueOf(nextId.getAndIncrement());
        Schedule schedule = new Schedule(id, kothName, cron, action, true);
        schedules.put(id, schedule);
        save();
        return schedule;
    }

    public boolean remove(@NotNull String id) {
        if (schedules.remove(id) == null) {
            return false;
        }
        save();
        return true;
    }

    public @Nullable Schedule get(@NotNull String id) {
        return schedules.get(id);
    }

    public @NotNull List<Schedule> all() {
        return new ArrayList<>(schedules.values());
    }

    public int size() {
        return schedules.size();
    }
}
