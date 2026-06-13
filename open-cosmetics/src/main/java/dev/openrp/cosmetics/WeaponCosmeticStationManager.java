package dev.openrp.cosmetics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeaponCosmeticStationManager implements Listener {
    private static final String USE_PERMISSION = "openrp.cosmetics.use";
    private static final String ADMIN_PERMISSION = "openrp.cosmetics.admin";
    private static final String LEGACY_USE_PERMISSION = "openrp.weapons.cosmetic.use";
    private static final String LEGACY_ADMIN_PERMISSION = "openrp.weapons.cosmetic.admin";
    private static final String STATION_ROOT = "stations";

    private final OpenCosmeticsPlugin plugin;
    private final WeaponCosmeticWorkbenchGUI gui;
    private final NamespacedKey stationKey;
    private final NamespacedKey stationIdKey;
    private final File stationsFile;
    private final Map<String, StationRecord> stations = new LinkedHashMap<>();

    public WeaponCosmeticStationManager(OpenCosmeticsPlugin plugin, WeaponCosmeticWorkbenchGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
        this.stationKey = new NamespacedKey(plugin, "weapon_cosmetic_station");
        this.stationIdKey = new NamespacedKey(plugin, "weapon_cosmetic_station_id");
        this.stationsFile = new File(plugin.getDataFolder(), "weapon_cosmetic_stations.yml");
    }

    public void load() {
        stations.clear();
        if (stationsFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(stationsFile);
            ConfigurationSection root = config.getConfigurationSection(STATION_ROOT);
            if (root != null) {
                for (String rawId : root.getKeys(false)) {
                    ConfigurationSection section = root.getConfigurationSection(rawId);
                    if (section == null) {
                        continue;
                    }
                    String id = normalizeStationId(rawId);
                    String world = section.getString("world", "");
                    if (id.isBlank() || world.isBlank()) {
                        continue;
                    }
                    stations.put(id, new StationRecord(
                            id,
                            world,
                            section.getDouble("x"),
                            section.getDouble("y"),
                            section.getDouble("z"),
                            (float) section.getDouble("yaw"),
                            (float) section.getDouble("pitch")));
                }
            }
        }
        respawnStations();
    }

    public boolean createStation(Player player, String rawId) {
        String id = normalizeStationId(rawId);
        if (id.isBlank()) {
            return false;
        }
        Location location = player.getLocation();
        StationRecord record = new StationRecord(
                id,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        stations.put(id, record);
        removeStationEntities(id);
        spawnStation(record);
        save();
        return true;
    }

    public boolean removeStation(String rawId) {
        String id = normalizeStationId(rawId);
        StationRecord removed = stations.remove(id);
        removeStationEntities(id);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public Collection<StationRecord> stations() {
        return List.copyOf(stations.values());
    }

    @EventHandler
    public void onStationInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isStation(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission(USE_PERMISSION)
                && !player.hasPermission(LEGACY_USE_PERMISSION)
                && !player.hasPermission(ADMIN_PERMISSION)
                && !player.hasPermission(LEGACY_ADMIN_PERMISSION)) {
            player.sendMessage(Component.text("Non hai il permesso di usare i cosmetici arma.", NamedTextColor.RED));
            return;
        }
        gui.open(player);
    }

    private void respawnStations() {
        for (StationRecord record : stations.values()) {
            spawnStation(record);
        }
    }

    private void spawnStation(StationRecord record) {
        World world = Bukkit.getWorld(record.world());
        if (world == null || findStationEntity(record.id()) != null) {
            return;
        }
        Location location = new Location(world, record.x(), record.y(), record.z(), record.yaw(), record.pitch());
        world.spawn(location, Villager.class, villager -> {
            villager.customName(Component.text("Armaiolo Cosmetico", NamedTextColor.GOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.WEAPONSMITH);
            villager.getPersistentDataContainer().set(stationKey, PersistentDataType.BYTE, (byte) 1);
            villager.getPersistentDataContainer().set(stationIdKey, PersistentDataType.STRING, record.id());
        });
    }

    private Entity findStationEntity(String id) {
        String normalizedId = normalizeStationId(id);
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                String entityId = villager.getPersistentDataContainer().get(stationIdKey, PersistentDataType.STRING);
                if (isStation(villager) && normalizedId.equals(normalizeStationId(entityId))) {
                    return villager;
                }
            }
        }
        return null;
    }

    private void removeStationEntities(String id) {
        String normalizedId = normalizeStationId(id);
        List<Entity> toRemove = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                String entityId = villager.getPersistentDataContainer().get(stationIdKey, PersistentDataType.STRING);
                if (isStation(villager) && normalizedId.equals(normalizeStationId(entityId))) {
                    toRemove.add(villager);
                }
            }
        }
        toRemove.forEach(Entity::remove);
    }

    private boolean isStation(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(stationKey, PersistentDataType.BYTE);
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection(STATION_ROOT);
        for (StationRecord record : stations.values()) {
            ConfigurationSection section = root.createSection(record.id());
            section.set("world", record.world());
            section.set("x", record.x());
            section.set("y", record.y());
            section.set("z", record.z());
            section.set("yaw", record.yaw());
            section.set("pitch", record.pitch());
        }
        try {
            config.save(stationsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[OpenCosmetics] Impossibile salvare le postazioni cosmetiche armi: " + e.getMessage());
        }
    }

    public static String normalizeStationId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
    }

    public record StationRecord(String id, String world, double x, double y, double z, float yaw, float pitch) {
    }
}
