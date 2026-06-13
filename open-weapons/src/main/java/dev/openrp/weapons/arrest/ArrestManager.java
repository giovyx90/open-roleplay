package dev.openrp.weapons.arrest;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import it.meridian.core.staffboard.StaffBoardMetadata;
import it.meridian.core.staffboard.model.StaffBoardCategory;
import it.meridian.core.staffboard.model.StaffBoardLogEvent;
import it.meridian.core.staffboard.model.StaffBoardSensitivity;
import it.meridian.core.staffboard.model.StaffBoardSeverity;
import it.meridian.core.web.WebRecordPublisher;
import it.meridian.police.module.PoliceModule;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArrestManager {
    private final WeaponsModule module;
    private final Map<UUID, ArrestRecord> arrestedPlayers = new ConcurrentHashMap<>();

    public ArrestManager(WeaponsModule module) {
        this.module = module;
        load();
        startExpiryChecker();
    }

    public void arrest(ArrestRecord record) {
        arrestedPlayers.put(record.getPlayerUuid(), record);
        save();
        publish(record);
        publishPoliceArrest(record);
        emitArrestCreated(record);

        Player player = Bukkit.getPlayer(record.getPlayerUuid());
        if (player != null && player.isOnline()) {
            // Uncuff first
            module.getHandcuffManager().uncuff(player);

            // Teleport to jail region center
            Location jailLoc = getRegionCenter(record.getJailRegionId());
            if (jailLoc != null) {
                player.teleport(jailLoc);
            }

            String timeStr = record.getJailTimeHours() >= 1
                    ? String.format("%.0f ora/e", record.getJailTimeHours())
                    : String.format("%.0f minuto/i", record.getJailTimeHours() * 60);

            player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_RED));
            player.sendMessage(Component.text("  Sei stato arrestato!", NamedTextColor.RED));
            player.sendMessage(Component.text("  Motivo: " + record.getReason(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("  Pena: " + timeStr, NamedTextColor.GRAY));
            if (record.getBailAmount() > 0) {
                player.sendMessage(Component.text("  Cauzione: $" + String.format("%.2f", record.getBailAmount()), NamedTextColor.GOLD));
                player.sendMessage(Component.text("  Usa /bail per pagare e venire rilasciato.", NamedTextColor.YELLOW));
            }
            player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_RED));
        }
    }

    public void release(UUID playerUuid, String reason) {
        ArrestRecord record = arrestedPlayers.remove(playerUuid);
        if (record == null) return;
        save();
        WebRecordPublisher.delete(module.getCore(), "arrest", playerUuid.toString());
        emitArrestReleased(record, reason);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            // Teleport to world spawn
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(Component.text("Sei stato rilasciato dal carcere! Motivo: " + reason, NamedTextColor.GREEN));
        }
    }

    public void save() {
        java.io.File file = new java.io.File(module.getCore().getDataFolder(), "arrests.yml");
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, ArrestRecord> entry : arrestedPlayers.entrySet()) {
            ArrestRecord r = entry.getValue();
            String path = "arrests." + entry.getKey().toString();
            config.set(path + ".playerName", r.getPlayerName());
            config.set(path + ".officerUuid", r.getOfficerUuid().toString());
            config.set(path + ".officerName", r.getOfficerName());
            config.set(path + ".jailRegionId", r.getJailRegionId());
            config.set(path + ".reason", r.getReason());
            config.set(path + ".jailTimeHours", r.getJailTimeHours());
            config.set(path + ".bailAmount", r.getBailAmount());
            config.set(path + ".arrestTime", r.getArrestTime().toEpochMilli());
            config.set(path + ".releaseTime", r.getReleaseTime().toEpochMilli());
        }
        try {
            config.save(file);
        } catch (Exception e) {
            module.getCore().getLogger().warning("Impossibile salvare arrests.yml: " + e.getMessage());
        }
    }

    public void load() {
        java.io.File file = new java.io.File(module.getCore().getDataFolder(), "arrests.yml");
        if (!file.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("arrests");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID playerUuid = UUID.fromString(key);
                String path = "arrests." + key;
                String playerName = config.getString(path + ".playerName");
                UUID officerUuid = UUID.fromString(config.getString(path + ".officerUuid"));
                String officerName = config.getString(path + ".officerName");
                String jailRegionId = config.getString(path + ".jailRegionId");
                String reason = config.getString(path + ".reason");
                double jailTimeHours = config.getDouble(path + ".jailTimeHours");
                double bailAmount = config.getDouble(path + ".bailAmount");
                java.time.Instant arrestTime = java.time.Instant.ofEpochMilli(config.getLong(path + ".arrestTime"));
                java.time.Instant releaseTime = java.time.Instant.ofEpochMilli(config.getLong(path + ".releaseTime"));

                ArrestRecord r = new ArrestRecord(playerUuid, playerName, officerUuid, officerName,
                        jailRegionId, reason, jailTimeHours, bailAmount, arrestTime, releaseTime);
                arrestedPlayers.put(playerUuid, r);
                publish(r);
            } catch (Exception e) {
                module.getCore().getLogger().warning("Impossibile caricare il record arresto per " + key + ": " + e.getMessage());
            }
        }
    }

    public boolean isArrested(UUID uuid) {
        return arrestedPlayers.containsKey(uuid);
    }

    public ArrestRecord getRecord(UUID uuid) {
        return arrestedPlayers.get(uuid);
    }

    public Collection<ArrestRecord> getAllArrests() {
        return Collections.unmodifiableCollection(arrestedPlayers.values());
    }

    public List<String> getJailRegions() {
        List<String> regions = new ArrayList<>();
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            for (World world : Bukkit.getWorlds()) {
                RegionManager rm = container.get(BukkitAdapter.adapt(world));
                if (rm != null) {
                    for (String id : rm.getRegions().keySet()) {
                        if (id.startsWith("jail_")) {
                            regions.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            module.getCore().getLogger().warning("[Arrest] Impossibile interrogare le regioni WorldGuard: " + e.getMessage());
        }
        return regions;
    }

    public Location getRegionCenter(String regionId) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            for (World world : Bukkit.getWorlds()) {
                RegionManager rm = container.get(BukkitAdapter.adapt(world));
                if (rm != null) {
                    ProtectedRegion region = rm.getRegion(regionId);
                    if (region != null) {
                        com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
                        com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
                        double x = (min.x() + max.x()) / 2.0;
                        double y = min.y() + 1;
                        double z = (min.z() + max.z()) / 2.0;
                        return new Location(world, x, y, z);
                    }
                }
            }
        } catch (Exception e) {
            module.getCore().getLogger().warning("[Arrest] Impossibile ottenere il centro della regione: " + e.getMessage());
        }
        return null;
    }

    public boolean isInJailRegion(Player player, String regionId) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (rm != null) {
                ProtectedRegion region = rm.getRegion(regionId);
                if (region != null) {
                    com.sk89q.worldedit.math.BlockVector3 pos = BukkitAdapter.asBlockVector(player.getLocation());
                    return region.contains(pos);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimer(module.getCore(), () -> {
            List<UUID> toRelease = new ArrayList<>();
            for (Map.Entry<UUID, ArrestRecord> entry : arrestedPlayers.entrySet()) {
                if (entry.getValue().isExpired()) {
                    toRelease.add(entry.getKey());
                }
            }
            for (UUID uuid : toRelease) {
                release(uuid, "Pena scontata");
            }
        }, 20L, 20L); // Check every second
    }

    private void publish(ArrestRecord record) {
        String json = "{"
                + WebRecordPublisher.jsonPair("playerName", record.getPlayerName()) + ","
                + WebRecordPublisher.jsonPair("reason", record.getReason()) + ","
                + WebRecordPublisher.jsonPair("officerName", record.getOfficerName()) + ","
                + WebRecordPublisher.jsonPair("jailRegionId", record.getJailRegionId()) + ","
                + WebRecordPublisher.jsonPair("jailTimeHours", record.getJailTimeHours()) + ","
                + WebRecordPublisher.jsonPair("bailAmount", record.getBailAmount()) + ","
                + WebRecordPublisher.jsonPair("arrestTime", record.getArrestTime().toEpochMilli()) + ","
                + WebRecordPublisher.jsonPair("releaseTime", record.getReleaseTime().toEpochMilli())
                + "}";
        WebRecordPublisher.upsert(module.getCore(), "arrest", record.getPlayerUuid().toString(),
                record.getPlayerUuid().toString(), json);
    }

    private void publishPoliceArrest(ArrestRecord record) {
        try {
            PoliceModule police = module.getCore().getModuleManager().getModule(PoliceModule.class);
            if (police == null || police.getService() == null) return;
            police.getService().recordArrest(
                    record.getPlayerUuid(),
                    record.getPlayerName(),
                    record.getOfficerUuid(),
                    record.getOfficerName(),
                    record.getJailRegionId(),
                    record.getReason(),
                    record.getJailTimeHours(),
                    record.getBailAmount(),
                    record.getArrestTime(),
                    record.getReleaseTime(),
                    "OpenWeapons");
        } catch (Exception e) {
            module.getCore().getLogger().warning("[OpenWeapons] Impossibile pubblicare l'arresto verso NEXTPolice: " + e.getMessage());
        }
    }

    private void emitArrestCreated(ArrestRecord record) {
        StaffBoardMetadata metadata = StaffBoardMetadata.create()
                .put("officer_uuid", record.getOfficerUuid())
                .put("officer_name", record.getOfficerName())
                .put("target_uuid", record.getPlayerUuid())
                .put("target_name", record.getPlayerName())
                .put("reason", record.getReason())
                .put("duration", record.getJailTimeHours())
                .put("jail_region_id", record.getJailRegionId())
                .put("bail_amount", record.getBailAmount())
                .put("source_system", "OpenWeapons")
                .put("arrest_time", record.getArrestTime().toString())
                .put("release_time", record.getReleaseTime().toString());

        module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("fdo.arrest.created", "OpenWeapons")
                .category(StaffBoardCategory.FDO)
                .severity(StaffBoardSeverity.WARNING)
                .sensitivity(StaffBoardSensitivity.SENSITIVE)
                .actor(record.getOfficerUuid(), record.getOfficerName())
                .target(record.getPlayerUuid(), record.getPlayerName())
                .message(record.getPlayerName() + " arrestato da " + record.getOfficerName() + ".")
                .metadataJson(metadata.toJson())
                .build());
    }

    private void emitArrestReleased(ArrestRecord record, String reason) {
        StaffBoardMetadata metadata = StaffBoardMetadata.create()
                .put("target_uuid", record.getPlayerUuid())
                .put("target_name", record.getPlayerName())
                .put("reason", reason)
                .put("jail_region_id", record.getJailRegionId())
                .put("source_system", "OpenWeapons");

        module.getCore().getStaffBoardPublisher().emit(StaffBoardLogEvent.builder("fdo.arrest.released", "OpenWeapons")
                .category(StaffBoardCategory.FDO)
                .severity(StaffBoardSeverity.NOTICE)
                .sensitivity(StaffBoardSensitivity.DEPARTMENT_ONLY)
                .target(record.getPlayerUuid(), record.getPlayerName())
                .message(record.getPlayerName() + " rilasciato dal carcere.")
                .metadataJson(metadata.toJson())
                .build());
    }
}
