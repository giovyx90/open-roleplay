package dev.openrp.companies.adapter.vault;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import dev.openrp.companies.adapter.EconomyAdapter;

/**
 * <b>Optional</b> example economy adapter that bridges to Vault entirely through reflection, so the
 * plugin keeps <em>zero</em> compile-time or mandatory runtime dependency on Vault (the same approach
 * other Open Roleplay modules use for soft integrations). The constructor throws if Vault or an
 * economy provider is absent; the plugin catches that and falls back to the default economy.
 *
 * <p>Vault models a single balance per player, so the {@code account} argument is ignored for player
 * methods. Named (company) accounts map to Vault <em>banks</em> when the provider supports them;
 * otherwise those operations return {@code false}/0 and the core falls back to the demo treasury.</p>
 */
public final class VaultEconomyAdapter implements EconomyAdapter {

    private final Object economy;
    private final Logger logger;

    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;
    private final Method balanceMethod;
    private final Method hasBankMethod;
    private final Method bankDepositMethod;
    private final Method bankWithdrawMethod;
    private final Method bankBalanceMethod;
    private final Method transactionSuccessMethod;
    private final Field responseBalanceField;

    public VaultEconomyAdapter(Logger logger) {
        this.logger = logger;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null || registration.getProvider() == null) {
                throw new IllegalStateException("Vault is installed but no economy provider is registered");
            }
            this.economy = registration.getProvider();
            this.hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            this.withdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            this.depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            this.balanceMethod = economyClass.getMethod("getBalance", OfflinePlayer.class);
            this.hasBankMethod = economyClass.getMethod("hasBankSupport");
            this.bankDepositMethod = economyClass.getMethod("bankDeposit", String.class, double.class);
            this.bankWithdrawMethod = economyClass.getMethod("bankWithdraw", String.class, double.class);
            this.bankBalanceMethod = economyClass.getMethod("bankBalance", String.class);
            Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            this.transactionSuccessMethod = responseClass.getMethod("transactionSuccess");
            this.responseBalanceField = responseClass.getField("balance");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Vault economy API not available: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String id() {
        return "vault";
    }

    @Override
    public double balance(OfflinePlayer player, String account) {
        try {
            return (double) balanceMethod.invoke(economy, player);
        } catch (ReflectiveOperationException exception) {
            return warnAndReturn(exception, 0.0);
        }
    }

    @Override
    public boolean has(OfflinePlayer player, String account, double amount) {
        try {
            return (boolean) hasMethod.invoke(economy, player, amount);
        } catch (ReflectiveOperationException exception) {
            return warnAndReturn(exception, false);
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, String account, double amount) {
        return succeeded(invoke(withdrawMethod, economy, player, amount));
    }

    @Override
    public boolean deposit(OfflinePlayer player, String account, double amount) {
        return succeeded(invoke(depositMethod, economy, player, amount));
    }

    @Override
    public double accountBalance(String accountId) {
        if (!bankSupported()) {
            return 0.0;
        }
        Object response = invoke(bankBalanceMethod, economy, accountId);
        if (response == null) {
            return 0.0;
        }
        try {
            return responseBalanceField.getDouble(response);
        } catch (ReflectiveOperationException exception) {
            return warnAndReturn(exception, 0.0);
        }
    }

    @Override
    public boolean depositToAccount(String accountId, double amount) {
        return bankSupported() && succeeded(invoke(bankDepositMethod, economy, accountId, amount));
    }

    @Override
    public boolean withdrawFromAccount(String accountId, double amount) {
        return bankSupported() && succeeded(invoke(bankWithdrawMethod, economy, accountId, amount));
    }

    private boolean bankSupported() {
        try {
            return (boolean) hasBankMethod.invoke(economy);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException exception) {
            return warnAndReturn(exception, null);
        }
    }

    private boolean succeeded(Object response) {
        if (response == null) {
            return false;
        }
        try {
            return (boolean) transactionSuccessMethod.invoke(response);
        } catch (ReflectiveOperationException exception) {
            return warnAndReturn(exception, false);
        }
    }

    private <T> T warnAndReturn(ReflectiveOperationException exception, T fallback) {
        logger.warning("[OpenCompanies] Vault economy call failed: " + exception.getMessage());
        return fallback;
    }
}
