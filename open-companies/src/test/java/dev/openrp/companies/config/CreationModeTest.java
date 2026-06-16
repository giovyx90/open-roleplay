package dev.openrp.companies.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CreationModeTest {

    @Test
    public void parsesKnownModes() {
        assertEquals(CreationMode.PLAYER_DIRECT, CreationMode.fromString("PLAYER_DIRECT"));
        assertEquals(CreationMode.PLAYER_APPLICATION, CreationMode.fromString("player_application"));
        assertEquals(CreationMode.ADMIN_ONLY, CreationMode.fromString("Admin_Only"));
    }

    @Test
    public void unknownFallsBackToPlayerDirect() {
        assertEquals(CreationMode.PLAYER_DIRECT, CreationMode.fromString("nonsense"));
        assertEquals(CreationMode.PLAYER_DIRECT, CreationMode.fromString(null));
        assertEquals(CreationMode.PLAYER_DIRECT, CreationMode.fromString("  "));
    }

    @Test
    public void gatingFlags() {
        assertTrue(CreationMode.PLAYER_DIRECT.allowsDirectPlayerCreate());
        assertFalse(CreationMode.ADMIN_ONLY.allowsDirectPlayerCreate());
        assertFalse(CreationMode.PLAYER_APPLICATION.allowsDirectPlayerCreate());

        assertTrue(CreationMode.PLAYER_APPLICATION.allowsApplication());
        assertFalse(CreationMode.PLAYER_DIRECT.allowsApplication());
        assertFalse(CreationMode.ADMIN_ONLY.allowsApplication());
    }
}
