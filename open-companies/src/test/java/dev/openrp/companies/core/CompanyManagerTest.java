package dev.openrp.companies.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import dev.openrp.companies.adapter.AdapterRegistry;
import dev.openrp.companies.adapter.defaults.MemoryStorageAdapter;
import dev.openrp.companies.config.CompaniesSettings;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyRole;

/**
 * Exercises the pure rule engine end to end with the in-memory storage adapter: creation modes,
 * limits, name uniqueness, the invite/accept flow, role changes and member removal.
 */
public class CompanyManagerTest {

    private static CompaniesSettings settings(String mode, int maxOwned, int cooldownSeconds) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("companies.creation.mode", mode);
        config.set("companies.creation.max-owned-per-player", maxOwned);
        config.set("companies.creation.cooldown-seconds", cooldownSeconds);
        config.set("companies.creation.max-members-per-company", 30);
        config.set("companies.creation.allowed-types", List.of("food", "generic", "retail"));
        config.set("companies.creation.name.reserved", List.of("admin"));
        CompaniesSettings settings = new CompaniesSettings();
        settings.load(config);
        return settings;
    }

    private static CompanyManager manager(CompaniesSettings settings) {
        AdapterRegistry adapters = new AdapterRegistry();
        adapters.setStorage(new MemoryStorageAdapter());
        return new CompanyManager(settings, new CompanyValidator(settings), adapters);
    }

    @Test
    public void playerDirectCreatesCompanyWithOwnerAsCeo() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 1, 0));
        UUID owner = UUID.randomUUID();

        CompanyResult result = manager.createForPlayer(owner, "Owner", "Acme Foods", "food");

        assertTrue(result.success());
        Company company = result.company().orElseThrow();
        assertEquals("acme-foods", company.id());
        assertEquals(owner, company.ownerUuid());
        assertEquals(CompanyRole.CEO, company.member(owner).orElseThrow().role());
        assertTrue(manager.exists("acme-foods"));
    }

    @Test
    public void adminOnlyBlocksPlayerCreationButAllowsAdminCreation() {
        CompanyManager manager = manager(settings("ADMIN_ONLY", 5, 0));
        UUID owner = UUID.randomUUID();

        CompanyResult blocked = manager.createForPlayer(owner, "Owner", "Acme", "generic");
        assertTrue(blocked.failed());
        assertEquals("creation.player_disabled", blocked.messageKey());
        assertTrue(manager.all().isEmpty());

        CompanyResult allowed = manager.createAsAdmin(owner, "Owner", "Acme", "generic");
        assertTrue(allowed.success());
        assertEquals(1, manager.all().size());
    }

    @Test
    public void perPlayerLimitIsEnforced() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 1, 0));
        UUID owner = UUID.randomUUID();

        assertTrue(manager.createForPlayer(owner, "Owner", "Acme", "generic").success());
        CompanyResult second = manager.createForPlayer(owner, "Owner", "Beta", "generic");
        assertTrue(second.failed());
        assertEquals("creation.limit_reached", second.messageKey());
    }

    @Test
    public void duplicateNamesAreRejected() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 5, 0));
        assertTrue(manager.createAsAdmin(UUID.randomUUID(), "A", "Acme", "generic").success());

        CompanyResult duplicate = manager.createAsAdmin(UUID.randomUUID(), "B", "acme", "generic");
        assertTrue(duplicate.failed());
        assertEquals("creation.name_taken", duplicate.messageKey());
    }

    @Test
    public void inviteThenAcceptAddsTheMember() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 5, 0));
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Company company = manager.createAsAdmin(owner, "Owner", "Acme", "generic").company().orElseThrow();

        CompanyResult invited = manager.invite(company.id(), owner, target, "Target", CompanyRole.EMPLOYEE);
        assertTrue(invited.success());
        assertTrue(manager.pendingInvite(target).isPresent());

        CompanyResult accepted = manager.acceptInvite(target, "Target");
        assertTrue(accepted.success());
        assertEquals(2, company.memberCount());
        assertEquals(CompanyRole.EMPLOYEE, company.member(target).orElseThrow().role());
        assertTrue(manager.pendingInvite(target).isEmpty());
    }

    @Test
    public void inviteRespectsCapabilityAndRank() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 5, 0));
        UUID owner = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        Company company = manager.createAsAdmin(owner, "Owner", "Acme", "generic").company().orElseThrow();
        manager.invite(company.id(), owner, employee, "Employee", CompanyRole.EMPLOYEE);
        manager.acceptInvite(employee, "Employee");

        CompanyResult byEmployee = manager.invite(company.id(), employee, outsider, "Outsider", CompanyRole.TRAINING);
        assertTrue(byEmployee.failed());
        assertEquals("member.no_permission", byEmployee.messageKey());

        CompanyResult asCeo = manager.invite(company.id(), owner, outsider, "Outsider", CompanyRole.CEO);
        assertTrue(asCeo.failed());
        assertEquals("role.cannot_assign_ceo", asCeo.messageKey());
    }

    @Test
    public void changeRolePromotesAndProtectsOwnerAndCeo() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 5, 0));
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Company company = manager.createAsAdmin(owner, "Owner", "Acme", "generic").company().orElseThrow();
        manager.invite(company.id(), owner, member, "Member", CompanyRole.EMPLOYEE);
        manager.acceptInvite(member, "Member");

        CompanyResult promote = manager.changeRole(company.id(), owner, member, CompanyRole.MANAGER);
        assertTrue(promote.success());
        assertEquals(CompanyRole.MANAGER, company.member(member).orElseThrow().role());

        assertEquals("role.cannot_assign_ceo",
                manager.changeRole(company.id(), owner, member, CompanyRole.CEO).messageKey());
        assertEquals("role.cannot_change_owner",
                manager.changeRole(company.id(), owner, owner, CompanyRole.MANAGER).messageKey());
    }

    @Test
    public void removeMemberFiresButProtectsOwner() {
        CompanyManager manager = manager(settings("PLAYER_DIRECT", 5, 0));
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Company company = manager.createAsAdmin(owner, "Owner", "Acme", "generic").company().orElseThrow();
        manager.invite(company.id(), owner, member, "Member", CompanyRole.EMPLOYEE);
        manager.acceptInvite(member, "Member");

        CompanyResult fired = manager.removeMember(company.id(), owner, member);
        assertTrue(fired.success());
        assertFalse(company.isMember(member));

        CompanyResult removeOwner = manager.removeMember(company.id(), owner, owner);
        assertTrue(removeOwner.failed());
        assertEquals("member.cannot_remove_owner", removeOwner.messageKey());
    }
}
