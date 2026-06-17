package dev.openrp.fdo.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import dev.openrp.fdo.capability.Capability;

/**
 * Holds the configured ranks per corps and resolves capabilities with inheritance: a rank holds its
 * own capabilities plus every capability granted to lower-order ranks in the same corps. Unknown
 * capability ids in config are skipped (not fatal), so a typo never crashes startup.
 */
public final class RankRegistry {

    private final Map<String, List<Rank>> byCorps = new LinkedHashMap<>();

    /** Replaces the registry contents from the {@code ranks} section of {@code ranks.yml}. */
    public void load(ConfigurationSection root) {
        byCorps.clear();
        if (root == null) {
            return;
        }
        for (String corpsId : root.getKeys(false)) {
            List<?> rawList = root.getList(corpsId);
            List<Rank> ranks = new ArrayList<>();
            if (rawList != null) {
                for (Object element : rawList) {
                    if (element instanceof Map<?, ?> map) {
                        Rank rank = readRank(map);
                        if (rank != null) {
                            ranks.add(rank);
                        }
                    }
                }
            }
            ranks.sort(Comparator.comparingInt(Rank::order));
            byCorps.put(corpsId, ranks);
        }
    }

    private Rank readRank(Map<?, ?> map) {
        Object idValue = map.get("id");
        if (idValue == null) {
            return null;
        }
        String id = String.valueOf(idValue);
        String displayName = map.get("display_name") == null ? id : String.valueOf(map.get("display_name"));
        int order = map.get("order") instanceof Number number ? number.intValue() : 0;
        boolean apical = Boolean.parseBoolean(String.valueOf(map.get("apical")));
        Set<Capability> caps = EnumSet.noneOf(Capability.class);
        Object capsRaw = map.get("capabilities");
        if (capsRaw instanceof List<?> list) {
            for (Object cap : list) {
                Capability.fromString(String.valueOf(cap)).ifPresent(caps::add);
            }
        }
        return new Rank(id, displayName, order, apical, caps);
    }

    public List<Rank> ranks(String corpsId) {
        return byCorps.getOrDefault(corpsId, List.of());
    }

    public Optional<Rank> rank(String corpsId, String rankId) {
        if (rankId == null) {
            return Optional.empty();
        }
        return ranks(corpsId).stream().filter(rank -> rank.id().equals(rankId)).findFirst();
    }

    public Optional<Rank> lowest(String corpsId) {
        List<Rank> ranks = ranks(corpsId);
        return ranks.isEmpty() ? Optional.empty() : Optional.of(ranks.get(0));
    }

    public Optional<Rank> highest(String corpsId) {
        List<Rank> ranks = ranks(corpsId);
        return ranks.isEmpty() ? Optional.empty() : Optional.of(ranks.get(ranks.size() - 1));
    }

    /** The rank immediately above {@code rankId} in the corps, or empty if it is already the top. */
    public Optional<Rank> next(String corpsId, String rankId) {
        List<Rank> ranks = ranks(corpsId);
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).id().equals(rankId) && i + 1 < ranks.size()) {
                return Optional.of(ranks.get(i + 1));
            }
        }
        return Optional.empty();
    }

    /** The rank immediately below {@code rankId} in the corps, or empty if it is already the bottom. */
    public Optional<Rank> previous(String corpsId, String rankId) {
        List<Rank> ranks = ranks(corpsId);
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).id().equals(rankId) && i - 1 >= 0) {
                return Optional.of(ranks.get(i - 1));
            }
        }
        return Optional.empty();
    }

    /**
     * Effective capabilities of a rank: its own plus every lower-order rank's, in the same corps.
     * Returns an empty set for an unknown corps/rank.
     */
    public Set<Capability> capabilitiesFor(String corpsId, String rankId) {
        Set<Capability> result = EnumSet.noneOf(Capability.class);
        Optional<Rank> target = rank(corpsId, rankId);
        if (target.isEmpty()) {
            return result;
        }
        int targetOrder = target.get().order();
        for (Rank rank : ranks(corpsId)) {
            if (rank.order() <= targetOrder) {
                result.addAll(rank.capabilities());
            }
        }
        return result;
    }

    public boolean has(String corpsId, String rankId, Capability capability) {
        return capability != null && capabilitiesFor(corpsId, rankId).contains(capability);
    }
}
