package dev.openrp.weapons.bridge;

import org.bukkit.plugin.Plugin;

public final class OpenWebRecordPublisher {
    private OpenWebRecordPublisher() {
    }

    public static void upsert(Plugin plugin, String type, String id, String subject, String json) {
        // Public Open Roleplay does not expose a web record bus yet.
    }

    public static void delete(Plugin plugin, String type, String id) {
        // Public Open Roleplay does not expose a web record bus yet.
    }

    public static String jsonPair(String key, Object value) {
        return "\"" + escape(key) + "\":" + jsonValue(value);
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
