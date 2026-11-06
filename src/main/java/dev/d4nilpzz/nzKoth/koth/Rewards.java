package dev.d4nilpzz.nzKoth.koth;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class Rewards {

    private final List<String> commands = new ArrayList<>();
    private final List<String> broadcastCommands = new ArrayList<>();
    private final List<ItemReward> items = new ArrayList<>();

    public @NotNull List<String> commands() {
        return commands;
    }

    public @NotNull List<String> broadcastCommands() {
        return broadcastCommands;
    }

    public @NotNull List<ItemReward> items() {
        return items;
    }

    public void items(@NotNull List<ItemReward> value) {
        items.clear();
        items.addAll(value);
    }

    public int size() {
        return commands.size() + broadcastCommands.size() + items.size();
    }

    public void commands(@NotNull List<String> value) {
        commands.clear();
        commands.addAll(value);
    }

    public void broadcastCommands(@NotNull List<String> value) {
        broadcastCommands.clear();
        broadcastCommands.addAll(value);
    }

    public boolean isEmpty() {
        return commands.isEmpty() && broadcastCommands.isEmpty() && items.isEmpty();
    }
}
