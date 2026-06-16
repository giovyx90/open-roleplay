package dev.openrp.companies.adapter;

import org.bukkit.entity.Player;
import dev.openrp.companies.model.Company;
import dev.openrp.companies.model.CompanyMember;

/**
 * Optional bridge for reflecting a player's company in their on-server identity (tab list, chat
 * prefix/suffix, name plate, ...). The default {@code NoopIdentityAdapter} does nothing, so the core
 * never imposes a particular prefix system or pulls in LuckPerms. A server that wants company prefixes
 * provides its own adapter and decides exactly how the identity is rendered.
 */
public interface IdentityAdapter {

    String id();

    /** Apply the company identity for a member who just joined / changed role / logged in. */
    void applyIdentity(Player player, Company company, CompanyMember member);

    /** Remove any company identity previously applied (member left / company dissolved). */
    void clearIdentity(Player player, Company company);
}
