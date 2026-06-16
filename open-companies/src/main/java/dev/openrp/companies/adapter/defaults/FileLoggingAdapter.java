package dev.openrp.companies.adapter.defaults;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import dev.openrp.companies.adapter.LoggingAdapter;

/**
 * Logging adapter that appends timestamped audit lines to a file. Writes are synchronized and
 * flushed immediately; a write failure is reported to the server log rather than thrown, so logging
 * problems never abort an operation.
 */
public final class FileLoggingAdapter implements LoggingAdapter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path file;
    private final Logger logger;

    public FileLoggingAdapter(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not create audit log " + file, exception);
        }
    }

    @Override
    public String id() {
        return "file";
    }

    @Override
    public synchronized void log(String category, String message) {
        String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] [" + category + "] " + message
                + System.lineSeparator();
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.warning("[OpenCompanies] Failed to write audit log: " + exception.getMessage());
        }
    }
}
