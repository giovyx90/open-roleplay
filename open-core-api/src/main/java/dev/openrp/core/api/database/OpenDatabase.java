package dev.openrp.core.api.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface OpenDatabase extends AutoCloseable {
    Connection getConnection() throws SQLException;

    void execute(String sql) throws SQLException;

    boolean isReady();

    @Override
    void close();
}
