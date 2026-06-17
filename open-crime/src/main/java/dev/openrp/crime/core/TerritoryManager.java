package dev.openrp.crime.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.crime.adapter.AdapterRegistry;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Territory;

/**
 * Owns territory control. It records who controls a region and flags contested ones; it never
 * arbitrates a territory war - two orgs claiming the same region just makes it contested, and the
 * players resolve it physically in RP. No numbers grow over time here.
 */
public final class TerritoryManager {

    private final AdapterRegistry adapters;
    private final OrgManager orgs;
    private final Map<String, Territory> byRegion = new LinkedHashMap<>();

    public TerritoryManager(AdapterRegistry adapters, OrgManager orgs) {
        this.adapters = adapters;
        this.orgs = orgs;
    }

    public synchronized void loadAll() {
        byRegion.clear();
        for (Territory territory : adapters.storage().loadTerritories()) {
            byRegion.put(territory.regionId(), territory);
        }
    }

    public Optional<Territory> get(String regionId) {
        return Optional.ofNullable(regionId == null ? null : byRegion.get(regionId));
    }

    public Optional<IllegalOrg> controller(String regionId) {
        Territory territory = regionId == null ? null : byRegion.get(regionId);
        if (territory == null || territory.orgId() == null) {
            return Optional.empty();
        }
        // A region whose owner has been dissolved counts as uncontrolled, not owned by a dead org.
        return orgs.get(territory.orgId()).filter(IllegalOrg::isActive);
    }

    public boolean isContested(String regionId) {
        Territory territory = regionId == null ? null : byRegion.get(regionId);
        return territory != null && territory.contested();
    }

    public synchronized List<Territory> ofOrg(String orgId) {
        List<Territory> result = new ArrayList<>();
        if (orgId == null) {
            return result;
        }
        for (Territory territory : byRegion.values()) {
            if (orgId.equals(territory.orgId())) {
                result.add(territory);
            }
        }
        return result;
    }

    public Collection<Territory> all() {
        return Collections.unmodifiableCollection(byRegion.values());
    }

    /**
     * Claims the region for the actor's org. Uncontrolled -> becomes theirs. Controlled by someone
     * else -> becomes contested (no transfer; resolve it in RP). Already theirs -> rejected.
     */
    public synchronized CrimeResult claim(UUID actor, String regionId) {
        IllegalOrg org = orgs.byMember(actor).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!orgs.has(actor, Capability.TERRITORY_CLAIM)) {
            return CrimeResult.fail("general.no_capability");
        }
        if (regionId == null || regionId.isBlank()) {
            return CrimeResult.fail("territory.no_region");
        }
        Territory territory = byRegion.computeIfAbsent(regionId,
                id -> new Territory(id, null, false, 0L));
        // Free when nobody holds it OR the holder has been dissolved - a dead org can't lock a region.
        IllegalOrg holder = territory.orgId() == null ? null : orgs.get(territory.orgId()).orElse(null);
        boolean free = holder == null || !holder.isActive();
        if (free) {
            territory.setOrgId(org.id());
            territory.setContested(false);
            territory.setControlSince(System.currentTimeMillis());
            org.addTerritory(regionId);
            adapters.storage().saveTerritory(territory);
            adapters.storage().saveOrg(org);
            return CrimeResult.ok("territory.claimed", "region", regionId).withPayload(territory);
        }
        if (org.id().equals(territory.orgId())) {
            return CrimeResult.fail("territory.already_yours", "region", regionId);
        }
        territory.setContested(true);
        adapters.storage().saveTerritory(territory);
        return CrimeResult.ok("territory.contested", "region", regionId).withPayload(territory);
    }

    public synchronized CrimeResult abandon(UUID actor, String regionId) {
        IllegalOrg org = orgs.byMember(actor).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!orgs.has(actor, Capability.TERRITORY_CLAIM)) {
            return CrimeResult.fail("general.no_capability");
        }
        Territory territory = regionId == null ? null : byRegion.get(regionId);
        if (territory == null || !org.id().equals(territory.orgId())) {
            return CrimeResult.fail("territory.not_yours", "region", String.valueOf(regionId));
        }
        territory.setOrgId(null);
        territory.setContested(false);
        territory.setControlSince(0L);
        org.removeTerritory(regionId);
        adapters.storage().saveTerritory(territory);
        adapters.storage().saveOrg(org);
        return CrimeResult.ok("territory.abandoned", "region", regionId).withPayload(territory);
    }
}
