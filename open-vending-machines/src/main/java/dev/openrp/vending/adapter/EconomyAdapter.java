package dev.openrp.vending.adapter;

import org.bukkit.OfflinePlayer;

/**
 * Bridge to whatever economy a server runs. The core never assumes Vault, a particular currency or
 * even that "cash" and "bank" are different things - those are just opaque {@code account} keys the
 * adapter interprets.
 *
 * <p>Two account flavours are supported:</p>
 * <ul>
 *   <li><b>Player accounts</b> - identified by an {@link OfflinePlayer} plus an {@code account} key
 *       (e.g. {@code "cash"}, {@code "bank"} or a server-specific custom account type).</li>
 *   <li><b>Named accounts</b> - identified by a free-form id (e.g. a company treasury), used when
 *       paying out a machine's cash box.</li>
 * </ul>
 *
 * <p>Implementations must perform the money movement atomically and return {@code false} (without
 * side effects) if it cannot be completed - the transaction services rely on that contract to keep
 * purchases and withdrawals duplication-free.</p>
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

    /** Pays {@code amount} into a named account; returns {@code false} if it could not. */
    boolean depositToAccount(String accountId, double amount);

    /** Takes {@code amount} from a named account; returns {@code false} if it could not. */
    boolean withdrawFromAccount(String accountId, double amount);
}
