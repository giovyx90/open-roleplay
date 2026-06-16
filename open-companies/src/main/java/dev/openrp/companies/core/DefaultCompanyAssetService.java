package dev.openrp.companies.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.api.CompanyAssetService;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyAssetType;
import dev.openrp.companies.model.CompanyCapability;

/**
 * Bukkit-facing implementation of {@link CompanyAssetService}. Adds company-level authorization
 * (MANAGE_ASSETS to register/remove) and per-company locking on top of the pure {@link AssetManager},
 * and writes the audit log. Read queries and the {@code canUse}/{@code canManage} checks pass straight
 * through so other modules can ask them cheaply and often.
 */
public final class DefaultCompanyAssetService implements CompanyAssetService {

    private final OpenCompaniesPlugin plugin;

    public DefaultCompanyAssetService(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    private AssetManager assets() {
        return plugin.assetManager();
    }

    @Override
    public CompanyResult registerAsset(String companyId, UUID actorUuid, CompanyAssetType type,
                                       String world, int x, int y, int z) {
        if (actorUuid != null
                && !plugin.companyManager().hasCapability(actorUuid, companyId, CompanyCapability.MANAGE_ASSETS)) {
            return CompanyResult.fail("member.no_permission");
        }
        ReentrantLock lock = plugin.locks().get(companyId == null ? "" : companyId.toLowerCase(java.util.Locale.ROOT));
        lock.lock();
        try {
            CompanyResult result = assets().registerAsset(companyId, type,
                    new CompanyAsset.BlockPosition(world, x, y, z));
            if (result.success()) {
                plugin.adapters().logging().log("ASSET",
                        "Registered " + type.key() + " for '" + companyId + "' at " + world + " " + x + "," + y + "," + z);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompanyResult removeAsset(UUID assetId, UUID actorUuid) {
        CompanyAsset asset = assets().assetById(assetId).orElse(null);
        if (asset == null) {
            return CompanyResult.fail("asset.not_found");
        }
        if (actorUuid != null && !assets().canManage(actorUuid, asset)) {
            return CompanyResult.fail("member.no_permission");
        }
        ReentrantLock lock = plugin.locks().get(asset.companyId().toLowerCase(java.util.Locale.ROOT));
        lock.lock();
        try {
            CompanyResult result = assets().removeAsset(assetId);
            if (result.success()) {
                plugin.adapters().logging().log("ASSET",
                        "Removed " + asset.type().key() + " (" + asset.shortId() + ") of '" + asset.companyId() + "'");
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<CompanyAsset> assetsOf(String companyId) {
        return assets().assetsOf(companyId);
    }

    @Override
    public Optional<CompanyAsset> assetAt(String world, int x, int y, int z) {
        return assets().assetAt(new CompanyAsset.BlockPosition(world, x, y, z));
    }

    @Override
    public Optional<CompanyAsset> assetById(UUID assetId) {
        return assets().assetById(assetId);
    }

    @Override
    public boolean canUseAsset(UUID playerUuid, CompanyAsset asset) {
        return assets().canUse(playerUuid, asset);
    }

    @Override
    public boolean canManageAsset(UUID playerUuid, CompanyAsset asset) {
        return assets().canManage(playerUuid, asset);
    }
}
