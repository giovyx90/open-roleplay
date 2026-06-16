package dev.openrp.companies.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A player's membership in a company: their role, optional salary and when they joined. Pure data
 * (no Bukkit types) so it can be unit-tested and persisted by any storage adapter. The player name is
 * cached for display and refreshed opportunistically when the player is seen online.
 */
public final class CompanyMember {

    private final String companyId;
    private final UUID playerUuid;
    private String playerName;
    private CompanyRole role;
    private double salary;
    private final long joinedAt;

    public CompanyMember(String companyId, UUID playerUuid, String playerName, CompanyRole role,
                         double salary, long joinedAt) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.playerName = playerName;
        this.role = Objects.requireNonNull(role, "role");
        this.salary = Math.max(0.0, salary);
        this.joinedAt = joinedAt;
    }

    public String companyId() {
        return companyId;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName == null ? playerUuid.toString() : playerName;
    }

    public void setPlayerName(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            this.playerName = playerName;
        }
    }

    public CompanyRole role() {
        return role;
    }

    public void setRole(CompanyRole role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public double salary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = Math.max(0.0, salary);
    }

    public long joinedAt() {
        return joinedAt;
    }
}
