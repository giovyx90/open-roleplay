package dev.openrp.crime.adapter.defaults;

import java.util.Optional;
import java.util.UUID;
import dev.openrp.crime.adapter.CompanyAdapter;

/**
 * Default company adapter when no company plugin is present: reports no companies and cannot charge
 * anyone. The racket subsystem degrades cleanly - {@code /racket imponi} simply finds no company.
 */
public final class NoopCompanyAdapter implements CompanyAdapter {

    @Override
    public String id() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<String> companyOwnedBy(UUID owner) {
        return Optional.empty();
    }

    @Override
    public boolean isOwner(UUID player, String companyId) {
        return false;
    }

    @Override
    public String companyName(String companyId) {
        return companyId;
    }

    @Override
    public void applyReputationMalus(String companyId, int malus) {
        // no-op
    }

    @Override
    public boolean chargeCompany(String companyId, long amount) {
        return false;
    }
}
