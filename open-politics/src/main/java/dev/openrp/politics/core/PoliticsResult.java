package dev.openrp.politics.core;

import java.util.Optional;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Outcome of a service operation: a success flag, a message key (resolved by the message layer) with
 * its placeholder pairs, and an optional payload. Centralising the result means every entry point -
 * command, public API - runs the exact same validated path and reports failures consistently. Pure
 * data, fully unit-testable without a running server.
 */
public final class PoliticsResult {

    private final boolean success;
    private final String messageKey;
    private final Object[] placeholders;
    private final Object payload;

    private PoliticsResult(boolean success, String messageKey, Object payload, Object[] placeholders) {
        this.success = success;
        this.messageKey = messageKey;
        this.payload = payload;
        this.placeholders = placeholders == null ? new Object[0] : placeholders;
    }

    public static PoliticsResult ok(String messageKey, Object... placeholders) {
        return new PoliticsResult(true, messageKey, null, placeholders);
    }

    public static PoliticsResult fail(String messageKey, Object... placeholders) {
        return new PoliticsResult(false, messageKey, null, placeholders);
    }

    public PoliticsResult withPayload(Object payload) {
        return new PoliticsResult(success, messageKey, payload, placeholders);
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

    public Optional<PoliticalAct> act() {
        return payload instanceof PoliticalAct act ? Optional.of(act) : Optional.empty();
    }

    public Optional<Law> law() {
        return payload instanceof Law law ? Optional.of(law) : Optional.empty();
    }

    public Optional<Election> election() {
        return payload instanceof Election election ? Optional.of(election) : Optional.empty();
    }
}
