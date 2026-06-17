package dev.openrp.fdo.api;

import java.util.UUID;
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

/**
 * Public API, registered with the Bukkit ServicesManager. Other plugins use it to read the registry,
 * drive the same validated services the commands use, swap adapters at runtime, and feed back events
 * such as a confirmed escape. Retrieve it with
 * {@code Bukkit.getServicesManager().load(OpenFdoApi.class)}.
 */
public interface OpenFdoApi {

    /** The live adapter set; register your detention/audit/external-record/radio adapters here. */
    AdapterRegistry adapters();

    AgentManager agents();

    DossierManager dossiers();

    EvidenceManager evidence();

    WantedManager wanted();

    DetentionManager detention();

    ActService acts();

    AlertManager alerts();

    DutyService duty();

    /**
     * Hook for a detention adapter: report a confirmed escape so the core updates the dossier, alerts
     * on-duty members and proposes wanted status (manually confirmed by a member with FLAG_WANTED).
     */
    FdoResult reportEscape(UUID inmate, String world, double x, double y, double z);
}
