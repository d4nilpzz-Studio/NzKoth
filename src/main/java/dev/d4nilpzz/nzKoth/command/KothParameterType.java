package dev.d4nilpzz.nzKoth.command;

import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothManager;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public final class KothParameterType implements ParameterType<BukkitCommandActor, Koth> {

    private final KothManager manager;

    public KothParameterType(@NotNull KothManager manager) {
        this.manager = manager;
    }

    @Override
    public Koth parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        String name = input.readString();
        Koth koth = manager.get(name);
        if (koth == null) {
            throw new UnknownKothException(name);
        }
        return koth;
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return context -> manager.names();
    }
}
