package dev.openrp.companies.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import dev.openrp.companies.core.CompanyResult;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyCapability;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.companies.model.PendingInvite;

/**
 * Companies and their membership. Every mutating method runs the same validated, locked path the
 * commands use and returns a {@link CompanyResult} describing the outcome (success flag, message key,
 * optional payload), so calling it programmatically is just as safe as typing the command.
 */
public interface CompanyService {

    // --- queries -----------------------------------------------------------------------------

    Optional<Company> findById(String companyId);

    /** Finds a company by its id slug or (case-insensitively) by its display name. */
    Optional<Company> findByName(String name);

    /** All companies the player is a member of. */
    List<Company> findByPlayer(UUID playerUuid);

    Collection<Company> allCompanies();

    boolean exists(String companyId);

    Optional<CompanyRole> roleOf(UUID playerUuid, String companyId);

    /** Whether the player's role in the company grants the capability (CEO/ADMIN implies all). */
    boolean hasCapability(UUID playerUuid, String companyId, CompanyCapability capability);

    Optional<PendingInvite> pendingInvite(UUID playerUuid);

    // --- lifecycle ---------------------------------------------------------------------------

    /**
     * Canonical creation used by the admin flow, application approval and the API. Enforces name and
     * type validity and uniqueness; does not apply the per-player limit, cooldown or fee. The owner
     * becomes CEO.
     */
    CompanyResult createCompany(UUID ownerUuid, String ownerName, String displayName, String type);

    /**
     * Player self-service creation. Only succeeds when {@code creation.mode} is {@code PLAYER_DIRECT};
     * additionally enforces the per-player limit, the creation cooldown and the (optional) fee through
     * the economy adapter. The creator becomes CEO.
     */
    CompanyResult createCompanyForPlayer(Player creator, String displayName, String type);

    CompanyResult deleteCompany(String companyId);

    // --- membership --------------------------------------------------------------------------

    /** Invites a player to the company. {@code inviterUuid} may be {@code null} for API/admin invites. */
    CompanyResult inviteMember(String companyId, UUID inviterUuid, UUID targetUuid, String targetName, CompanyRole role);

    CompanyResult acceptInvite(UUID playerUuid, String playerName);

    CompanyResult denyInvite(UUID playerUuid);

    /** Removes a member. When {@code actorUuid == targetUuid} this is a voluntary leave. */
    CompanyResult removeMember(String companyId, UUID actorUuid, UUID targetUuid);

    CompanyResult changeRole(String companyId, UUID actorUuid, UUID targetUuid, CompanyRole newRole);

    /** Transfers ownership; the new owner becomes CEO and the previous owner is demoted to DIRECTOR. */
    CompanyResult transferOwnership(String companyId, UUID newOwnerUuid, String newOwnerName);
}
