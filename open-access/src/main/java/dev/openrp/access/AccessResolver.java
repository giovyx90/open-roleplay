package dev.openrp.access;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessCheckResult;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessPrincipalType;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessRule;

import java.util.Collection;
import java.util.Set;

public class AccessResolver {

    public AccessCheckResult resolve(AccessProfile profile,
                                     Collection<AccessRule> regionRules,
                                     Collection<AccessRule> blockRules,
                                     Set<AccessPrincipal> principals,
                                     AccessAction action,
                                     boolean bypass,
                                     boolean managerImplicit) {
        if (profile == null) {
            return AccessCheckResult.pass("Nessun profilo accesso copre questa posizione.");
        }
        if (!profile.isEnabled()) {
            return AccessCheckResult.deny("Questo profilo accesso e' disabilitato.");
        }
        if (bypass) {
            return AccessCheckResult.allow("Permesso bypass.");
        }
        if (managerImplicit) {
            return AccessCheckResult.allow("Accesso manager.");
        }

        Collection<AccessRule> safeBlockRules = blockRules == null ? Set.of() : blockRules;
        if (!safeBlockRules.isEmpty()) {
            return matchingRuleAllows(safeBlockRules, principals, action)
                    ? AccessCheckResult.allow("Regola blocco.")
                    : AccessCheckResult.deny("Non hai accesso a questo blocco.");
        }

        if (matchingRuleAllows(regionRules == null ? Set.of() : regionRules, principals, action)) {
            return AccessCheckResult.allow("Regola regione.");
        }

        if (presetAllows(profile.getDefaultPreset(), principals, action)) {
            return AccessCheckResult.allow("Preset regione.");
        }

        if (hasOwnerPrincipal(principals)) {
            return AccessCheckResult.allow("Accesso proprietario.");
        }

        return AccessCheckResult.deny("Non hai accesso qui.");
    }

    private boolean matchingRuleAllows(Collection<AccessRule> rules, Set<AccessPrincipal> principals, AccessAction action) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (AccessRule rule : rules) {
            if (rule != null && rule.isAllow() && rule.matches(action, principals)) {
                return true;
            }
        }
        return false;
    }

    private boolean presetAllows(AccessPreset preset, Set<AccessPrincipal> principals, AccessAction action) {
        AccessPreset safePreset = preset == null ? AccessPreset.PRIVATE : preset;
        if (!AccessAction.USE_ACTIONS.contains(action)) {
            return false;
        }
        return switch (safePreset) {
            case PUBLIC -> true;
            case MEMBERS -> hasAny(principals,
                    AccessPrincipalType.PROPERTY_MEMBER,
                    AccessPrincipalType.COMPANY_MEMBER,
                    AccessPrincipalType.COMPANY_MANAGER,
                    AccessPrincipalType.COMPANY_DIRECTOR,
                    AccessPrincipalType.COMPANY_OWNER,
                    AccessPrincipalType.HOTEL_GUEST);
            case MANAGERS -> hasAny(principals,
                    AccessPrincipalType.COMPANY_MANAGER,
                    AccessPrincipalType.COMPANY_DIRECTOR,
                    AccessPrincipalType.COMPANY_OWNER,
                    AccessPrincipalType.PROPERTY_OWNER);
            case PRIVATE, CUSTOM -> false;
        };
    }

    private boolean hasOwnerPrincipal(Set<AccessPrincipal> principals) {
        return hasAny(principals, AccessPrincipalType.PROPERTY_OWNER, AccessPrincipalType.COMPANY_OWNER);
    }

    private boolean hasAny(Set<AccessPrincipal> principals, AccessPrincipalType... types) {
        if (principals == null || principals.isEmpty()) {
            return false;
        }
        for (AccessPrincipal principal : principals) {
            if (principal == null) {
                continue;
            }
            for (AccessPrincipalType type : types) {
                if (principal.type() == type) {
                    return true;
                }
            }
        }
        return false;
    }
}
