package dev.openrp.weapons.attachments;

import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class AttachmentAuditLogger {
    private final WeaponsModule module;
    private final Path logPath;

    public AttachmentAuditLogger(WeaponsModule module) {
        this.module = module;
        this.logPath = module.getCore().getDataFolder().toPath().resolve("weapon_attachment_audit.log");
    }

    public void log(Player player, String action, WeaponDefinition weapon, AttachmentDefinition attachment) {
        if (player == null || weapon == null || attachment == null) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String line = String.format(Locale.ROOT,
                "timestamp=%d iso=%s action=%s player_uuid=%s player_name=%s weapon_id=%s attachment_id=%s illegal=%s%n",
                timestamp,
                Instant.ofEpochMilli(timestamp),
                clean(action),
                safeUuid(player.getUniqueId()),
                clean(player.getName()),
                clean(weapon.getId()),
                clean(attachment.getId()),
                attachment.isIllegal());

        module.getCore().getLogger().info("[OpenWeapons] Attachment audit: " + line.strip());
        module.getCore().getServer().getScheduler().runTaskAsynchronously(module.getCore(), () -> append(line));
    }

    private void append(String line) {
        try {
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            module.getCore().getLogger().warning("[OpenWeapons] Impossibile scrivere il log audit accessori: " + e.getMessage());
        }
    }

    private String safeUuid(UUID uuid) {
        return uuid != null ? uuid.toString() : "unknown";
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replace('\n', '_').replace('\r', '_').replace(' ', '_');
    }
}
