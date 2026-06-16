package dev.openrp.companies.adapter;

/**
 * Audit sink for company-affecting actions. The core logs every creation, deletion, membership and
 * role change, status/license change and headquarters move here; route it to a file, the console, a
 * database or nowhere.
 */
public interface LoggingAdapter {

    String id();

    /**
     * @param category short tag such as CREATE, DELETE, INVITE, JOIN, LEAVE, ROLE, STATUS, LICENSE, HQ, ASSET
     * @param message  human-readable audit line
     */
    void log(String category, String message);

    default void close() {
    }
}
