package dev.openrp.access;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessCheckResult;
import dev.openrp.access.model.AccessDecision;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessPrincipalType;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessProfileType;
import dev.openrp.access.model.AccessRule;
import dev.openrp.access.model.AccessRuleScope;
import org.junit.Test;

import java.time.Instant;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccessResolverTest {

    private final AccessResolver resolver = new AccessResolver();

    @Test
    public void blockOverrideWinsOverRegionPreset() {
        AccessProfile profile = profile(AccessPreset.PUBLIC);
        AccessRule marker = new AccessRule("marker", profile.getId(), AccessRuleScope.BLOCK,
                "world", 1, 2, 3, AccessPrincipal.marker(), AccessAction.ALL_ACTIONS, true, Instant.now());

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(marker),
                Set.of(AccessPrincipal.publicAccess()), AccessAction.OPEN, false, false);

        assertEquals(AccessDecision.DENY, result.decision());
    }

    @Test
    public void explicitPlayerRuleAllowsPrivateProfile() {
        AccessProfile profile = profile(AccessPreset.PRIVATE);
        UUID playerId = UUID.randomUUID();
        AccessRule rule = new AccessRule("rule", profile.getId(), AccessRuleScope.REGION,
                null, null, null, null, AccessPrincipal.player(playerId),
                EnumSet.of(AccessAction.CONTAINER), true, Instant.now());

        AccessCheckResult result = resolver.resolve(profile, List.of(rule), List.of(),
                Set.of(AccessPrincipal.publicAccess(), AccessPrincipal.player(playerId)),
                AccessAction.CONTAINER, false, false);

        assertEquals(AccessDecision.ALLOW, result.decision());
    }

    @Test
    public void serviceIncludesExplicitPlayerPrincipal() {
        AccessProfile profile = profile(AccessPreset.PRIVATE);
        UUID playerId = UUID.randomUUID();
        AccessService service = new AccessService(null, null, resolver);

        Set<AccessPrincipal> principals = service.principalsFor(profile, mockPlayer(playerId, "TrustedPlayer"));

        assertTrue(principals.contains(AccessPrincipal.player(playerId)));
    }

    @Test
    public void openRuleAllowsAllNormalUseActions() {
        AccessProfile profile = profile(AccessPreset.PRIVATE);
        UUID playerId = UUID.randomUUID();
        AccessRule rule = new AccessRule("rule", profile.getId(), AccessRuleScope.REGION,
                null, null, null, null, AccessPrincipal.player(playerId),
                EnumSet.of(AccessAction.OPEN), true, Instant.now());
        Set<AccessPrincipal> principals = Set.of(AccessPrincipal.publicAccess(), AccessPrincipal.player(playerId));

        assertEquals(AccessDecision.ALLOW, resolver.resolve(profile, List.of(rule), List.of(),
                principals, AccessAction.OPEN, false, false).decision());
        assertEquals(AccessDecision.ALLOW, resolver.resolve(profile, List.of(rule), List.of(),
                principals, AccessAction.CONTAINER, false, false).decision());
        assertEquals(AccessDecision.ALLOW, resolver.resolve(profile, List.of(rule), List.of(),
                principals, AccessAction.SIGNAL, false, false).decision());
        assertEquals(AccessDecision.ALLOW, resolver.resolve(profile, List.of(rule), List.of(),
                principals, AccessAction.MACHINE, false, false).decision());
    }

    @Test
    public void manageRuleAllowsManageAction() {
        AccessProfile profile = profile(AccessPreset.PRIVATE);
        UUID playerId = UUID.randomUUID();
        AccessRule rule = new AccessRule("rule", profile.getId(), AccessRuleScope.REGION,
                null, null, null, null, AccessPrincipal.player(playerId),
                EnumSet.of(AccessAction.MANAGE), true, Instant.now());

        AccessCheckResult result = resolver.resolve(profile, List.of(rule), List.of(),
                Set.of(AccessPrincipal.publicAccess(), AccessPrincipal.player(playerId)),
                AccessAction.MANAGE, false, false);

        assertEquals(AccessDecision.ALLOW, result.decision());
    }

    @Test
    public void companyMemberPresetAllowsUseAction() {
        AccessProfile profile = profile(AccessPreset.MEMBERS);

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(),
                Set.of(new AccessPrincipal(AccessPrincipalType.COMPANY_MEMBER, "*")),
                AccessAction.OPEN, false, false);

        assertEquals(AccessDecision.ALLOW, result.decision());
    }

    @Test
    public void hotelGuestPresetAllowsUseAction() {
        AccessProfile profile = profile(AccessPreset.MEMBERS);

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(),
                Set.of(new AccessPrincipal(AccessPrincipalType.HOTEL_GUEST, "*")),
                AccessAction.CONTAINER, false, false);

        assertEquals(AccessDecision.ALLOW, result.decision());
    }

    @Test
    public void memberPresetDoesNotAllowBreak() {
        AccessProfile profile = profile(AccessPreset.MEMBERS);

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(),
                Set.of(new AccessPrincipal(AccessPrincipalType.COMPANY_MEMBER, "*")),
                AccessAction.BREAK, false, false);

        assertEquals(AccessDecision.DENY, result.decision());
    }

    @Test
    public void managerImplicitAllowsSensitiveActions() {
        AccessProfile profile = profile(AccessPreset.PRIVATE);

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(),
                Set.of(), AccessAction.MANAGE, false, true);

        assertEquals(AccessDecision.ALLOW, result.decision());
    }

    @Test
    public void disabledProfileDeniesEvenWithBypass() {
        AccessProfile profile = new AccessProfile("profile", AccessProfileType.REGION, "world", "spawn",
                null, null, null, "Spawn", AccessPreset.PRIVATE, false, Instant.now(), Instant.now());

        AccessCheckResult result = resolver.resolve(profile, List.of(), List.of(),
                Set.of(), AccessAction.OPEN, true, false);

        assertEquals(AccessDecision.DENY, result.decision());
    }

    private AccessProfile profile(AccessPreset preset) {
        return new AccessProfile("profile", AccessProfileType.REGION, "world", "spawn",
                null, null, null, "Spawn", preset, true, Instant.now(), Instant.now());
    }

    private org.bukkit.entity.Player mockPlayer(UUID uuid, String name) {
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getName" -> name;
                    case "hasPermission" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
