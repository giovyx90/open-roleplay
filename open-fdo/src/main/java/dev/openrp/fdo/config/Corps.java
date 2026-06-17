package dev.openrp.fdo.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A configured authority (a "corps"). The core knows only its id, its display label, the short code
 * used in id patterns, and an abstract set of other corps it has jurisdiction over (so a body can
 * request another's service sheets). The display name is the only place a setting's flavour lives -
 * the core never hardcodes "Police" or "City Guard".
 */
public record Corps(String id, String displayName, String sigla, Set<String> jurisdictionOver) {

    public Corps {
        jurisdictionOver = jurisdictionOver == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(jurisdictionOver));
    }

    public boolean hasJurisdictionOver(String otherCorpsId) {
        return otherCorpsId != null && jurisdictionOver.contains(otherCorpsId);
    }
}
