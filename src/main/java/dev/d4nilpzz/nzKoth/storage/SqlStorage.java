package dev.d4nilpzz.nzKoth.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.leaderboard.Leaderboard;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SqlStorage implements Storage {

    private final NzKoth plugin;
    private final SqlDialect dialect;
    private final ConfigurationSection config;
    private final ExecutorService executor;
    private final String wins;
    private final String kothWins;
    private final String boards;
    private final String boardEntries;
    private final String boardTeams;
    private HikariDataSource dataSource;

    public SqlStorage(@NotNull NzKoth plugin, @NotNull SqlDialect dialect, @NotNull ConfigurationSection config) {
        this.plugin = plugin;
        this.dialect = dialect;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "NzKoth-Storage");
            thread.setDaemon(true);
            return thread;
        });
        String prefix = config.getString("table-prefix", "nzkoth_");
        if (!prefix.matches("[A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("storage.table-prefix can only contain letters, numbers and _");
        }
        this.wins = prefix + "wins";
        this.kothWins = prefix + "koth_wins";
        this.boards = prefix + "boards";
        this.boardEntries = prefix + "board_entries";
        this.boardTeams = prefix + "board_teams";
    }

    @Override
    public @NotNull String name() {
        return dialect.displayName();
    }

    @Override
    public void initialize() throws SQLException {
        int port = config.getInt("port", 0);
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("NzKoth");
        hikari.setDriverClassName(dialect.driverClass());
        hikari.setJdbcUrl(dialect.jdbcUrl(
                config.getString("host", "localhost"),
                port > 0 ? port : dialect.defaultPort(),
                config.getString("database", "nzkoth")));
        hikari.setUsername(config.getString("username", ""));
        hikari.setPassword(config.getString("password", ""));
        hikari.setMaximumPoolSize(Math.max(1, config.getInt("pool-size", 4)));
        hikari.setConnectionTimeout(10_000L);
        ConfigurationSection properties = config.getConfigurationSection("properties");
        if (properties != null) {
            for (String key : properties.getKeys(false)) {
                hikari.addDataSourceProperty(key, String.valueOf(properties.get(key)));
            }
        }
        dataSource = new HikariDataSource(hikari);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + wins + " ("
                    + "player_uuid CHAR(36) NOT NULL PRIMARY KEY, "
                    + "player_name VARCHAR(64) NOT NULL, "
                    + "wins INT NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + kothWins + " ("
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "koth VARCHAR(64) NOT NULL, "
                    + "wins INT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (player_uuid, koth))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + boards + " ("
                    + "id VARCHAR(64) NOT NULL PRIMARY KEY, "
                    + "period_start BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS " + boardEntries + " ("
                    + "board VARCHAR(64) NOT NULL, "
                    + "player_uuid CHAR(36) NOT NULL, "
                    + "player_name VARCHAR(64) NOT NULL, "
                    + "wins INT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (board, player_uuid))");
            statement.execute("CREATE TABLE IF NOT EXISTS " + boardTeams + " ("
                    + "board VARCHAR(64) NOT NULL, "
                    + "team VARCHAR(128) NOT NULL, "
                    + "wins INT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (board, team))");
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getComponentLogger().warn("Some database writes did not finish before shutdown");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public boolean shared() {
        return true;
    }

    @Override
    public void addWin(@NotNull UUID playerId, @NotNull String playerName, @NotNull String kothName) {
        run(connection -> {
            try (PreparedStatement total = connection.prepareStatement(
                         dialect.increment(wins, List.of("player_uuid"), "player_name"));
                 PreparedStatement perKoth = connection.prepareStatement(
                         dialect.increment(kothWins, List.of("player_uuid", "koth"), null))) {
                total.setString(1, playerId.toString());
                total.setString(2, playerName);
                total.executeUpdate();
                perKoth.setString(1, playerId.toString());
                perKoth.setString(2, kothName.toLowerCase(Locale.ROOT));
                perKoth.executeUpdate();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Integer> wins(@NotNull UUID playerId) {
        return query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT wins FROM " + wins + " WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> resetWins(@NotNull UUID playerId) {
        return query(connection -> {
            for (String table : List.of(wins, kothWins)) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE player_uuid = ?")) {
                    statement.setString(1, playerId.toString());
                    statement.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<List<WinRecord>> topWins(int limit) {
        return query(connection -> {
            List<WinRecord> records = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_uuid, player_name, wins FROM " + wins + " ORDER BY wins DESC LIMIT ?")) {
                statement.setInt(1, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        records.add(new WinRecord(UUID.fromString(result.getString(1)),
                                result.getString(2), result.getInt(3)));
                    }
                }
            }
            return records;
        });
    }

    @Override
    public @NotNull List<BoardSnapshot> loadBoards() {
        try {
            return query(this::readBoards).join();
        } catch (CompletionException exception) {
            return List.of();
        }
    }

    private @NotNull List<BoardSnapshot> readBoards(@NotNull Connection connection) throws SQLException {
        Map<String, Instant> starts = new LinkedHashMap<>();
        Map<String, List<WinRecord>> entries = new HashMap<>();
        Map<String, Map<String, Integer>> teams = new HashMap<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT id, period_start FROM " + boards)) {
                while (result.next()) {
                    starts.put(result.getString(1), Instant.ofEpochSecond(result.getLong(2)));
                }
            }
            try (ResultSet result = statement.executeQuery(
                    "SELECT board, player_uuid, player_name, wins FROM " + boardEntries)) {
                while (result.next()) {
                    try {
                        entries.computeIfAbsent(result.getString(1), key -> new ArrayList<>())
                                .add(new WinRecord(UUID.fromString(result.getString(2)),
                                        result.getString(3), result.getInt(4)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            try (ResultSet result = statement.executeQuery("SELECT board, team, wins FROM " + boardTeams)) {
                while (result.next()) {
                    teams.computeIfAbsent(result.getString(1), key -> new HashMap<>())
                            .put(result.getString(2), result.getInt(3));
                }
            }
        }
        List<BoardSnapshot> snapshots = new ArrayList<>(starts.size());
        for (Map.Entry<String, Instant> board : starts.entrySet()) {
            snapshots.add(new BoardSnapshot(board.getKey(), board.getValue(),
                    entries.getOrDefault(board.getKey(), List.of()),
                    teams.getOrDefault(board.getKey(), Map.of())));
        }
        return snapshots;
    }

    @Override
    public void createBoard(@NotNull String boardId, @NotNull Instant periodStart) {
        run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    dialect.insertIgnore(boards, List.of("id", "period_start")))) {
                statement.setString(1, boardId);
                statement.setLong(2, periodStart.getEpochSecond());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void recordBoardWin(@NotNull String boardId, @NotNull UUID playerId, @NotNull String playerName,
                               @NotNull String team) {
        run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    dialect.increment(boardEntries, List.of("board", "player_uuid"), "player_name"))) {
                statement.setString(1, boardId);
                statement.setString(2, playerId.toString());
                statement.setString(3, playerName);
                statement.executeUpdate();
            }
            if (!team.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        dialect.increment(boardTeams, List.of("board", "team"), null))) {
                    statement.setString(1, boardId);
                    statement.setString(2, team);
                    statement.executeUpdate();
                }
            }
        });
    }

    @Override
    public void rollBoard(@NotNull String boardId, @NotNull Instant previousStart, @NotNull Instant newStart) {
        run(connection -> {
            connection.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + boards + " SET period_start = ? WHERE id = ? AND period_start = ?")) {
                    statement.setLong(1, newStart.getEpochSecond());
                    statement.setString(2, boardId);
                    statement.setLong(3, previousStart.getEpochSecond());
                    updated = statement.executeUpdate();
                }
                if (updated > 0) {
                    for (String table : List.of(boardEntries, boardTeams)) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM " + table + " WHERE board = ?")) {
                            statement.setString(1, boardId);
                            statement.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    @Override
    public void resetBoardPlayer(@NotNull UUID playerId) {
        run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + boardEntries + " WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void saveBoards(@NotNull Collection<Leaderboard> boards) {
    }

    private void run(@NotNull SqlTask task) {
        executor.execute(() -> {
            try (Connection connection = dataSource.getConnection()) {
                task.run(connection);
            } catch (SQLException exception) {
                plugin.getComponentLogger().error("Database write failed", exception);
            }
        });
    }

    private <T> @NotNull CompletableFuture<T> query(@NotNull SqlQuery<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return query.run(connection);
            } catch (SQLException exception) {
                plugin.getComponentLogger().error("Database query failed", exception);
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @FunctionalInterface
    private interface SqlTask {
        void run(@NotNull Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlQuery<T> {
        T run(@NotNull Connection connection) throws SQLException;
    }
}
