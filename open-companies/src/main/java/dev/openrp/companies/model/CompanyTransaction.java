package dev.openrp.companies.model;

import java.util.UUID;

/**
 * One immutable line of a company's treasury ledger: when it happened, why ({@link TransactionType}),
 * how much, who triggered it and a human-readable note. The ledger is append-only - balances are
 * mutated on {@link Company#balance()} for fast reads, while these rows are the audit trail of how the
 * balance got there. Pure data (no Bukkit types) so it is storage-agnostic and unit-testable.
 *
 * @param id           stable unique id of this ledger line
 * @param companyId    the company whose treasury moved
 * @param timestamp    epoch millis when the movement was recorded
 * @param type         why the treasury moved
 * @param amount       the always-positive magnitude of the movement (direction comes from {@link TransactionType#credit()})
 * @param actor        the player who triggered it, or {@code null} for system/automated movements
 * @param counterparty the other side (a player uuid, a company id, or a free label), or {@code null}
 * @param note         optional human-readable note shown in the ledger view
 */
public record CompanyTransaction(UUID id, String companyId, long timestamp, TransactionType type,
                                 double amount, UUID actor, String counterparty, String note) {

    public CompanyTransaction {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId");
        }
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        amount = Math.abs(amount);
        note = note == null ? "" : note;
    }

    /** Signed amount: positive when it credited the treasury, negative when it debited it. */
    public double signedAmount() {
        return type.credit() ? amount : -amount;
    }
}
