package dev.openrp.companies.core;

import java.util.Optional;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyApplication;
import dev.openrp.companies.model.CompanyMember;
import dev.openrp.companies.model.CompanyTransaction;

/**
 * Outcome of a service operation. Carries a success flag, a message key (resolved by the message
 * layer) with its placeholder pairs, and an optional payload (the created company, the affected
 * member, ...). Centralising the result here means every entry point - command, GUI, public API -
 * runs the exact same validated path and reports failures consistently. Pure data, so it is fully
 * unit-testable without a running server.
 */
public final class CompanyResult {

    private final boolean success;
    private final String messageKey;
    private final Object[] placeholders;
    private final Object payload;

    private CompanyResult(boolean success, String messageKey, Object payload, Object[] placeholders) {
        this.success = success;
        this.messageKey = messageKey;
        this.payload = payload;
        this.placeholders = placeholders == null ? new Object[0] : placeholders;
    }

    public static CompanyResult ok(String messageKey, Object... placeholders) {
        return new CompanyResult(true, messageKey, null, placeholders);
    }

    public static CompanyResult fail(String messageKey, Object... placeholders) {
        return new CompanyResult(false, messageKey, null, placeholders);
    }

    /** Returns a copy of this result carrying the given payload. */
    public CompanyResult withPayload(Object payload) {
        return new CompanyResult(success, messageKey, payload, placeholders);
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

    /** Placeholder key/value pairs, ready to spread into the message layer. */
    public Object[] placeholders() {
        return placeholders;
    }

    public Object payload() {
        return payload;
    }

    public Optional<Company> company() {
        return payload instanceof Company company ? Optional.of(company) : Optional.empty();
    }

    public Optional<CompanyMember> member() {
        return payload instanceof CompanyMember member ? Optional.of(member) : Optional.empty();
    }

    public Optional<CompanyApplication> application() {
        return payload instanceof CompanyApplication application ? Optional.of(application) : Optional.empty();
    }

    public Optional<CompanyTransaction> transaction() {
        return payload instanceof CompanyTransaction transaction ? Optional.of(transaction) : Optional.empty();
    }
}
