package dev.openrp.crime.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.crime.capability.Capability;

/**
 * The configured organisation hierarchy and founding requirements, loaded from {@code syndicate.yml}.
 * Ranks are ordered low-to-high; a fantasy server's "Adepto -> Gran Maestro" loads exactly the same
 * way as a realistic "Picciotto -> Boss". The core never knows the difference.
 */
public final class Hierarchy {

    private final Map<String, OrgRank> byId = new LinkedHashMap<>();
    private final List<OrgRank> ordered = new ArrayList<>();

    private int minMembers = 1;
    private long initialCapital;
    private boolean requireLocation;
    private String locationRegion = "";

    public void load(ConfigurationSection root) {
        byId.clear();
        ordered.clear();
        minMembers = 1;
        initialCapital = 0L;
        requireLocation = false;
        locationRegion = "";
        if (root == null) {
            return;
        }
        for (Map<?, ?> raw : root.getMapList("hierarchy")) {
            String id = string(raw.get("id"));
            if (id == null || id.isBlank()) {
                continue;
            }
            int order = intValue(raw.get("order"), ordered.size());
            boolean apical = Boolean.parseBoolean(String.valueOf(raw.get("apical")));
            java.util.Set<Capability> caps = new java.util.HashSet<>();
            Object capsRaw = raw.get("capabilities");
            if (capsRaw instanceof List<?> list) {
                for (Object capName : list) {
                    Capability.fromString(String.valueOf(capName)).ifPresent(caps::add);
                }
            }
            String display = string(raw.get("display_name"));
            OrgRank rank = new OrgRank(id, display == null || display.isBlank() ? id : display, order, apical, caps);
            byId.put(id, rank);
            ordered.add(rank);
        }
        ordered.sort((a, b) -> Integer.compare(a.order(), b.order()));

        ConfigurationSection founding = root.getConfigurationSection("founding");
        if (founding != null) {
            minMembers = Math.max(1, founding.getInt("min_members", 1));
            initialCapital = Math.max(0L, founding.getLong("initial_capital", 0L));
            requireLocation = founding.getBoolean("require_location", false);
            locationRegion = founding.getString("location_region", "");
        }
    }

    public List<OrgRank> ranks() {
        return Collections.unmodifiableList(ordered);
    }

    public Optional<OrgRank> rank(String id) {
        return Optional.ofNullable(id == null ? null : byId.get(id));
    }

    /** The lowest rank, given to newly recruited members. */
    public Optional<OrgRank> defaultRank() {
        return ordered.isEmpty() ? Optional.empty() : Optional.of(ordered.get(0));
    }

    /** The apical (top) rank, given to the founder. Falls back to the highest-ordered rank. */
    public Optional<OrgRank> apicalRank() {
        for (OrgRank rank : ordered) {
            if (rank.apical()) {
                return Optional.of(rank);
            }
        }
        return ordered.isEmpty() ? Optional.empty() : Optional.of(ordered.get(ordered.size() - 1));
    }

    public Optional<OrgRank> nextRank(String id) {
        OrgRank current = byId.get(id);
        if (current == null) {
            return Optional.empty();
        }
        OrgRank best = null;
        for (OrgRank rank : ordered) {
            if (rank.order() > current.order() && (best == null || rank.order() < best.order())) {
                best = rank;
            }
        }
        return Optional.ofNullable(best);
    }

    public Optional<OrgRank> previousRank(String id) {
        OrgRank current = byId.get(id);
        if (current == null) {
            return Optional.empty();
        }
        OrgRank best = null;
        for (OrgRank rank : ordered) {
            if (rank.order() < current.order() && (best == null || rank.order() > best.order())) {
                best = rank;
            }
        }
        return Optional.ofNullable(best);
    }

    public boolean isApical(String rankId) {
        return rank(rankId).map(OrgRank::apical).orElse(false);
    }

    public List<String> ids() {
        return List.copyOf(byId.keySet());
    }

    public int minMembers() {
        return minMembers;
    }

    public long initialCapital() {
        return initialCapital;
    }

    public boolean requireLocation() {
        return requireLocation;
    }

    public String locationRegion() {
        return locationRegion;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException | NullPointerException invalid) {
            return fallback;
        }
    }
}
