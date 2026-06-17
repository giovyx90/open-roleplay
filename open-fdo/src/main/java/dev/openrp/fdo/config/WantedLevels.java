package dev.openrp.fdo.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Holds the configured wanted levels ({@code wanted.yml}). The number of levels and their meaning are
 * config-defined; the core only validates that a requested level exists.
 */
public final class WantedLevels {

    private final List<WantedLevelDef> levels = new ArrayList<>();

    /** Replaces the levels from the {@code levels} list of {@code wanted.yml}. */
    public void load(ConfigurationSection root) {
        levels.clear();
        if (root == null) {
            return;
        }
        List<?> rawList = root.getList("levels");
        if (rawList != null) {
            for (Object element : rawList) {
                if (element instanceof Map<?, ?> map && map.get("level") instanceof Number number) {
                    levels.add(new WantedLevelDef(
                            number.intValue(),
                            map.get("label") == null ? ("L" + number.intValue()) : String.valueOf(map.get("label")),
                            map.get("color") == null ? "red" : String.valueOf(map.get("color"))));
                }
            }
        }
        levels.sort((a, b) -> Integer.compare(a.level(), b.level()));
    }

    public Optional<WantedLevelDef> get(int level) {
        return levels.stream().filter(def -> def.level() == level).findFirst();
    }

    public boolean exists(int level) {
        return get(level).isPresent();
    }

    public List<WantedLevelDef> all() {
        return Collections.unmodifiableList(levels);
    }

    public int max() {
        return levels.isEmpty() ? 0 : levels.get(levels.size() - 1).level();
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }
}
