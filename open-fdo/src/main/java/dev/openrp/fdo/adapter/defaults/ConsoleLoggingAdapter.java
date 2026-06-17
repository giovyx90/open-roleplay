package dev.openrp.fdo.adapter.defaults;

import java.util.logging.Logger;
import dev.openrp.fdo.adapter.LoggingAdapter;

/** Logging adapter that writes the audit trail to the server console logger. */
public final class ConsoleLoggingAdapter implements LoggingAdapter {

    private final Logger logger;

    public ConsoleLoggingAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String id() {
        return "console";
    }

    @Override
    public void log(String line) {
        if (line != null) {
            logger.info("[audit] " + line);
        }
    }

    @Override
    public void close() {
        // nothing to release
    }
}
