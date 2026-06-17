package dev.openrp.crime.core;

import java.util.Optional;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.IllegalOrg;

/**
 * Outcome of a service operation: a success flag, a message key (resolved by the message layer) with
 * its placeholder pairs, and an optional payload. Centralising the result means every entry point -
 * command, public API - runs the exact same validated path and reports failures consistently. Pure
 * data, fully unit-testable without a running server.
 */
public final class CrimeResult {

    private final boolean success;
    private final String messageKey;
    private final Object[] placeholders;
    private final Object payload;

    private CrimeResult(boolean success, String messageKey, Object payload, Object[] placeholders) {
        this.success = success;
        this.messageKey = messageKey;
        this.payload = payload;
        this.placeholders = placeholders == null ? new Object[0] : placeholders;
    }

    public static CrimeResult ok(String messageKey, Object... placeholders) {
        return new CrimeResult(true, messageKey, null, placeholders);
    }

    public static CrimeResult fail(String messageKey, Object... placeholders) {
        return new CrimeResult(false, messageKey, null, placeholders);
    }

    public CrimeResult withPayload(Object payload) {
        return new CrimeResult(success, messageKey, payload, placeholders);
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

    public Optional<IllegalOrg> org() {
        return payload instanceof IllegalOrg org ? Optional.of(org) : Optional.empty();
    }

    public Optional<Discovery> discovery() {
        return payload instanceof Discovery discovery ? Optional.of(discovery) : Optional.empty();
    }
}
