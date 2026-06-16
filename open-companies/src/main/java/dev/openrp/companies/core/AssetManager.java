package dev.openrp.companies.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.StorageAdapter;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyAssetType;
import dev.openrp.companies.model.CompanyCapability;

/**
 * In-memory registry and rule engine for physical company assets. Pure Java (no Bukkit, no events),
 * resolving company membership through the injected {@link CompanyManager}, so its authorization
 * helpers and the position index can be unit-tested directly. Assets are indexed both by id and by a
 * compact position key for O(1) "what's at this block?" lookups.
 */
public final class AssetManager {

    private final CompanyManager companies;
    private final AdapterRegistry adapters;

    private final Map<UUID, CompanyAsset> assets = new LinkedHashMap<>();
    private final Map<String, UUID> byPosition = new LinkedHashMap<>();

    public AssetManager(CompanyManager companies, AdapterRegistry adapters) {
        this.companies = companies;
        this.adapters = adapters;
    }

    /** The active storage backend, read live so a swapped adapter takes effect immediately. */
    private StorageAdapter storage() {
        return adapters.storage();
    }

    public void loadAll() {
        assets.clear();
        byPosition.clear();
        for (CompanyAsset asset : storage().loadAssets()) {
            assets.put(asset.id(), asset);
            byPosition.put(asset.position().key(), asset.id());
        }
    }

    public CompanyResult registerAsset(String companyId, CompanyAssetType type, CompanyAsset.BlockPosition position) {
        var company = companies.company(companyId).orElse(null);
        if (company == null) {
            return CompanyResult.fail("company.not_found", "company", companyId);
        }
        if (!company.status().canOperate()) {
            return CompanyResult.fail("company.not_active", "status", company.status().name());
        }
        if (type == null) {
            return CompanyResult.fail("asset.unknown_type");
        }
        if (position == null) {
            return CompanyResult.fail("asset.bad_position");
        }
        if (byPosition.containsKey(position.key())) {
            return CompanyResult.fail("asset.occupied");
        }
        CompanyAsset asset = new CompanyAsset(UUID.randomUUID(), company.id(), type, position,
                System.currentTimeMillis());
        assets.put(asset.id(), asset);
        byPosition.put(position.key(), asset.id());
        storage().saveAsset(asset);
        return CompanyResult.ok("asset.registered", "type", type.key(), "id", asset.shortId()).withPayload(asset);
    }

    public CompanyResult removeAsset(UUID assetId) {
        CompanyAsset asset = assets.remove(assetId);
        if (asset == null) {
            return CompanyResult.fail("asset.not_found");
        }
        byPosition.remove(asset.position().key());
        storage().deleteAsset(assetId);
        return CompanyResult.ok("asset.removed", "type", asset.type().key(), "id", asset.shortId())
                .withPayload(asset);
    }

    /** Removes every asset belonging to a company (used when the company is deleted). */
    public List<CompanyAsset> removeAllOf(String companyId) {
        List<CompanyAsset> removed = assetsOf(companyId);
        for (CompanyAsset asset : removed) {
            assets.remove(asset.id());
            byPosition.remove(asset.position().key());
            storage().deleteAsset(asset.id());
        }
        return removed;
    }

    public List<CompanyAsset> assetsOf(String companyId) {
        if (companyId == null) {
            return List.of();
        }
        List<CompanyAsset> result = new ArrayList<>();
        for (CompanyAsset asset : assets.values()) {
            if (asset.companyId().equalsIgnoreCase(companyId)) {
                result.add(asset);
            }
        }
        return result;
    }

    public Optional<CompanyAsset> assetAt(CompanyAsset.BlockPosition position) {
        if (position == null) {
            return Optional.empty();
        }
        UUID id = byPosition.get(position.key());
        return id == null ? Optional.empty() : Optional.ofNullable(assets.get(id));
    }

    public Optional<CompanyAsset> assetById(UUID assetId) {
        return Optional.ofNullable(assets.get(assetId));
    }

    /** Total number of registered assets. */
    public int count() {
        return assets.size();
    }

    /** Whether the player's company role lets them operate the asset. */
    public boolean canUse(UUID playerUuid, CompanyAsset asset) {
        if (playerUuid == null || asset == null) {
            return false;
        }
        return companies.company(asset.companyId())
                .flatMap(company -> company.member(playerUuid))
                .map(member -> member.role().atLeast(asset.type().defaultUseRole()))
                .orElse(false);
    }

    /** Whether the player's company role lets them register/move/remove the asset. */
    public boolean canManage(UUID playerUuid, CompanyAsset asset) {
        if (playerUuid == null || asset == null) {
            return false;
        }
        return companies.company(asset.companyId())
                .flatMap(company -> company.member(playerUuid))
                .map(member -> member.role().grants(CompanyCapability.MANAGE_ASSETS)
                        && member.role().atLeast(asset.type().defaultManageRole()))
                .orElse(false);
    }
}
