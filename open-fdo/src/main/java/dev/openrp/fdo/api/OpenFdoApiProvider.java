package dev.openrp.fdo.api;

import java.util.UUID;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.adapter.AdapterRegistry;
import dev.openrp.fdo.core.ActService;
import dev.openrp.fdo.core.AgentManager;
import dev.openrp.fdo.core.AlertManager;
import dev.openrp.fdo.core.DetentionManager;
import dev.openrp.fdo.core.DossierManager;
import dev.openrp.fdo.core.DutyService;
import dev.openrp.fdo.core.EvidenceManager;
import dev.openrp.fdo.core.FdoResult;
import dev.openrp.fdo.core.WantedManager;

/** Thin delegating implementation of {@link OpenFdoApi} backed by the plugin's live services. */
public final class OpenFdoApiProvider implements OpenFdoApi {

    private final OpenFdoPlugin plugin;

    public OpenFdoApiProvider(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }

    @Override
    public AgentManager agents() {
        return plugin.agents();
    }

    @Override
    public DossierManager dossiers() {
        return plugin.dossiers();
    }

    @Override
    public EvidenceManager evidence() {
        return plugin.evidence();
    }

    @Override
    public WantedManager wanted() {
        return plugin.wanted();
    }

    @Override
    public DetentionManager detention() {
        return plugin.detention();
    }

    @Override
    public ActService acts() {
        return plugin.acts();
    }

    @Override
    public AlertManager alerts() {
        return plugin.alerts();
    }

    @Override
    public DutyService duty() {
        return plugin.duty();
    }

    @Override
    public FdoResult reportEscape(UUID inmate, String world, double x, double y, double z) {
        return plugin.detention().reportEscape(inmate, world, x, y, z);
    }
}
