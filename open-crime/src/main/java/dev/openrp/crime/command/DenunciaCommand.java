package dev.openrp.crime.command;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.DiscoveryType;

/**
 * {@code /denuncia} - a civilian (or agent) reports a crime they witnessed. It only works with an
 * authority agent physically nearby and only if a real crime event happened near here recently: you
 * cannot report into the void. The discovery links those events to a dossier the agent opens.
 */
public final class DenunciaCommand extends BaseCrimeCommand {

    public DenunciaCommand(OpenCrimePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player reporter = requirePlayer(sender);
        if (reporter == null) {
            return true;
        }
        int radius = plugin.config().settings().denunciaRadius();
        Optional<Player> agent = Proximity.nearestAgent(plugin.adapters().authority(), reporter, radius, false);
        if (agent.isEmpty()) {
            plugin.messages().warning(reporter, "denuncia.no_agent");
            return true;
        }
        Location at = reporter.getLocation();
        long since = System.currentTimeMillis() - plugin.config().settings().denunciaEventWindowMillis();
        List<CrimeEvent> events = plugin.events().nearbyRecent(at.getWorld().getName(),
                at.getBlockX(), at.getBlockY(), at.getBlockZ(), radius, since);
        if (events.isEmpty()) {
            plugin.messages().warning(reporter, "denuncia.nothing");
            return true;
        }

        Optional<String> dossier = plugin.adapters().authority().openDossier(agent.get(), null,
                plugin.messages().text(agent.get(), "denuncia.unknown_subject"));
        String dossierId = dossier.orElse(null);
        plugin.discoveries().open(DiscoveryType.DENUNCIA, reporter.getUniqueId(),
                at.getWorld().getName(), at.getBlockX(), at.getBlockY(), at.getBlockZ(), dossierId, events);
        markInvestigated(events);

        plugin.adapters().notification().send(agent.get(), plugin.messages().prefixed(agent.get(),
                "denuncia.agent_notified", "reporter", reporter.getName(), "count", String.valueOf(events.size())));
        plugin.messages().success(reporter, "denuncia.filed", "agent", agent.get().getName(),
                "count", String.valueOf(events.size()));
        return true;
    }

    private void markInvestigated(List<CrimeEvent> events) {
        Set<String> orgIds = new LinkedHashSet<>();
        for (CrimeEvent event : events) {
            if (event.orgId() != null) {
                orgIds.add(event.orgId());
            }
        }
        orgIds.forEach(plugin.orgs()::markInvestigated);
    }
}
