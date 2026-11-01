package dev.d4nilpzz.nzKoth.api.event;

import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class KothEvent extends Event {

    private final Koth koth;

    protected KothEvent(@NotNull Koth koth) {
        super(false);
        this.koth = koth;
    }

    public @NotNull Koth koth() {
        return koth;
    }
}
