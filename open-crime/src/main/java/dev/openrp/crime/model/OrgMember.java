package dev.openrp.crime.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A member of an {@link IllegalOrg}: a player, the configured rank they hold, when they joined and
 * whether they have turned informant. The {@code roleId} is a free string resolved against the
 * configured hierarchy - the core never hardcodes rank names.
 */
public final class OrgMember {

    private final UUID uuid;
    private String name;
    private String roleId;
    private final long joinedAt;
    private boolean informant;

    public OrgMember(UUID uuid, String name, String roleId, long joinedAt) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
        this.roleId = roleId == null ? "" : roleId;
        this.joinedAt = joinedAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String roleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId == null ? "" : roleId;
    }

    public long joinedAt() {
        return joinedAt;
    }

    public boolean isInformant() {
        return informant;
    }

    public void setInformant(boolean informant) {
        this.informant = informant;
    }
}
