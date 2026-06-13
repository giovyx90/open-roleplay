package dev.openrp.weapons.commands;

import it.meridian.core.permissions.NextPermissions;
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
         new DummyPreset("plain", "No armor", null, null, List.of("none")),
         new DummyPreset("vest_light", "Light vest", "vest_light", null, List.of("light", "light_vest")),
         new DummyPreset("vest_heavy", "Heavy vest", "vest_heavy", null, List.of("heavy", "heavy_vest")),
         new DummyPreset("vest_heavy_plated", "Heavy plated vest", "vest_heavy_plated", null, List.of("plated", "heavy_plated", "plate")),
         new DummyPreset("helmet_ballistic", "Ballistic helmet", null, "ballistic_helmet", List.of("ballistic_helmet", "helmet")),
         new DummyPreset("helmet_riot", "Riot helmet", null, "riot_helmet", List.of("riot_helmet", "riot")),
         new DummyPreset("helmet_sf", "Special forces helmet", null, "sf_helmet", List.of("sf_helmet", "sf")),
         new DummyPreset("light_full", "Light vest + ballistic helmet", "vest_light", "ballistic_helmet", List.of("light_helmet")),
         new DummyPreset("heavy_full", "Heavy vest + ballistic helmet", "vest_heavy", "ballistic_helmet", List.of("heavy_helmet")),
         new DummyPreset("plated_full", "Heavy plated vest + SF helmet", "vest_heavy_plated", "sf_helmet", List.of("plated_sf", "full"))
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
               player.sendMessage(Component.text("You need " + NextPermissions.Weapons.VIEW + " to browse the item catalog.", NamedTextColor.RED));
               return true;
            }
            this.gui.open(player);
         } else {
            sender.sendMessage("Usage: /items <search|list|give|reload>");
         }
         return true;
      }

      switch (args[0].toLowerCase(Locale.ROOT)) {
         case "reload" -> {
            if (!canReload(sender)) {
               sender.sendMessage(Component.text("You need " + NextPermissions.Weapons.DEBUG + " or " + NextPermissions.Staff.RELOAD + " to reload weapon items.", NamedTextColor.RED));
               return true;
            }
            this.reloadItems(sender);
            return true;
         }
         case "search" -> {
	            if (!(sender instanceof Player player)) {
	               sender.sendMessage("Only players can open the Items search GUI.");
	               return true;
	            }
            if (!canViewCatalog(player)) {
               player.sendMessage(Component.text("You need " + NextPermissions.Weapons.VIEW + " to search the item catalog.", NamedTextColor.RED));
               return true;
            }
            if (args.length < 2) {
               player.sendMessage(Component.text("Usage: /items search <query> [page]", NamedTextColor.RED));
               return true;
            }
            boolean hasPage = args.length > 2 && this.isPositiveInteger(args[args.length - 1]);
            int page = hasPage ? this.parsePage(args[args.length - 1], 1) : 1;
            int queryEnd = hasPage ? args.length - 1 : args.length;
            String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, queryEnd));
            this.gui.openSearch(player, query, page);
            return true;
         }
         case "list" -> {
	            if (!(sender instanceof Player player)) {
	               sender.sendMessage("Only players can open the Items list GUI.");
	               return true;
	            }
            if (!canViewCatalog(player)) {
               player.sendMessage(Component.text("You need " + NextPermissions.Weapons.VIEW + " to list the item catalog.", NamedTextColor.RED));
               return true;
            }
            String category = args.length >= 2 ? args[1] : "all";
            int page = args.length >= 3 ? this.parsePage(args[2], 1) : 1;
            this.gui.openCatalogList(player, category, page);
            return true;
         }
         case "give" -> {
            this.handleGive(sender, args);
            return true;
         }
         case "dummy" -> {
            this.handleDummy(sender, args);
            return true;
         }
	         default -> {
	            if (sender instanceof Player player) {
                  if (!canViewCatalog(player)) {
                     player.sendMessage(Component.text("You need " + NextPermissions.Weapons.VIEW + " to browse the item catalog.", NamedTextColor.RED));
                     return true;
                  }
	               this.gui.open(player);
            } else {
               sender.sendMessage("Usage: /items <search|list|give|reload>");
            }
            return true;
         }
      }
   }

   private void handleDummy(CommandSender sender, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Only players can spawn weapon damage dummies.");
         return;
      }
      if (!NextPermissions.hasAny(player, NextPermissions.Weapons.DEBUG, NextPermissions.Weapons.ADMIN, NextPermissions.Test.DEBUG)) {
         player.sendMessage(Component.text("You need " + NextPermissions.Weapons.DEBUG + " or " + NextPermissions.Test.DEBUG + " to spawn a damage dummy.", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Usage: /items dummy [health] [preset]", NamedTextColor.RED));
            player.sendMessage(Component.text("Presets: " + String.join(", ", dummyPresetIds()), NamedTextColor.YELLOW));
            return;
         }
         preset = parsedPreset;
      }
      Zombie dummy = player.getWorld().spawn(player.getLocation(), Zombie.class);
      this.damageDummyListener.mark(dummy, health);
      applyDummyPreset(dummy, preset);
      player.sendMessage(Component.text("Spawned weapon damage dummy with " + String.format(Locale.US, "%.1f", health)
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
      if (this.module.getWeaponCosmeticManager() != null) {
         this.module.getWeaponCosmeticManager().load(new File(this.module.getCore().getDataFolder(), "weapon_cosmetics.yml"));
      }
      if (this.module.getArmorManager() != null) {
         this.module.getArmorManager().load(new File(this.module.getCore().getDataFolder(), "armor.yml"));
      }
      if (this.module.getHelmetManager() != null) {
         this.module.getHelmetManager().load(new File(this.module.getCore().getDataFolder(), "armor.yml"));
      }
      this.module.getGrenadeManager().load(new File(this.module.getCore().getDataFolder(), "grenades.yml"));
      sender.sendMessage(Component.text("Weapons, ammo, attachments, cosmetics, armor and grenades configuration reloaded.", NamedTextColor.GREEN));
   }

   private void handleGive(CommandSender sender, String[] args) {
      if (!canGiveItems(sender)) {
         sender.sendMessage(Component.text("You need " + NextPermissions.Weapons.GIVE + " or " + NextPermissions.Test.ITEMS + " to give catalog items.", NamedTextColor.RED));
         return;
      }
      if (args.length < 2) {
         sender.sendMessage(Component.text("Usage: /items give <id> [amount] [player]", NamedTextColor.RED));
         return;
      }

      Player target;
      if (args.length >= 4) {
         target = Bukkit.getPlayerExact(args[3]);
         if (target == null) {
            sender.sendMessage(Component.text("Target player is not online.", NamedTextColor.RED));
            return;
         }
      } else if (sender instanceof Player player) {
         target = player;
      } else {
         sender.sendMessage("Console usage: /items give <id> <amount> <player>");
         return;
      }

      int amount = args.length >= 3 ? this.parsePage(args[2], 1) : 1;
      Player catalogContext = sender instanceof Player player ? player : target;
      this.gui.findCatalogEntry(catalogContext, args[1]).ifPresentOrElse(entry -> {
         Player actor = sender instanceof Player player ? player : target;
         int actualAmount = Math.max(1, Math.min(amount, Math.max(1, entry.item().getMaxStackSize())));
         if (this.gui.giveCatalogItem(actor, target, entry, actualAmount, "ItemsCommand")) {
            sender.sendMessage(Component.text("Gave " + actualAmount + " x " + entry.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
         }
      }, () -> sender.sendMessage(Component.text("Unknown or ambiguous item ID. Use /items search <query>.", NamedTextColor.RED)));
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
         if (canViewCatalog(sender)) options.addAll(List.of("search", "list"));
         if (canGiveItems(sender)) options.add("give");
         if (canReload(sender)) options.add("reload");
         if (NextPermissions.hasAny(sender, NextPermissions.Weapons.DEBUG, NextPermissions.Weapons.ADMIN, NextPermissions.Test.DEBUG)) options.add("dummy");
         return this.filter(options, args[0]);
      }
      if (args[0].equalsIgnoreCase("dummy")
            && NextPermissions.hasAny(sender, NextPermissions.Weapons.DEBUG, NextPermissions.Weapons.ADMIN, NextPermissions.Test.DEBUG)) {
         if (args.length == 2 || args.length == 3) {
            List<String> options = new ArrayList<>(dummyPresetIds());
            options.addAll(List.of("100", "200", "500"));
            return this.filter(options, args[args.length - 1]);
         }
      }
      if (args.length == 2 && args[0].equalsIgnoreCase("list") && canViewCatalog(sender)) {
         return this.filter(this.gui.getCatalogCategories(), args[1]);
      }
      if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender instanceof Player player && canGiveItems(sender)) {
         List<String> ids = new ArrayList<>();
         this.gui.collectCatalog(player).forEach(entry -> {
            ids.add(entry.fullId());
            ids.add(entry.id());
         });
         return this.filter(ids.stream().distinct().sorted().toList(), args[1]);
      }
      if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
         return null;
      }
      return List.of();
   }

   private boolean canViewCatalog(CommandSender sender) {
      return NextPermissions.hasAny(sender, NextPermissions.Weapons.VIEW, NextPermissions.Weapons.GIVE,
            NextPermissions.Weapons.ADMIN, NextPermissions.Test.ITEMS);
   }

   private boolean canGiveItems(CommandSender sender) {
      return NextPermissions.hasAny(sender, NextPermissions.Weapons.GIVE, NextPermissions.Weapons.ADMIN, NextPermissions.Test.ITEMS);
   }

   private boolean canReload(CommandSender sender) {
      return NextPermissions.hasAny(sender, NextPermissions.Weapons.DEBUG, NextPermissions.Weapons.ADMIN, NextPermissions.Staff.RELOAD);
   }

   private List<String> filter(List<String> values, String prefix) {
      String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
      return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).limit(50).toList();
   }

   private record DummyPreset(String id, String displayName, String armorId, String helmetId, List<String> aliases) {
   }
}
