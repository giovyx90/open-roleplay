package dev.openrp.fdo.adapter.defaults;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.fdo.adapter.StorageAdapter;
import dev.openrp.fdo.model.ActRecord;
import dev.openrp.fdo.model.Agent;
import dev.openrp.fdo.model.AlertState;
import dev.openrp.fdo.model.Charge;
import dev.openrp.fdo.model.CustodyAction;
import dev.openrp.fdo.model.CustodyEntry;
import dev.openrp.fdo.model.DetentionOrder;
import dev.openrp.fdo.model.Dossier;
import dev.openrp.fdo.model.DossierStatus;
import dev.openrp.fdo.model.DutySession;
import dev.openrp.fdo.model.Evidence;
import dev.openrp.fdo.model.EvidenceState;
import dev.openrp.fdo.model.Verdict;
import dev.openrp.fdo.model.VerdictOutcome;
import dev.openrp.fdo.model.WantedEntry;

/**
 * Default storage adapter persisting every core record to a single YAML file. The schema mirrors the
 * model so the file doubles as readable documentation. Compound list elements (charges, custody
 * links, duty sessions) are encoded as {@code ';'}-delimited strings whose fields are ids, enums and
 * numbers - safe to split. Each mutation rewrites the file durably: it is written to a temp sibling
 * and atomically renamed over the live file, with the previous contents kept as {@code .bak}, so a
 * crash mid-write can never corrupt the whole data set (mirrors the Open Companies adapter).
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String AGENTS = "agents";
    private static final String DOSSIERS = "dossiers";
    private static final String EVIDENCE = "evidence";
    private static final String WANTED = "wanted";
    private static final String DETENTIONS = "detentions";
    private static final String ACTS = "acts";
    private static final String DUTY = "duty";
    private static final String COUNTERS = "counters";
    private static final String ALERT = "alert";

    private final File file;
    private final File tempFile;
    private final File backupFile;
    private final Logger logger;
    private YamlConfiguration yaml = new YamlConfiguration();

    public YamlStorageAdapter(File file, Logger logger) {
        this.file = file;
        this.tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        this.backupFile = new File(file.getParentFile(), file.getName() + ".bak");
        this.logger = logger;
    }

    @Override
    public String id() {
        return "yaml";
    }

    @Override
    public void init() {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        YamlConfiguration loaded = tryLoad(file);
        if (loaded == null) {
            loaded = tryLoad(tempFile);
            if (loaded != null) {
                logger.warning("[OpenFDO] Recovered data from interrupted write (" + tempFile.getName() + ").");
            }
        }
        if (loaded == null) {
            loaded = tryLoad(backupFile);
            if (loaded != null) {
                logger.warning("[OpenFDO] Primary data unreadable; recovered from backup (" + backupFile.getName() + ").");
            }
        }
        yaml = loaded != null ? loaded : new YamlConfiguration();
    }

    private YamlConfiguration tryLoad(File source) {
        if (source == null || !source.isFile()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(source);
            return config;
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            logger.severe("[OpenFDO] Failed to read '" + source.getName() + "': " + exception.getMessage());
            return null;
        }
    }

    // --- agents ------------------------------------------------------------------------------

    @Override
    public Collection<Agent> loadAgents() {
        List<Agent> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(AGENTS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(new Agent(
                        UUID.fromString(key),
                        section.getString("name", ""),
                        section.getString("corps", ""),
                        section.getString("rank", ""),
                        section.getString("matricola", ""),
                        section.getLong("enrolled-at")));
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed agent '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveAgent(Agent agent) {
        String base = AGENTS + "." + agent.uuid();
        yaml.set(base, null);
        yaml.set(base + ".name", agent.name());
        yaml.set(base + ".corps", agent.corpsId());
        yaml.set(base + ".rank", agent.rankId());
        yaml.set(base + ".matricola", agent.matricola());
        yaml.set(base + ".enrolled-at", agent.enrolledAt());
        persist();
    }

    @Override
    public void deleteAgent(UUID agentUuid) {
        yaml.set(AGENTS + "." + agentUuid, null);
        persist();
    }

    // --- dossiers ----------------------------------------------------------------------------

    @Override
    public Collection<Dossier> loadDossiers() {
        List<Dossier> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(DOSSIERS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(readDossier(section, key));
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed dossier '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveDossier(Dossier dossier) {
        writeDossier(dossier);
        persist();
    }

    @Override
    public void deleteDossier(String dossierId) {
        yaml.set(DOSSIERS + "." + escapeKey(dossierId), null);
        persist();
    }

    private Dossier readDossier(ConfigurationSection section, String key) {
        String id = section.getString("id", key);
        Dossier dossier = new Dossier(
                id,
                parseUuid(section.getString("subject")),
                section.getString("subject-name", ""),
                section.getString("corps", ""),
                parseUuid(section.getString("opened-by")),
                section.getLong("opened-at"));
        dossier.setStatus(DossierStatus.fromString(section.getString("status")));
        dossier.setCustodyDeadline(section.getLong("custody-deadline"));
        for (String raw : section.getStringList("charges")) {
            String[] parts = raw.split(";", -1);
            if (parts.length >= 3) {
                dossier.addCharge(new Charge(parts[0], parseUuid(parts[1]), parseLong(parts[2])));
            }
        }
        for (String raw : section.getStringList("evidence")) {
            UUID evidenceId = parseUuid(raw);
            if (evidenceId != null) {
                dossier.linkEvidence(evidenceId);
            }
        }
        for (String note : section.getStringList("notes")) {
            dossier.addNote(note);
        }
        ConfigurationSection verdict = section.getConfigurationSection("verdict");
        if (verdict != null) {
            VerdictOutcome outcome = VerdictOutcome.fromString(verdict.getString("outcome"));
            if (outcome != null) {
                dossier.signVerdict(new Verdict(
                        outcome,
                        verdict.getLong("sentence-seconds"),
                        verdict.getInt("security-level"),
                        parseUuid(verdict.getString("judge")),
                        verdict.getLong("signed-at"),
                        verdict.getString("note", "")));
            }
        }
        return dossier;
    }

    private void writeDossier(Dossier dossier) {
        String base = DOSSIERS + "." + escapeKey(dossier.id());
        yaml.set(base, null);
        yaml.set(base + ".id", dossier.id());
        yaml.set(base + ".subject", uuidString(dossier.subjectUuid()));
        yaml.set(base + ".subject-name", dossier.subjectName());
        yaml.set(base + ".corps", dossier.corpsId());
        yaml.set(base + ".opened-by", uuidString(dossier.openedBy()));
        yaml.set(base + ".opened-at", dossier.openedAt());
        yaml.set(base + ".status", dossier.status().name());
        yaml.set(base + ".custody-deadline", dossier.custodyDeadline());
        List<String> charges = new ArrayList<>();
        for (Charge charge : dossier.charges()) {
            charges.add(charge.crimeId() + ";" + uuidString(charge.addedBy()) + ";" + charge.addedAt());
        }
        yaml.set(base + ".charges", charges);
        List<String> evidence = new ArrayList<>();
        for (UUID evidenceId : dossier.evidenceIds()) {
            evidence.add(evidenceId.toString());
        }
        yaml.set(base + ".evidence", evidence);
        yaml.set(base + ".notes", new ArrayList<>(dossier.notes()));
        dossier.verdict().ifPresent(verdict -> {
            String verdictBase = base + ".verdict";
            yaml.set(verdictBase + ".outcome", verdict.outcome().name());
            yaml.set(verdictBase + ".sentence-seconds", verdict.sentenceSeconds());
            yaml.set(verdictBase + ".security-level", verdict.securityLevel());
            yaml.set(verdictBase + ".judge", uuidString(verdict.judge()));
            yaml.set(verdictBase + ".signed-at", verdict.signedAt());
            yaml.set(verdictBase + ".note", verdict.note());
        });
    }

    // --- evidence ----------------------------------------------------------------------------

    @Override
    public Collection<Evidence> loadEvidence() {
        List<Evidence> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(EVIDENCE);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                Evidence evidence = new Evidence(
                        UUID.fromString(key),
                        emptyToNull(section.getString("dossier")),
                        section.getString("label", ""),
                        section.getString("source", "manual"),
                        section.getString("nbt", ""),
                        section.getLong("created-at"));
                evidence.setState(EvidenceState.fromString(section.getString("state")));
                for (String raw : section.getStringList("chain")) {
                    String[] p = raw.split(";", -1);
                    if (p.length >= 8) {
                        // world is encoded LAST because it is the one free-text field and may itself
                        // contain ';' (custom/Multiverse worlds); rejoin any overflow back into it.
                        String world = String.join(";", java.util.Arrays.copyOfRange(p, 7, p.length));
                        evidence.addCustody(new CustodyEntry(
                                parseUuid(p[0]), parseUuid(p[1]), CustodyAction.fromString(p[2]),
                                parseLong(p[3]), world, parseDouble(p[4]), parseDouble(p[5]), parseDouble(p[6])));
                    }
                }
                result.add(evidence);
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed evidence '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveEvidence(Evidence item) {
        String base = EVIDENCE + "." + item.id();
        yaml.set(base, null);
        yaml.set(base + ".dossier", item.dossierId());
        yaml.set(base + ".label", item.label());
        yaml.set(base + ".source", item.source());
        yaml.set(base + ".nbt", item.nbt());
        yaml.set(base + ".created-at", item.createdAt());
        yaml.set(base + ".state", item.state().name());
        List<String> chain = new ArrayList<>();
        for (CustodyEntry entry : item.chain()) {
            // world is written LAST so a ';' in the world name cannot shift the coordinate fields.
            chain.add(uuidString(entry.fromAgent()) + ";" + uuidString(entry.toAgent()) + ";"
                    + entry.action().name() + ";" + entry.timestamp() + ";"
                    + entry.x() + ";" + entry.y() + ";" + entry.z() + ";" + entry.world());
        }
        yaml.set(base + ".chain", chain);
        persist();
    }

    @Override
    public void deleteEvidence(UUID evidenceId) {
        yaml.set(EVIDENCE + "." + evidenceId, null);
        persist();
    }

    // --- wanted ------------------------------------------------------------------------------

    @Override
    public Collection<WantedEntry> loadWanted() {
        List<WantedEntry> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(WANTED);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                WantedEntry entry = new WantedEntry(
                        UUID.fromString(key),
                        section.getString("name", ""),
                        section.getInt("level", 1),
                        section.getString("reason", ""),
                        parseUuid(section.getString("issued-by")),
                        section.getLong("issued-at"));
                entry.setActive(section.getBoolean("active", true));
                result.add(entry);
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed wanted entry '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveWanted(WantedEntry entry) {
        String base = WANTED + "." + entry.subjectUuid();
        yaml.set(base, null);
        yaml.set(base + ".name", entry.subjectName());
        yaml.set(base + ".level", entry.level());
        yaml.set(base + ".reason", entry.reason());
        yaml.set(base + ".issued-by", uuidString(entry.issuedBy()));
        yaml.set(base + ".issued-at", entry.issuedAt());
        yaml.set(base + ".active", entry.active());
        persist();
    }

    @Override
    public void deleteWanted(UUID subjectUuid) {
        yaml.set(WANTED + "." + subjectUuid, null);
        persist();
    }

    // --- detentions --------------------------------------------------------------------------

    @Override
    public Collection<DetentionOrder> loadDetentions() {
        List<DetentionOrder> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(DETENTIONS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(new DetentionOrder(
                        UUID.fromString(key),
                        section.getString("name", ""),
                        section.getString("dossier", ""),
                        section.getLong("sentence-seconds"),
                        section.getInt("security-level"),
                        section.getLong("started-at")));
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed detention '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveDetention(DetentionOrder order) {
        String base = DETENTIONS + "." + order.inmate();
        yaml.set(base, null);
        yaml.set(base + ".name", order.inmateName());
        yaml.set(base + ".dossier", order.dossierId());
        yaml.set(base + ".sentence-seconds", order.sentenceSeconds());
        yaml.set(base + ".security-level", order.securityLevel());
        yaml.set(base + ".started-at", order.startedAt());
        persist();
    }

    @Override
    public void deleteDetention(UUID inmate) {
        yaml.set(DETENTIONS + "." + inmate, null);
        persist();
    }

    // --- append-only logs --------------------------------------------------------------------

    @Override
    public Collection<ActRecord> loadActs() {
        List<ActRecord> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(ACTS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(new ActRecord(
                        UUID.fromString(key),
                        section.getString("type", ""),
                        parseUuid(section.getString("author")),
                        section.getString("author-name", ""),
                        parseUuid(section.getString("target")),
                        section.getString("target-name", ""),
                        section.getString("world", ""),
                        section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                        section.getLong("timestamp"),
                        emptyToNull(section.getString("dossier"))));
            } catch (RuntimeException exception) {
                logger.warning("[OpenFDO] Skipping malformed act '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void appendAct(ActRecord act) {
        String base = ACTS + "." + act.id();
        yaml.set(base + ".type", act.type());
        yaml.set(base + ".author", uuidString(act.author()));
        yaml.set(base + ".author-name", act.authorName());
        yaml.set(base + ".target", uuidString(act.target()));
        yaml.set(base + ".target-name", act.targetName());
        yaml.set(base + ".world", act.world());
        yaml.set(base + ".x", act.x());
        yaml.set(base + ".y", act.y());
        yaml.set(base + ".z", act.z());
        yaml.set(base + ".timestamp", act.timestamp());
        yaml.set(base + ".dossier", act.dossierId());
        persist();
    }

    @Override
    public Collection<DutySession> loadDutySessions() {
        List<DutySession> result = new ArrayList<>();
        for (String raw : yaml.getStringList(DUTY)) {
            String[] p = raw.split(";", -1);
            if (p.length >= 3) {
                UUID agent = parseUuid(p[0]);
                if (agent != null) {
                    result.add(new DutySession(agent, parseLong(p[1]), parseLong(p[2])));
                }
            }
        }
        return result;
    }

    @Override
    public void appendDutySession(DutySession session) {
        List<String> sessions = yaml.getStringList(DUTY);
        sessions.add(session.agent() + ";" + session.shiftStart() + ";" + session.shiftEnd());
        yaml.set(DUTY, sessions);
        persist();
    }

    // --- counters ----------------------------------------------------------------------------

    @Override
    public Map<String, Long> loadCounters() {
        Map<String, Long> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection(COUNTERS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            result.put(key, root.getLong(key));
        }
        return result;
    }

    @Override
    public void saveCounter(String key, long value) {
        yaml.set(COUNTERS + "." + key, value);
        persist();
    }

    // --- alert -------------------------------------------------------------------------------

    @Override
    public Optional<AlertState> loadAlert() {
        ConfigurationSection section = yaml.getConfigurationSection(ALERT);
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new AlertState(
                section.getInt("level"),
                section.getString("reason", ""),
                parseUuid(section.getString("declared-by")),
                section.getLong("declared-at")));
    }

    @Override
    public void saveAlert(AlertState state) {
        yaml.set(ALERT, null);
        if (state != null && state.active()) {
            yaml.set(ALERT + ".level", state.level());
            yaml.set(ALERT + ".reason", state.reason());
            yaml.set(ALERT + ".declared-by", uuidString(state.declaredBy()));
            yaml.set(ALERT + ".declared-at", state.declaredAt());
        }
        persist();
    }

    // --- lifecycle ---------------------------------------------------------------------------

    @Override
    public void flush() {
        persist();
    }

    @Override
    public void close() {
        persist();
    }

    // --- helpers -----------------------------------------------------------------------------

    private static String escapeKey(String id) {
        // '.' is the configuration path separator; ids from the default pattern use '/', which is
        // safe. Escape any '.' so a custom id pattern cannot split a key into sub-sections.
        return id == null ? "" : id.replace('.', '_');
    }

    private static String uuidString(UUID uuid) {
        return uuid == null ? "" : uuid.toString();
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

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return 0L;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return 0.0;
        }
    }

    /**
     * Durably writes the current state: temp file, then copy the live file aside as {@code .bak},
     * then atomically rename the temp over the live file. A crash at any point leaves either the
     * previous complete file or a recoverable {@code .tmp}/{@code .bak}.
     */
    private void persist() {
        try {
            yaml.save(tempFile);
        } catch (IOException exception) {
            logger.severe("[OpenFDO] Failed to write data: " + exception.getMessage());
            return;
        }
        if (file.exists()) {
            try {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.warning("[OpenFDO] Failed to refresh data backup: " + exception.getMessage());
            }
        }
        try {
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicUnsupported) {
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.severe("[OpenFDO] Failed to commit data: " + exception.getMessage());
            }
        } catch (IOException exception) {
            logger.severe("[OpenFDO] Failed to commit data: " + exception.getMessage());
        }
    }
}
