package dev.openrp.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.openrp.core.OpenCorePlugin;
import dev.openrp.core.api.database.OpenDatabase;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OpenDatabaseFactory {
    private OpenDatabaseFactory() {
    }

    public static DatabaseSettings readSettings(FileConfiguration config) {
        return new DatabaseSettings(
                config.getBoolean("database.enabled", false),
                config.getString("database.type", "sqlite"),
                config.getString("database.sqlite.file", "open_core.db"),
                config.getString("database.mysql.host", "127.0.0.1"),
                config.getInt("database.mysql.port", 3306),
                config.getString("database.mysql.database", "open_roleplay"),
                config.getString("database.mysql.username", "open_roleplay"),
                config.getString("database.mysql.password", ""),
                Math.max(1, config.getInt("database.mysql.pool.max-size", 10)),
                Math.max(0, config.getInt("database.mysql.pool.minimum-idle", 1)),
                Math.max(1000L, config.getLong("database.mysql.pool.connection-timeout-millis", 30_000L))
        );
    }

    public static OpenDatabase create(OpenCorePlugin plugin, DatabaseSettings settings) throws Exception {
        String type = settings.type();
        if ("mysql".equalsIgnoreCase(type) || "mariadb".equalsIgnoreCase(type)) {
            return mysql(settings);
        }
        if (type == null || "sqlite".equalsIgnoreCase(type)) {
            return sqlite(plugin, settings);
        }
        throw new IllegalArgumentException("Tipo database non supportato: " + type.toLowerCase(Locale.ROOT));
    }

    private static OpenDatabase sqlite(OpenCorePlugin plugin, DatabaseSettings settings) throws Exception {
        Path dataFolder = plugin.getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        String fileName = settings.sqliteFile();
        Path databaseFile = dataFolder.resolve(fileName == null || fileName.isBlank() ? "open_core.db" : fileName);

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("OpenCore-SQLite");
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(10_000L);
        HikariDataSource source = new HikariDataSource(hikari);
        return new OpenJdbcDatabase(source);
    }

    private static OpenDatabase mysql(DatabaseSettings settings) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("OpenCore-MySQL");
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setJdbcUrl("jdbc:mariadb://" + settings.mysqlHost() + ":" + settings.mysqlPort() + "/"
                + settings.mysqlDatabase());
        hikari.setUsername(settings.mysqlUsername());
        hikari.setPassword(settings.mysqlPassword());
        hikari.setMaximumPoolSize(settings.mysqlMaxSize());
        hikari.setMinimumIdle(settings.mysqlMinimumIdle());
        hikari.setConnectionTimeout(settings.mysqlConnectionTimeoutMillis());
        return new OpenJdbcDatabase(new HikariDataSource(hikari));
    }

    public record DatabaseSettings(
            boolean enabled,
            String type,
            String sqliteFile,
            String mysqlHost,
            int mysqlPort,
            String mysqlDatabase,
            String mysqlUsername,
            String mysqlPassword,
            int mysqlMaxSize,
            int mysqlMinimumIdle,
            long mysqlConnectionTimeoutMillis
    ) {
    }
}
