package dev.openrp.fdo.adapter.defaults;

import dev.openrp.fdo.adapter.LoggingAdapter;

/** Logging adapter that discards everything ({@code adapters.logging: none}). */
public final class NoopLoggingAdapter implements LoggingAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public void log(String line) {
        // intentionally no-op
    }

    @Override
    public void close() {
        // nothing to release
    }
}
