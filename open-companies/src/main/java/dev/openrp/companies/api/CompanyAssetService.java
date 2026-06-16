package dev.openrp.companies.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.CompanyAsset;
import dev.openrp.companies.model.CompanyAssetType;

/**
 * Registry of physical company assets (terminals, POS, printers, safes, ...). The company core only
 * tracks where an asset is and who owns it, plus who may use or manage it; the behaviour of an asset
 * is implemented by vertical modules that resolve it here, typically via {@link #assetAt}.
 */
public interface CompanyAssetService {

    /** Registers a new asset for a company at a block position. {@code actorUuid} needs MANAGE_ASSETS. */
    CompanyResult registerAsset(String companyId, UUID actorUuid, CompanyAssetType type,
                                String world, int x, int y, int z);

    CompanyResult removeAsset(UUID assetId, UUID actorUuid);

    List<CompanyAsset> assetsOf(String companyId);

    Optional<CompanyAsset> assetAt(String world, int x, int y, int z);

    Optional<CompanyAsset> assetById(UUID assetId);

    /** Whether the player's company role lets them operate the asset. */
    boolean canUseAsset(UUID playerUuid, CompanyAsset asset);

    /** Whether the player's company role lets them register/move/remove the asset. */
    boolean canManageAsset(UUID playerUuid, CompanyAsset asset);
}
