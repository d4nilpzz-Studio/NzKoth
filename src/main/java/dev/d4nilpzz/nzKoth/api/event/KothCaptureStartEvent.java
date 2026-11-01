package dev.d4nilpzz.nzKoth.api.event;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class KothCaptureStartEvent extends KothEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public KothCaptureStartEvent(@NotNull Koth koth, @NotNull Player player) {
        super(koth);
        this.player = player;
    }

    public @NotNull Player player() {
        return player;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
