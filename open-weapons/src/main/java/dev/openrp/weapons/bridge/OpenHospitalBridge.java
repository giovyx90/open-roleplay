package dev.openrp.weapons.bridge;

import org.bukkit.entity.Player;

public final class OpenHospitalBridge {
    public void applyGunshot(Player target, GunshotSeverity severity) {
        // Public Open Roleplay does not expose a hospital API yet.
    }

    public enum GunshotSeverity {
        MINOR,
        MODERATE,
        SEVERE,
        CRITICAL
    }
}
