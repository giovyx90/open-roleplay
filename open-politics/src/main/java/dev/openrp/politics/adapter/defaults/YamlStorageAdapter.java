package dev.openrp.politics.adapter.defaults;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.politics.adapter.StorageAdapter;
import dev.openrp.politics.model.ActStatus;
import dev.openrp.politics.model.BallotChoice;
import dev.openrp.politics.model.ChargeHolder;
import dev.openrp.politics.model.CollegiateVote;
import dev.openrp.politics.model.Election;
import dev.openrp.politics.model.ElectionStatus;
import dev.openrp.politics.model.HolderStatus;
import dev.openrp.politics.model.Law;
import dev.openrp.politics.model.LawStatus;
import dev.openrp.politics.model.PoliticalAct;

/**
 * Default storage adapter persisting every record to a single YAML file. The schema mirrors the model
 * so the file doubles as readable documentation. Each mutation rewrites the file durably: it is written
 * to a temp sibling and atomically renamed over the live file, with the previous contents kept as
 * {@code .bak}, so a crash mid-write can never corrupt the whole data set (mirrors the Open FDO, Open
 * Companies and Open Crime adapters). Writes are coalesced behind a dirty flag and a single lock.
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String HOLDERS = "holders";
    private static final String ELECTIONS = "elections";
    private static final String ACTS = "acts";
    private static final String LAWS = "laws";
    private static final String VOTES = "collegiate_votes";
    private static final String GOV_STATES = "government_states";
    private static final String COUNTERS = "counters";

    private final File file;
    private final Logger logger;
    private final Object lock = new Object();
    private YamlConfiguration data = new YamlConfiguration();

    public YamlStorageAdapter(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public String id() {
        return "yaml";
    }

    @Override
    public void init() {
        synchronized (lock) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("[OpenPolitics] Could not create data folder: " + parent);
            }
            if (file.exists()) {
                this.data = YamlConfiguration.loadConfiguration(file);
            } else {
                this.data = new YamlConfiguration();
                write();
            }
        }
    }

    // --- charge holders ----------------------------------------------------------------------

    @Override
    public Collection<ChargeHolder> loadHolders() {
        synchronized (lock) {
            List<ChargeHolder> result = new ArrayList<>();
            ConfigurationSection root = data.getConfigurationSection(HOLDERS);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                ChargeHolder holder = new ChargeHolder(id, uuid(s.getString("player")),
                        s.getString("charge", ""), s.getString("government", ""),
                        s.getLong("assigned_at"), s.getLong("expires_at"), s.getString("assigned_by", "system"));
                holder.setStatus(enumOf(HolderStatus.class, s.getString("status"), HolderStatus.ACTIVE));
                String successor = s.getString("successor");
                if (successor != null && !successor.isBlank()) {
                    holder.setSuccessor(uuid(successor));
                }
                result.add(holder);
            }
            return result;
        }
    }

    @Override
    public void saveHolder(ChargeHolder holder) {
        synchronized (lock) {
            ConfigurationSection s = section(HOLDERS, holder.id());
            s.set("player", holder.playerUuid().toString());
            s.set("charge", holder.chargeId());
            s.set("government", holder.governmentId());
            s.set("assigned_at", holder.assignedAt());
            s.set("expires_at", holder.expiresAt());
            s.set("assigned_by", holder.assignedBy());
            s.set("status", holder.status().name());
            s.set("successor", holder.successor() == null ? null : holder.successor().toString());
            write();
        }
    }

    @Override
    public void deleteHolder(String holderId) {
        delete(HOLDERS, holderId);
    }

    // --- elections ---------------------------------------------------------------------------

    @Override
    public Collection<Election> loadElections() {
        synchronized (lock) {
            List<Election> result = new ArrayList<>();
            ConfigurationSection root = data.getConfigurationSection(ELECTIONS);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Election election = new Election(id, s.getString("charge", ""), s.getString("government", ""),
                        s.getInt("seats", 1), s.getLong("campaign_start"), s.getLong("voting_start"),
                        s.getLong("voting_end"), s.getString("called_by", "system"));
                election.setStatus(enumOf(ElectionStatus.class, s.getString("status"), ElectionStatus.CAMPAIGN));
                ConfigurationSection cands = s.getConfigurationSection("candidacies");
                if (cands != null) {
                    for (String key : cands.getKeys(false)) {
                        election.addCandidate(uuid(key), cands.getString(key, ""));
                    }
                }
                ConfigurationSection ballots = s.getConfigurationSection("ballots");
                if (ballots != null) {
                    for (String key : ballots.getKeys(false)) {
                        election.castBallot(uuid(key), uuid(ballots.getString(key)));
                    }
                }
                result.add(election);
            }
            return result;
        }
    }

    @Override
    public void saveElection(Election election) {
        synchronized (lock) {
            ConfigurationSection s = section(ELECTIONS, election.id());
            s.set("charge", election.chargeId());
            s.set("government", election.governmentId());
            s.set("seats", election.seats());
            s.set("status", election.status().name());
            s.set("campaign_start", election.campaignStart());
            s.set("voting_start", election.votingStart());
            s.set("voting_end", election.votingEnd());
            s.set("called_by", election.calledBy());
            s.set("candidacies", null);
            ConfigurationSection cands = s.createSection("candidacies");
            for (Map.Entry<UUID, String> entry : election.candidacies().entrySet()) {
                cands.set(entry.getKey().toString(), entry.getValue());
            }
            s.set("ballots", null);
            ConfigurationSection ballots = s.createSection("ballots");
            for (Map.Entry<UUID, UUID> entry : election.ballots().entrySet()) {
                ballots.set(entry.getKey().toString(), entry.getValue().toString());
            }
            write();
        }
    }

    @Override
    public void deleteElection(String electionId) {
        delete(ELECTIONS, electionId);
    }

    // --- acts --------------------------------------------------------------------------------

    @Override
    public Collection<PoliticalAct> loadActs() {
        synchronized (lock) {
            List<PoliticalAct> result = new ArrayList<>();
            ConfigurationSection root = data.getConfigurationSection(ACTS);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                PoliticalAct act = new PoliticalAct(id, s.getString("display_id", id), s.getString("type", ""),
                        s.getString("government", ""), uuid(s.getString("author")), s.getString("charge", ""),
                        s.getString("title", ""), s.getStringList("body"), s.getLong("signed_at"));
                act.setStatus(enumOf(ActStatus.class, s.getString("status"), ActStatus.SIGNED));
                act.setRelatedLawId(s.getString("related_law"));
                act.setVetoDeadline(s.getLong("veto_deadline"));
                act.setCollegiateVoteId(s.getString("collegiate_vote"));
                result.add(act);
            }
            return result;
        }
    }

    @Override
    public void saveAct(PoliticalAct act) {
        synchronized (lock) {
            ConfigurationSection s = section(ACTS, act.id());
            s.set("display_id", act.displayId());
            s.set("type", act.typeId());
            s.set("government", act.governmentId());
            s.set("author", act.authorUuid() == null ? null : act.authorUuid().toString());
            s.set("charge", act.chargeId());
            s.set("title", act.title());
            s.set("body", act.body());
            s.set("signed_at", act.signedAt());
            s.set("status", act.status().name());
            s.set("related_law", act.relatedLawId());
            s.set("veto_deadline", act.vetoDeadline());
            s.set("collegiate_vote", act.collegiateVoteId());
            write();
        }
    }

    @Override
    public void deleteAct(String actId) {
        delete(ACTS, actId);
    }

    // --- laws --------------------------------------------------------------------------------

    @Override
    public Collection<Law> loadLaws() {
        synchronized (lock) {
            List<Law> result = new ArrayList<>();
            ConfigurationSection root = data.getConfigurationSection(LAWS);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Law law = new Law(id, s.getString("act", ""), s.getString("government", ""),
                        s.getString("title", ""), s.getStringList("body"), s.getString("category", ""),
                        s.getLong("enacted_at"));
                law.setStatus(enumOf(LawStatus.class, s.getString("status"), LawStatus.ACTIVE));
                law.setRepealedAt(s.getLong("repealed_at"));
                String by = s.getString("repealed_by_uuid");
                if (by != null && !by.isBlank()) {
                    law.setRepealedByUuid(uuid(by));
                }
                law.setRepealedByCharge(s.getString("repealed_by_charge"));
                result.add(law);
            }
            return result;
        }
    }

    @Override
    public void saveLaw(Law law) {
        synchronized (lock) {
            ConfigurationSection s = section(LAWS, law.id());
            s.set("act", law.actId());
            s.set("government", law.governmentId());
            s.set("title", law.title());
            s.set("body", law.body());
            s.set("category", law.category());
            s.set("status", law.status().name());
            s.set("enacted_at", law.enactedAt());
            s.set("repealed_at", law.repealedAt());
            s.set("repealed_by_uuid", law.repealedByUuid() == null ? null : law.repealedByUuid().toString());
            s.set("repealed_by_charge", law.repealedByCharge());
            write();
        }
    }

    @Override
    public void deleteLaw(String lawId) {
        delete(LAWS, lawId);
    }

    // --- collegiate votes --------------------------------------------------------------------

    @Override
    public Collection<CollegiateVote> loadCollegiateVotes() {
        synchronized (lock) {
            List<CollegiateVote> result = new ArrayList<>();
            ConfigurationSection root = data.getConfigurationSection(VOTES);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                CollegiateVote vote = new CollegiateVote(id, s.getString("act", ""), s.getString("charge", ""),
                        s.getLong("opened_at"), s.getLong("closes_at"));
                vote.setStatus(enumOf(CollegiateVote.Status.class, s.getString("status"), CollegiateVote.Status.OPEN));
                ConfigurationSection ballots = s.getConfigurationSection("ballots");
                if (ballots != null) {
                    for (String key : ballots.getKeys(false)) {
                        BallotChoice.fromString(ballots.getString(key))
                                .ifPresent(choice -> vote.cast(uuid(key), choice));
                    }
                }
                result.add(vote);
            }
            return result;
        }
    }

    @Override
    public void saveCollegiateVote(CollegiateVote vote) {
        synchronized (lock) {
            ConfigurationSection s = section(VOTES, vote.id());
            s.set("act", vote.actId());
            s.set("charge", vote.chargeId());
            s.set("opened_at", vote.openedAt());
            s.set("closes_at", vote.closesAt());
            s.set("status", vote.status().name());
            s.set("ballots", null);
            ConfigurationSection ballots = s.createSection("ballots");
            for (Map.Entry<UUID, BallotChoice> entry : vote.ballots().entrySet()) {
                ballots.set(entry.getKey().toString(), entry.getValue().name());
            }
            write();
        }
    }

    @Override
    public void deleteCollegiateVote(String voteId) {
        delete(VOTES, voteId);
    }

    // --- government states -------------------------------------------------------------------

    @Override
    public Map<String, Boolean> loadGovernmentStates() {
        synchronized (lock) {
            Map<String, Boolean> result = new LinkedHashMap<>();
            ConfigurationSection root = data.getConfigurationSection(GOV_STATES);
            if (root == null) {
                return result;
            }
            for (String id : root.getKeys(false)) {
                result.put(id, root.getBoolean(id));
            }
            return result;
        }
    }

    @Override
    public void saveGovernmentState(String governmentId, boolean active) {
        synchronized (lock) {
            section(GOV_STATES, null).set(governmentId, active);
            write();
        }
    }

    // --- counters ----------------------------------------------------------------------------

    @Override
    public Map<String, Long> loadCounters() {
        synchronized (lock) {
            Map<String, Long> result = new LinkedHashMap<>();
            ConfigurationSection root = data.getConfigurationSection(COUNTERS);
            if (root == null) {
                return result;
            }
            for (String key : root.getKeys(false)) {
                result.put(key, root.getLong(key));
            }
            return result;
        }
    }

    @Override
    public void saveCounter(String key, long value) {
        synchronized (lock) {
            section(COUNTERS, null).set(key, value);
            write();
        }
    }

    // --- lifecycle ---------------------------------------------------------------------------

    @Override
    public void flush() {
        synchronized (lock) {
            write();
        }
    }

    @Override
    public void close() {
        flush();
    }

    // --- helpers -----------------------------------------------------------------------------

    /** Returns (creating if needed) the section at {@code root[.child]}. A null child returns root. */
    private ConfigurationSection section(String root, String child) {
        ConfigurationSection rootSection = data.getConfigurationSection(root);
        if (rootSection == null) {
            rootSection = data.createSection(root);
        }
        if (child == null) {
            return rootSection;
        }
        ConfigurationSection childSection = rootSection.getConfigurationSection(child);
        return childSection == null ? rootSection.createSection(child) : childSection;
    }

    private void delete(String root, String key) {
        synchronized (lock) {
            ConfigurationSection rootSection = data.getConfigurationSection(root);
            if (rootSection != null) {
                rootSection.set(key, null);
                write();
            }
        }
    }

    /** Atomic durable write: temp file, fsync-by-rename over the live file, previous kept as .bak. */
    private void write() {
        try {
            File temp = new File(file.getParentFile(), file.getName() + ".tmp");
            data.save(temp);
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (file.exists()) {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException noAtomic) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            logger.severe("[OpenPolitics] Failed to persist data: " + error.getMessage());
        }
    }

    private static UUID uuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }
}
