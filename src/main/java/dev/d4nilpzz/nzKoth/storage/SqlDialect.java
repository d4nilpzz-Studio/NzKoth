package dev.d4nilpzz.nzKoth.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SqlDialect {

    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver");

    private final String displayName;
    private final int defaultPort;
    private final String driverClass;

    SqlDialect(@NotNull String displayName, int defaultPort, @NotNull String driverClass) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.driverClass = driverClass;
    }

    public static @Nullable SqlDialect parse(@NotNull String raw) {
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "MYSQL", "MARIADB" -> MYSQL;
            case "POSTGRESQL", "POSTGRES", "PSQL" -> POSTGRESQL;
            default -> null;
        };
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public int defaultPort() {
        return defaultPort;
    }

    public @NotNull String driverClass() {
        return driverClass;
    }

    public @NotNull String jdbcUrl(@NotNull String host, int port, @NotNull String database) {
        return switch (this) {
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database;
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
        };
    }

    public @NotNull String increment(@NotNull String table, @NotNull List<String> keys, @Nullable String nameColumn) {
        String columns = String.join(", ", keys) + (nameColumn == null ? "" : ", " + nameColumn) + ", wins";
        String values = keys.stream().map(key -> "?").collect(Collectors.joining(", "))
                + (nameColumn == null ? "" : ", ?") + ", 1";
        String insert = "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")";
        return switch (this) {
            case MYSQL -> insert + " ON DUPLICATE KEY UPDATE wins = wins + 1"
                    + (nameColumn == null ? "" : ", " + nameColumn + " = VALUES(" + nameColumn + ")");
            case POSTGRESQL -> insert + " ON CONFLICT (" + String.join(", ", keys) + ") DO UPDATE SET wins = "
                    + table + ".wins + 1"
                    + (nameColumn == null ? "" : ", " + nameColumn + " = EXCLUDED." + nameColumn);
        };
    }

    public @NotNull String insertIgnore(@NotNull String table, @NotNull List<String> columns) {
        String values = columns.stream().map(column -> "?").collect(Collectors.joining(", "));
        String body = table + " (" + String.join(", ", columns) + ") VALUES (" + values + ")";
        return switch (this) {
            case MYSQL -> "INSERT IGNORE INTO " + body;
            case POSTGRESQL -> "INSERT INTO " + body + " ON CONFLICT DO NOTHING";
        };
    }
}
