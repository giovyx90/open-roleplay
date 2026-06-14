package dev.openrp.weapons.bridge.staff;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public record StaffBoardLogEvent(
        String type,
        String source,
        StaffBoardCategory category,
        StaffBoardSeverity severity,
        StaffBoardSensitivity sensitivity,
        UUID actorUuid,
        String actorName,
        UUID targetUuid,
        String targetName,
        Location location,
        String message,
        String metadataJson
) {
    public static Builder builder(String type, String source) {
        return new Builder(type, source);
    }

    public static final class Builder {
        private final String type;
        private final String source;
        private StaffBoardCategory category;
        private StaffBoardSeverity severity;
        private StaffBoardSensitivity sensitivity;
        private UUID actorUuid;
        private String actorName;
        private UUID targetUuid;
        private String targetName;
        private Location location;
        private String message = "";
        private String metadataJson = "{}";

        private Builder(String type, String source) {
            this.type = type;
            this.source = source;
        }

        public Builder category(StaffBoardCategory category) {
            this.category = category;
            return this;
        }

        public Builder severity(StaffBoardSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder sensitivity(StaffBoardSensitivity sensitivity) {
            this.sensitivity = sensitivity;
            return this;
        }

        public Builder actor(Player player) {
            return player == null ? this : actor(player.getUniqueId(), player.getName());
        }

        public Builder actor(UUID uuid, String name) {
            this.actorUuid = uuid;
            this.actorName = name;
            return this;
        }

        public Builder target(Player player) {
            return player == null ? this : target(player.getUniqueId(), player.getName());
        }

        public Builder target(UUID uuid, String name) {
            this.targetUuid = uuid;
            this.targetName = name;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public Builder metadataJson(String metadataJson) {
            this.metadataJson = metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson;
            return this;
        }

        public StaffBoardLogEvent build() {
            return new StaffBoardLogEvent(type, source, category, severity, sensitivity, actorUuid, actorName,
                    targetUuid, targetName, location, message, metadataJson);
        }
    }
}
