package dev.openrp.politics.config;

import java.util.List;

/**
 * A configured government: an identity, an abbreviation used in act ids, the list of charges it owns,
 * a default assignment mechanism and a config-declared active flag. Runtime activation overrides live
 * in {@link dev.openrp.politics.core.GovernmentManager}; this record is the immutable definition.
 *
 * @param id               stable config key, e.g. {@code comune}
 * @param displayName      human name
 * @param sigla            short abbreviation injected into act ids, e.g. {@code COM}
 * @param activeByDefault  whether the government starts active before any admin override
 * @param defaultMechanism default mechanism type for charges that do not declare their own
 * @param chargeIds        the charges that belong to this government
 */
public record Government(String id, String displayName, String sigla, boolean activeByDefault,
                         String defaultMechanism, List<String> chargeIds) {

    public Government {
        sigla = sigla == null || sigla.isBlank() ? id : sigla;
        defaultMechanism = defaultMechanism == null || defaultMechanism.isBlank()
                ? AssignmentMechanism.APPOINTMENT : defaultMechanism;
        chargeIds = chargeIds == null ? List.of() : List.copyOf(chargeIds);
    }
}
