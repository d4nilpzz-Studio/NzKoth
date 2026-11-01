package dev.d4nilpzz.nzKoth.text;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PlaceholderResolver {

    @NotNull String resolve(@Nullable OfflinePlayer viewer, @NotNull String placeholder);
}
