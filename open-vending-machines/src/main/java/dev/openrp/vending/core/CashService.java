package dev.openrp.vending.core;

import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.entity.Player;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.event.VendingMachineCashWithdrawEvent;
import dev.openrp.vending.hook.VendingDecision;
import dev.openrp.vending.model.VendingMachine;

/**
 * Empties a machine's internal cash box under the machine lock. The destination is resolved first
 * (the owning company's account when the machine is owned and company deposits are enabled,
 * otherwise the withdrawing player); a cancellable {@link VendingMachineCashWithdrawEvent} fires
 * before any money moves. If a company deposit fails the cash falls back to the player, and if that
 * fails too the cash box is refunded - it can never vanish.
 */
public final class CashService {

    private final OpenVendingMachinesPlugin plugin;

    public CashService(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    public WithdrawResult withdraw(Player staff, VendingMachine machine) {
        String symbol = plugin.settings().currencySymbol();
        ReentrantLock lock = plugin.locks().get(machine.id());
        lock.lock();
        try {
            if (!Authorization.canWithdraw(plugin, staff, machine) || plugin.hooks().canWithdraw(staff, machine).denied()) {
                plugin.messages().warning(staff, "withdraw.denied");
                return WithdrawResult.fail();
            }
            double amount = Money.round(machine.cashBalance());
            if (amount <= 0) {
                plugin.messages().warning(staff, "withdraw.empty");
                return WithdrawResult.fail();
            }

            String destination = null;
            String companyName = null;
            if (machine.hasOwner() && plugin.settings().depositToCompanyAccount()) {
                String owner = machine.ownerCompanyId().orElseThrow();
                destination = plugin.adapters().business().companyAccount(owner).orElse(owner);
                companyName = plugin.adapters().business().companyDisplayName(owner).orElse(owner);
            }

            VendingMachineCashWithdrawEvent event = new VendingMachineCashWithdrawEvent(staff, machine, amount, destination);
            plugin.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return WithdrawResult.fail();
            }

            double drained = machine.drainCash();
            String finalDestination = destination;
            boolean paid;
            if (destination != null) {
                paid = plugin.adapters().economy().depositToAccount(destination, drained);
                if (!paid) {
                    // Company account unavailable (e.g. economy has no bank support): pay the staff.
                    paid = plugin.adapters().economy().deposit(staff, plugin.settings().withdrawAccount(), drained);
                    finalDestination = null;
                    companyName = null;
                }
            } else {
                paid = plugin.adapters().economy().deposit(staff, plugin.settings().withdrawAccount(), drained);
            }
            if (!paid) {
                machine.depositCash(drained); // refund the cash box, nothing was paid out
                plugin.messages().error(staff, "general.operation_failed");
                return WithdrawResult.fail();
            }

            plugin.machines().save(machine);
            plugin.adapters().logging().log("WITHDRAW", staff.getName() + " withdrew " + symbol + Money.format(drained)
                    + " from machine " + machine.id() + (finalDestination == null ? "" : " -> account " + finalDestination));
            plugin.messages().success(staff, "withdraw.success",
                    "symbol", symbol, "amount", Money.format(drained), "id", machine.shortId());
            if (finalDestination != null && companyName != null) {
                plugin.messages().info(staff, "withdraw.to_company",
                        "symbol", symbol, "amount", Money.format(drained), "company", companyName);
            }
            return WithdrawResult.ok(drained, finalDestination);
        } finally {
            lock.unlock();
        }
    }
}
