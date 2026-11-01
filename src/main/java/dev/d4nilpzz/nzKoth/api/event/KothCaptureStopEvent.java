package dev.d4nilpzz.nzKoth.api.event;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class KothCaptureStopEvent extends KothEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public KothCaptureStopEvent(@NotNull Koth koth) {
        super(koth);
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
