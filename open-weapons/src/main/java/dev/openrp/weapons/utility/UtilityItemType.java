package dev.openrp.weapons.utility;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;

public enum UtilityItemType {
   C4_REMOTE("c4_remote", "Telecomando C4", Material.PAPER, 90, "Apre il pannello di controllo remoto C4."),
   FINGERPRINT_SHEET("fingerprint_sheet", "Scheda impronte", Material.PAPER, 0, "Conserva una traccia forense scansionata."),
   UV_FLASHLIGHT("uv_flashlight", "Torcia UV", Material.TORCH, 0, "Evidenzia tracce forensi vicine."),
   GPS_TRACKER("gps_tracker", "Tracker GPS", Material.FIREWORK_STAR, 335, "Shift + clic destro per piazzarlo, clic destro per aprire i segnali tracciati."),
   GAG("gag", "Bavaglio", Material.PAPER, 0, "Silenzia un giocatore vicino. Rimuovi con le forbici."),
   BLINDFOLD("blindfold", "Benda", Material.PAPER, 0, "Acceca un giocatore vicino. Rimuovi con le forbici."),
   PARACHUTE("parachute", "Paracadute", Material.FIREWORK_STAR, 193, "Clic destro mentre cadi per rallentare la discesa."),
   GRAPPLING_HOOK("grappling_hook", "Rampino", Material.FISHING_ROD, 0, "Ti tira verso il blocco mirato."),
   STRETCHER("stretcher", "Barella", Material.PAPER, 99, "Trasporta giocatori immobilizzati."),
   FIRE_EXTINGUISHER("fire_extinguisher", "Estintore", Material.FIREWORK_STAR, 240, "Spruzza un cono corto che spegne il fuoco."),
   GASOLINE_CAN("gasoline_can", "Tanica di benzina", Material.BUCKET, 0, "Contenitore sigillato di carburante per veicoli e macchinari."),
   TRAFFIC_PADDLE("traffic_paddle", "Paletta stradale", Material.PAPER, 92, "Paletta segnaletica per posti di blocco."),
   ROAD_BARRIER("road_barrier", "Barriera stradale", Material.FIREWORK_STAR, 45, "Barriera stradale piazzabile."),
   SPIKE_STRIP("spike_strip", "Striscia chiodata", Material.FIREWORK_STAR, 46, "Striscia piazzabile che fora gli pneumatici dei veicoli."),
   SCANNER("scanner", "Scanner", Material.PAPER, 40, "Scansiona un giocatore vicino alla ricerca di oggetti metallici."),
   PAINT_SPRAY("paint_spray", "Spray al peperoncino", Material.FIREWORK_STAR, 357, "Acceca e rallenta un bersaglio vicino."),
   DUFFEL_BAG("duffel_bag", "Borsone", Material.FIREWORK_STAR, 60, "Deposito portatile da 9 slot."),
   GAS_MASK("gas_mask", "Maschera antigas", Material.FIREWORK_STAR, 244, "Indossala per vedere attraverso il fumo."),
   NIGHT_VISION_GOGGLES("night_vision_goggles", "Visore notturno", Material.FIREWORK_STAR, 331, "Indossalo per ottenere Visione Notturna I."),
   FIRE_AXE("fire_axe", "Ascia antincendio", Material.PAPER, 115, "Rompe porte non metalliche e blocchi di legno."),
   SCISSORS("scissors", "Forbici", Material.SHEARS, 0, "Taglia corde, bavagli e bende."),
   ROPE("rope", "Corda", Material.PAPER, 17, "Clic destro su un giocatore per legarlo.");

   private final String id;
   private final String displayName;
   private final Material material;
   private final int customModelData;
   private final List<String> lore;

   UtilityItemType(String id, String displayName, Material material, int customModelData, String... lore) {
      this.id = id;
      this.displayName = displayName;
      this.material = material;
      this.customModelData = customModelData;
      this.lore = Arrays.asList(lore);
   }

   public String getId() {
      return this.id;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Material getMaterial() {
      return this.material;
   }

   public int getCustomModelData() {
      return this.customModelData;
   }

   public List<String> getLore() {
      return this.lore;
   }

   public static List<UtilityItemType> openWeaponsCatalogTypes() {
      return List.of(C4_REMOTE, ROPE, SCISSORS);
   }

   public static UtilityItemType fromId(String id) {
      if (id == null) {
         return null;
      }

      for (UtilityItemType type : values()) {
         if (type.id.equalsIgnoreCase(id)) {
            return type;
         }
      }

      return null;
   }
}
