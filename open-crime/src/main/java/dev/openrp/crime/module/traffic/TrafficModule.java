package dev.openrp.crime.module.traffic;

import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.module.CrimeModule;

/** Open Traffic: shipments of illegal goods along physical, interceptable routes. */
public final class TrafficModule implements CrimeModule {

    private final OpenCrimePlugin plugin;
    private final TrafficService service;

    public TrafficModule(OpenCrimePlugin plugin) {
        this.plugin = plugin;
        this.service = new TrafficService(plugin);
    }

    public TrafficService service() {
        return service;
    }

    @Override
    public String id() {
        return "traffic";
    }

    @Override
    public void enable() {
        service.loadAll();
        plugin.setExecutor("traffic", new TrafficCommand(plugin, service));
    }

    @Override
    public void disable() {
        // nothing to release
    }
}
