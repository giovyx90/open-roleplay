package dev.openrp.weapons.utility;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;

public enum UtilityItemType {
   C4_REMOTE("c4_remote", "C4 Remote", Material.PAPER, 90, "Open the C4 remote control panel."),
   FINGERPRINT_SHEET("fingerprint_sheet", "Fingerprint Sheet", Material.PAPER, 0, "Stores one scanned forensic trace."),
   UV_FLASHLIGHT("uv_flashlight", "UV Flashlight", Material.TORCH, 0, "Highlights nearby forensic traces."),
   GPS_TRACKER("gps_tracker", "GPS Tracker", Material.FIREWORK_STAR, 335, "Sneak right-click to place, right-click to open tracked signals."),
   GAG("gag", "Gag", Material.PAPER, 0, "Silences a nearby player. Remove with scissors."),
   BLINDFOLD("blindfold", "Blindfold", Material.PAPER, 0, "Blinds a nearby player. Remove with scissors."),
   PARACHUTE("parachute", "Parachute", Material.FIREWORK_STAR, 193, "Right-click while falling to slow descent."),
   GRAPPLING_HOOK("grappling_hook", "Grappling Hook", Material.FISHING_ROD, 0, "Pull yourself toward the targeted block."),
   STRETCHER("stretcher", "Stretcher", Material.PAPER, 99, "Carry restrained players."),
   FIRE_EXTINGUISHER("fire_extinguisher", "Fire Extinguisher", Material.FIREWORK_STAR, 240, "Sprays a short cone that puts out fire."),
   GASOLINE_CAN("gasoline_can", "Gasoline Can", Material.BUCKET, 0, "Sealed fuel container for vehicles and machinery."),
   TRAFFIC_PADDLE("traffic_paddle", "Traffic Paddle", Material.PAPER, 92, "Hand signal paddle for traffic stops."),
   ROAD_BARRIER("road_barrier", "Road Barrier", Material.FIREWORK_STAR, 45, "Placeable road barrier."),
   SPIKE_STRIP("spike_strip", "Spike Strip", Material.FIREWORK_STAR, 46, "Deployable strip that punctures vehicle tires."),
   SCANNER("scanner", "Scanner", Material.PAPER, 40, "Scans a nearby player for metallic items."),
   PAINT_SPRAY("paint_spray", "Pepper Spray", Material.FIREWORK_STAR, 357, "Blinds and slows a nearby target."),
   DUFFEL_BAG("duffel_bag", "Duffel Bag", Material.FIREWORK_STAR, 60, "Portable 9-slot storage."),
   GAS_MASK("gas_mask", "Gas Mask", Material.FIREWORK_STAR, 244, "Wear to see through smoke."),
   NIGHT_VISION_GOGGLES("night_vision_goggles", "Night Vision Goggles", Material.FIREWORK_STAR, 331, "Wear to gain Night Vision I."),
   FIRE_AXE("fire_axe", "Fire Axe", Material.PAPER, 115, "Breaks non-metal doors and wooden blocks."),
   SCISSORS("scissors", "Scissors", Material.SHEARS, 0, "Cuts rope, gags and blindfolds."),
   ROPE("rope", "Rope", Material.PAPER, 17, "Right-click a player to tie them.");

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
