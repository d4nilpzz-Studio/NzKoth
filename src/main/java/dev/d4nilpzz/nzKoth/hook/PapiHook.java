package dev.d4nilpzz.nzKoth.hook;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.text.Text;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PapiHook {

    public static final String PLUGIN_NAME = "PlaceholderAPI";

    private PapiHook() {
    }

    public static boolean install(@NotNull NzKoth plugin) {
        if (plugin.getServer().getPluginManager().getPlugin(PLUGIN_NAME) == null) {
            return false;
        }
        new NzKothExpansion(plugin).register();
        Text.placeholderResolver(PapiHook::resolve);
        return true;
    }

    public static void uninstall() {
        Text.placeholderResolver(null);
    }

    private static @NotNull String resolve(@Nullable OfflinePlayer viewer, @NotNull String placeholder) {
        String value = PlaceholderAPI.setPlaceholders(viewer, '%' + placeholder + '%');
        return value == null ? "" : value;
    }
}
