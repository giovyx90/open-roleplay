package dev.openrp.access.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySqlAccessStorage extends JdbcAccessStorage {
    public MySqlAccessStorage(String host, int port, String database, String username, String password,
                              int maxPoolSize, int minimumIdle, long connectionTimeoutMillis) {
        this(dataSource(host, port, database, username, password, maxPoolSize, minimumIdle, connectionTimeoutMillis));
    }

    private MySqlAccessStorage(HikariDataSource dataSource) {
        super(dataSource, new MySqlAccessSqlDialect(), dataSource::close);
    }

    private static HikariDataSource dataSource(String host, int port, String database, String username, String password,
                                               int maxPoolSize, int minimumIdle, long connectionTimeoutMillis) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=utf8");
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password == null ? "" : password);
        config.setMaximumPoolSize(Math.max(1, maxPoolSize));
        config.setMinimumIdle(Math.max(0, minimumIdle));
        config.setConnectionTimeout(Math.max(1000L, connectionTimeoutMillis));
        config.setPoolName("OpenAccess-MySQL");
        return new HikariDataSource(config);
    }
}
