package dev.openrp.crime.module.production;

import org.bukkit.scheduler.BukkitTask;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.module.CrimeModule;

/**
 * Open Production: the multi-stage production of illegal goods, plus the physical-discovery scanner
 * that lets an agent standing in an active location learn about it - the only automatic timing the
 * subsystem does, and even that just notifies a physically present agent.
 */
public final class ProductionModule implements CrimeModule {

    private static final long SCAN_PERIOD_TICKS = 100L;

    private final OpenCrimePlugin plugin;
    private final ProductionService service;
    private BukkitTask scanTask;

    public ProductionModule(OpenCrimePlugin plugin) {
        this.plugin = plugin;
        this.service = new ProductionService(plugin);
    }

    public ProductionService service() {
        return service;
    }

    @Override
    public String id() {
        return "production";
    }

    @Override
    public void enable() {
        service.loadAll();
        plugin.setExecutor("produce", new ProduceCommand(plugin, service));
        scanTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, service::scanPhysicalDiscovery, SCAN_PERIOD_TICKS, SCAN_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }
}
