package dev.openrp.companies.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.model.RecurringPayment;

/**
 * In-memory cache of configured recurring salaries, write-through to the storage adapter - same shape
 * as {@link LedgerManager}. The {@link dev.openrp.companies.core.PayrollService} reads {@link #due} on
 * a timer; the terminal UI sets and clears entries through {@link #set} / {@link #remove}.
 */
public final class RecurringManager {

    private final AdapterRegistry adapters;
    private final Map<String, RecurringPayment> byKey = new LinkedHashMap<>();

    public RecurringManager(AdapterRegistry adapters) {
        this.adapters = adapters;
    }

    public void loadAll() {
        byKey.clear();
        for (RecurringPayment payment : adapters.storage().loadRecurringPayments()) {
            byKey.put(payment.key(), payment);
        }
    }

    /** Creates or updates a recurring salary and persists it. */
    public void set(RecurringPayment payment) {
        byKey.put(payment.key(), payment);
        adapters.storage().saveRecurringPayment(payment);
    }

    /** Persists an in-place mutation (e.g. after advancing the next-due time). */
    public void save(RecurringPayment payment) {
        adapters.storage().saveRecurringPayment(payment);
    }

    public void remove(String companyId, UUID memberUuid) {
        byKey.remove(RecurringPayment.key(companyId, memberUuid));
        adapters.storage().deleteRecurringPayment(companyId, memberUuid);
    }

    public void removeAllOf(String companyId) {
        List<RecurringPayment> toRemove = new ArrayList<>();
        for (RecurringPayment payment : byKey.values()) {
            if (payment.companyId().equals(companyId)) {
                toRemove.add(payment);
            }
        }
        for (RecurringPayment payment : toRemove) {
            remove(companyId, payment.memberUuid());
        }
    }

    public Optional<RecurringPayment> get(String companyId, UUID memberUuid) {
        return Optional.ofNullable(byKey.get(RecurringPayment.key(companyId, memberUuid)));
    }

    /** A snapshot of payments due at {@code now} (safe to iterate while paying advances their schedule). */
    public List<RecurringPayment> due(long now) {
        List<RecurringPayment> due = new ArrayList<>();
        for (RecurringPayment payment : byKey.values()) {
            if (payment.isDue(now)) {
                due.add(payment);
            }
        }
        return due;
    }
}
