package dev.openrp.politics.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * How a charge is assigned. The core supports four mechanism families - {@code election},
 * {@code appointment}, {@code hereditary}, {@code conquest} - all configured here. Beyond the type the
 * parameters differ per family, so they are kept as an opaque key/value bag the relevant service
 * reads; the config decides every value, the core never hardcodes one.
 */
public final class AssignmentMechanism {

    public static final String ELECTION = "election";
    public static final String APPOINTMENT = "appointment";
    public static final String HEREDITARY = "hereditary";
    public static final String CONQUEST = "conquest";

    private final String type;
    private final Map<String, Object> params;

    public AssignmentMechanism(String type, Map<String, Object> params) {
        this.type = type == null || type.isBlank() ? APPOINTMENT : type.trim().toLowerCase(Locale.ROOT);
        this.params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /** Reads a mechanism block; falls back to the government default type when none is declared. */
    public static AssignmentMechanism from(ConfigurationSection section, String defaultType) {
        if (section == null) {
            return new AssignmentMechanism(defaultType, Map.of());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (!"type".equals(key)) {
                values.put(key, section.get(key));
            }
        }
        return new AssignmentMechanism(section.getString("type", defaultType), values);
    }

    public String type() {
        return type;
    }

    public boolean is(String candidate) {
        return type.equalsIgnoreCase(candidate);
    }

    public String string(String key, String fallback) {
        Object value = params.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public int integer(String key, int fallback) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    public boolean bool(String key, boolean fallback) {
        Object value = params.get(key);
        return value instanceof Boolean b ? b : fallback;
    }

    public Map<String, Object> params() {
        return params;
    }
}
