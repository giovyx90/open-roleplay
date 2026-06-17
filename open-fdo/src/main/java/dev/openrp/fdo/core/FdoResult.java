package dev.openrp.fdo.core;

import java.util.Optional;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.WantedEntry;

/**
 * Outcome of a service operation: a success flag, a message key (resolved by the message layer) with
 * its placeholder pairs, and an optional payload. Centralising the result here means every entry
 * point - command, GUI, public API - runs the exact same validated path and reports failures
 * consistently. Pure data, fully unit-testable without a running server.
 */
public final class FdoResult {

    private final boolean success;
    private final String messageKey;
    private final Object[] placeholders;
    private final Object payload;

    private FdoResult(boolean success, String messageKey, Object payload, Object[] placeholders) {
        this.success = success;
        this.messageKey = messageKey;
        this.payload = payload;
        this.placeholders = placeholders == null ? new Object[0] : placeholders;
    }

    public static FdoResult ok(String messageKey, Object... placeholders) {
        return new FdoResult(true, messageKey, null, placeholders);
    }

    public static FdoResult fail(String messageKey, Object... placeholders) {
        return new FdoResult(false, messageKey, null, placeholders);
    }

    public FdoResult withPayload(Object payload) {
        return new FdoResult(success, messageKey, payload, placeholders);
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

    public Optional<Dossier> dossier() {
        return payload instanceof Dossier dossier ? Optional.of(dossier) : Optional.empty();
    }

    public Optional<Evidence> evidence() {
        return payload instanceof Evidence evidence ? Optional.of(evidence) : Optional.empty();
    }

    public Optional<WantedEntry> wanted() {
        return payload instanceof WantedEntry wanted ? Optional.of(wanted) : Optional.empty();
    }
}
