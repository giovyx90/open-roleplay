package dev.openrp.companies.adapter.defaults;

import dev.openrp.companies.adapter.LoggingAdapter;

/** Logging adapter that discards everything (set {@code adapters.logging: none}). */
public final class NoopLoggingAdapter implements LoggingAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public void log(String category, String message) {
        // intentionally empty
    }
}
