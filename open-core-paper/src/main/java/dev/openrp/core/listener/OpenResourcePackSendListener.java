package dev.openrp.core.listener;

import dev.openrp.core.OpenCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.UUID;

public final class OpenResourcePackSendListener implements Listener {
    private final OpenCorePlugin plugin;

    public OpenResourcePackSendListener(OpenCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ResourcePackSettings settings = loadSettings();
        if (!settings.enabled() || settings.url().isBlank()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            try {
                player.setResourcePack(settings.id(), settings.url(), settings.sha1(), settings.prompt(), settings.required());
                plugin.getLogger().info("[OpenCore] Resource pack inviato a " + player.getName() + ".");
            } catch (RuntimeException error) {
                plugin.getLogger().warning("[OpenCore] Invio resource pack fallito per "
                        + player.getName() + ": " + error.getMessage());
            }
        }, settings.delayTicks());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            return;
        }
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED
                || status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || status == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD
                || status == PlayerResourcePackStatusEvent.Status.INVALID_URL) {
            plugin.getLogger().warning("[OpenCore] Resource pack status di "
                    + event.getPlayer().getName() + ": " + status + ".");
        }
    }

    private ResourcePackSettings loadSettings() {
        FileConfiguration config = plugin.getConfig();
        String url = config.getString("resource-pack.url", "");
        byte[] hash = parseSha1(config.getString("resource-pack.sha1", ""));
        UUID id = parseId(config.getString("resource-pack.id", ""), hash);
        boolean required = config.getBoolean("resource-pack.required", true);
        String prompt = config.getString("resource-pack.prompt",
                "Open Roleplay richiede il resource pack del server.");
        long delayTicks = Math.max(1L, config.getLong("resource-pack.delay-ticks", 20L));
        boolean enabled = config.getBoolean("resource-pack.enabled", false);
        return new ResourcePackSettings(enabled, url == null ? "" : url.strip(), hash, id, required, prompt, delayTicks);
    }

    private byte[] parseSha1(String value) {
        String cleaned = value == null ? "" : value.strip();
        if (!cleaned.matches("(?i)[0-9a-f]{40}")) {
            if (!cleaned.isBlank()) {
                plugin.getLogger().warning("[OpenCore] resource-pack.sha1 non valido; uso hash vuoto.");
            }
            return new byte[0];
        }
        byte[] bytes = new byte[20];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(cleaned.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private UUID parseId(String value, byte[] hash) {
        String cleaned = value == null ? "" : value.strip();
        if (!cleaned.isBlank()) {
            try {
                return UUID.fromString(cleaned);
            } catch (IllegalArgumentException error) {
                plugin.getLogger().warning("[OpenCore] resource-pack.id non valido; genero un id dal pack.");
            }
        }
        return hash.length == 0 ? UUID.randomUUID() : UUID.nameUUIDFromBytes(hash);
    }

    private record ResourcePackSettings(
            boolean enabled,
            String url,
            byte[] sha1,
            UUID id,
            boolean required,
            String prompt,
            long delayTicks
    ) {
    }
}
