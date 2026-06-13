package dev.openrp.weapons.attachments;

import java.util.Locale;

public enum AttachmentSlot {
    OPTIC("optic"),
    BARREL("barrel"),
    UNDERBARREL("underbarrel"),
    SIDE("side"),
    MAGAZINE("magazine"),
    INTERNAL("internal");

    private final String id;

    AttachmentSlot(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static AttachmentSlot fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AttachmentSlot slot : values()) {
            if (slot.id.equals(normalized) || slot.name().equalsIgnoreCase(value.trim())) {
                return slot;
            }
        }
        return null;
    }
}
