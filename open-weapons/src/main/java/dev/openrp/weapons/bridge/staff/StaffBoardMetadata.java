package dev.openrp.weapons.bridge.staff;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StaffBoardMetadata {
    private final Map<String, Object> values = new LinkedHashMap<>();

    private StaffBoardMetadata() {
    }

    public static StaffBoardMetadata create() {
        return new StaffBoardMetadata();
    }

    public StaffBoardMetadata put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public StaffBoardMetadata putLocation(Location location) {
        if (location == null) {
            return this;
        }
        put("world", location.getWorld() == null ? "" : location.getWorld().getName());
        put("x", location.getX());
        put("y", location.getY());
        put("z", location.getZ());
        return this;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(entry.getKey())).append('"').append(':')
                    .append(jsonValue(entry.getValue()));
        }
        return json.append('}').toString();
    }

    private String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
