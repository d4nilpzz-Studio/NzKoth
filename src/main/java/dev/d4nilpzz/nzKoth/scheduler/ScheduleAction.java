package dev.d4nilpzz.nzKoth.scheduler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ScheduleAction {

    START,
    STOP;

    public static @Nullable ScheduleAction parse(@NotNull String raw) {
        for (ScheduleAction action : values()) {
            if (action.name().equalsIgnoreCase(raw)) {
                return action;
            }
        }
        return null;
    }
}
