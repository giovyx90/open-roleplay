package dev.openrp.weapons.commands;

import dev.openrp.weapons.util.OpenPermissions;
import dev.openrp.weapons.gui.ItemsGUI;
import dev.openrp.weapons.module.WeaponsModule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class ItemsCommand implements CommandExecutor, TabCompleter {
   private static final List<DummyPreset> DUMMY_PRESETS = List.of(
         new DummyPreset("plain", "Senza armatura", null, null, List.of("none")),
         new DummyPreset("vest_light", "Giubbotto leggero", "vest_light", null, List.of("light", "light_vest")),
         new DummyPreset("vest_heavy", "Giubbotto pesante", "vest_heavy", null, List.of("heavy", "heavy_vest")),
         new DummyPreset("vest_heavy_plated", "Giubbotto pesante con piastra", "vest_heavy_plated", null, List.of("plated", "heavy_plated", "plate")),
         new DummyPreset("helmet_ballistic", "Casco balistico", null, "ballistic_helmet", List.of("ballistic_helmet", "helmet")),
         new DummyPreset("helmet_riot", "Casco antisommossa", null, "riot_helmet", List.of("riot_helmet", "riot")),
         new DummyPreset("helmet_sf", "Casco forze speciali", null, "sf_helmet", List.of("sf_helmet", "sf")),
         new DummyPreset("light_full", "Giubbotto leggero + casco balistico", "vest_light", "ballistic_helmet", List.of("light_helmet")),
         new DummyPreset("heavy_full", "Giubbotto pesante + casco balistico", "vest_heavy", "ballistic_helmet", List.of("heavy_helmet")),
         new DummyPreset("plated_full", "Giubbotto pesante con piastra + casco FS", "vest_heavy_plated", "sf_helmet", List.of("plated_sf", "full"))
   );

   private final WeaponsModule module;
   private final ItemsGUI gui;
   private final DamageDummyListener damageDummyListener;

   public ItemsCommand(WeaponsModule module, ItemsGUI gui, DamageDummyListener damageDummyListener) {
      this.module = module;
      this.gui = gui;
      this.damageDummyListener = damageDummyListener;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         if (sender instanceof Player player) {
            if (!canViewCatalog(player)) {
               player.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.VIEW + " per sfogliare il catalogo oggetti.", NamedTextColor.RED));
               return true;
            }
            this.gui.open(player);
         } else {
            sender.sendMessage("Uso: /oggetti <cerca|lista|dai|ricarica|bersaglio>");
         }
         return true;
      }

      switch (args[0].toLowerCase(Locale.ROOT)) {
         case "ricarica" -> {
            if (!canReload(sender)) {
               sender.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.DEBUG + " o " + OpenPermissions.Staff.RELOAD + " per ricaricare gli oggetti arma.", NamedTextColor.RED));
               return true;
            }
            this.reloadItems(sender);
            return true;
         }
         case "cerca" -> {
	            if (!(sender instanceof Player player)) {
	               sender.sendMessage("Solo i giocatori possono aprire la GUI di ricerca oggetti.");
	               return true;
	            }
            if (!canViewCatalog(player)) {
               player.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.VIEW + " per cercare nel catalogo oggetti.", NamedTextColor.RED));
               return true;
            }
            if (args.length < 2) {
               player.sendMessage(Component.text("Uso: /oggetti cerca <query> [pagina]", NamedTextColor.RED));
               return true;
            }
            boolean hasPage = args.length > 2 && this.isPositiveInteger(args[args.length - 1]);
            int page = hasPage ? this.parsePage(args[args.length - 1], 1) : 1;
            int queryEnd = hasPage ? args.length - 1 : args.length;
            String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, queryEnd));
            this.gui.openSearch(player, query, page);
            return true;
         }
         case "lista" -> {
	            if (!(sender instanceof Player player)) {
	               sender.sendMessage("Solo i giocatori possono aprire la lista oggetti.");
	               return true;
	            }
            if (!canViewCatalog(player)) {
               player.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.VIEW + " per elencare il catalogo oggetti.", NamedTextColor.RED));
               return true;
            }
            String category = args.length >= 2 ? args[1] : "all";
            int page = args.length >= 3 ? this.parsePage(args[2], 1) : 1;
            this.gui.openCatalogList(player, category, page);
            return true;
         }
         case "dai" -> {
            this.handleGive(sender, args);
            return true;
         }
         case "bersaglio" -> {
            this.handleDummy(sender, args);
            return true;
         }
	         default -> {
	            if (sender instanceof Player player) {
                  if (!canViewCatalog(player)) {
                     player.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.VIEW + " per sfogliare il catalogo oggetti.", NamedTextColor.RED));
                     return true;
                  }
	               this.gui.open(player);
            } else {
               sender.sendMessage("Uso: /oggetti <cerca|lista|dai|ricarica|bersaglio>");
            }
            return true;
         }
      }
   }

   private void handleDummy(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Solo i giocatori possono generare bersagli di test danno armi.");
         return;
      }
      if (!OpenPermissions.hasAny(player, OpenPermissions.Weapons.DEBUG, OpenPermissions.Weapons.ADMIN, OpenPermissions.Test.DEBUG)) {
         player.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.DEBUG + " o " + OpenPermissions.Test.DEBUG + " per generare un bersaglio test danno.", NamedTextColor.RED));
         return;
      }
      double health = 100.0D;
      DummyPreset preset = findDummyPreset("plain");
      for (int i = 1; i < args.length; i++) {
         String part = args[i];
         try {
            health = Math.max(1.0D, Math.min(2048.0D, Double.parseDouble(part)));
            continue;
         } catch (NumberFormatException ignored) {
            // Try as preset below.
         }
         DummyPreset parsedPreset = findDummyPreset(part);
         if (parsedPreset == null) {
            player.sendMessage(Component.text("Uso: /oggetti bersaglio [vita] [preset]", NamedTextColor.RED));
            player.sendMessage(Component.text("Preset: " + String.join(", ", dummyPresetIds()), NamedTextColor.YELLOW));
            return;
         }
         preset = parsedPreset;
      }
      Zombie dummy = player.getWorld().spawn(player.getLocation(), Zombie.class);
      this.damageDummyListener.mark(dummy, health);
      applyDummyPreset(dummy, preset);
      player.sendMessage(Component.text("Bersaglio test danno armi generato con " + String.format(Locale.US, "%.1f", health)
            + " HP (" + preset.displayName() + ").", NamedTextColor.GREEN));
   }

   private void applyDummyPreset(Zombie dummy, DummyPreset preset) {
      if (dummy == null || preset == null) {
         return;
      }
      EntityEquipment equipment = dummy.getEquipment();
      if (equipment == null) {
         return;
      }
      if (preset.armorId() != null && this.module.getArmorManager() != null) {
         ItemStack chestplate = this.module.getArmorManager().createItemStack(preset.armorId());
         if (chestplate != null) {
            equipment.setChestplate(chestplate);
            equipment.setChestplateDropChance(0.0f);
         }
      }
      if (preset.helmetId() != null && this.module.getHelmetManager() != null) {
         ItemStack helmet = this.module.getHelmetManager().createItemStack(preset.helmetId());
         if (helmet != null) {
            equipment.setHelmet(helmet);
            equipment.setHelmetDropChance(0.0f);
         }
      }
   }

   private DummyPreset findDummyPreset(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      String normalized = raw.toLowerCase(Locale.ROOT);
      for (DummyPreset preset : DUMMY_PRESETS) {
         if (preset.id().equals(normalized) || preset.aliases().contains(normalized)) {
            return preset;
         }
      }
      return null;
   }

   private List<String> dummyPresetIds() {
      return DUMMY_PRESETS.stream().map(DummyPreset::id).toList();
   }

   private void reloadItems(CommandSender sender) {
      this.module.getWeaponRegistry().load(new File(this.module.getCore().getDataFolder(), "weapons.yml"));
      this.module.getAmmoRegistry().load(new File(this.module.getCore().getDataFolder(), "ammo.yml"));
      this.module.getAttachmentRegistry().load(new File(this.module.getCore().getDataFolder(), "attachments.yml"));
      this.module.reloadOpenCosmetics();
      if (this.module.getArmorManager() != null) {
         this.module.getArmorManager().load(new File(this.module.getCore().getDataFolder(), "armor.yml"));
      }
      if (this.module.getHelmetManager() != null) {
         this.module.getHelmetManager().load(new File(this.module.getCore().getDataFolder(), "armor.yml"));
      }
      this.module.getGrenadeManager().load(new File(this.module.getCore().getDataFolder(), "grenades.yml"));
      sender.sendMessage(Component.text("Configurazioni di armi, munizioni, accessori, cosmetici, armature e granate ricaricate.", NamedTextColor.GREEN));
   }

   private void handleGive(CommandSender sender, String[] args) {
      if (!canGiveItems(sender)) {
         sender.sendMessage(Component.text("Ti serve " + OpenPermissions.Weapons.GIVE + " o " + OpenPermissions.Test.ITEMS + " per dare oggetti dal catalogo.", NamedTextColor.RED));
         return;
      }
      if (args.length < 2) {
         sender.sendMessage(Component.text("Uso: /oggetti dai <id> [quantita'] [giocatore]", NamedTextColor.RED));
         return;
      }

      Player target;
      if (args.length >= 4) {
         target = Bukkit.getPlayerExact(args[3]);
         if (target == null) {
            sender.sendMessage(Component.text("Il giocatore bersaglio non e' online.", NamedTextColor.RED));
            return;
         }
      } else if (sender instanceof Player player) {
         target = player;
      } else {
         sender.sendMessage("Uso da console: /oggetti dai <id> <quantita'> <giocatore>");
         return;
      }

      int amount = args.length >= 3 ? this.parsePage(args[2], 1) : 1;
      Player catalogContext = sender instanceof Player player ? player : target;
      this.gui.findCatalogEntry(catalogContext, args[1]).ifPresentOrElse(entry -> {
         Player actor = sender instanceof Player player ? player : target;
         int actualAmount = Math.max(1, Math.min(amount, Math.max(1, entry.item().getMaxStackSize())));
         if (this.gui.giveCatalogItem(actor, target, entry, actualAmount, "ItemsCommand")) {
            sender.sendMessage(Component.text("Dato " + actualAmount + " x " + entry.displayName() + " a " + target.getName() + ".", NamedTextColor.GREEN));
         }
      }, () -> sender.sendMessage(Component.text("ID oggetto sconosciuto o ambiguo. Usa /oggetti cerca <query>.", NamedTextColor.RED)));
   }

   private int parsePage(String raw, int fallback) {
      try {
         return Math.max(1, Integer.parseInt(raw));
      } catch (NumberFormatException ignored) {
         return fallback;
      }
   }

   private boolean isPositiveInteger(String raw) {
      try {
         return Integer.parseInt(raw) > 0;
      } catch (NumberFormatException ignored) {
         return false;
      }
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         List<String> options = new ArrayList<>();
         if (canViewCatalog(sender)) options.addAll(List.of("cerca", "lista"));
         if (canGiveItems(sender)) options.add("dai");
         if (canReload(sender)) options.add("ricarica");
         if (OpenPermissions.hasAny(sender, OpenPermissions.Weapons.DEBUG, OpenPermissions.Weapons.ADMIN, OpenPermissions.Test.DEBUG)) options.add("bersaglio");
         return this.filter(options, args[0]);
      }
      if (args[0].equalsIgnoreCase("bersaglio")
            && OpenPermissions.hasAny(sender, OpenPermissions.Weapons.DEBUG, OpenPermissions.Weapons.ADMIN, OpenPermissions.Test.DEBUG)) {
         if (args.length == 2 || args.length == 3) {
            List<String> options = new ArrayList<>(dummyPresetIds());
            options.addAll(List.of("100", "200", "500"));
            return this.filter(options, args[args.length - 1]);
         }
      }
      if (args.length == 2 && args[0].equalsIgnoreCase("lista") && canViewCatalog(sender)) {
         return this.filter(this.gui.getCatalogCategories(), args[1]);
      }
      if (args.length == 2 && args[0].equalsIgnoreCase("dai") && sender instanceof Player player && canGiveItems(sender)) {
         List<String> ids = new ArrayList<>();
         this.gui.collectCatalog(player).forEach(entry -> {
            ids.add(entry.fullId());
            ids.add(entry.id());
         });
         return this.filter(ids.stream().distinct().sorted().toList(), args[1]);
      }
      if (args.length == 4 && args[0].equalsIgnoreCase("dai")) {
         return null;
      }
      return List.of();
   }

   private boolean canViewCatalog(CommandSender sender) {
      return OpenPermissions.hasAny(sender, OpenPermissions.Weapons.VIEW, OpenPermissions.Weapons.GIVE,
            OpenPermissions.Weapons.ADMIN, OpenPermissions.Test.ITEMS);
   }

   private boolean canGiveItems(CommandSender sender) {
      return OpenPermissions.hasAny(sender, OpenPermissions.Weapons.GIVE, OpenPermissions.Weapons.ADMIN, OpenPermissions.Test.ITEMS);
   }

   private boolean canReload(CommandSender sender) {
      return OpenPermissions.hasAny(sender, OpenPermissions.Weapons.DEBUG, OpenPermissions.Weapons.ADMIN, OpenPermissions.Staff.RELOAD);
   }

   private List<String> filter(List<String> values, String prefix) {
      String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
      return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).limit(50).toList();
   }

   private record DummyPreset(String id, String displayName, String armorId, String helmetId, List<String> aliases) {
   }
}
