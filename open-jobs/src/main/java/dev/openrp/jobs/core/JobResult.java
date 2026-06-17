package dev.openrp.jobs.core;

/**
 * Outcome of a service operation: a success flag, a message key (resolved by the message layer) with
 * its placeholder pairs, and an optional payload. Centralising the result means every entry point -
 * command, public API - runs the exact same validated path and reports failures consistently. Pure
 * data, fully unit-testable without a running server.
 */
public final class JobResult {

    private final boolean success;
    private final String messageKey;
    private final Object[] placeholders;
    private final Object payload;

    private JobResult(boolean success, String messageKey, Object payload, Object[] placeholders) {
        this.success = success;
        this.messageKey = messageKey;
        this.payload = payload;
        this.placeholders = placeholders == null ? new Object[0] : placeholders;
    }

    public static JobResult ok(String messageKey, Object... placeholders) {
        return new JobResult(true, messageKey, null, placeholders);
    }

    public static JobResult fail(String messageKey, Object... placeholders) {
        return new JobResult(false, messageKey, null, placeholders);
    }

    public JobResult withPayload(Object payload) {
        return new JobResult(success, messageKey, payload, placeholders);
    }

    public boolean success() {
        return success;
    }

    public boolean failed() {
        return !success;
    }

    public String messageKey() {
        return messageKey;
    }

    public Object[] placeholders() {
        return placeholders;
    }

    public Object payload() {
        return payload;
    }
}
