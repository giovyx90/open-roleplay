package dev.openrp.vending.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RestockModeTest {

    @Test
    public void parsesKnownValuesCaseInsensitively() {
        assertEquals(RestockMode.PLAYER_INVENTORY, RestockMode.fromString("player_inventory"));
        assertEquals(RestockMode.BUSINESS_WAREHOUSE, RestockMode.fromString("BUSINESS_WAREHOUSE"));
        assertEquals(RestockMode.FREE, RestockMode.fromString("free"));
    }

    @Test
    public void defaultsToPlayerInventory() {
        assertEquals(RestockMode.PLAYER_INVENTORY, RestockMode.fromString("nonsense"));
        assertEquals(RestockMode.PLAYER_INVENTORY, RestockMode.fromString(null));
    }
}
