package dev.openrp.access.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

public final class AccessStorageFactory {
    private AccessStorageFactory() {
    }

    public static AccessStorage create(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("storage.type", "sqlite").trim().toLowerCase(Locale.ROOT);
        if (type.equals("mysql") || type.equals("mariadb")) {
            return new MySqlAccessStorage(
                    config.getString("storage.mysql.host", "127.0.0.1"),
                    config.getInt("storage.mysql.port", 3306),
                    config.getString("storage.mysql.database", "open_access"),
                    config.getString("storage.mysql.username", "open_access"),
                    config.getString("storage.mysql.password", ""),
                    config.getInt("storage.mysql.pool.max-size", 10),
                    config.getInt("storage.mysql.pool.minimum-idle", 1),
                    config.getLong("storage.mysql.pool.connection-timeout-millis", 30000L));
        }
        String sqliteFile = config.getString("storage.sqlite.file", "open_access.db");
        File file = new File(sqliteFile == null ? "open_access.db" : sqliteFile);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), sqliteFile == null ? "open_access.db" : sqliteFile);
        }
        return new SqliteAccessStorage(file);
    }
}
