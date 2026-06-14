package dev.openrp.weapons.config;

import dev.openrp.weapons.util.ItemBuilder;
import dev.openrp.weapons.module.WeaponsModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class WeaponConfigGUI implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final WeaponsModule module;
    private final WeaponConfigEditor editor;
    private final Map<UUID, PendingEdit> pendingEdits = new HashMap<>();

    public WeaponConfigGUI(WeaponsModule module, WeaponConfigEditor editor) {
        this.module = module;
        this.editor = editor;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new RootHolder(), 27,
                Component.text("Config Armi", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        fill(inv);
        inv.setItem(11, new ItemBuilder(Material.CROSSBOW)
                .name(Component.text("Armi", NamedTextColor.RED))
                .lore(
                        Component.text("Modifica i campi armi di weapons.yml", NamedTextColor.GRAY),
                        Component.text("Danno, rinculo, model data, caricatori", NamedTextColor.DARK_GRAY))
                .build());
        inv.setItem(15, new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .name(Component.text("Protezioni", NamedTextColor.AQUA))
                .lore(
                        Component.text("Modifica i campi protezione di armor.yml", NamedTextColor.GRAY),
                        Component.text("Giubbotti, caschi e valori protezione", NamedTextColor.DARK_GRAY))
                .build());
        player.openInventory(inv);
    }

    public void openWeapons(Player player, int page) {
        List<String> weapons = editor.weaponIds();
        Inventory inv = Bukkit.createInventory(new WeaponListHolder(page), 54,
                Component.text("Config Armi", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        fill(inv);
        int start = Math.max(0, page) * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < weapons.size(); slot++) {
            String weaponId = weapons.get(start + slot);
            ItemStack preview = module.getWeaponRegistry().createItemStack(weaponId);
            Material material = preview == null ? Material.CROSSBOW : preview.getType();
            inv.setItem(slot, new ItemBuilder(material)
                    .name(Component.text(weaponId, NamedTextColor.RED))
                    .lore(
                            Component.text("Clic sinistro: modifica campi", NamedTextColor.GRAY),
                            Component.text("Comando: /configarmi imposta " + weaponId + " <percorso> <valore>", NamedTextColor.DARK_GRAY))
                    .build());
        }
        setNavigation(inv, page, weapons.size(), true, "Torna al menu");
        player.openInventory(inv);
    }

    public void openProtections(Player player, int page) {
        List<ProtectionEntry> entries = protectionEntries();
        Inventory inv = Bukkit.createInventory(new ProtectionListHolder(page), 54,
                Component.text("Config Protezioni", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        fill(inv);
        int start = Math.max(0, page) * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < entries.size(); slot++) {
            ProtectionEntry entry = entries.get(start + slot);
            ItemStack preview = previewFor(entry.type(), entry.id());
            Material material = preview == null ? entry.type().fallbackMaterial() : preview.getType();
            inv.setItem(slot, new ItemBuilder(material)
                    .name(Component.text(entry.id(), entry.type().color()))
                    .lore(
                            Component.text(entry.type().label(), NamedTextColor.GRAY),
                            Component.text("Clic sinistro: modifica campi", NamedTextColor.GREEN),
                            Component.text("Comando: /configarmi " + entry.type().commandName()
                                    + " imposta " + entry.id() + " <percorso> <valore>", NamedTextColor.DARK_GRAY))
                    .build());
        }
        setNavigation(inv, page, entries.size(), true, "Torna al menu");
        player.openInventory(inv);
    }

    public void openFields(Player player, String weaponId, int page) {
        openFields(player, ConfigType.WEAPON, weaponId, page);
    }

    public void openFields(Player player, ConfigType type, String itemId, int page) {
        List<WeaponConfigEditor.FieldSpec> fields = fieldsFor(type, itemId);
        Inventory inv = Bukkit.createInventory(new FieldListHolder(type, itemId, page), 54,
                Component.text(type.titlePrefix() + ": " + itemId, type.color(), TextDecoration.BOLD));
        fill(inv);
        int start = Math.max(0, page) * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && start + slot < fields.size(); slot++) {
            WeaponConfigEditor.FieldSpec field = fields.get(start + slot);
            String value = get(type, itemId, field.path()).message();
            inv.setItem(slot, new ItemBuilder(iconFor(field.type()))
                    .name(Component.text(field.path(), NamedTextColor.YELLOW))
                    .lore(
                            Component.text(field.label(), NamedTextColor.GRAY),
                            Component.text("Attuale: " + trim(value, 34), NamedTextColor.WHITE),
                            Component.text("Clic sinistro: modifica via chat", NamedTextColor.GREEN),
                            Component.text("Clic destro: rimuovi/svuota", NamedTextColor.RED))
                    .build());
        }
        setNavigation(inv, page, fields.size(), true, type == ConfigType.WEAPON ? "Torna alle armi" : "Torna alle protezioni");
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof WeaponConfigHolder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (holder instanceof RootHolder) {
            handleRootClick(player, slot);
            return;
        }
        if (holder instanceof WeaponListHolder weaponHolder) {
            handleWeaponListClick(player, weaponHolder, slot);
            return;
        }
        if (holder instanceof ProtectionListHolder protectionHolder) {
            handleProtectionListClick(player, protectionHolder, slot);
            return;
        }
        if (holder instanceof FieldListHolder fieldHolder) {
            handleFieldClick(player, fieldHolder, slot, event.getClick());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingEdit edit = pendingEdits.remove(event.getPlayer().getUniqueId());
        if (edit == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(module.getCore(), () -> {
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(Component.text("Modifica config annullata.", NamedTextColor.YELLOW));
                openFields(player, edit.type(), edit.itemId(), edit.page());
                return;
            }
            WeaponConfigEditor.EditResult result = set(edit.type(), player.getName(), edit.itemId(), edit.path(), message);
            player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (result.success()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.3f);
            }
            openFields(player, edit.type(), edit.itemId(), edit.page());
        });
    }

    private void handleRootClick(Player player, int slot) {
        if (slot == 11) {
            openWeapons(player, 0);
        } else if (slot == 15) {
            openProtections(player, 0);
        }
    }

    private void handleWeaponListClick(Player player, WeaponListHolder holder, int slot) {
        List<String> weapons = editor.weaponIds();
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_SLOT && holder.page() > 0) {
            openWeapons(player, holder.page() - 1);
            return;
        }
        if (slot == NEXT_SLOT && (holder.page() + 1) * PAGE_SIZE < weapons.size()) {
            openWeapons(player, holder.page() + 1);
            return;
        }
        int index = holder.page() * PAGE_SIZE + slot;
        if (slot >= PAGE_SIZE || index < 0 || index >= weapons.size()) {
            return;
        }
        openFields(player, ConfigType.WEAPON, weapons.get(index), 0);
    }

    private void handleProtectionListClick(Player player, ProtectionListHolder holder, int slot) {
        List<ProtectionEntry> entries = protectionEntries();
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_SLOT && holder.page() > 0) {
            openProtections(player, holder.page() - 1);
            return;
        }
        if (slot == NEXT_SLOT && (holder.page() + 1) * PAGE_SIZE < entries.size()) {
            openProtections(player, holder.page() + 1);
            return;
        }
        int index = holder.page() * PAGE_SIZE + slot;
        if (slot >= PAGE_SIZE || index < 0 || index >= entries.size()) {
            return;
        }
        ProtectionEntry entry = entries.get(index);
        openFields(player, entry.type(), entry.id(), 0);
    }

    private void handleFieldClick(Player player, FieldListHolder holder, int slot, ClickType click) {
        List<WeaponConfigEditor.FieldSpec> fields = fieldsFor(holder.type(), holder.itemId());
        if (slot == BACK_SLOT) {
            if (holder.type() == ConfigType.WEAPON) {
                openWeapons(player, 0);
            } else {
                openProtections(player, 0);
            }
            return;
        }
        if (slot == PREV_SLOT && holder.page() > 0) {
            openFields(player, holder.type(), holder.itemId(), holder.page() - 1);
            return;
        }
        if (slot == NEXT_SLOT && (holder.page() + 1) * PAGE_SIZE < fields.size()) {
            openFields(player, holder.type(), holder.itemId(), holder.page() + 1);
            return;
        }
        int index = holder.page() * PAGE_SIZE + slot;
        if (slot >= PAGE_SIZE || index < 0 || index >= fields.size()) {
            return;
        }
        WeaponConfigEditor.FieldSpec field = fields.get(index);
        if (click.isRightClick()) {
            WeaponConfigEditor.EditResult result = remove(holder.type(), player.getName(), holder.itemId(), field.path());
            player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            openFields(player, holder.type(), holder.itemId(), holder.page());
            return;
        }
        pendingEdits.put(player.getUniqueId(), new PendingEdit(holder.type(), holder.itemId(), field.path(), holder.page()));
        player.closeInventory();
        player.sendMessage(Component.text("Scrivi il nuovo valore per " + holder.itemId() + "." + field.path()
                + " in chat, oppure 'cancel'.", NamedTextColor.YELLOW));
    }

    private List<ProtectionEntry> protectionEntries() {
        List<ProtectionEntry> entries = new ArrayList<>();
        editor.armorIds().forEach(id -> entries.add(new ProtectionEntry(ConfigType.ARMOR, id)));
        editor.helmetIds().forEach(id -> entries.add(new ProtectionEntry(ConfigType.HELMET, id)));
        return entries;
    }

    private ItemStack previewFor(ConfigType type, String itemId) {
        return switch (type) {
            case WEAPON -> module.getWeaponRegistry().createItemStack(itemId);
            case ARMOR -> module.getArmorManager() == null ? null : module.getArmorManager().createItemStack(itemId);
            case HELMET -> module.getHelmetManager() == null ? null : module.getHelmetManager().createItemStack(itemId);
        };
    }

    private List<WeaponConfigEditor.FieldSpec> fieldsFor(ConfigType type, String itemId) {
        return switch (type) {
            case WEAPON -> editor.fieldsFor(itemId);
            case ARMOR -> editor.armorFieldsFor(itemId);
            case HELMET -> editor.helmetFieldsFor(itemId);
        };
    }

    private WeaponConfigEditor.EditResult get(ConfigType type, String itemId, String path) {
        return switch (type) {
            case WEAPON -> editor.get(itemId, path);
            case ARMOR -> editor.getArmor(itemId, path);
            case HELMET -> editor.getHelmet(itemId, path);
        };
    }

    private WeaponConfigEditor.EditResult set(ConfigType type, String actorName, String itemId, String path, String value) {
        return switch (type) {
            case WEAPON -> editor.set(actorName, itemId, path, value);
            case ARMOR -> editor.setArmor(actorName, itemId, path, value);
            case HELMET -> editor.setHelmet(actorName, itemId, path, value);
        };
    }

    private WeaponConfigEditor.EditResult remove(ConfigType type, String actorName, String itemId, String path) {
        return switch (type) {
            case WEAPON -> editor.remove(actorName, itemId, path);
            case ARMOR -> editor.removeArmor(actorName, itemId, path);
            case HELMET -> editor.removeHelmet(actorName, itemId, path);
        };
    }

    private void setNavigation(Inventory inv, int page, int total, boolean showBack, String backName) {
        if (page > 0) {
            inv.setItem(PREV_SLOT, button(Material.ARROW, "Pagina precedente", NamedTextColor.YELLOW));
        }
        if ((page + 1) * PAGE_SIZE < total) {
            inv.setItem(NEXT_SLOT, button(Material.ARROW, "Pagina successiva", NamedTextColor.YELLOW));
        }
        if (showBack) {
            inv.setItem(BACK_SLOT, button(Material.BARRIER, backName, NamedTextColor.RED));
        }
    }

    private ItemStack button(Material material, String name, NamedTextColor color) {
        return new ItemBuilder(material).name(Component.text(name, color)).build();
    }

    private void fill(Inventory inv) {
        ItemStack filler = ItemBuilder.filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private Material iconFor(WeaponConfigEditor.FieldType type) {
        return switch (type) {
            case BOOLEAN -> Material.LEVER;
            case INT, NULLABLE_INT -> Material.REPEATER;
            case DOUBLE -> Material.COMPARATOR;
            case MATERIAL -> Material.ITEM_FRAME;
            case WEAPON_CATEGORY -> Material.NAME_TAG;
            case FIRE_MODE_LIST -> Material.REDSTONE;
            case STRING -> Material.PAPER;
        };
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "<unset>";
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public enum ConfigType {
        WEAPON("arma", "Arma", "Config", Material.CROSSBOW, NamedTextColor.RED),
        ARMOR("armatura", "Giubbotto antiproiettile", "Armatura", Material.LEATHER_CHESTPLATE, NamedTextColor.AQUA),
        HELMET("casco", "Casco", "Casco", Material.LEATHER_HELMET, NamedTextColor.BLUE);

        private final String commandName;
        private final String label;
        private final String titlePrefix;
        private final Material fallbackMaterial;
        private final NamedTextColor color;

        ConfigType(String commandName, String label, String titlePrefix, Material fallbackMaterial, NamedTextColor color) {
            this.commandName = commandName;
            this.label = label;
            this.titlePrefix = titlePrefix;
            this.fallbackMaterial = fallbackMaterial;
            this.color = color;
        }

        public String commandName() {
            return commandName;
        }

        public String label() {
            return label;
        }

        public String titlePrefix() {
            return titlePrefix;
        }

        public Material fallbackMaterial() {
            return fallbackMaterial;
        }

        public NamedTextColor color() {
            return color;
        }
    }

    private interface WeaponConfigHolder extends InventoryHolder {
        @Override
        default Inventory getInventory() {
            return null;
        }
    }

    private record RootHolder() implements WeaponConfigHolder {
    }

    private record WeaponListHolder(int page) implements WeaponConfigHolder {
    }

    private record ProtectionListHolder(int page) implements WeaponConfigHolder {
    }

    private record FieldListHolder(ConfigType type, String itemId, int page) implements WeaponConfigHolder {
    }

    private record ProtectionEntry(ConfigType type, String id) {
    }

    private record PendingEdit(ConfigType type, String itemId, String path, int page) {
    }
}
