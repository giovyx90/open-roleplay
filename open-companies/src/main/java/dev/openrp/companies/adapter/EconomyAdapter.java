package dev.openrp.companies.adapter;

import org.bukkit.OfflinePlayer;

/**
 * Bridge to whatever economy a server runs. The core never assumes Vault, a particular currency, or
 * that "cash" and "bank" are different things - those are just opaque {@code account} keys the
 * adapter interprets. Used for the company creation fee and for moving money in and out of a
 * company treasury (a named account).
 *
 * <p>Implementations must move money atomically and return {@code false} (without side effects) if
 * the operation cannot be completed - the services rely on that contract to stay duplication-free.</p>
 */
public interface EconomyAdapter {

    /** Stable identifier, e.g. {@code "default"} or {@code "vault"}. */
    String id();

    double balance(OfflinePlayer player, String account);

    boolean has(OfflinePlayer player, String account, double amount);

    /** Removes {@code amount} from the player's account; returns {@code false} if it could not. */
    boolean withdraw(OfflinePlayer player, String account, double amount);

    /** Adds {@code amount} to the player's account; returns {@code false} if it could not. */
    boolean deposit(OfflinePlayer player, String account, double amount);

    double accountBalance(String accountId);

    /** Pays {@code amount} into a named (company) account; returns {@code false} if it could not. */
    boolean depositToAccount(String accountId, double amount);

    /** Takes {@code amount} from a named (company) account; returns {@code false} if it could not. */
    boolean withdrawFromAccount(String accountId, double amount);
}
