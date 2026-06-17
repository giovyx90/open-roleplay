package dev.openrp.crime.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.config.Good;
import dev.openrp.crime.config.OrgRank;
import dev.openrp.crime.core.GoodsService;
import dev.openrp.crime.core.OrgManager;
import dev.openrp.crime.core.TerritoryManager;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.IllegalOrg;

/**
 * Public API, registered with the Bukkit ServicesManager. It is the central registry of illegality,
 * but <strong>opaque to the authorities by default</strong>: the only way to read crime events tied
 * to an org is through a dossier id that real {@link Discovery discoveries} have been linked to. The
 * core never exposes a crime automatically. Retrieve it with
 * {@code Bukkit.getServicesManager().load(OpenCrimeApi.class)}.
 */
public interface OpenCrimeApi {

    /** The live adapter set; register your economy/company/authority/region adapter here. */
    AdapterRegistry adapters();

    // --- organisations -----------------------------------------------------------------------

    Optional<IllegalOrg> getOrg(String orgId);

    Optional<IllegalOrg> getOrgByMember(UUID member);

    boolean isMember(UUID player, String orgId);

    /** The rank the player holds in the named org, or empty when they are not a member of it. */
    Optional<OrgRank> getRole(UUID player, String orgId);

    // --- illegal goods -----------------------------------------------------------------------

    boolean isIllegal(ItemStack item);

    Optional<Good> getIllegalGood(ItemStack item);

    /** Marks an existing item stack as the named illegal good and starts tracking it. */
    void markIllegal(ItemStack item, String goodId, UUID producer);

    // --- territory ---------------------------------------------------------------------------

    Optional<IllegalOrg> getTerritoryController(String regionId);

    boolean isContested(String regionId);

    // --- crime events (write only from subsystems / integrations) ----------------------------

    void registerCrimeEvent(CrimeEvent event);

    // --- discoveries: the only path for the authorities --------------------------------------

    void registerDiscovery(Discovery discovery);

    List<Discovery> getDiscoveriesByDossier(String dossierId);

    /**
     * Crime events of an org that have been linked to the given dossier. Requires a non-null dossier
     * id: the authorities can never query an org's events freely - only those a real discovery tied to
     * an open case has surfaced.
     */
    List<CrimeEvent> getDiscoveredEventsByOrg(String orgId, String dossierId);

    // --- manager access for richer integrations ----------------------------------------------
    // Note: the raw CrimeEventLog and DiscoveryService are deliberately NOT exposed here. Letting any
    // integration read every crime event directly would defeat the opacity invariant - the authorities
    // must go through getDiscoveredEventsByOrg(orgId, dossierId), which requires a real dossier link.

    OrgManager orgs();

    TerritoryManager territories();

    GoodsService goods();
}
