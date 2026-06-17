package dev.openrp.crime.adapter.defaults;

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
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import dev.openrp.crime.adapter.StorageAdapter;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.Discovery;
import dev.openrp.crime.model.DiscoveryType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.LaunderingProcess;
import dev.openrp.crime.model.LaunderingStatus;
import dev.openrp.crime.model.OrgMember;
import dev.openrp.crime.model.OrgStatus;
import dev.openrp.crime.model.ProductionProcess;
import dev.openrp.crime.model.ProductionStatus;
import dev.openrp.crime.model.Protection;
import dev.openrp.crime.model.ProtectionStatus;
import dev.openrp.crime.model.Shipment;
import dev.openrp.crime.model.ShipmentStatus;
import dev.openrp.crime.model.Territory;
import dev.openrp.crime.model.TrackedGood;
import dev.openrp.crime.model.TrackedGoodStatus;

/**
 * Default storage adapter persisting every record to a single YAML file. The schema mirrors the model
 * so the file doubles as readable documentation. Each mutation rewrites the file durably: it is
 * written to a temp sibling and atomically renamed over the live file, with the previous contents
 * kept as {@code .bak}, so a crash mid-write can never corrupt the whole data set (mirrors the Open
 * FDO and Open Companies adapters).
 */
public final class YamlStorageAdapter implements StorageAdapter {

    private static final String ORGS = "orgs";
    private static final String TERRITORIES = "territories";
    private static final String GOODS = "goods";
    private static final String PRODUCTION = "production";
    private static final String SHIPMENTS = "shipments";
    private static final String LAUNDERING = "laundering";
    private static final String PROTECTIONS = "protections";
    private static final String EVENTS = "events";
    private static final String DISCOVERIES = "discoveries";
    private static final String TREASURIES = "treasuries";
    private static final String COUNTERS = "counters";

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
                logger.warning("[OpenCrime] Recovered data from interrupted write (" + tempFile.getName() + ").");
            }
        }
        if (loaded == null) {
            loaded = tryLoad(backupFile);
            if (loaded != null) {
                logger.warning("[OpenCrime] Primary data unreadable; recovered from backup (" + backupFile.getName() + ").");
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
            logger.severe("[OpenCrime] Failed to read '" + source.getName() + "': " + exception.getMessage());
            return null;
        }
    }

    // --- organisations -----------------------------------------------------------------------

    @Override
    public Collection<IllegalOrg> loadOrgs() {
        List<IllegalOrg> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(ORGS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                IllegalOrg org = new IllegalOrg(
                        section.getString("id", key),
                        section.getString("name", ""),
                        section.getString("type", ""),
                        parseUuid(section.getString("founder")),
                        section.getLong("created-at"),
                        parseUuid(section.getString("treasury")));
                org.setStatus(OrgStatus.fromString(section.getString("status")));
                for (String region : section.getStringList("territories")) {
                    org.addTerritory(region);
                }
                ConfigurationSection members = section.getConfigurationSection("members");
                if (members != null) {
                    for (String memberKey : members.getKeys(false)) {
                        ConfigurationSection member = members.getConfigurationSection(memberKey);
                        UUID uuid = parseUuid(memberKey);
                        if (member == null || uuid == null) {
                            continue;
                        }
                        OrgMember added = org.addMember(uuid, member.getString("name", ""),
                                member.getString("role", ""), member.getLong("joined-at"));
                        added.setInformant(member.getBoolean("informant", false));
                    }
                }
                result.add(org);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed org '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveOrg(IllegalOrg org) {
        String base = ORGS + "." + escapeKey(org.id());
        yaml.set(base, null);
        yaml.set(base + ".id", org.id());
        yaml.set(base + ".name", org.name());
        yaml.set(base + ".type", org.type());
        yaml.set(base + ".status", org.status().name());
        yaml.set(base + ".founder", uuidString(org.founder()));
        yaml.set(base + ".created-at", org.createdAt());
        yaml.set(base + ".treasury", uuidString(org.treasury()));
        yaml.set(base + ".territories", new ArrayList<>(org.territories()));
        for (OrgMember member : org.members()) {
            String memberBase = base + ".members." + member.uuid();
            yaml.set(memberBase + ".name", member.name());
            yaml.set(memberBase + ".role", member.roleId());
            yaml.set(memberBase + ".joined-at", member.joinedAt());
            yaml.set(memberBase + ".informant", member.isInformant());
        }
        persist();
    }

    @Override
    public void deleteOrg(String orgId) {
        yaml.set(ORGS + "." + escapeKey(orgId), null);
        persist();
    }

    // --- territories -------------------------------------------------------------------------

    @Override
    public Collection<Territory> loadTerritories() {
        List<Territory> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(TERRITORIES);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(new Territory(
                        section.getString("region", key),
                        emptyToNull(section.getString("org")),
                        section.getBoolean("contested", false),
                        section.getLong("control-since")));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed territory '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveTerritory(Territory territory) {
        String base = TERRITORIES + "." + escapeKey(territory.regionId());
        yaml.set(base, null);
        yaml.set(base + ".region", territory.regionId());
        yaml.set(base + ".org", territory.orgId());
        yaml.set(base + ".contested", territory.contested());
        yaml.set(base + ".control-since", territory.controlSince());
        persist();
    }

    @Override
    public void deleteTerritory(String regionId) {
        yaml.set(TERRITORIES + "." + escapeKey(regionId), null);
        persist();
    }

    // --- tracked goods -----------------------------------------------------------------------

    @Override
    public Collection<TrackedGood> loadTrackedGoods() {
        List<TrackedGood> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(GOODS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                TrackedGood good = new TrackedGood(
                        section.getString("item", key),
                        section.getString("good", ""),
                        parseUuid(section.getString("producer")),
                        emptyToNull(section.getString("org")),
                        section.getLong("produced-at"));
                good.setStatus(TrackedGoodStatus.fromString(section.getString("status")));
                good.setQuality(section.getInt("quality", 1));
                result.add(good);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed tracked good '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveTrackedGood(TrackedGood good) {
        String base = GOODS + "." + escapeKey(good.itemUuid());
        yaml.set(base, null);
        yaml.set(base + ".item", good.itemUuid());
        yaml.set(base + ".good", good.goodId());
        yaml.set(base + ".producer", uuidString(good.producer()));
        yaml.set(base + ".org", good.orgId());
        yaml.set(base + ".produced-at", good.producedAt());
        yaml.set(base + ".status", good.status().name());
        yaml.set(base + ".quality", good.quality());
        persist();
    }

    @Override
    public void deleteTrackedGood(String itemUuid) {
        yaml.set(GOODS + "." + escapeKey(itemUuid), null);
        persist();
    }

    // --- production --------------------------------------------------------------------------

    @Override
    public Collection<ProductionProcess> loadProduction() {
        List<ProductionProcess> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(PRODUCTION);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                ProductionProcess process = new ProductionProcess(
                        section.getString("id", key),
                        section.getString("org", ""),
                        section.getString("recipe", ""),
                        section.getString("stage", ""),
                        section.getString("region", ""),
                        parseUuid(section.getString("worker")),
                        section.getString("world", ""),
                        section.getInt("x"), section.getInt("y"), section.getInt("z"),
                        section.getLong("started-at"),
                        section.getLong("expected-at"));
                process.setStatus(ProductionStatus.fromString(section.getString("status")));
                process.setQuality(section.getInt("quality", 1));
                process.setEventId(emptyToNull(section.getString("event")));
                result.add(process);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed production '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveProduction(ProductionProcess process) {
        String base = PRODUCTION + "." + escapeKey(process.id());
        yaml.set(base, null);
        yaml.set(base + ".id", process.id());
        yaml.set(base + ".org", process.orgId());
        yaml.set(base + ".recipe", process.recipeId());
        yaml.set(base + ".stage", process.stageId());
        yaml.set(base + ".region", process.locationRegion());
        yaml.set(base + ".worker", uuidString(process.worker()));
        yaml.set(base + ".world", process.world());
        yaml.set(base + ".x", process.x());
        yaml.set(base + ".y", process.y());
        yaml.set(base + ".z", process.z());
        yaml.set(base + ".started-at", process.startedAt());
        yaml.set(base + ".expected-at", process.expectedAt());
        yaml.set(base + ".status", process.status().name());
        yaml.set(base + ".quality", process.quality());
        yaml.set(base + ".event", process.eventId());
        persist();
    }

    @Override
    public void deleteProduction(String id) {
        yaml.set(PRODUCTION + "." + escapeKey(id), null);
        persist();
    }

    // --- shipments ---------------------------------------------------------------------------

    @Override
    public Collection<Shipment> loadShipments() {
        List<Shipment> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(SHIPMENTS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                Shipment shipment = new Shipment(
                        section.getString("id", key),
                        section.getString("org", ""),
                        section.getString("route", ""),
                        parseUuid(section.getString("carrier")),
                        section.getLong("started-at"),
                        section.getLong("expected-at"));
                shipment.setStatus(ShipmentStatus.fromString(section.getString("status")));
                shipment.setDeliveredAt(section.getLong("delivered-at"));
                for (String raw : section.getStringList("goods")) {
                    String[] parts = raw.split(";", 2);
                    if (parts.length == 2) {
                        shipment.addGood(parts[1], parseInt(parts[0]));
                    }
                }
                for (String raw : section.getStringList("escorts")) {
                    shipment.addEscort(parseUuid(raw));
                }
                result.add(shipment);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed shipment '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveShipment(Shipment shipment) {
        String base = SHIPMENTS + "." + escapeKey(shipment.id());
        yaml.set(base, null);
        yaml.set(base + ".id", shipment.id());
        yaml.set(base + ".org", shipment.orgId());
        yaml.set(base + ".route", shipment.routeId());
        yaml.set(base + ".carrier", uuidString(shipment.carrier()));
        yaml.set(base + ".status", shipment.status().name());
        yaml.set(base + ".started-at", shipment.startedAt());
        yaml.set(base + ".expected-at", shipment.expectedAt());
        yaml.set(base + ".delivered-at", shipment.deliveredAt());
        List<String> goods = new ArrayList<>();
        // amount first, good id last: a ';' in a good id can never shift the amount field.
        shipment.goods().forEach((goodId, amount) -> goods.add(amount + ";" + goodId));
        yaml.set(base + ".goods", goods);
        List<String> escorts = new ArrayList<>();
        for (UUID escort : shipment.escorts()) {
            escorts.add(escort.toString());
        }
        yaml.set(base + ".escorts", escorts);
        persist();
    }

    @Override
    public void deleteShipment(String id) {
        yaml.set(SHIPMENTS + "." + escapeKey(id), null);
        persist();
    }

    // --- laundering --------------------------------------------------------------------------

    @Override
    public Collection<LaunderingProcess> loadLaundering() {
        List<LaunderingProcess> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(LAUNDERING);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                LaunderingProcess process = new LaunderingProcess(
                        section.getString("id", key),
                        section.getString("org", ""),
                        section.getString("method", ""),
                        section.getLong("dirty"),
                        section.getLong("started-at"),
                        section.getLong("expected-at"));
                process.setAmountClean(section.getLong("clean"));
                process.setStatus(LaunderingStatus.fromString(section.getString("status")));
                process.setCompletedAt(section.getLong("completed-at"));
                result.add(process);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed laundering '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveLaundering(LaunderingProcess process) {
        String base = LAUNDERING + "." + escapeKey(process.id());
        yaml.set(base, null);
        yaml.set(base + ".id", process.id());
        yaml.set(base + ".org", process.orgId());
        yaml.set(base + ".method", process.methodId());
        yaml.set(base + ".dirty", process.amountDirty());
        yaml.set(base + ".clean", process.amountClean());
        yaml.set(base + ".status", process.status().name());
        yaml.set(base + ".started-at", process.startedAt());
        yaml.set(base + ".expected-at", process.expectedAt());
        yaml.set(base + ".completed-at", process.completedAt());
        persist();
    }

    @Override
    public void deleteLaundering(String id) {
        yaml.set(LAUNDERING + "." + escapeKey(id), null);
        persist();
    }

    // --- protections -------------------------------------------------------------------------

    @Override
    public Collection<Protection> loadProtections() {
        List<Protection> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(PROTECTIONS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                Protection protection = new Protection(
                        section.getString("id", key),
                        section.getString("org", ""),
                        section.getString("company", ""),
                        section.getLong("amount"),
                        section.getInt("period-days", 7));
                protection.setStatus(ProtectionStatus.fromString(section.getString("status")));
                protection.setCoercionLevel(section.getInt("coercion", 0));
                protection.setLastPayment(section.getLong("last-payment"));
                protection.setNextDue(section.getLong("next-due"));
                result.add(protection);
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed protection '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveProtection(Protection protection) {
        String base = PROTECTIONS + "." + escapeKey(protection.id());
        yaml.set(base, null);
        yaml.set(base + ".id", protection.id());
        yaml.set(base + ".org", protection.orgId());
        yaml.set(base + ".company", protection.companyId());
        yaml.set(base + ".amount", protection.amount());
        yaml.set(base + ".period-days", protection.periodDays());
        yaml.set(base + ".status", protection.status().name());
        yaml.set(base + ".coercion", protection.coercionLevel());
        yaml.set(base + ".last-payment", protection.lastPayment());
        yaml.set(base + ".next-due", protection.nextDue());
        persist();
    }

    @Override
    public void deleteProtection(String id) {
        yaml.set(PROTECTIONS + "." + escapeKey(id), null);
        persist();
    }

    // --- crime events ------------------------------------------------------------------------

    @Override
    public Collection<CrimeEvent> loadEvents() {
        List<CrimeEvent> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(EVENTS);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                List<UUID> members = new ArrayList<>();
                for (String raw : section.getStringList("members")) {
                    UUID uuid = parseUuid(raw);
                    if (uuid != null) {
                        members.add(uuid);
                    }
                }
                result.add(new CrimeEvent(
                        section.getString("id", key),
                        CrimeEventType.fromString(section.getString("type")),
                        emptyToNull(section.getString("org")),
                        members,
                        section.getStringList("goods"),
                        section.getString("world", ""),
                        section.getInt("x"), section.getInt("y"), section.getInt("z"),
                        section.getLong("timestamp"),
                        emptyToNull(section.getString("dossier"))));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed event '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveEvent(CrimeEvent event) {
        String base = EVENTS + "." + escapeKey(event.id());
        yaml.set(base, null);
        yaml.set(base + ".id", event.id());
        yaml.set(base + ".type", event.type().name());
        yaml.set(base + ".org", event.orgId());
        yaml.set(base + ".world", event.world());
        yaml.set(base + ".x", event.x());
        yaml.set(base + ".y", event.y());
        yaml.set(base + ".z", event.z());
        yaml.set(base + ".timestamp", event.timestamp());
        yaml.set(base + ".dossier", event.dossierId());
        List<String> members = new ArrayList<>();
        for (UUID member : event.members()) {
            members.add(member.toString());
        }
        yaml.set(base + ".members", members);
        yaml.set(base + ".goods", new ArrayList<>(event.goodItemUuids()));
        persist();
    }

    // --- discoveries -------------------------------------------------------------------------

    @Override
    public Collection<Discovery> loadDiscoveries() {
        List<Discovery> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection(DISCOVERIES);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                result.add(new Discovery(
                        section.getString("id", key),
                        section.getString("event", ""),
                        DiscoveryType.fromString(section.getString("type")),
                        parseUuid(section.getString("by")),
                        section.getLong("at"),
                        section.getString("world", ""),
                        section.getInt("x"), section.getInt("y"), section.getInt("z"),
                        emptyToNull(section.getString("dossier"))));
            } catch (RuntimeException exception) {
                logger.warning("[OpenCrime] Skipping malformed discovery '" + key + "': " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void saveDiscovery(Discovery discovery) {
        String base = DISCOVERIES + "." + escapeKey(discovery.id());
        yaml.set(base, null);
        yaml.set(base + ".id", discovery.id());
        yaml.set(base + ".event", discovery.crimeEventId());
        yaml.set(base + ".type", discovery.type().name());
        yaml.set(base + ".by", uuidString(discovery.discoveredBy()));
        yaml.set(base + ".at", discovery.discoveredAt());
        yaml.set(base + ".world", discovery.world());
        yaml.set(base + ".x", discovery.x());
        yaml.set(base + ".y", discovery.y());
        yaml.set(base + ".z", discovery.z());
        yaml.set(base + ".dossier", discovery.dossierId());
        persist();
    }

    // --- treasuries --------------------------------------------------------------------------

    @Override
    public Map<UUID, long[]> loadTreasuries() {
        Map<UUID, long[]> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection(TREASURIES);
        if (root == null) {
            return result;
        }
        for (String key : root.getKeys(false)) {
            UUID treasury = parseUuid(key);
            ConfigurationSection section = root.getConfigurationSection(key);
            if (treasury == null || section == null) {
                continue;
            }
            result.put(treasury, new long[]{section.getLong("clean"), section.getLong("dirty")});
        }
        return result;
    }

    @Override
    public void saveTreasury(UUID treasury, long clean, long dirty) {
        String base = TREASURIES + "." + treasury;
        yaml.set(base + ".clean", clean);
        yaml.set(base + ".dirty", dirty);
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
            result.put(unescapeKey(key), root.getLong(key));
        }
        return result;
    }

    @Override
    public void saveCounter(String key, long value) {
        yaml.set(COUNTERS + "." + escapeKey(key), value);
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
        // '.' is the configuration path separator. Percent-encode it (and the escape char itself) so the
        // mapping is REVERSIBLE and injective: two ids differing only by '.' vs another char can never
        // collide on the same YAML key. '%' -> %25, '.' -> %2E.
        if (id == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '%') {
                out.append("%25");
            } else if (c == '.') {
                out.append("%2E");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String unescapeKey(String key) {
        if (key == null || key.indexOf('%') < 0) {
            return key == null ? "" : key;
        }
        StringBuilder out = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '%' && i + 2 < key.length()) {
                String hex = key.substring(i + 1, i + 3);
                if (hex.equalsIgnoreCase("2E")) {
                    out.append('.');
                    i += 2;
                    continue;
                }
                if (hex.equals("25")) {
                    out.append('%');
                    i += 2;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
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

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return 0;
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
            logger.severe("[OpenCrime] Failed to write data: " + exception.getMessage());
            return;
        }
        if (file.exists()) {
            try {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.warning("[OpenCrime] Failed to refresh data backup: " + exception.getMessage());
            }
        }
        try {
            Files.move(tempFile.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException atomicUnsupported) {
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.severe("[OpenCrime] Failed to commit data: " + exception.getMessage());
            }
        } catch (IOException exception) {
            logger.severe("[OpenCrime] Failed to commit data: " + exception.getMessage());
        }
    }
}
