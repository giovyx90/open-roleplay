package dev.openrp.fdo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import dev.openrp.fdo.OpenFdoPlugin;
import dev.openrp.fdo.capability.Capability;
import dev.openrp.fdo.config.ActDefinition;
import dev.openrp.fdo.event.ActProducedEvent;
import dev.openrp.fdo.event.DossierOpenedEvent;
import dev.openrp.fdo.model.ActRecord;
import dev.openrp.fdo.model.Agent;

/**
 * The unified "produce a document" engine. {@link #beginAct} validates the member's capability and
 * the required adapter, then hands them a tagged writable book - the plugin never writes the content.
 * {@link #completeAct} runs on signing: it re-checks authority, applies the act's configured effects
 * (open a dossier, start custody, seize evidence, flag wanted, ...), stamps the book and appends an
 * audit line. Availability of an act is gated by both the rank capability and the presence of any
 * adapter the act requires, exactly as the design demands.
 */
public final class ActService {

    private final OpenFdoPlugin plugin;
    private final List<ActRecord> acts = new ArrayList<>();

    public ActService(OpenFdoPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        acts.clear();
        acts.addAll(plugin.adapters().storage().loadActs());
    }

    /** Acts the member may currently produce: rank holds the capability AND any required adapter is present. */
    public List<ActDefinition> available(Agent agent) {
        List<ActDefinition> result = new ArrayList<>();
        if (agent == null) {
            return result;
        }
        for (ActDefinition act : plugin.config().acts().all()) {
            if (canProduce(agent, act)) {
                result.add(act);
            }
        }
        return result;
    }

    public boolean canProduce(Agent agent, ActDefinition act) {
        if (agent == null || act == null) {
            return false;
        }
        boolean hasCapability = plugin.config().ranks().has(agent.corpsId(), agent.rankId(), act.capability());
        return hasCapability && plugin.adapters().hasAdapter(act.effectiveRequiredAdapter());
    }

    /** Validates and hands the member a blank, tagged act book to fill in and sign. */
    public FdoResult beginAct(Player issuer, String actId, String targetName) {
        Optional<Agent> agentOpt = plugin.agents().agent(issuer.getUniqueId());
        if (agentOpt.isEmpty()) {
            return FdoResult.fail("act.not_agent");
        }
        Agent agent = agentOpt.get();
        Optional<ActDefinition> actOpt = plugin.config().acts().get(actId);
        if (actOpt.isEmpty()) {
            return FdoResult.fail("act.unknown", "act", String.valueOf(actId));
        }
        ActDefinition act = actOpt.get();
        if (!plugin.config().ranks().has(agent.corpsId(), agent.rankId(), act.capability())) {
            return FdoResult.fail("act.no_capability", "act", act.displayName());
        }
        if (!plugin.adapters().hasAdapter(act.effectiveRequiredAdapter())) {
            return FdoResult.fail("act.adapter_missing", "act", act.displayName());
        }
        String targetUuid = null;
        String resolvedName = targetName;
        if (targetName != null && !targetName.isBlank()) {
            OfflinePlayer target = resolvePlayer(targetName);
            if (target != null && target.getUniqueId() != null) {
                targetUuid = target.getUniqueId().toString();
                resolvedName = target.getName() == null ? targetName : target.getName();
            }
        }
        if (requiresTarget(act) && targetUuid == null) {
            return FdoResult.fail("act.target_required", "act", act.displayName());
        }
        ItemStack book = plugin.actBook().createWritable(act, agent.name(), targetUuid, resolvedName);
        issuer.getInventory().addItem(book).values()
                .forEach(leftover -> issuer.getWorld().dropItemNaturally(issuer.getLocation(), leftover));
        return FdoResult.ok("act.book_given", "act", act.displayName());
    }

    /**
     * Runs when an act book is signed. Returns {@code true} when the book was an FDO act handled here
     * (so the listener should keep the stamped meta), {@code false} when it was an ordinary book.
     */
    public boolean completeAct(Player issuer, ItemMeta previousMeta, BookMeta newMeta) {
        String actId = plugin.actBook().actId(previousMeta);
        if (actId == null) {
            return false;
        }
        Optional<ActDefinition> actOpt = plugin.config().acts().get(actId);
        Optional<Agent> agentOpt = plugin.agents().agent(issuer.getUniqueId());
        if (actOpt.isEmpty() || agentOpt.isEmpty()) {
            plugin.messages().warning(issuer, "act.not_agent");
            return false;
        }
        ActDefinition act = actOpt.get();
        Agent agent = agentOpt.get();
        if (!canProduce(agent, act)) {
            plugin.messages().warning(issuer, "act.no_capability", "act", act.displayName());
            return false;
        }

        UUID targetUuid = parseUuid(plugin.actBook().targetUuid(previousMeta));
        String targetName = plugin.actBook().targetName(previousMeta);
        String dossierId = applyEffects(issuer, agent, act, targetUuid, targetName);

        plugin.actBook().stamp(newMeta, agent, act, dossierId);
        recordAct(issuer, agent, act, targetUuid, targetName, dossierId);
        plugin.messages().success(issuer, "act.produced", "act", act.displayName());
        return true;
    }

    public List<ActRecord> actsBy(UUID author) {
        List<ActRecord> result = new ArrayList<>();
        for (ActRecord record : acts) {
            if (author != null && author.equals(record.author())) {
                result.add(record);
            }
        }
        return result;
    }

    public List<ActRecord> all() {
        return List.copyOf(acts);
    }

    // --- effects -----------------------------------------------------------------------------

    private String applyEffects(Player issuer, Agent agent, ActDefinition act, UUID targetUuid, String targetName) {
        String dossierId = null;
        boolean opensDossier = act.opensDossier() || act.startsCustody();
        if (opensDossier) {
            long custodyHours = resolveCustodyHours(act);
            FdoResult opened = plugin.dossiers().open(targetUuid, targetName, agent.corpsId(),
                    agent.uuid(), custodyHours);
            dossierId = opened.dossier().map(d -> d.id()).orElse(null);
            opened.dossier().ifPresent(dossier ->
                    plugin.getServer().getPluginManager().callEvent(new DossierOpenedEvent(dossier)));
        }
        if (act.seizesEvidence()) {
            Location location = issuer.getLocation();
            String source = sourceOfOffhand(issuer);
            plugin.evidence().seize(dossierId, act.displayName() + (targetName == null ? "" : " - " + targetName),
                    source, "", agent.uuid(),
                    location.getWorld() == null ? "world" : location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ());
        }
        if (act.flagsWanted() && targetUuid != null) {
            FdoResult wantedResult = plugin.wanted().flag(targetUuid, targetName, act.wantedLevel(),
                    act.displayName(), agent.uuid());
            if (wantedResult.failed()) {
                // The book is still stamped, but warn the member and note the dossier so a misconfigured
                // wanted level does not silently produce a "wanted" act with no register entry.
                plugin.messages().warning(issuer, wantedResult.messageKey(), wantedResult.placeholders());
                if (dossierId != null) {
                    plugin.dossiers().addNote(dossierId,
                            "Wanted flag NOT applied: level " + act.wantedLevel() + " is not configured.");
                }
            }
        }
        if (act.capability() == Capability.ECONOMIC_AUDIT && targetUuid != null) {
            String linkedDossier = dossierId;
            plugin.adapters().economyAudit().ifPresent(audit -> {
                var report = audit.snapshot(targetUuid);
                if (linkedDossier != null) {
                    plugin.dossiers().addNote(linkedDossier, "Economic audit: " + report.summary());
                }
                plugin.adapters().logging().log("audit subject=" + targetUuid + " summary=" + report.summary());
            });
        }
        if (act.issuesFine() && dossierId != null) {
            plugin.dossiers().addNote(dossierId, "Fine issued by " + agent.name() + ".");
        }
        return dossierId;
    }

    private String sourceOfOffhand(Player issuer) {
        ItemStack offhand = issuer.getInventory().getItemInOffHand();
        return plugin.adapters().evidenceSource()
                .flatMap(adapter -> adapter.sourceOf(offhand))
                .orElse("manual");
    }

    private void recordAct(Player issuer, Agent agent, ActDefinition act, UUID targetUuid, String targetName, String dossierId) {
        Location location = issuer.getLocation();
        ActRecord record = new ActRecord(UUID.randomUUID(), act.id(), agent.uuid(), agent.name(),
                targetUuid, targetName == null ? "" : targetName,
                location.getWorld() == null ? "world" : location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), System.currentTimeMillis(), dossierId);
        acts.add(record);
        plugin.adapters().storage().appendAct(record);
        plugin.adapters().logging().log("act type=" + act.id() + " by=" + agent.matricola()
                + " target=" + targetName + " dossier=" + dossierId);
        plugin.getServer().getPluginManager().callEvent(new ActProducedEvent(record, dossierId));
    }

    /**
     * Custody length for a custody-opening act: an explicit {@code custody_hours} wins; otherwise a
     * {@code DETAIN_TEMPORARY} act uses {@code custody.detain-hours} and any other uses
     * {@code custody.default-hours}. Returns {@code 0} for acts that do not start custody.
     */
    private long resolveCustodyHours(ActDefinition act) {
        if (!act.startsCustody()) {
            return 0L;
        }
        if (act.custodyHours() >= 0) {
            return act.custodyHours();
        }
        return act.capability() == Capability.DETAIN_TEMPORARY
                ? plugin.config().settings().detainHours()
                : plugin.config().settings().defaultCustodyHours();
    }

    private boolean requiresTarget(ActDefinition act) {
        return act.opensDossier() || act.startsCustody() || act.flagsWanted()
                || act.capability() == Capability.ECONOMIC_AUDIT;
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return plugin.getServer().getOfflinePlayerIfCached(name);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
