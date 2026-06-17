package dev.openrp.crime.module.syndicate;

import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.module.CrimeModule;

/** Open Syndicate: organisation life (founding, hierarchy, recruitment) and territory. */
public final class SyndicateModule implements CrimeModule {

    private final OpenCrimePlugin plugin;

    public SyndicateModule(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "syndicate";
    }

    @Override
    public void enable() {
        plugin.setExecutor("syndicate", new SyndicateCommand(plugin));
        plugin.setExecutor("territory", new TerritoryCommand(plugin));
    }

    @Override
    public void disable() {
        // commands are cleared by Bukkit on disable; nothing else to release
    }
}
