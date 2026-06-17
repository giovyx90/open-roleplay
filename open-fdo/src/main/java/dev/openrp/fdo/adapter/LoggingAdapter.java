package dev.openrp.fdo.adapter;

/** Audit trail for sensitive operations (acts produced, custody changes, verdicts, detention). */
public interface LoggingAdapter {

    String id();

    void log(String line);

    void close();
}
