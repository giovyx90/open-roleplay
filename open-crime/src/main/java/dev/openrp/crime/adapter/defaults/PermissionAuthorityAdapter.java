package dev.openrp.crime.adapter.defaults;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import dev.openrp.crime.adapter.AuthorityAdapter;
import dev.openrp.crime.core.Ids;

/**
 * Default authority bridge for servers without Open FDO. Agents and capabilities are recognised
 * through Bukkit permission nodes, and "opening a dossier" mints a synthetic local id so the
 * discovery system still records and links events. {@link #available()} is {@code false}: an Open FDO
 * bridge that registers a real {@code AuthorityAdapter} takes over and opens actual dossiers.
 */
public final class PermissionAuthorityAdapter implements AuthorityAdapter {

    private static final String AGENT_NODE = "opencrime.authority.agent";
    private static final String INFORMANT_NODE = "opencrime.authority.informant";
    private static final String INVESTIGATE_NODE = "opencrime.authority.investigate";

    @Override
    public String id() {
        return "permission";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean isAgent(Player player) {
        return player != null && player.hasPermission(AGENT_NODE);
    }

    @Override
    public boolean canReceiveInformant(Player agent) {
        return agent != null && (agent.hasPermission(INFORMANT_NODE) || agent.hasPermission(AGENT_NODE));
    }

    @Override
    public boolean canInvestigate(Player agent) {
        return agent != null && (agent.hasPermission(INVESTIGATE_NODE) || agent.hasPermission(AGENT_NODE));
    }

    @Override
    public Optional<String> openDossier(Player agent, UUID subject, String subjectName) {
        return Optional.of("LOCALE/" + Ids.shortId());
    }
}
