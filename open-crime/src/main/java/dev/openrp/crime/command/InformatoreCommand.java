package dev.openrp.crime.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.DiscoveryType;
import dev.openrp.crime.model.IllegalOrg;

/**
 * {@code /informatore} - a member of an organisation collaborates with a nearby capable agent. It
 * links the crime events the member knows (their org's history) to a dossier and marks the member as
 * a collaborator - unless informant protection is on, in which case the identity is kept out of the
 * database. Narrative consequences are left to RP.
 */
public final class InformatoreCommand extends BaseCrimeCommand {

    public InformatoreCommand(OpenCrimePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player informant = requirePlayer(sender);
        if (informant == null) {
            return true;
        }
        IllegalOrg org = plugin.orgs().byMember(informant.getUniqueId()).orElse(null);
        if (org == null) {
            plugin.messages().warning(informant, "informatore.not_member");
            return true;
        }
        int radius = plugin.config().settings().denunciaRadius();
        Optional<Player> agent = Proximity.nearestAgent(plugin.adapters().authority(), informant, radius, true);
        if (agent.isEmpty()) {
            plugin.messages().warning(informant, "informatore.no_agent");
            return true;
        }

        List<CrimeEvent> events = plugin.events().recentByOrg(org.id(), 0L);
        Optional<String> dossier = plugin.adapters().authority().openDossier(agent.get(),
                org.founder(), org.name());
        String dossierId = dossier.orElse(null);

        boolean protect = plugin.config().settings().informantProtection();
        UUID attributed = protect ? null : informant.getUniqueId();
        Location at = informant.getLocation();
        plugin.discoveries().open(DiscoveryType.INFORMATORE, attributed, at.getWorld().getName(),
                at.getBlockX(), at.getBlockY(), at.getBlockZ(), dossierId, events);
        if (!protect) {
            plugin.orgs().markInformant(org.id(), informant.getUniqueId());
        }
        plugin.orgs().markInvestigated(org.id());

        plugin.adapters().notification().send(agent.get(), plugin.messages().prefixed(agent.get(),
                "informatore.agent_notified", "org", org.name(), "count", String.valueOf(events.size())));
        plugin.messages().success(informant, "informatore.done", "agent", agent.get().getName());
        return true;
    }
}
