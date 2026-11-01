package dev.d4nilpzz.nzKoth.scheduler;

import dev.d4nilpzz.nzKoth.util.CronExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public final class Schedule {

    private final String id;
    private String kothName;
    private CronExpression cron;
    private ScheduleAction action;
    private boolean enabled;

    public Schedule(@NotNull String id,
                    @NotNull String kothName,
                    @NotNull CronExpression cron,
                    @NotNull ScheduleAction action,
                    boolean enabled) {
        this.id = id;
        this.kothName = kothName;
        this.cron = cron;
        this.action = action;
        this.enabled = enabled;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String kothName() {
        return kothName;
    }

    public void kothName(@NotNull String value) {
        this.kothName = value;
    }

    public @NotNull CronExpression cron() {
        return cron;
    }

    public void cron(@NotNull CronExpression value) {
        this.cron = value;
    }

    public @NotNull ScheduleAction action() {
        return action;
    }

    public void action(@NotNull ScheduleAction value) {
        this.action = value;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public @Nullable LocalDateTime nextRun(@NotNull LocalDateTime from) {
        return enabled ? cron.nextAfter(from) : null;
    }
}
