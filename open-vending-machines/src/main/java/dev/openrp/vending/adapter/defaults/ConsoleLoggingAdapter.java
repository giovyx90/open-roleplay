package dev.openrp.vending.adapter.defaults;

import java.util.logging.Logger;
import dev.openrp.vending.adapter.LoggingAdapter;

/** Logging adapter that writes audit lines to the server console/log. */
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
    public void log(String category, String message) {
        logger.info("[audit] [" + category + "] " + message);
    }
}
