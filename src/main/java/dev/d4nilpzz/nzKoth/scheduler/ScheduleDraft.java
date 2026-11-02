package dev.d4nilpzz.nzKoth.scheduler;

import dev.d4nilpzz.nzKoth.util.CronExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ScheduleDraft {

    enum Frequency {
        DAILY,
        EVERY_HOURS,
        EVERY_MINUTES
    }

    private static final String[] DAY_NAMES = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

    final @Nullable String id;
    String kothName;
    ScheduleAction action = ScheduleAction.START;
    boolean enabled = true;
    Frequency frequency = Frequency.DAILY;
    int hour = 20;
    int minute = 0;
    int interval = 2;
    final boolean[] days = new boolean[7];
    @Nullable String customCron;

    private ScheduleDraft(@Nullable String id, @NotNull String kothName) {
        this.id = id;
        this.kothName = kothName;
        Arrays.fill(days, true);
    }

    static @NotNull ScheduleDraft fresh(@NotNull String kothName) {
        return new ScheduleDraft(null, kothName);
    }

    static @NotNull ScheduleDraft of(@NotNull Schedule schedule) {
        ScheduleDraft draft = new ScheduleDraft(schedule.id(), schedule.kothName());
        draft.action = schedule.action();
        draft.enabled = schedule.enabled();
        if (!draft.load(schedule.cron().raw())) {
            draft.customCron = schedule.cron().raw();
        }
        return draft;
    }

    private boolean load(@NotNull String raw) {
        String[] parts = raw.trim().split("\\s+");
        if (parts.length != 5 || !parts[2].equals("*") || !parts[3].equals("*")) {
            return false;
        }
        boolean[] parsedDays = parseDays(parts[4]);
        if (parsedDays == null) {
            return false;
        }
        Integer fixedMinute = number(parts[0], 0, 59);
        Integer fixedHour = number(parts[1], 0, 23);
        Integer minuteStep = step(parts[0], 59);
        Integer hourStep = step(parts[1], 23);

        if (fixedMinute != null && fixedHour != null) {
            frequency = Frequency.DAILY;
            minute = fixedMinute;
            hour = fixedHour;
        } else if (fixedMinute != null && (hourStep != null || parts[1].equals("*"))) {
            frequency = Frequency.EVERY_HOURS;
            minute = fixedMinute;
            interval = hourStep == null ? 1 : hourStep;
        } else if (minuteStep != null && parts[1].equals("*")) {
            frequency = Frequency.EVERY_MINUTES;
            interval = minuteStep;
        } else {
            return false;
        }
        System.arraycopy(parsedDays, 0, days, 0, 7);
        return true;
    }

    private static @Nullable boolean[] parseDays(@NotNull String spec) {
        boolean[] out = new boolean[7];
        if (spec.equals("*")) {
            Arrays.fill(out, true);
            return out;
        }
        for (String token : spec.split(",")) {
            int index = Arrays.asList(DAY_NAMES).indexOf(token.toLowerCase(Locale.ROOT));
            if (index < 0) {
                Integer value = number(token, 0, 7);
                if (value == null) {
                    return null;
                }
                index = value % 7;
            }
            out[index] = true;
        }
        return out;
    }

    private static @Nullable Integer number(@NotNull String text, int min, int max) {
        try {
            int value = Integer.parseInt(text);
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static @Nullable Integer step(@NotNull String text, int max) {
        if (!text.startsWith("*/")) {
            return null;
        }
        return number(text.substring(2), 1, max);
    }

    void touchTime() {
        customCron = null;
    }

    int maxInterval() {
        return frequency == Frequency.EVERY_HOURS ? 23 : 59;
    }

    boolean allDays() {
        for (boolean day : days) {
            if (!day) {
                return false;
            }
        }
        return true;
    }

    boolean noDays() {
        for (boolean day : days) {
            if (day) {
                return false;
            }
        }
        return true;
    }

    @NotNull String cron() {
        if (customCron != null) {
            return customCron;
        }
        String dayField = dayField();
        return switch (frequency) {
            case DAILY -> minute + " " + hour + " * * " + dayField;
            case EVERY_HOURS -> minute + " " + (interval == 1 ? "*" : "*/" + interval) + " * * " + dayField;
            case EVERY_MINUTES -> (interval == 1 ? "*" : "*/" + interval) + " * * * " + dayField;
        };
    }

    private @NotNull String dayField() {
        if (allDays() || noDays()) {
            return "*";
        }
        List<String> selected = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            if (days[index]) {
                selected.add(DAY_NAMES[index]);
            }
        }
        return String.join(",", selected);
    }

    @NotNull CronExpression expression() {
        return CronExpression.parse(cron());
    }
}
