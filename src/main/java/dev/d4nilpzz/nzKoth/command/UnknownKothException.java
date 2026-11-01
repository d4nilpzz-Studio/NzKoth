package dev.d4nilpzz.nzKoth.command;

import org.jetbrains.annotations.NotNull;

public final class UnknownKothException extends RuntimeException {

    private final String name;

    public UnknownKothException(@NotNull String name) {
        super("Unknown koth: " + name, null, false, false);
        this.name = name;
    }

    public @NotNull String name() {
        return name;
    }
}
