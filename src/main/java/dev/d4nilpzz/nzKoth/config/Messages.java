package dev.d4nilpzz.nzKoth.config;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Messages {

    private final NzKoth plugin;
    private YamlConfiguration yaml;

    public Messages(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public @NotNull String raw(@NotNull String key) {
        String value = yaml.getString(key);
        return value == null ? key : value;
    }

    public @NotNull Component get(@NotNull String key, @NotNull TagResolver... resolvers) {
        return Text.parse(null, raw(key), resolvers);
    }

    public @NotNull Component get(@Nullable OfflinePlayer viewer,
                                  @NotNull String key,
                                  @NotNull TagResolver... resolvers) {
        return Text.parse(viewer, raw(key), resolvers);
    }

    public @NotNull List<Component> list(@Nullable OfflinePlayer viewer,
                                         @NotNull String key,
                                         @NotNull TagResolver... resolvers) {
        List<String> lines = yaml.getStringList(key);
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(Text.parse(viewer, line, resolvers));
        }
        return out;
    }

    public @NotNull List<String> rawList(@NotNull String key) {
        return yaml.getStringList(key);
    }
}
