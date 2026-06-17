package dev.openrp.crime.module.laundering;

import org.bukkit.scheduler.BukkitTask;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.module.CrimeModule;

/**
 * Open Laundering: turns dirty money clean over time. Requires an economy that distinguishes dirty
 * money (the bundled internal ledger qualifies); a periodic task settles completed washes.
 */
public final class LaunderingModule implements CrimeModule {

    private static final long SETTLE_PERIOD_TICKS = 200L;

    private final OpenCrimePlugin plugin;
    private final LaunderingService service;
    private BukkitTask settleTask;

    public LaunderingModule(OpenCrimePlugin plugin) {
        this.plugin = plugin;
        this.service = new LaunderingService(plugin);
    }

    public LaunderingService service() {
        return service;
    }

    @Override
    public String id() {
        return "laundering";
    }

    @Override
    public void enable() {
        service.loadAll();
        plugin.setExecutor("launder", new LaunderCommand(plugin, service));
        if (!service.bankReady()) {
            plugin.getLogger().info("[OpenCrime] Laundering requires a bank adapter; running inert until one registers.");
        }
        settleTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, service::settleCompleted, SETTLE_PERIOD_TICKS, SETTLE_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (settleTask != null) {
            settleTask.cancel();
            settleTask = null;
        }
    }
}
