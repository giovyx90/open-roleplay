package dev.openrp.access.model;

import org.bukkit.Location;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AccessRule {

    private final String id;
    private final String profileId;
    private final AccessRuleScope scope;
    private final String world;
    private final Integer x;
    private final Integer y;
    private final Integer z;
    private final AccessPrincipal principal;
    private final Set<AccessAction> actions;
    private final boolean allow;
    private final Instant createdAt;

    public AccessRule(String id, String profileId, AccessRuleScope scope, String world,
                      Integer x, Integer y, Integer z, AccessPrincipal principal,
                      Set<AccessAction> actions, boolean allow, Instant createdAt) {
        this.id = id == null || id.isBlank() ? "rule-" + UUID.randomUUID() : id;
        this.profileId = profileId;
        this.scope = scope == null ? AccessRuleScope.REGION : scope;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.principal = principal == null ? AccessPrincipal.marker() : principal;
        this.actions = normalizeActions(actions);
        this.allow = allow;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static Set<AccessAction> normalizeActions(Set<AccessAction> actions) {
        EnumSet<AccessAction> normalized = actions == null || actions.isEmpty()
                ? EnumSet.copyOf(AccessAction.ALL_ACTIONS)
                : EnumSet.copyOf(actions);
        if (normalized.contains(AccessAction.OPEN)) {
            normalized.addAll(AccessAction.USE_ACTIONS);
        }
        return normalized;
    }

    public static String blockKey(String world, int x, int y, int z) {
        return (world == null ? "" : world.trim().toLowerCase(Locale.ROOT)) + ":" + x + ":" + y + ":" + z;
    }

    public static String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return blockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean appliesTo(Location location) {
        if (scope != AccessRuleScope.BLOCK) {
            return true;
        }
        return location != null
                && location.getWorld() != null
                && world != null
                && world.equalsIgnoreCase(location.getWorld().getName())
                && x != null && x == location.getBlockX()
                && y != null && y == location.getBlockY()
                && z != null && z == location.getBlockZ();
    }

    public boolean matches(AccessAction action, Set<AccessPrincipal> actualPrincipals) {
        if (!actions.contains(action)) {
            return false;
        }
        if (principal.type() == AccessPrincipalType.OVERRIDE_MARKER) {
            return false;
        }
        if (principal.type() == AccessPrincipalType.PUBLIC) {
            return true;
        }
        if (actualPrincipals == null) {
            return false;
        }
        return actualPrincipals.stream().anyMatch(principal::matches);
    }

    public String getId() {
        return id;
    }

    public String getProfileId() {
        return profileId;
    }

    public AccessRuleScope getScope() {
        return scope;
    }

    public String getWorld() {
        return world;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getZ() {
        return z;
    }

    public AccessPrincipal getPrincipal() {
        return principal;
    }

    public Set<AccessAction> getActions() {
        return EnumSet.copyOf(actions);
    }

    public boolean isAllow() {
        return allow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
