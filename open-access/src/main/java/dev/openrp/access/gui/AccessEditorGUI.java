package dev.openrp.access.gui;

import dev.openrp.access.AccessService;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessRule;
import dev.openrp.access.util.AccessMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccessEditorGUI implements InventoryHolder {

    private final AccessService service;
    private final UUID viewerId;
    private final AccessProfile profile;
    private final Location blockLocation;
    private final Inventory inventory;

    public AccessEditorGUI(AccessService service, Player viewer, AccessProfile profile, Location blockLocation) {
        this.service = service;
        this.viewerId = viewer == null ? null : viewer.getUniqueId();
        this.profile = profile;
        this.blockLocation = blockLocation == null ? null : blockLocation.clone();
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Open Access", NamedTextColor.DARK_AQUA));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (viewerId != null && !viewerId.equals(player.getUniqueId())) {
            return;
        }
        int slot = event.getRawSlot();
        AccessPreset regionPreset = presetForRegionSlot(slot);
        if (regionPreset != null) {
            service.setRegionPreset(profile, regionPreset, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                        if (error != null) {
                            player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                            return;
                        }
                        player.sendMessage(AccessMessages.success("Access", "Preset regione impostato su " + regionPreset.displayName() + "."));
                        service.findProfileByRegion(profile.getWorld(), profile.getRegionId())
                                .ifPresent(next -> service.openEditor(player, next, blockLocation));
                    }));
            return;
        }

        AccessPreset blockPreset = presetForBlockSlot(slot);
        if (blockPreset != null && blockLocation != null) {
            service.setBlockPreset(profile, blockLocation, blockPreset, player.getUniqueId(), player.getName())
                    .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(service.core(), () -> {
                        if (error != null) {
                            player.sendMessage(AccessMessages.error("Access", rootMessage(error)));
                            return;
                        }
                        player.sendMessage(AccessMessages.success("Access", "Override blocco impostato su " + blockPreset.displayName() + "."));
                        service.findProfileByRegion(profile.getWorld(), profile.getRegionId())
                                .ifPresent(next -> service.openEditor(player, next, blockLocation));
                    }));
            return;
        }

        if (slot == 49) {
            player.closeInventory();
        }
    }

    private void render() {
        fill();
        inventory.setItem(10, item(Material.MAP, "Profilo", NamedTextColor.AQUA, List.of(
                "Tipo: " + profile.getType().name(),
                "Regione: " + profile.getWorld() + "/" + profile.getRegionId(),
                "Preset: " + profile.getDefaultPreset().displayName())));

        inventory.setItem(12, item(Material.ENDER_EYE, "Preset Regione", NamedTextColor.GREEN, List.of(
                "Si applica ai blocchi sensibili",
                "senza override dedicato.")));

        List<String> playerLore = new ArrayList<>();
        playerLore.add("Usa /access trust <player> [manage]");
        playerLore.add("Usa /access untrust <player>");
        playerLore.add("Regole player: " + service.regionRules(profile).stream()
                .filter(rule -> rule.getPrincipal().type().name().equals("PLAYER"))
                .count());
        inventory.setItem(14, item(Material.PLAYER_HEAD, "Player Custom", NamedTextColor.YELLOW, playerLore));

        if (blockLocation != null) {
            List<AccessRule> blockRules = service.blockRules(profile, blockLocation);
            inventory.setItem(16, item(Material.TRIPWIRE_HOOK, "Override Blocco", NamedTextColor.LIGHT_PURPLE, List.of(
                    "Blocco: " + blockLocation.getBlockX() + "," + blockLocation.getBlockY() + "," + blockLocation.getBlockZ(),
                    blockRules.isEmpty() ? "Eredita il preset regione." : "Regole override: " + blockRules.size())));
        } else {
            inventory.setItem(16, item(Material.GRAY_DYE, "Nessun Blocco", NamedTextColor.GRAY, List.of(
                    "Guarda un blocco o shift-click",
                    "per gestire accessi dedicati.")));
        }

        placePresetRow(19, "Regione", profile.getDefaultPreset());
        if (blockLocation != null) {
            placePresetRow(28, "Blocco", null);
        }
        inventory.setItem(49, item(Material.BARRIER, "Chiudi", NamedTextColor.RED, List.of("Chiude questo menu.")));
    }

    private void placePresetRow(int startSlot, String scope, AccessPreset selected) {
        AccessPreset[] presets = AccessPreset.values();
        for (int i = 0; i < presets.length; i++) {
            AccessPreset preset = presets[i];
            List<String> lore = new ArrayList<>();
            lore.add("Preset " + scope.toLowerCase() + ".");
            if (selected == preset) {
                lore.add("Selezionato ora.");
            }
            lore.add(switch (preset) {
                case PRIVATE -> "Solo proprietari e manager.";
                case MEMBERS -> "Membri e manager.";
                case MANAGERS -> "Solo manager.";
                case PUBLIC -> "Tutti usano i blocchi sensibili.";
                case CUSTOM -> "Solo regole esplicite e manager.";
            });
            inventory.setItem(startSlot + i, item(materialFor(preset), preset.displayName(), colorFor(preset), lore));
        }
    }

    private AccessPreset presetForRegionSlot(int slot) {
        return slot >= 19 && slot <= 23 ? AccessPreset.values()[slot - 19] : null;
    }

    private AccessPreset presetForBlockSlot(int slot) {
        return slot >= 28 && slot <= 32 ? AccessPreset.values()[slot - 28] : null;
    }

    private void fill() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<String> loreLines) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.displayName(Component.text(name, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines == null ? List.<String>of() : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private Material materialFor(AccessPreset preset) {
        return switch (preset) {
            case PRIVATE -> Material.IRON_DOOR;
            case MEMBERS -> Material.OAK_DOOR;
            case MANAGERS -> Material.GOLDEN_HELMET;
            case PUBLIC -> Material.LIME_DYE;
            case CUSTOM -> Material.NAME_TAG;
        };
    }

    private NamedTextColor colorFor(AccessPreset preset) {
        return switch (preset) {
            case PRIVATE -> NamedTextColor.RED;
            case MEMBERS -> NamedTextColor.GREEN;
            case MANAGERS -> NamedTextColor.GOLD;
            case PUBLIC -> NamedTextColor.AQUA;
            case CUSTOM -> NamedTextColor.LIGHT_PURPLE;
        };
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error != null && error.getCause() != null ? error.getCause() : error;
        return cause != null && cause.getMessage() != null ? cause.getMessage()
                : cause == null ? "Errore sconosciuto" : cause.getClass().getSimpleName();
    }
}
