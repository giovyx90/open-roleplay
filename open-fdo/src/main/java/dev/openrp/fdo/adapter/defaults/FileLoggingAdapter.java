package dev.openrp.fdo.adapter.defaults;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.logging.Logger;
import dev.openrp.fdo.adapter.LoggingAdapter;

/**
 * Default logging adapter: appends one timestamped line per audit event to a file. Each write is a
 * single append + flush so a crash never loses already-logged lines; failures are reported to the
 * server logger and never throw into the caller.
 */
public final class FileLoggingAdapter implements LoggingAdapter {

    private final Path file;
    private final Logger logger;

    public FileLoggingAdapter(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (IOException exception) {
            logger.warning("[OpenFDO] Could not create audit log directory: " + exception.getMessage());
        }
    }

    @Override
    public String id() {
        return "file";
    }

    @Override
    public synchronized void log(String line) {
        if (line == null) {
            return;
        }
        String entry = Instant.now() + " " + line + System.lineSeparator();
        try {
            Files.writeString(file, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.warning("[OpenFDO] Failed to write audit line: " + exception.getMessage());
        }
    }

    @Override
    public void close() {
        // each log() call is self-contained; nothing to release
    }
}
