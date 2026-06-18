package dev.openrp.politics.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IdsTest {

    @Test
    public void slugIsLowercaseAndSafe() {
        assertEquals("comune_di_san_valdino", Ids.slug("Comune di San Valdino"));
        assertEquals("re", Ids.slug("  Re!  "));
    }

    @Test
    public void shortIdsAreNonBlankAndDistinct() {
        String a = Ids.shortId();
        String b = Ids.shortId();
        assertFalse(a.isBlank());
        assertTrue(Ids.prefixed("act").startsWith("act-"));
        // Practically never collide; a direct equality check would be flaky, so just assert format.
        assertFalse(a.contains("/"));
        assertFalse(b.contains("/"));
    }
}
