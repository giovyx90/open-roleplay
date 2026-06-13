package dev.openrp.weapons.wanted;

import it.meridian.core.web.WebRecordPublisher;
import dev.openrp.weapons.module.WeaponsModule;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class WantedManager {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Europe/Rome"));
   private final WeaponsModule module;
   private final File file;
   private final Map<UUID, WantedRecord> records = new ConcurrentHashMap<>();

   public WantedManager(WeaponsModule module) {
      this.module = module;
      this.file = new File(module.getCore().getDataFolder(), "wanted.yml");
      this.load();
   }

   public boolean addRecord(WantedRecord record) {
      if (this.records.containsKey(record.getPlayerUuid())) {
         return false;
      }

      this.records.put(record.getPlayerUuid(), record);
      this.save();
      this.publish(record);
      this.broadcastAdded(record);
      return true;
   }

   public WantedRecord removeRecord(String playerName) {
      WantedRecord byStoredName = this.findByName(playerName);
      if (byStoredName != null) {
         this.records.remove(byStoredName.getPlayerUuid());
         this.save();
         WebRecordPublisher.delete(this.module.getCore(), "wanted", byStoredName.getPlayerUuid().toString());
         return byStoredName;
      }

      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
      if (offlinePlayer != null) {
         WantedRecord removed = this.records.remove(offlinePlayer.getUniqueId());
         if (removed != null) {
            this.save();
            WebRecordPublisher.delete(this.module.getCore(), "wanted", removed.getPlayerUuid().toString());
            return removed;
         }
      }

      return null;
   }

   public boolean isWanted(UUID uuid) {
      return this.records.containsKey(uuid);
   }

   public WantedRecord getRecord(UUID uuid) {
      return this.records.get(uuid);
   }

   public WantedRecord findByName(String playerName) {
      for (WantedRecord record : this.records.values()) {
         if (record.getPlayerName().equalsIgnoreCase(playerName)) {
            return record;
         }
      }

      return null;
   }

   public Collection<WantedRecord> getAllRecords() {
      return this.records.values().stream().sorted(Comparator.comparing(WantedRecord::getCreatedAt)).toList();
   }

   public List<WantedRecord> getOnlineRecords() {
      List<WantedRecord> online = new ArrayList<>();

      for (WantedRecord record : this.getAllRecords()) {
         Player player = Bukkit.getPlayer(record.getPlayerUuid());
         if (player != null && player.isOnline()) {
            online.add(record);
         }
      }

      return online;
   }

   public String formatDate(Instant instant) {
      return DATE_FORMAT.format(instant);
   }

   private void broadcastAdded(WantedRecord record) {
      Component message = ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)Component.text()
                                                   .append(
                                                      Component.text("Nuova scheda ricercato aggiunta", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD})
                                                   ))
                                                .append(Component.newline()))
                                             .append(Component.newline()))
                                          .append(Component.text("Aggiunta da: ", NamedTextColor.GRAY)))
                                       .append(Component.text(record.getOfficerName(), NamedTextColor.WHITE)))
                                    .append(Component.newline()))
                                 .append(Component.text("Ricercato: ", NamedTextColor.GRAY)))
                              .append(Component.text(record.getPlayerName(), NamedTextColor.WHITE)))
                           .append(Component.newline()))
                        .append(Component.text("Motivo: ", NamedTextColor.GRAY)))
                     .append(Component.text(record.getReason(), NamedTextColor.WHITE)))
                  .append(Component.newline()))
               .append(Component.newline()))
            .append(Component.text(this.formatDate(record.getCreatedAt()), NamedTextColor.YELLOW)))
         .build();

      for (Player player : Bukkit.getOnlinePlayers()) {
         if (this.module.isLEO(player.getUniqueId()) || player.hasPermission("openrp.wanted.manage")) {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.2F);
         }
      }
   }

   private void save() {
      YamlConfiguration config = new YamlConfiguration();

      for (WantedRecord record : this.records.values()) {
         String path = "wanted." + record.getPlayerUuid();
         config.set(path + ".playerName", record.getPlayerName());
         config.set(path + ".reason", record.getReason());
         config.set(path + ".arrestRequired", record.isArrestRequired());
         config.set(path + ".officerUuid", record.getOfficerUuid().toString());
         config.set(path + ".officerName", record.getOfficerName());
         config.set(path + ".createdAt", record.getCreatedAt().toEpochMilli());
      }

      try {
         config.save(this.file);
      } catch (Exception e) {
         this.module.getCore().getLogger().warning("[Wanted] Impossibile salvare wanted.yml: " + e.getMessage());
      }
   }

   private void load() {
      if (this.file.exists()) {
         YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
         ConfigurationSection section = config.getConfigurationSection("wanted");
         if (section != null) {
            for (String key : section.getKeys(false)) {
               try {
                  String path = "wanted." + key;
                  WantedRecord record = new WantedRecord(
                     UUID.fromString(key),
                     config.getString(path + ".playerName", "Sconosciuto"),
                     config.getString(path + ".reason", "Nessun motivo indicato"),
                     config.getBoolean(path + ".arrestRequired", false),
                     UUID.fromString(config.getString(path + ".officerUuid")),
                     config.getString(path + ".officerName", "Sconosciuto"),
                     Instant.ofEpochMilli(config.getLong(path + ".createdAt"))
                  );
                  this.records.put(record.getPlayerUuid(), record);
                  this.publish(record);
               } catch (Exception e) {
                  this.module.getCore().getLogger().warning("[Wanted] Impossibile caricare la scheda ricercato " + key + ": " + e.getMessage());
               }
            }
         }
      }
   }

   private void publish(WantedRecord record) {
      String json = "{"
         + WebRecordPublisher.jsonPair("playerName", record.getPlayerName())
         + ","
         + WebRecordPublisher.jsonPair("reason", record.getReason())
         + ","
         + WebRecordPublisher.jsonPair("arrestRequired", record.isArrestRequired())
         + ","
         + WebRecordPublisher.jsonPair("officerName", record.getOfficerName())
         + ","
         + WebRecordPublisher.jsonPair("createdAt", record.getCreatedAt().toEpochMilli())
         + "}";
      WebRecordPublisher.upsert(this.module.getCore(), "wanted", record.getPlayerUuid().toString(), record.getPlayerUuid().toString(), json);
   }
}
