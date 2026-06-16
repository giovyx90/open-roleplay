package dev.openrp.companies.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.RecurringPayment;
import dev.openrp.companies.model.TransactionType;

/**
 * Drives recurring salaries. A synchronous repeating task (cadence {@code finance.payroll.tick-seconds})
 * pays every due {@link RecurringPayment} from its company treasury into the member's bank account via
 * {@link dev.openrp.companies.api.TreasuryService#transferToPlayer}, then reschedules it. The amount and
 * cadence remain the director's decision; this only automates the repetition. A payment that cannot be
 * funded is skipped (its schedule still advances, so it simply retries next cycle) and logged.
 */
public final class PayrollService {

    private final OpenCompaniesPlugin plugin;
    private BukkitTask task;

    public PayrollService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long ticks = Math.max(1L, plugin.settings().payrollTickSeconds()) * 20L;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::runDuePayments, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Pays everything due now. Runs on the main thread (fires events, no async Bukkit access). */
    public void runDuePayments() {
        long now = System.currentTimeMillis();
        for (RecurringPayment payment : plugin.recurring().due(now)) {
            pay(payment, now);
        }
    }

    private void pay(RecurringPayment payment, long now) {
        OfflinePlayer member = Bukkit.getOfflinePlayer(payment.memberUuid());
        CompanyResult result = plugin.treasury().transferToPlayer(payment.companyId(), member,
                payment.amount(), TransactionType.SALARY, null, "stipendio ricorrente");
        // Advance the schedule whether or not it could be funded, so an unfunded cycle is simply skipped
        // rather than retried every tick.
        payment.scheduleNext(now);
        plugin.recurring().save(payment);

        Player online = member.getPlayer();
        if (result.success()) {
            if (online != null) {
                plugin.messages().success(online, "recurring.received",
                        "amount", plugin.settings().currencySymbol() + String.format("%.2f", payment.amount()));
            }
        } else {
            plugin.adapters().logging().log("PAYROLL", "Skipped recurring salary for "
                    + payment.memberUuid() + " at '" + payment.companyId() + "': " + result.messageKey());
        }
    }
}
