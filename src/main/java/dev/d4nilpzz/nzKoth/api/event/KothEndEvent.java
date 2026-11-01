package dev.d4nilpzz.nzKoth.api.event;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KothEndEvent extends KothEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final OfflinePlayer winner;

    public KothEndEvent(@NotNull Koth koth, @Nullable OfflinePlayer winner) {
        super(koth);
        this.winner = winner;
    }

    public @Nullable OfflinePlayer winner() {
        return winner;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
