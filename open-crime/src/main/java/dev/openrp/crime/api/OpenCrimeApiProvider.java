package dev.openrp.crime.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.config.Good;
import dev.openrp.crime.config.OrgRank;
import dev.openrp.crime.core.GoodsService;
import dev.openrp.crime.core.OrgManager;
import dev.openrp.crime.core.TerritoryManager;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.IllegalOrg;

/** Thin delegating implementation of {@link OpenCrimeApi} backed by the plugin's live services. */
public final class OpenCrimeApiProvider implements OpenCrimeApi {

    private final OpenCrimePlugin plugin;

    public OpenCrimeApiProvider(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public AdapterRegistry adapters() {
        return plugin.adapters();
    }

    @Override
    public Optional<IllegalOrg> getOrg(String orgId) {
        return plugin.orgs().get(orgId);
    }

    @Override
    public Optional<IllegalOrg> getOrgByMember(UUID member) {
        return plugin.orgs().byMember(member);
    }

    @Override
    public boolean isMember(UUID player, String orgId) {
        return plugin.orgs().isMember(player, orgId);
    }

    @Override
    public Optional<OrgRank> getRole(UUID player, String orgId) {
        if (!plugin.orgs().isMember(player, orgId)) {
            return Optional.empty();
        }
        return plugin.orgs().roleOf(player);
    }

    @Override
    public boolean isIllegal(ItemStack item) {
        return plugin.goods().isIllegal(item);
    }

    @Override
    public Optional<Good> getIllegalGood(ItemStack item) {
        return plugin.goods().getIllegalGood(item);
    }

    @Override
    public void markIllegal(ItemStack item, String goodId, UUID producer) {
        plugin.goods().markIllegal(item, goodId, producer);
    }

    @Override
    public Optional<IllegalOrg> getTerritoryController(String regionId) {
        return plugin.territories().controller(regionId);
    }

    @Override
    public boolean isContested(String regionId) {
        return plugin.territories().isContested(regionId);
    }

    @Override
    public void registerCrimeEvent(CrimeEvent event) {
        plugin.events().register(event);
    }

    @Override
    public void registerDiscovery(Discovery discovery) {
        plugin.discoveries().register(discovery);
    }

    @Override
    public List<Discovery> getDiscoveriesByDossier(String dossierId) {
        return plugin.discoveries().byDossier(dossierId);
    }

    @Override
    public List<CrimeEvent> getDiscoveredEventsByOrg(String orgId, String dossierId) {
        return plugin.events().discoveredByOrg(orgId, dossierId);
    }

    @Override
    public OrgManager orgs() {
        return plugin.orgs();
    }

    @Override
    public TerritoryManager territories() {
        return plugin.territories();
    }

    @Override
    public GoodsService goods() {
        return plugin.goods();
    }
}
