package dev.openrp.weapons.gui;

import dev.openrp.weapons.bridge.food.FoodModule;
import dev.openrp.weapons.bridge.food.FoodRecipe;
import dev.openrp.weapons.bridge.food.FoodWorkstation;
import dev.openrp.weapons.util.OpenGuiItems;
import dev.openrp.weapons.bridge.staff.StaffBoardMetadata;
import dev.openrp.weapons.bridge.staff.StaffBoardCategory;
import dev.openrp.weapons.bridge.staff.StaffBoardLogEvent;
import dev.openrp.weapons.bridge.staff.StaffBoardSensitivity;
import dev.openrp.weapons.bridge.staff.StaffBoardSeverity;
import dev.openrp.weapons.util.ItemBuilder;
import dev.openrp.weapons.attachments.AttachmentDefinition;
import dev.openrp.weapons.grenades.GrenadeDefinition;
import dev.openrp.weapons.model.AmmoDefinition;
import dev.openrp.weapons.model.ArmorDefinition;
import dev.openrp.weapons.model.HelmetDefinition;
import dev.openrp.weapons.model.WeaponCategory;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import dev.openrp.weapons.utility.UtilityItemType;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemsGUI implements Listener {
   private static final String MAIN_TITLE = "Catalogo Admin";
   private static final String FIREARMS_TITLE = "Catalogo Admin - Armi da fuoco";
   private static final String MELEE_TITLE = "Catalogo Admin - Corpo a corpo";
   private static final String AMMO_TITLE = "Catalogo Admin - Munizioni";
   private static final String MAGAZINES_TITLE = "Catalogo Admin - Caricatori";
   private static final String EQUIPMENT_TITLE = "Catalogo Admin - Equipaggiamento";
   private static final String UTILITIES_TITLE = "Catalogo Admin - Utility";
   private static final String FURNITURE_TITLE = "Catalogo Admin - Arredi";
   private static final String FOOD_TITLE = "Catalogo Admin - Cibo";
   private static final String DRINKS_TITLE = "Catalogo Admin - Bevande";
   private static final String ATTACHMENTS_TITLE = "Catalogo Admin - Accessori";
   private static final String SEARCH_TITLE = "Catalogo Admin - Ricerca";
   private static final String LIST_TITLE = "Catalogo Admin - Lista";
   private static final int PAGED_ITEMS_PER_PAGE = 45;
   private final WeaponsModule module;
   private final Map<UUID, String> activeSearchQueries = new HashMap<>();
   private final Map<UUID, String> activeListCategories = new HashMap<>();
   private final Set<String> warnedNexoItemBuildFailures = new HashSet<>();
   private final NamespacedKey menuCategoryKey;

   public ItemsGUI(WeaponsModule module) {
      this.module = module;
      this.menuCategoryKey = new NamespacedKey(module.getCore(), "items_gui_category");
   }

   public List<String> getCatalogCategories() {
      return List.of("all", "firearms", "melee", "ammo", "magazines", "attachments", "equipment", "utilities");
   }

   public List<CatalogEntry> collectCatalog(Player player) {
      List<CatalogEntry> entries = new ArrayList<>();

      for (WeaponDefinition weapon : this.module.getWeaponRegistry().getAll()) {
         String category = switch (weapon.getCategory()) {
            case MELEE -> "melee";
            case TASER -> "utilities";
            default -> "firearms";
         };
         entries.add(this.catalogEntry(category, weapon.getId(), weapon.getDisplayName(), this.module.getWeaponRegistry().createItemStack(weapon.getId())));
      }

      for (AmmoDefinition ammo : this.module.getAmmoRegistry().getAll()) {
         entries.add(this.catalogEntry("ammo", ammo.getId(), ammo.getDisplayName(), this.module.getAmmoRegistry().createItemStack(ammo.getId(), ammo.getMaxStack())));
      }

      this.module.getWeaponRegistry()
         .getAll()
         .stream()
         .filter(this::hasStandaloneMagazine)
         .forEach(weapon -> entries.add(this.catalogEntry(
            "magazines",
            weapon.getId() + "_magazine",
            weapon.getDisplayName() + " Caricatore",
            this.module.getMagazineManager().createMagazine(weapon, weapon.getMagazineSize())
         )));

      for (ArmorDefinition armor : this.module.getArmorManager().getAll()) {
         entries.add(this.catalogEntry("equipment", armor.getId(), armor.getDisplayName(), this.module.getArmorManager().createItemStack(armor.getId())));
      }
      entries.add(this.catalogEntry("equipment", "ceramic_plate", "Piastra ceramica", this.module.getArmorManager().createCeramicPlate()));
      for (HelmetDefinition helmet : this.module.getHelmetManager().getAll()) {
         entries.add(this.catalogEntry("equipment", helmet.getId(), helmet.getDisplayName(), this.module.getHelmetManager().createItemStack(helmet.getId())));
      }
      entries.add(this.catalogEntry("equipment", "riot_shield", "Scudo antisommossa", this.module.getShieldManager().createRiotShield()));
      entries.add(this.catalogEntry("equipment", "ballistic_shield", "Scudo balistico", this.module.getShieldManager().createBallisticShield()));
      for (GrenadeDefinition grenade : this.module.getGrenadeManager().getAll()) {
         String category = this.module.getC4Manager().isC4(grenade) ? "utilities" : "equipment";
         entries.add(this.catalogEntry(category, grenade.getId(), grenade.getDisplayName(), this.module.getGrenadeManager().createItemStack(grenade.getId())));
      }

      entries.add(this.catalogEntry("utilities", "balaclava", "Passamontagna", this.module.getBalaclavaManager().createBalaclava()));
      entries.add(this.catalogEntry("utilities", "handcuffs", "Manette", this.module.getHandcuffManager().createHandcuffs()));
      entries.add(this.catalogEntry("utilities", "bolt_cutters", "Tronchesi", this.module.getHandcuffManager().createBoltCutters()));
      for (UtilityItemType type : UtilityItemType.openWeaponsCatalogTypes()) {
         entries.add(this.catalogEntry("utilities", type.getId(), type.getDisplayName(), this.module.getUtilityItemManager().createItem(type)));
      }

      for (AttachmentDefinition attachment : this.module.getAttachmentRegistry().getAll()) {
         entries.add(this.catalogEntry("attachments", attachment.getId(), attachment.getDisplayName(), this.module.getAttachmentRegistry().createItemStack(attachment.getId())));
      }

      entries.sort(Comparator.comparing(CatalogEntry::category).thenComparing(CatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER));
      return entries;
   }

   public List<CatalogEntry> searchCatalog(Player player, String query) {
      String normalized = this.normalizeSearch(query);
      if (normalized.isBlank()) {
         return List.of();
      }
      return this.collectCatalog(player).stream().filter(entry -> entry.matches(normalized)).toList();
   }

   public List<CatalogEntry> catalogByCategory(Player player, String category) {
      String normalized = this.normalizeCategory(category);
      return this.collectCatalog(player).stream().filter(entry -> "all".equals(normalized) || entry.category().equals(normalized)).toList();
   }

   public Optional<CatalogEntry> findCatalogEntry(Player player, String input) {
      String normalized = this.normalizeCatalogId(input);
      if (normalized.isBlank()) {
         return Optional.empty();
      }

      List<CatalogEntry> entries = this.collectCatalog(player);
      Optional<CatalogEntry> exact = entries.stream().filter(entry -> entry.fullId().equals(normalized)).findFirst();
      if (exact.isPresent()) {
         return exact;
      }

      List<CatalogEntry> shortMatches = entries.stream().filter(entry -> entry.id().equals(normalized)).toList();
      return shortMatches.size() == 1 ? Optional.of(shortMatches.get(0)) : Optional.empty();
   }

   public void openSearch(Player player, String query, int page) {
      this.activeSearchQueries.put(player.getUniqueId(), query == null ? "" : query);
      this.openPagedCatalog(player, this.searchCatalog(player, query), SEARCH_TITLE, page);
   }

   public void openCatalogList(Player player, String category, int page) {
      String normalized = this.normalizeCategory(category);
      this.activeListCategories.put(player.getUniqueId(), normalized);
      this.openPagedCatalog(player, this.catalogByCategory(player, normalized), LIST_TITLE + " — " + this.humanizeId(normalized), page);
   }

   public boolean giveCatalogItem(Player actor, Player target, CatalogEntry entry, int amount, String source) {
      Optional<ItemStack> resolvedItem = this.resolveCatalogItemForGive(actor, entry);
      if (resolvedItem.isEmpty()) {
         return false;
      }
      ItemStack item = resolvedItem.get();
      int clampedAmount = Math.max(1, Math.min(amount, Math.max(1, item.getMaxStackSize())));
      item.setAmount(clampedAmount);
      Map<Integer, ItemStack> leftovers = target.getInventory().addItem(item);
      leftovers.values().forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
      target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
      WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(item);
      if (weapon != null) {
         this.emitWeaponObtained(actor, weapon, item, source);
      }
      return true;
   }

   public void open(Player player) {
      Inventory inv = Bukkit.createInventory(new ItemsGuiHolder(), 36, Component.text(MAIN_TITLE, NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD}));
      ItemStack filler = ItemBuilder.filler();

      for (int i = 0; i < 36; i++) {
         inv.setItem(i, filler);
      }

      Map<String, Integer> categoryCounts = this.countCatalogCategories(player);
      int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 22};
      int slotIndex = 0;
      for (CategoryButton button : this.mainCategoryButtons()) {
         int count = categoryCounts.getOrDefault(button.category(), 0);
         if (count <= 0 || slotIndex >= slots.length) {
            continue;
         }
         inv.setItem(slots[slotIndex++], this.categoryButton(button, count));
      }

      if (slotIndex == 0) {
         inv.setItem(
            13,
            new ItemBuilder(Material.BARRIER)
               .name(Component.text("Catalogo vuoto", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
               .lore(new Component[]{Component.text("Controlla weapons.yml, ammo.yml e grenades.yml.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
               .build()
         );
      }
      player.openInventory(inv);
   }

   private Map<String, Integer> countCatalogCategories(Player player) {
      Map<String, Integer> counts = new HashMap<>();
      for (CatalogEntry entry : this.collectCatalog(player)) {
         counts.merge(entry.category(), 1, Integer::sum);
      }
      return counts;
   }

   private List<CategoryButton> mainCategoryButtons() {
      return List.of(
         new CategoryButton("firearms", Material.CROSSBOW, "Armi da fuoco", "Pistole, SMG, fucili, shotgun e sniper.", NamedTextColor.GOLD),
         new CategoryButton("melee", Material.IRON_SWORD, "Corpo a corpo", "Coltelli, manganelli e strumenti da mischia.", NamedTextColor.RED),
         new CategoryButton("ammo", Material.ARROW, "Munizioni", "Munizioni pronte per riempire caricatori e shotgun.", NamedTextColor.YELLOW),
         new CategoryButton("magazines", Material.IRON_NUGGET, "Caricatori", "Caricatori pieni per le armi compatibili.", NamedTextColor.WHITE),
         new CategoryButton("attachments", Material.SPYGLASS, "Accessori armi", "Ottiche, canne, impugnature e modifiche.", NamedTextColor.AQUA),
         new CategoryButton("equipment", Material.IRON_CHESTPLATE, "Protezioni", "Armature, caschi, scudi e granate non C4.", NamedTextColor.BLUE),
         new CategoryButton("utilities", Material.SHEARS, "Strumenti RP", "Passamontagna, C4, manette, tronchesi, taser, corda e forbici.", NamedTextColor.GREEN)
      );
   }

   private ItemStack categoryButton(CategoryButton button, int count) {
      ItemStack item = new ItemBuilder(button.material())
         .name(Component.text(button.label(), button.color(), new TextDecoration[]{TextDecoration.BOLD}).decoration(TextDecoration.ITALIC, false))
         .lore(
            new Component[]{
               Component.text(button.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
               Component.text(count + " oggetti", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            }
         )
         .build();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.getPersistentDataContainer().set(this.menuCategoryKey, PersistentDataType.STRING, button.category());
         item.setItemMeta(meta);
      }
      return item;
   }

   private Optional<String> readMenuCategory(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return Optional.empty();
      }
      String category = item.getItemMeta().getPersistentDataContainer().get(this.menuCategoryKey, PersistentDataType.STRING);
      return category == null || category.isBlank() ? Optional.empty() : Optional.of(category);
   }

   private void openCategory(Player player, String category) {
      switch (category) {
         case "firearms" -> this.openFirearms(player);
         case "melee" -> this.openMelee(player);
         case "ammo" -> this.openAmmo(player);
         case "magazines" -> this.openMagazines(player);
         case "attachments" -> this.openAttachments(player);
         case "equipment" -> this.openEquipment(player);
         case "utilities" -> this.openUtilities(player);
         default -> this.open(player);
      }
   }

   private void openFirearms(Player player) {
      List<WeaponDefinition> firearms = this.module.getWeaponRegistry().getAll().stream().filter(w -> w.getCategory() != WeaponCategory.MELEE && w.getCategory() != WeaponCategory.TASER).toList();
      int size = this.clampSize(firearms.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Armi da fuoco", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (WeaponDefinition weapon : firearms) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getWeaponRegistry().createItemStack(weapon.getId()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private void openMelee(Player player) {
      List<WeaponDefinition> meleeWeapons = this.module.getWeaponRegistry().getByCategory(WeaponCategory.MELEE);
      int size = this.clampSize(meleeWeapons.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Corpo a corpo", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (WeaponDefinition weapon : meleeWeapons) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getWeaponRegistry().createItemStack(weapon.getId()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private void openAmmo(Player player) {
      List<AmmoDefinition> ammoList = this.module.getAmmoRegistry().getAll();
      int size = this.clampSize(ammoList.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Munizioni", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (AmmoDefinition ammo : ammoList) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getAmmoRegistry().createItemStack(ammo.getId(), ammo.getMaxStack()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private void openMagazines(Player player) {
      List<WeaponDefinition> firearms = this.module
         .getWeaponRegistry()
         .getAll()
         .stream()
         .filter(this::hasStandaloneMagazine)
         .toList();
      int size = this.clampSize(firearms.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Caricatori", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (WeaponDefinition weapon : firearms) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getMagazineManager().createMagazine(weapon, weapon.getMagazineSize()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private boolean hasStandaloneMagazine(WeaponDefinition weapon) {
      return weapon != null
         && weapon.getCategory() != WeaponCategory.MELEE
         && weapon.getCategory() != WeaponCategory.TASER
         && weapon.getCategory() != WeaponCategory.SHOTGUN
         && weapon.getMagazineSize() > 0;
   }

   private void openEquipment(Player player) {
      int count = this.module.getArmorManager().getAll().size()
         + 1
         + this.module.getHelmetManager().getAll().size()
         + 2
         + (int)this.module.getGrenadeManager().getAll().stream().filter(grenade -> !this.module.getC4Manager().isC4(grenade)).count();
      int size = this.clampSize(count + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Equipaggiamento", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (ArmorDefinition armor : this.module.getArmorManager().getAll()) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getArmorManager().createItemStack(armor.getId()));
      }

      if (slot < size - 1) {
         inv.setItem(slot++, this.module.getArmorManager().createCeramicPlate());
      }

      for (HelmetDefinition helmet : this.module.getHelmetManager().getAll()) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getHelmetManager().createItemStack(helmet.getId()));
      }

      if (slot < size - 1) {
         inv.setItem(slot++, this.module.getShieldManager().createRiotShield());
      }

      if (slot < size - 1) {
         inv.setItem(slot++, this.module.getShieldManager().createBallisticShield());
      }

      for (GrenadeDefinition grenade : this.module.getGrenadeManager().getAll()) {
         if (this.module.getC4Manager().isC4(grenade)) {
            continue;
         }
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getGrenadeManager().createItemStack(grenade.getId()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private void openUtilities(Player player) {
      List<ItemStack> utilities = this.collectOpenUtilityItems();
      int size = this.clampSize(utilities.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Utility", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;
      for (ItemStack item : utilities) {
         if (slot >= size - 1) {
            break;
         }
         inv.setItem(slot++, item);
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private List<ItemStack> collectOpenUtilityItems() {
      List<ItemStack> items = new ArrayList<>();
      items.add(this.module.getBalaclavaManager().createBalaclava());
      GrenadeDefinition c4 = this.module.getGrenadeManager().getGrenade("c4_charge");
      if (c4 != null) {
         items.add(this.module.getGrenadeManager().createItemStack(c4.getId()));
      }
      items.add(this.module.getUtilityItemManager().createItem(UtilityItemType.C4_REMOTE));
      items.add(this.module.getHandcuffManager().createHandcuffs());
      items.add(this.module.getHandcuffManager().createBoltCutters());
      this.module.getWeaponRegistry().getByCategory(WeaponCategory.TASER)
         .forEach(taser -> items.add(this.module.getWeaponRegistry().createItemStack(taser.getId())));
      for (UtilityItemType type : UtilityItemType.openWeaponsCatalogTypes()) {
         if (type != UtilityItemType.C4_REMOTE) {
            items.add(this.module.getUtilityItemManager().createItem(type));
         }
      }
      return items.stream().filter(item -> item != null && item.getType() != Material.AIR).toList();
   }

   private void openAttachments(Player player) {
      List<AttachmentDefinition> attachments = this.module.getAttachmentRegistry().getAll();
      int size = this.clampSize(attachments.size() + 1);
      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(), size, Component.text("Catalogo Admin - Accessori", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int slot = 0;

      for (AttachmentDefinition attachment : attachments) {
         if (slot >= size - 1) {
            break;
         }

         inv.setItem(slot++, this.module.getAttachmentRegistry().createItemStack(attachment.getId()));
      }

      this.fillRemaining(inv, slot, size);
      inv.setItem(size - 1, this.backButton());
      player.openInventory(inv);
   }

   private void openFurniture(Player player, int page) {
      List<CatalogEntry> entries = this.collectFurnitureEntries()
         .stream()
         .map(entry -> this.furnitureCatalogEntry(entry, null))
         .sorted(Comparator.comparing(CatalogEntry::displayName, String.CASE_INSENSITIVE_ORDER))
         .toList();

      int totalItems = entries.size();
      int maxPerPage = 45;
      int totalPages = (int)Math.ceil((double)totalItems / maxPerPage);
      if (totalPages == 0) {
         totalPages = 1;
      }

      if (page < 1) {
         page = 1;
      }

      if (page > totalPages) {
         page = totalPages;
      }

      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(),
         54,
         Component.text("Items Admin — Furniture (" + page + "/" + totalPages + ")", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int startIndex = (page - 1) * maxPerPage;
      int endIndex = Math.min(startIndex + maxPerPage, totalItems);
      int slot = 0;

      for (int i = startIndex; i < endIndex; i++) {
         inv.setItem(slot++, this.decorateCatalogItem(entries.get(i)));
      }

      this.fillRemaining(inv, slot, 54);
      if (page > 1) {
         inv.setItem(45, OpenGuiItems.getPrevPageButton());
      }

      if (page < totalPages) {
         inv.setItem(53, OpenGuiItems.getNextPageButton());
      }

      inv.setItem(49, this.backButton());
      player.openInventory(inv);
   }

   private void openFood(Player player, int page) {
      this.openFoodCategory(player, page, false);
   }

   private void openDrinks(Player player, int page) {
      this.openFoodCategory(player, page, true);
   }

   private void openFoodCategory(Player player, int page, boolean drinksOnly) {
      List<ItemStack> items = new ArrayList<>();
      FoodModule foodModule = this.getFoodModule();
      if (foodModule != null) {
         foodModule.config().recipes().values().stream()
            .filter(recipe -> drinksOnly == this.isDrinkRecipe(foodModule, recipe))
            .sorted(Comparator.comparing(FoodRecipe::displayName, String.CASE_INSENSITIVE_ORDER))
            .map(recipe -> this.createRecipeAdminItem(foodModule, player, recipe))
            .forEach(items::add);
      }

      String baseTitle = drinksOnly ? DRINKS_TITLE : FOOD_TITLE;
      this.openPagedItems(player, items, baseTitle, page);
   }

   private void openPagedItems(Player player, List<ItemStack> items, String baseTitle, int page) {
      int totalItems = items.size();
      int maxPerPage = PAGED_ITEMS_PER_PAGE;
      int totalPages = (int)Math.ceil((double)totalItems / maxPerPage);
      if (totalPages == 0) {
         totalPages = 1;
      }

      if (page < 1) {
         page = 1;
      }

      if (page > totalPages) {
         page = totalPages;
      }

      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(),
         54,
         Component.text(baseTitle + " (" + page + "/" + totalPages + ")", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int startIndex = (page - 1) * maxPerPage;
      int endIndex = Math.min(startIndex + maxPerPage, totalItems);
      int slot = 0;

      for (int i = startIndex; i < endIndex; i++) {
         inv.setItem(slot++, items.get(i));
      }

      this.fillRemaining(inv, slot, 54);
      if (page > 1) {
         inv.setItem(45, OpenGuiItems.getPrevPageButton());
      }

      if (page < totalPages) {
         inv.setItem(53, OpenGuiItems.getNextPageButton());
      }

      inv.setItem(49, this.backButton());
      player.openInventory(inv);
   }

   private void openPagedCatalog(Player player, List<CatalogEntry> entries, String baseTitle, int page) {
      int totalItems = entries.size();
      int totalPages = (int)Math.ceil((double)totalItems / PAGED_ITEMS_PER_PAGE);
      if (totalPages == 0) {
         totalPages = 1;
      }
      page = Math.max(1, Math.min(page, totalPages));

      Inventory inv = Bukkit.createInventory(
         new ItemsGuiHolder(),
         54,
         Component.text(baseTitle + " (" + page + "/" + totalPages + ")", NamedTextColor.DARK_RED, new TextDecoration[]{TextDecoration.BOLD})
      );
      int startIndex = (page - 1) * PAGED_ITEMS_PER_PAGE;
      int endIndex = Math.min(startIndex + PAGED_ITEMS_PER_PAGE, totalItems);
      int slot = 0;

      for (int i = startIndex; i < endIndex; i++) {
         inv.setItem(slot++, this.decorateCatalogItem(entries.get(i)));
      }

      if (entries.isEmpty()) {
         inv.setItem(
            22,
            new ItemBuilder(Material.BARRIER)
               .name(Component.text("Nessun oggetto trovato", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
               .lore(new Component[]{Component.text("Prova una ricerca o categoria diversa.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)})
               .build()
         );
      }

      this.fillRemaining(inv, slot, 54);
      if (page > 1) {
         inv.setItem(45, OpenGuiItems.getPrevPageButton());
      }
      if (page < totalPages) {
         inv.setItem(53, OpenGuiItems.getNextPageButton());
      }
      inv.setItem(49, this.backButton());
      player.openInventory(inv);
   }

   private ItemStack decorateCatalogItem(CatalogEntry entry) {
      ItemStack item = entry.item().clone();
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         List<Component> lore = new ArrayList<>();
         if (meta.lore() != null) {
            lore.addAll(meta.lore());
         }
         if (entry.hasNexoId()) {
            lore.add(Component.text("Nexo ID: " + entry.nexoId(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
         }
         lore.add(Component.text("ID catalogo: " + entry.fullId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
         lore.add(Component.text("Categoria: " + this.humanizeId(entry.category()), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
         meta.lore(lore);
         item.setItemMeta(meta);
      }
      return item;
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      // Identify our menus by holder, not by title: a player-named inventory can spoof the title but
      // not the holder, so this is what guarantees we only act on inventories we actually opened.
      if (!(event.getView().getTopInventory().getHolder() instanceof ItemsGuiHolder)) {
         return;
      }
      if (event.getWhoClicked() instanceof Player player) {
         String title = this.getPlainTitle(event);
         if (title != null) {
            if (title.startsWith("Catalogo Admin")) {
               event.setCancelled(true);
               ItemStack clicked = event.getCurrentItem();
               if (clicked != null && clicked.getType() != Material.AIR && clicked.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                  if (title.equals(MAIN_TITLE)) {
                     this.readMenuCategory(clicked).ifPresent(category -> this.openCategory(player, category));
                  } else if (clicked.getType() == Material.SPECTRAL_ARROW) {
                     this.open(player);
                  } else {
                     if (this.isPagedAdminCategory(title)) {
                        if (clicked.isSimilar(OpenGuiItems.getNextPageButton())) {
                           try {
                              this.openPageByTitle(player, title, this.extractCurrentPage(title) + 1);
                           } catch (Exception var8) {
                           }

                           return;
                        }

                        if (clicked.isSimilar(OpenGuiItems.getPrevPageButton())) {
                           try {
                              this.openPageByTitle(player, title, this.extractCurrentPage(title) - 1);
                           } catch (Exception var9) {
                           }

                           return;
                        }
                     }

                     if (this.isAdminGiveItem(title, clicked)) {
                        Optional<CatalogEntry> metadataEntry = this.catalogEntryFromMetadata(player, clicked);
                        if (metadataEntry.isPresent()) {
                           this.giveCatalogItem(player, player, metadataEntry.get(), 1, title);
                        } else {
                           ItemStack giveItem = clicked.clone();
                           Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{giveItem});
                           leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                           player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                           WeaponDefinition weapon = this.module.getWeaponRegistry().getWeapon(giveItem);
                           if (weapon != null) {
                              emitWeaponObtained(player, weapon, giveItem, title);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void emitWeaponObtained(Player player, WeaponDefinition weapon, ItemStack item, String sourceView) {
      StaffBoardMetadata metadata = StaffBoardMetadata.create()
         .put("weapon_id", weapon.getId())
         .put("weapon_name", weapon.getDisplayName())
         .put("weapon_category", weapon.getCategory().name())
         .put("weapon_instance_id", this.module.getWeaponRegistry().getInstanceId(item))
         .put("source_view", sourceView)
         .put("source_system", "ItemsGUI")
         .putLocation(player.getLocation());

      this.module.getStaffLogBridge().emit(StaffBoardLogEvent.builder("combat.weapon_obtained", "OpenWeapons")
         .category(StaffBoardCategory.COMBAT)
         .severity(StaffBoardSeverity.WARNING)
         .sensitivity(StaffBoardSensitivity.SENSITIVE)
         .actor(player)
         .location(player.getLocation())
         .message(player.getName() + " ha ottenuto " + weapon.getDisplayName() + " dal Catalogo Admin.")
         .metadataJson(metadata.toJson())
         .build());
   }

   private boolean isKnownItem(ItemStack item) {
      return this.module.getWeaponRegistry().getWeapon(item) != null
         || this.module.getAmmoRegistry().getAmmo(item) != null
         || this.module.getAttachmentRegistry().getAttachment(item) != null
         || this.module.getBalaclavaManager().isBalaclava(item)
         || this.module.getArmorManager().getArmor(item) != null
         || this.module.getArmorManager().isCeramicPlate(item)
         || this.module.getHelmetManager().getHelmet(item) != null
         || this.module.getShieldManager().isShield(item)
         || this.module.getGrenadeManager().getGrenade(item) != null
         || this.module.getMagazineManager().isMagazine(item)
         || this.module.getHandcuffManager().isHandcuffItem(item)
         || this.module.getHandcuffManager().isBoltCutterItem(item)
         || this.module.getUtilityItemManager().isUtilityItem(item)
         || this.isFoodItem(item);
   }

   private boolean isFoodItem(ItemStack item) {
      FoodModule foodModule = this.getFoodModule();
      return foodModule != null && foodModule.freshnessService().read(item).isPresent();
   }

   private boolean isAdminGiveItem(String title, ItemStack item) {
      if (item == null || item.getType() == Material.AIR || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
         return false;
      }
      if (item.isSimilar(this.backButton()) || item.isSimilar(OpenGuiItems.getPrevPageButton()) || item.isSimilar(OpenGuiItems.getNextPageButton())) {
         return false;
      }
      return this.isKnownItem(item)
         || title.startsWith(FURNITURE_TITLE)
         || title.startsWith(FOOD_TITLE)
         || title.startsWith(DRINKS_TITLE)
         || this.hasCatalogMetadata(item);
   }

   private boolean hasCatalogMetadata(ItemStack item) {
      if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) {
         return false;
      }
      return item.getItemMeta().lore().stream().map(PlainTextComponentSerializer.plainText()::serialize).anyMatch(line -> line.startsWith("ID catalogo: "));
   }

   private boolean isPagedAdminCategory(String title) {
      return title.startsWith(FURNITURE_TITLE)
         || title.startsWith(FOOD_TITLE)
         || title.startsWith(DRINKS_TITLE)
         || title.startsWith(SEARCH_TITLE)
         || title.startsWith(LIST_TITLE);
   }

   private int extractCurrentPage(String title) {
      String[] parts = title.split("\\(");
      String[] pageParts = parts[parts.length - 1].split("/");
      return Integer.parseInt(pageParts[0].trim());
   }

   private void openPageByTitle(Player player, String title, int page) {
      if (title.startsWith(FURNITURE_TITLE)) {
         this.openFurniture(player, page);
      } else if (title.startsWith(FOOD_TITLE)) {
         this.openFood(player, page);
      } else if (title.startsWith(DRINKS_TITLE)) {
         this.openDrinks(player, page);
      } else if (title.startsWith(SEARCH_TITLE)) {
         this.openSearch(player, this.activeSearchQueries.getOrDefault(player.getUniqueId(), ""), page);
      } else if (title.startsWith(LIST_TITLE)) {
         this.openCatalogList(player, this.activeListCategories.getOrDefault(player.getUniqueId(), "all"), page);
      }
   }

   private CatalogEntry catalogEntry(String category, String id, String displayName, ItemStack item) {
      String normalizedCategory = this.normalizeCategory(category);
      String normalizedId = this.normalizeCatalogId(id);
      String safeDisplayName = displayName == null || displayName.isBlank() ? this.humanizeId(normalizedId) : displayName;
      return new CatalogEntry(normalizedCategory, normalizedId, normalizedCategory + ":" + normalizedId, safeDisplayName, item.clone(), "");
   }

   private CatalogEntry furnitureCatalogEntry(FurnitureEntry entry, FoodWorkstation workstation) {
      String normalizedId = this.normalizeCatalogId(entry.id());
      String displayName = entry.label() == null || entry.label().isBlank() ? this.humanizeId(entry.id()) : entry.label();
      ItemStack preview = this.createFurnitureItem(entry, workstation).orElseGet(() -> this.createUnavailableFurniturePreview(entry, workstation));
      return new CatalogEntry("furniture", normalizedId, "furniture:" + normalizedId, displayName, preview, entry.id());
   }

   private Optional<CatalogEntry> catalogEntryFromMetadata(Player player, ItemStack item) {
      if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) {
         return Optional.empty();
      }
      for (Component line : item.getItemMeta().lore()) {
         String plain = PlainTextComponentSerializer.plainText().serialize(line);
         if (plain.startsWith("ID catalogo: ")) {
            return this.findCatalogEntry(player, plain.substring("ID catalogo: ".length()));
         }
      }
      return Optional.empty();
   }

   private Optional<ItemStack> resolveCatalogItemForGive(Player actor, CatalogEntry entry) {
      if ("furniture".equals(entry.category()) && entry.hasNexoId()) {
         Optional<ItemStack> nexoItem = this.createNexoItem(entry.nexoId());
         if (nexoItem.isPresent()) {
            return nexoItem;
         }
         actor.sendMessage(Component.text(
            "Oggetto Nexo non disponibile a runtime: " + entry.nexoId() + ". Usa /nexo reload o controlla l'id esatto dell'oggetto Nexo.",
            NamedTextColor.RED
         ));
         return Optional.empty();
      }
      return Optional.of(entry.item().clone());
   }

   private Optional<String> extractNexoId(ItemStack item) {
      if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) {
         return Optional.empty();
      }
      for (Component line : item.getItemMeta().lore()) {
         String plain = PlainTextComponentSerializer.plainText().serialize(line);
         if (plain.startsWith("Nexo ID: ")) {
            return Optional.of(this.normalizeCatalogId(plain.substring("Nexo ID: ".length())));
         }
      }
      return Optional.empty();
   }

   private String plainItemName(ItemStack item) {
      if (item == null || !item.hasItemMeta() || item.getItemMeta().displayName() == null) {
         return item == null ? "Sconosciuto" : this.humanizeId(item.getType().name());
      }
      return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
   }

   private String normalizeSearch(String raw) {
      return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
   }

   private String normalizeCategory(String raw) {
      String normalized = this.normalizeCatalogId(raw);
      return this.getCatalogCategories().contains(normalized) ? normalized : "all";
   }

   private String normalizeCatalogId(String raw) {
      return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
   }

   private String slug(String raw) {
      String normalized = this.normalizeCatalogId(raw).replaceAll("[^a-z0-9:_-]", "_").replaceAll("_+", "_");
      return normalized.isBlank() ? "unknown" : normalized;
   }

   private List<FurnitureEntry> collectFurnitureEntries() {
      List<FurnitureEntry> entries = new ArrayList<>();
      FoodModule foodModule = this.getFoodModule();
      Set<String> foodVisualOnlyIds = this.collectFoodVisualOnlyIds(foodModule);

      List<FurnitureEntry> liveNexoEntries = this.detectLiveNexoFurnitureEntries();
      if (!liveNexoEntries.isEmpty()) {
         return liveNexoEntries.stream()
            .filter(entry -> !foodVisualOnlyIds.contains(this.normalizeNexoId(entry.id())))
            .sorted(Comparator.comparing(FurnitureEntry::label, String.CASE_INSENSITIVE_ORDER).thenComparing(FurnitureEntry::id, String.CASE_INSENSITIVE_ORDER))
            .toList();
      }

      List<FurnitureEntry> detectedEntries = this.detectNexoFurnitureEntries();
      if (!detectedEntries.isEmpty()) {
         return detectedEntries.stream()
            .filter(entry -> !foodVisualOnlyIds.contains(this.normalizeNexoId(entry.id())))
            .sorted(Comparator.comparing(FurnitureEntry::label, String.CASE_INSENSITIVE_ORDER).thenComparing(FurnitureEntry::id, String.CASE_INSENSITIVE_ORDER))
            .toList();
      }

      if (foodModule == null) {
         return List.of();
      }

      foodModule.config().machineSettings().nexoWorkstations().entrySet().stream()
         .sorted(
            Comparator.comparing((Map.Entry<String, FoodWorkstation> entry) -> entry.getValue().name())
               .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
         )
         .forEach(entry -> entries.add(new FurnitureEntry(entry.getKey(), this.humanizeId(entry.getKey()), null, null)));
      return entries;
   }

   private List<FurnitureEntry> detectLiveNexoFurnitureEntries() {
      if (!Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
         return List.of();
      }

      try {
         Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
         Method entriesMethod = nexoItemsClass.getMethod("entries");
         Method hasMechanicMethod = nexoItemsClass.getMethod("hasMechanic", String.class, String.class);
         Object entriesObject = entriesMethod.invoke(null);
         if (!(entriesObject instanceof Map<?, ?> nexoEntries)) {
            return List.of();
         }

         List<FurnitureEntry> entries = new ArrayList<>();
         for (Map.Entry<?, ?> entry : nexoEntries.entrySet()) {
            if (!(entry.getKey() instanceof String nexoId) || nexoId.isBlank()) {
               continue;
            }

            try {
               Object hasFurnitureMechanic = hasMechanicMethod.invoke(null, nexoId, "furniture");
               if (!Boolean.TRUE.equals(hasFurnitureMechanic)) {
                  continue;
               }

               FurnitureEntry configured = this.findNexoFurnitureEntry(nexoId)
                  .orElse(new FurnitureEntry(nexoId, this.humanizeId(nexoId), Material.OAK_STAIRS, null));
               entries.add(configured);
            } catch (ReflectiveOperationException | LinkageError error) {
               this.warnNexoItemBuildFailure(nexoId, error);
            }
         }
         return entries.stream()
            .collect(java.util.stream.Collectors.toMap(
               entry -> this.normalizeNexoId(entry.id()),
               entry -> entry,
               (left, right) -> left
            ))
            .values()
            .stream()
            .toList();
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return List.of();
      }
   }

   private ItemStack buildNexoItem(Object builder, String nexoId) throws ReflectiveOperationException {
      if (builder == null) {
         return null;
      }
      if (builder instanceof Optional<?> optional) {
         builder = optional.orElse(null);
         if (builder == null) {
            return null;
         }
      }
      if (builder instanceof ItemStack item) {
         return item;
      }

      InvocationTargetException lastFailure = null;
      for (String methodName : List.of("build", "referenceCopy", "getFinalItemStack")) {
         try {
            Method buildMethod = builder.getClass().getMethod(methodName);
            Object built = buildMethod.invoke(builder);
            if (built instanceof ItemStack item) {
               return item;
            }
         } catch (NoSuchMethodException ignored) {
         } catch (InvocationTargetException error) {
            lastFailure = error;
         }
      }
      if (lastFailure != null) {
         throw lastFailure;
      }
      return null;
   }

   private Integer customModelData(ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
         return null;
      }
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.hasCustomModelData() ? meta.getCustomModelData() : null;
   }

   private Set<String> collectFoodVisualOnlyIds(FoodModule foodModule) {
      Set<String> ids = new HashSet<>();
      if (foodModule != null) {
         ids.addAll(foodModule.config().machineSettings().visualOnlyNexoItems());
      } else {
         this.collectBundledFoodMachineVisualOnlyIds(ids);
      }
      this.collectLocalFoodMachineVisualOnlyIds(ids);
      this.collectNexoFurnitureStateVisualOnlyIds(ids);
      return Set.copyOf(ids);
   }

   private Optional<ItemStack> createFurnitureItem(FurnitureEntry entry, FoodWorkstation workstation) {
      Optional<ItemStack> rawItem = this.createNexoItem(entry.id());
      if (rawItem.isPresent()) {
         return rawItem;
      }

      Material material = entry.material() != null ? entry.material()
         : (workstation != null ? this.fallbackFurnitureMaterial(workstation) : Material.OAK_STAIRS);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return Optional.of(item);
      }

      meta.displayName(Component.text(entry.label() == null || entry.label().isBlank() ? this.humanizeId(entry.id()) : entry.label(), NamedTextColor.LIGHT_PURPLE)
         .decoration(TextDecoration.ITALIC, false));
      if (entry.customModelData() != null) {
         meta.setCustomModelData(entry.customModelData());
      }
      List<Component> lore = new ArrayList<>();
      lore.add(Component.text("Nexo ID: " + entry.id(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      if (workstation != null) {
         lore.add(Component.text("Postazione: " + workstation.name(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
      }
      lore.add(Component.text("Solo anteprima - Nexo non disponibile", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);
      item.setItemMeta(meta);
      return Optional.of(item);
   }

   private ItemStack createUnavailableFurniturePreview(FurnitureEntry entry, FoodWorkstation workstation) {
      return this.createFurnitureItem(entry, workstation).orElseGet(() -> new ItemStack(Material.BARRIER));
   }

   private ItemStack createRecipeAdminItem(FoodModule foodModule, Player player, FoodRecipe recipe) {
      ItemStack item = foodModule.freshnessService().createFoodItem(recipe.output(), player, true, NamedTextColor.GREEN);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      List<Component> lore = new ArrayList<>();
      if (meta.lore() != null) {
         lore.addAll(meta.lore());
      }
      lore.add(Component.text("ID ricetta: " + recipe.id(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("Postazione: " + recipe.workstation().name(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
      if (recipe.alcoholDelta() > 0.0D) {
         lore.add(Component.text("Alcol: +" + recipe.alcoholDelta(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
      } else if (recipe.hydrationDelta() > 0) {
         lore.add(Component.text("Idratazione: +" + recipe.hydrationDelta(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
      }
      lore.add(Component.text("Oggetto spawn admin", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);
      item.setItemMeta(meta);
      return item;
   }

   private boolean isDrinkRecipe(FoodModule foodModule, FoodRecipe recipe) {
      if (recipe.alcoholDelta() > 0.0D) {
         return true;
      }
      if (foodModule.config().alcoholSettings().isAlcoholProductionWorkstation(recipe.workstation())) {
         return true;
      }
      return switch (recipe.workstation()) {
         case BAR_COUNTER, COFFEE_MACHINE, BOTTLER, KEG_TAP, DISTILLER, FERMENTER -> true;
         case BLENDER, MIXER -> recipe.hungerRestore() <= 2 && recipe.hydrationDelta() >= 0;
         default -> recipe.hungerRestore() == 0 && recipe.hydrationDelta() >= 0;
      };
   }

   private FoodModule getFoodModule() {
      return null;
   }

   private Optional<ItemStack> createNexoItem(String nexoId) {
      if (nexoId == null || nexoId.isBlank() || !Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
         return Optional.empty();
      }

      Optional<Boolean> runtimeExists = this.nexoItemExists(nexoId);
      if (runtimeExists.isPresent() && !runtimeExists.get()) {
         return Optional.empty();
      }

      Optional<ItemStack> configuredItem = this.createConfiguredNexoItem(nexoId);
      if (configuredItem.isPresent()) {
         return configuredItem;
      }

      try {
         Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
         for (String methodName : List.of("itemFromId", "itemFromID", "itemFromNexoId", "optionalItemFromId")) {
            try {
               ItemStack item = this.invokeNexoFactory(nexoItemsClass, methodName, nexoId);
               if (item != null) {
                  return Optional.of(item.clone());
               }
            } catch (ReflectiveOperationException | LinkageError error) {
               this.warnNexoItemBuildFailure(nexoId, error);
            }
         }
      } catch (ReflectiveOperationException | LinkageError error) {
         this.warnNexoItemBuildFailure(nexoId, error);
      }
      return Optional.empty();
   }

   private Optional<Boolean> nexoItemExists(String nexoId) {
      try {
         Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
         Method existsMethod = nexoItemsClass.getMethod("exists", String.class);
         Object exists = existsMethod.invoke(null, nexoId);
         return exists instanceof Boolean value ? Optional.of(value) : Optional.empty();
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return Optional.empty();
      }
   }

   private Optional<ItemStack> createConfiguredNexoItem(String nexoId) {
      return this.findNexoItemSection(nexoId)
         .filter(section -> section.isConfigurationSection("Mechanics.furniture"))
         .map(section -> this.createConfiguredNexoItem(nexoId, section));
   }

   private ItemStack createConfiguredNexoItem(String nexoId, ConfigurationSection section) {
      Material material = Material.matchMaterial(section.getString("material", "PAPER"));
      ItemStack item = new ItemStack(material == null || material.isAir() ? Material.PAPER : material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      }

      String itemName = section.getString("itemname", this.humanizeId(nexoId));
      meta.displayName(Component.text(itemName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
      if (section.isInt("Pack.custom_model_data")) {
         meta.setCustomModelData(section.getInt("Pack.custom_model_data"));
      }
      this.applyItemModel(meta, section.getString("Pack.model", section.getString("Pack.item_model", "")));

      org.bukkit.plugin.Plugin nexoPlugin = Bukkit.getPluginManager().getPlugin("Nexo");
      if (nexoPlugin != null) {
         meta.getPersistentDataContainer().set(new NamespacedKey(nexoPlugin, "id"), PersistentDataType.STRING, nexoId);
      }

      item.setItemMeta(meta);
      return item;
   }

   private void applyItemModel(ItemMeta meta, String rawModel) {
      if (meta == null || rawModel == null || rawModel.isBlank()) {
         return;
      }
      NamespacedKey modelKey = NamespacedKey.fromString(rawModel.trim());
      if (modelKey == null) {
         return;
      }
      try {
         Method setItemModel = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
         setItemModel.invoke(meta, modelKey);
      } catch (ReflectiveOperationException | LinkageError ignored) {
      }
   }

   private ItemStack invokeNexoFactory(Class<?> nexoItemsClass, String methodName, String nexoId)
      throws ReflectiveOperationException {
      try {
         Method factoryMethod = nexoItemsClass.getMethod(methodName, String.class);
         Object builder = factoryMethod.invoke(null, nexoId);
         return this.buildNexoItem(builder, nexoId);
      } catch (NoSuchMethodException ignored) {
         return null;
      }
   }

   private void warnNexoItemBuildFailure(String nexoId, Throwable error) {
      String key = this.normalizeNexoId(nexoId) + ":" + error.getClass().getName();
      if (!this.warnedNexoItemBuildFailures.add(key)) {
         return;
      }
      Throwable root = error instanceof InvocationTargetException invocationError && invocationError.getCause() != null
         ? invocationError.getCause()
         : error;
      this.module.getCore().getLogger().warning(
         "[OpenWeapons] Unable to build Nexo item '" + nexoId + "' through Nexo API: "
            + root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage())
      );
   }

   private Material fallbackFurnitureMaterial(FoodWorkstation workstation) {
      return switch (workstation) {
         case CUTTING_BOARD -> Material.OAK_PRESSURE_PLATE;
         case STOVE, OVEN, PIZZA_OVEN -> Material.FURNACE;
         case POT -> Material.DECORATED_POT;
         case PAN, GRILL, GRIDDLE -> Material.SMOOTH_STONE_SLAB;
         case WATER_PURIFIER -> Material.WATER_CAULDRON;
         case SINK -> Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
         case TOASTER -> Material.COPPER_GRATE;
         case FRIDGE -> Material.IRON_DOOR;
         case FREEZER -> Material.BLUE_ICE;
         case BAR_COUNTER, PASTRY_BENCH, ASSEMBLY_COUNTER -> Material.SMOOTH_QUARTZ_SLAB;
         case STREET_CART, FOOD_TRUCK -> Material.CHEST_MINECART;
         case GELATO_COUNTER, ICE_CREAM_MACHINE -> Material.QUARTZ_BLOCK;
         case FRYER -> Material.CAULDRON;
         case BLENDER -> Material.GLASS;
         case MIXER -> Material.WHITE_CONCRETE;
         case ICE_MACHINE -> Material.ICE;
         case COFFEE_MACHINE -> Material.BREWING_STAND;
         case FERMENTER -> Material.BARREL;
         case MEAT_GRINDER -> Material.STONECUTTER;
         case SLICER -> Material.IRON_BARS;
         case PASTEURIZER -> Material.COPPER_BLOCK;
         case BOTTLER -> Material.GLASS_PANE;
         case KEG_TAP -> Material.DARK_OAK_FENCE;
         case DISTILLER -> Material.COPPER_BULB;
         default -> Material.OAK_STAIRS;
      };
   }

   private String humanizeId(String raw) {
      return raw == null ? "Sconosciuto" : raw.replace('_', ' ').replace('-', ' ').trim()
         .transform(value -> value.isEmpty() ? "Sconosciuto" : Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT));
   }

   private List<FurnitureEntry> detectNexoFurnitureEntries() {
      List<FurnitureEntry> entries = new ArrayList<>();
      for (File yamlFile : this.resolveFurnitureYamlFiles()) {
         YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
         Set<String> stateOnlyIds = this.collectNexoStateOnlyIds(yaml);
         for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null || !section.isConfigurationSection("Mechanics.furniture")) {
               continue;
            }
            if (stateOnlyIds.contains(this.normalizeNexoId(key))) {
               continue;
            }
            entries.add(this.toFurnitureEntry(key, section));
         }
      }
      return entries.stream()
         .collect(java.util.stream.Collectors.toMap(
            entry -> entry.id().toLowerCase(Locale.ROOT),
            entry -> entry,
            (left, right) -> left
         ))
         .values()
         .stream()
         .toList();
   }

   private Optional<FurnitureEntry> findNexoFurnitureEntry(String nexoId) {
      return this.findNexoItemSection(nexoId)
         .filter(section -> section.isConfigurationSection("Mechanics.furniture"))
         .map(section -> this.toFurnitureEntry(nexoId, section));
   }

   private FurnitureEntry toFurnitureEntry(String nexoId, ConfigurationSection section) {
      String label = section.getString("itemname", this.humanizeId(nexoId));
      Material material = Material.matchMaterial(section.getString("material", ""));
      Integer customModelData = section.isInt("Pack.custom_model_data") ? section.getInt("Pack.custom_model_data") : null;
      return new FurnitureEntry(
         nexoId,
         label,
         material == null || material.isAir() ? Material.OAK_STAIRS : material,
         customModelData
      );
   }

   private Optional<ConfigurationSection> findNexoItemSection(String nexoId) {
      String normalized = this.normalizeNexoId(nexoId);
      if (normalized.isBlank()) {
         return Optional.empty();
      }
      for (File yamlFile : this.resolveFurnitureYamlFiles()) {
         YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
         ConfigurationSection section = yaml.getConfigurationSection(normalized);
         if (section != null) {
            return Optional.of(section);
         }
         for (String key : yaml.getKeys(false)) {
            if (this.normalizeNexoId(key).equals(normalized)) {
               section = yaml.getConfigurationSection(key);
               if (section != null) {
                  return Optional.of(section);
               }
            }
         }
      }
      return Optional.empty();
   }

   private Set<String> collectNexoStateOnlyIds(YamlConfiguration yaml) {
      Set<String> stateIds = new HashSet<>();
      Set<String> statefulFurnitureIds = new HashSet<>();
      for (String key : yaml.getKeys(false)) {
         ConfigurationSection section = yaml.getConfigurationSection(key);
         if (section == null || !section.isConfigurationSection("Mechanics.furniture.states")) {
            continue;
         }
         statefulFurnitureIds.add(this.normalizeNexoId(key));
         ConfigurationSection states = section.getConfigurationSection("Mechanics.furniture.states");
         if (states == null) {
            continue;
         }
         for (String state : states.getKeys(false)) {
            this.addNormalizedNexoId(stateIds, states.getString(state + ".nexo_item", ""));
            this.addNexoModelBasename(stateIds, states.getString(state + ".item_model", ""));
         }
      }
      stateIds.removeAll(statefulFurnitureIds);
      return stateIds;
   }

   private void collectBundledFoodMachineVisualOnlyIds(Set<String> ids) {
      try (InputStream input = this.module.getCore().getResource("food/food_machines.yml")) {
         if (input == null) {
            return;
         }
         YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
         this.collectConfiguredFoodVisualOnlyIds(yaml, ids);
      } catch (Exception ignored) {
      }
   }

   private void collectLocalFoodMachineVisualOnlyIds(Set<String> ids) {
      for (File file : this.resolveFoodMachineFiles()) {
         this.collectConfiguredFoodVisualOnlyIds(YamlConfiguration.loadConfiguration(file), ids);
      }
   }

   private void collectNexoFurnitureStateVisualOnlyIds(Set<String> ids) {
      for (File yamlFile : this.resolveFurnitureYamlFiles()) {
         ids.addAll(this.collectNexoStateOnlyIds(YamlConfiguration.loadConfiguration(yamlFile)));
      }
   }

   private void collectConfiguredFoodVisualOnlyIds(YamlConfiguration yaml, Set<String> ids) {
      if (yaml == null || ids == null) {
         return;
      }
      Set<String> workstationIds = new HashSet<>();
      Set<String> configuredVisualIds = new HashSet<>();
      ConfigurationSection nexoIds = yaml.getConfigurationSection("nexo_ids");
      if (nexoIds != null) {
         for (String key : nexoIds.getKeys(false)) {
            for (String nexoId : nexoIds.getStringList(key)) {
               this.addNormalizedNexoId(workstationIds, nexoId);
            }
         }
      }

      ConfigurationSection visualStates = yaml.getConfigurationSection("visual_states");
      if (visualStates != null) {
         for (String workstation : visualStates.getKeys(false)) {
            ConfigurationSection states = visualStates.getConfigurationSection(workstation);
            if (states == null) {
               continue;
            }
            for (String state : states.getKeys(false)) {
               this.addNormalizedNexoId(configuredVisualIds, states.getString(state, ""));
            }
         }
      }

      ConfigurationSection recipeVisuals = yaml.getConfigurationSection("recipe_visuals");
      if (recipeVisuals != null) {
         for (String workstation : recipeVisuals.getKeys(false)) {
            ConfigurationSection recipes = recipeVisuals.getConfigurationSection(workstation + ".recipes");
            if (recipes == null) {
               continue;
            }
            for (String recipe : recipes.getKeys(false)) {
               this.addNormalizedNexoId(configuredVisualIds, recipes.getString(recipe + ".nexo_item", ""));
            }
         }
      }
      configuredVisualIds.removeAll(workstationIds);
      ids.addAll(configuredVisualIds);
   }

   private void addNexoModelBasename(Set<String> ids, String itemModel) {
      if (itemModel == null || itemModel.isBlank()) {
         return;
      }
      String normalized = itemModel.trim();
      int slash = normalized.lastIndexOf('/');
      if (slash >= 0 && slash + 1 < normalized.length()) {
         normalized = normalized.substring(slash + 1);
      }
      int colon = normalized.lastIndexOf(':');
      if (colon >= 0 && colon + 1 < normalized.length()) {
         normalized = normalized.substring(colon + 1);
      }
      if (normalized.endsWith(".json")) {
         normalized = normalized.substring(0, normalized.length() - ".json".length());
      }
      this.addNormalizedNexoId(ids, normalized);
   }

   private void addNormalizedNexoId(Set<String> ids, String nexoId) {
      String normalized = this.normalizeNexoId(nexoId);
      if (!normalized.isBlank()) {
         ids.add(normalized);
      }
   }

   private String normalizeNexoId(String raw) {
      return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
   }

   private List<File> resolveFurnitureDirectories() {
      List<File> directories = new ArrayList<>();
      File pluginsFolder = this.module.getCore().getDataFolder().getParentFile();
      if (pluginsFolder != null) {
         directories.add(new File(new File(pluginsFolder, "Nexo"), "items"));
      }
      File repoResourcePack = new File("resource_pack/Nexo/items");
      directories.add(repoResourcePack);
      File nextRoleplayResourcePack = new File("Open Roleplay/resource_pack/Nexo/items");
      directories.add(nextRoleplayResourcePack);
      return directories.stream().filter(File::isDirectory).distinct().toList();
   }

   private List<File> resolveFurnitureYamlFiles() {
      List<File> files = new ArrayList<>();
      for (File directory : this.resolveFurnitureDirectories()) {
         this.collectYamlFiles(directory, files);
      }
      return files.stream().filter(File::isFile).distinct().toList();
   }

   private void collectYamlFiles(File directory, List<File> files) {
      File[] children = directory.listFiles();
      if (children == null) {
         return;
      }
      for (File child : children) {
         if (child.isDirectory()) {
            this.collectYamlFiles(child, files);
         } else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
            files.add(child);
         }
      }
   }

   private List<File> resolveFoodMachineFiles() {
      List<File> files = new ArrayList<>();
      files.add(new File(new File(this.module.getCore().getDataFolder(), "food"), "food_machines.yml"));
      files.add(new File("Open Roleplay/core/src/main/resources/food/food_machines.yml"));
      files.add(new File("Open Roleplay/bundle/src/main/resources/food/food_machines.yml"));
      return files.stream().filter(File::isFile).distinct().toList();
   }

   private record FurnitureEntry(String id, String label, Material material, Integer customModelData) {
   }

   private record CategoryButton(String category, Material material, String label, String description, NamedTextColor color) {
   }

   public record CatalogEntry(String category, String id, String fullId, String displayName, ItemStack item, String nexoId) {
      public CatalogEntry {
         nexoId = nexoId == null ? "" : nexoId.trim();
      }

      private boolean hasNexoId() {
         return !this.nexoId.isBlank();
      }

      private boolean matches(String normalizedQuery) {
         String haystack = (this.fullId + " " + this.id + " " + this.displayName + " " + this.category + " " + this.nexoId).toLowerCase(Locale.ROOT);
         for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank() && !haystack.contains(token)) {
               return false;
            }
         }
         return true;
      }
   }

   private ItemStack backButton() {
      return new ItemBuilder(Material.SPECTRAL_ARROW).name(Component.text("← Indietro", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)).build();
   }

   private void fillRemaining(Inventory inv, int fromSlot, int size) {
      ItemStack filler = ItemBuilder.filler();

      for (int i = fromSlot; i < size - 1; i++) {
         if (inv.getItem(i) == null) {
            inv.setItem(i, filler);
         }
      }
   }

   private int clampSize(int itemCount) {
      int rows = (int)Math.ceil(itemCount / 9.0);
      rows = Math.max(1, Math.min(rows, 6));
      return rows * 9;
   }

   private String getPlainTitle(InventoryClickEvent event) {
      Component titleComponent = event.getView().title();
      return PlainTextComponentSerializer.plainText().serialize(titleComponent);
   }

   /**
    * Marker holder attached to every inventory this GUI opens. Its presence is the unspoofable signal
    * that an inventory belongs to the admin catalog; sub-menu routing still keys off the title, but
    * only ever for inventories carrying this holder.
    */
   private static final class ItemsGuiHolder implements InventoryHolder {
      @Override
      public Inventory getInventory() {
         return null;
      }
   }
}
