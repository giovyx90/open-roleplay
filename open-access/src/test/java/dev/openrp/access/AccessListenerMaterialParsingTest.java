package dev.openrp.access;

import org.bukkit.Material;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessListenerMaterialParsingTest {
    @Test
    public void parsesConfiguredInteractiveMaterials() {
        Set<Material> materials = AccessListener.parseConfiguredMaterials(List.of(
                "note_block",
                "LIGHT",
                "not_a_material",
                "",
                "barrier"
        ));

        assertTrue(materials.contains(Material.NOTE_BLOCK));
        assertTrue(materials.contains(Material.LIGHT));
        assertTrue(materials.contains(Material.BARRIER));
        assertFalse(materials.contains(Material.CHEST));
    }
}
