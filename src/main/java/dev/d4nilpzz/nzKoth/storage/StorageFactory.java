package dev.d4nilpzz.nzKoth.storage;

import dev.d4nilpzz.nzKoth.NzKoth;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;

public final class StorageFactory {

    private StorageFactory() {
    }

    public static @NotNull Storage create(@NotNull NzKoth plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("storage");
        if (section == null) {
            section = new MemoryConfiguration();
        }
        String type = section.getString("type", "YAML");
        if (!type.equalsIgnoreCase("YAML")) {
            SqlDialect dialect = SqlDialect.parse(type);
            if (dialect == null) {
                plugin.getComponentLogger().error("Unknown storage type '{}', using YAML", type);
            } else {
                SqlStorage sql = null;
                try {
                    sql = new SqlStorage(plugin, dialect, section);
                    sql.initialize();
                    return sql;
                } catch (Exception exception) {
                    plugin.getComponentLogger().error(
                            "Could not connect to {}, falling back to YAML. Wins will be saved in local files until this is fixed",
                            dialect.displayName(), exception);
                    if (sql != null) {
                        sql.close();
                    }
                }
            }
        }
        YamlStorage yaml = new YamlStorage(plugin);
        yaml.initialize();
        return yaml;
    }
}
