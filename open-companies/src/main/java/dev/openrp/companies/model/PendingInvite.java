package dev.openrp.companies.model;

import java.util.UUID;

/**
 * A standing invitation for a player to join a company at a given role. Transient by design (kept in
 * memory, not persisted): if the server restarts before the player accepts, the invite is simply
 * re-sent. A player holds at most one pending invite at a time; a newer invite replaces an older one.
 *
 * @param companyId   company the player is invited to
 * @param role        role they will hold on accepting
 * @param inviterUuid who sent the invite, or {@code null} for API/admin invites
 * @param createdAt   epoch millis the invite was issued
 */
public record PendingInvite(String companyId, CompanyRole role, UUID inviterUuid, long createdAt) {
}
