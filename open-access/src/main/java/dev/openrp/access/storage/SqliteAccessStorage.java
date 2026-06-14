package dev.openrp.access.storage;

import org.sqlite.SQLiteDataSource;

import java.io.File;

public class SqliteAccessStorage extends JdbcAccessStorage {
    public SqliteAccessStorage(File file) {
        super(dataSource(file), new SqliteAccessSqlDialect(), () -> { });
    }

    private static SQLiteDataSource dataSource(File file) {
        File parent = file == null ? null : file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + file.getAbsolutePath());
        return dataSource;
    }
}
