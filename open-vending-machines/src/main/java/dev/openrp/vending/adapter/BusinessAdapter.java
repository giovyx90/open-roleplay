package dev.openrp.vending.adapter;

import java.util.Optional;
import org.bukkit.OfflinePlayer;

/**
 * Bridge to a server's company/organisation system. Resolves companies, membership, roles, machine
 * limits and the account a company's cash is paid into. The core stores only a company <em>id</em>
 * string on each machine and asks this adapter everything else.
 */
public interface BusinessAdapter {

    String id();

    boolean companyExists(String companyId);

    Optional<String> companyDisplayName(String companyId);

    boolean isMember(OfflinePlayer player, String companyId);

    Optional<String> roleOf(OfflinePlayer player, String companyId);

    /** Whether the player's role in the company grants the given capability. */
    boolean hasCapability(OfflinePlayer player, String companyId, BusinessCapability capability);

    /** Maximum machines this company may own; {@code -1} means unlimited. */
    int machineLimit(String companyId);

    /** Account id (for the {@code EconomyAdapter}) the company's cash should be paid into. */
    Optional<String> companyAccount(String companyId);
}
