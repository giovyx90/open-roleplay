package dev.openrp.vending.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bukkit.Material;
import org.junit.Test;

public class ProductDefinitionTest {

    @Test
    public void normalizesAndDefaultsFields() {
        ProductDefinition product = new ProductDefinition("  CHIPS ", "", null, 0, -5.0, 0, -2, null);
        assertEquals("chips", product.id());
        assertEquals("chips", product.displayName());
        assertEquals(Material.PAPER, product.material());
        assertEquals(1, product.amount());
        assertEquals(0.0, product.defaultPrice(), 1e-9);
        assertEquals(1, product.defaultMaxStock());
        assertEquals(0, product.customModelData());
        assertTrue(product.lore().isEmpty());
    }

    @Test
    public void plainNameStripsMiniMessageTags() {
        ProductDefinition product = new ProductDefinition("x", "<gold>Gold Bar</gold>", Material.PAPER, 1, 1.0, 10, 0, null);
        assertEquals("Gold Bar", product.plainName());
    }
}
