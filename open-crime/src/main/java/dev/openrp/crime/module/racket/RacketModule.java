package dev.openrp.crime.module.racket;

import org.bukkit.scheduler.BukkitTask;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.module.CrimeModule;

/**
 * Open Racket: extortion / protection over companies. Requires a {@code CompanyAdapter}; a periodic
 * task bills due contracts when the owner can pay, otherwise flags them overdue for manual collection.
 */
public final class RacketModule implements CrimeModule {

    private static final long BILL_PERIOD_TICKS = 200L;

    private final OpenCrimePlugin plugin;
    private final RacketService service;
    private BukkitTask billTask;

    public RacketModule(OpenCrimePlugin plugin) {
        this.plugin = plugin;
        this.service = new RacketService(plugin);
    }

    public RacketService service() {
        return service;
    }

    @Override
    public String id() {
        return "racket";
    }

    @Override
    public void enable() {
        service.loadAll();
        plugin.setExecutor("racket", new RacketCommand(plugin, service));
        if (plugin.config().settings().racketRequiresCompaniesAdapter() && !service.companiesReady()) {
            plugin.getLogger().info("[OpenCrime] Racket requires a company adapter; running inert until one registers.");
        }
        billTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, service::billDue, BILL_PERIOD_TICKS, BILL_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (billTask != null) {
            billTask.cancel();
            billTask = null;
        }
    }
}
