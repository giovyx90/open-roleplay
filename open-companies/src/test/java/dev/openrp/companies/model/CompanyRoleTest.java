package dev.openrp.companies.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompanyRoleTest {

    @Test
    public void levelsFollowTheHierarchy() {
        assertEquals(6, CompanyRole.CEO.level());
        assertEquals(5, CompanyRole.DIRECTOR.level());
        assertEquals(4, CompanyRole.VICE_DIRECTOR.level());
        assertEquals(3, CompanyRole.MANAGER.level());
        assertEquals(2, CompanyRole.EMPLOYEE.level());
        assertEquals(1, CompanyRole.TRAINING.level());
    }

    @Test
    public void ceoGrantsEveryCapability() {
        for (CompanyCapability capability : CompanyCapability.values()) {
            assertTrue("CEO should grant " + capability, CompanyRole.CEO.grants(capability));
        }
    }

    @Test
    public void capabilitiesAreMappedPerRole() {
        assertTrue(CompanyRole.TRAINING.grants(CompanyCapability.VIEW));
        assertFalse(CompanyRole.TRAINING.grants(CompanyCapability.FIRE));

        assertTrue(CompanyRole.EMPLOYEE.grants(CompanyCapability.USE_ASSETS));
        assertFalse(CompanyRole.EMPLOYEE.grants(CompanyCapability.INVITE));

        assertTrue(CompanyRole.MANAGER.grants(CompanyCapability.INVITE));
        assertTrue(CompanyRole.MANAGER.grants(CompanyCapability.MANAGE_ASSETS));
        assertFalse(CompanyRole.MANAGER.grants(CompanyCapability.FIRE));
        assertFalse(CompanyRole.MANAGER.grants(CompanyCapability.MANAGE_LICENSES));

        assertTrue(CompanyRole.VICE_DIRECTOR.grants(CompanyCapability.FIRE));
        assertTrue(CompanyRole.VICE_DIRECTOR.grants(CompanyCapability.CHANGE_ROLE));
        assertFalse(CompanyRole.VICE_DIRECTOR.grants(CompanyCapability.MANAGE_FINANCE));

        assertTrue(CompanyRole.DIRECTOR.grants(CompanyCapability.MANAGE_LICENSES));
        assertTrue(CompanyRole.DIRECTOR.grants(CompanyCapability.MANAGE_FINANCE));
        assertFalse(CompanyRole.DIRECTOR.grants(CompanyCapability.ADMIN));
    }

    @Test
    public void rankComparisons() {
        assertTrue(CompanyRole.DIRECTOR.atLeast(CompanyRole.DIRECTOR));
        assertTrue(CompanyRole.DIRECTOR.atLeast(CompanyRole.MANAGER));
        assertFalse(CompanyRole.MANAGER.atLeast(CompanyRole.DIRECTOR));
        assertTrue(CompanyRole.MANAGER.outranks(CompanyRole.EMPLOYEE));
        assertFalse(CompanyRole.MANAGER.outranks(CompanyRole.MANAGER));
    }

    @Test
    public void fromStringIsLenient() {
        assertEquals(CompanyRole.MANAGER, CompanyRole.fromString("manager"));
        assertEquals(CompanyRole.VICE_DIRECTOR, CompanyRole.fromString("VICE_DIRECTOR"));
        assertNull(CompanyRole.fromString("nonsense"));
        assertNull(CompanyRole.fromString(null));
    }
}
