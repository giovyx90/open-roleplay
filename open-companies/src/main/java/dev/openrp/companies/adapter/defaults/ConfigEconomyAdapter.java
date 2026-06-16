package dev.openrp.companies.adapter.defaults;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.OfflinePlayer;
import dev.openrp.companies.adapter.EconomyAdapter;

/**
 * In-memory demo economy driven by a configured starting balance. Player wallets start at that
 * balance the first time they are seen; named (company) accounts start empty. Balances are not
 * persisted - this exists so the plugin is fully playable out of the box. Replace it with a real
 * adapter (e.g. Vault) in production.
 */
public final class ConfigEconomyAdapter implements EconomyAdapter {

    private final double startingBalance;
    private final Map<UUID, Map<String, Double>> playerAccounts = new ConcurrentHashMap<>();
    private final Map<String, Double> namedAccounts = new ConcurrentHashMap<>();

    public ConfigEconomyAdapter(double startingBalance) {
        this.startingBalance = Math.max(0.0, startingBalance);
    }

    @Override
    public String id() {
        return "default";
    }

    @Override
    public double balance(OfflinePlayer player, String account) {
        return wallet(player).getOrDefault(key(account), startingBalance);
    }

    @Override
    public boolean has(OfflinePlayer player, String account, double amount) {
        return amount <= 0 || balance(player, account) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, String account, double amount) {
        if (amount < 0) {
            return false;
        }
        // Atomic read-modify-write: two concurrent withdrawals can't both pass the balance check and
        // over-withdraw. A never-seen wallet is treated as holding the starting balance.
        boolean[] ok = {false};
        wallet(player).compute(key(account), (k, current) -> {
            double balance = current == null ? startingBalance : current;
            if (balance < amount) {
                return current;
            }
            ok[0] = true;
            return balance - amount;
        });
        return ok[0];
    }

    @Override
    public boolean deposit(OfflinePlayer player, String account, double amount) {
        if (amount < 0) {
            return false;
        }
        wallet(player).compute(key(account), (k, current) -> (current == null ? startingBalance : current) + amount);
        return true;
    }

    @Override
    public double accountBalance(String accountId) {
        return namedAccounts.getOrDefault(key(accountId), 0.0);
    }

    @Override
    public boolean depositToAccount(String accountId, double amount) {
        if (amount < 0) {
            return false;
        }
        namedAccounts.merge(key(accountId), amount, Double::sum);
        return true;
    }

    @Override
    public boolean withdrawFromAccount(String accountId, double amount) {
        if (amount < 0) {
            return false;
        }
        // Atomic check-and-debit so concurrent company-account withdrawals can't over-draw.
        boolean[] ok = {false};
        namedAccounts.compute(key(accountId), (k, current) -> {
            double balance = current == null ? 0.0 : current;
            if (balance < amount) {
                return current;
            }
            ok[0] = true;
            return balance - amount;
        });
        return ok[0];
    }

    private Map<String, Double> wallet(OfflinePlayer player) {
        return playerAccounts.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
    }

    private static String key(String account) {
        return account == null || account.isBlank() ? "cash" : account.toLowerCase(Locale.ROOT);
    }
}
