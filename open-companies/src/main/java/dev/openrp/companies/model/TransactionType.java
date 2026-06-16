package dev.openrp.companies.model;

import java.util.Locale;

/**
 * Why a company's treasury moved. Each type carries whether it {@link #credit() credits} the treasury
 * (money in) or debits it (money out), so the ledger reads the same way to humans and to code. The
 * core never invents money: every credit has a matching withdrawal somewhere (a customer's bank
 * account, consumed banknotes) and every debit a matching deposit (a member's bank account, dispensed
 * banknotes); the type only records the <em>reason</em>.
 */
public enum TransactionType {

    /** Card sale: a customer paid from their bank account at a POS/register. */
    SALE_CARD(true),
    /** Cash sale: a customer paid with physical banknotes at a register. */
    SALE_CASH(true),
    /** Physical cash a director fed into the company treasury. */
    TREASURY_DEPOSIT(true),
    /** An inbound transfer from another account. */
    TRANSFER_IN(true),
    /** A discretionary salary/bonus bonifico paid to a member. */
    SALARY(false),
    /** An outbound transfer to a player or company. */
    TRANSFER_OUT(false),
    /** Physical cash a director drew out of the company treasury. */
    TREASURY_WITHDRAW(false),
    /** A fee charged against the treasury. */
    FEE(false);

    private final boolean credit;

    TransactionType(boolean credit) {
        this.credit = credit;
    }

    /** Whether this movement increases the treasury (money in) rather than decreasing it. */
    public boolean credit() {
        return credit;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lenient parser; returns {@code null} for unknown values. */
    public static TransactionType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
