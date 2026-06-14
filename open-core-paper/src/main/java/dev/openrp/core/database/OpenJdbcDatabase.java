package dev.openrp.core.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.openrp.core.api.database.OpenDatabase;

import java.sql.Connection;
import java.sql.SQLException;

public final class OpenJdbcDatabase implements OpenDatabase {
    private final HikariDataSource dataSource;

    public OpenJdbcDatabase(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!isReady()) {
            throw new SQLException("Database non inizializzato o gia' chiuso.");
        }
        return dataSource.getConnection();
    }

    @Override
    public void execute(String sql) throws SQLException {
        try (Connection connection = getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public boolean isReady() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
