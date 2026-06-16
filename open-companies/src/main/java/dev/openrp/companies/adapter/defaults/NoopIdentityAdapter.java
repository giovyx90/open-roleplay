package dev.openrp.companies.adapter.defaults;

import org.bukkit.entity.Player;
import dev.openrp.companies.adapter.IdentityAdapter;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;

/**
 * Default identity adapter: does nothing. The core never forces a prefix/suffix system or pulls in
 * LuckPerms; a server that wants company prefixes in tab/chat provides its own {@link IdentityAdapter}.
 */
public final class NoopIdentityAdapter implements IdentityAdapter {

    @Override
    public String id() {
        return "noop";
    }

    @Override
    public void applyIdentity(Player player, Company company, CompanyMember member) {
        // intentionally empty - identity integration is opt-in
    }

    @Override
    public void clearIdentity(Player player, Company company) {
        // intentionally empty
    }
}
