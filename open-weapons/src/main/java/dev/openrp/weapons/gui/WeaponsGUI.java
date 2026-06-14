package dev.openrp.weapons.gui;

import dev.openrp.weapons.util.ItemBuilder;
import dev.openrp.weapons.model.AmmoDefinition;
import dev.openrp.weapons.model.ArmorDefinition;
import dev.openrp.weapons.grenades.GrenadeDefinition;
import dev.openrp.weapons.model.WeaponDefinition;
import dev.openrp.weapons.module.WeaponsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WeaponsGUI implements Listener {
    private final WeaponsModule module;

    public WeaponsGUI(WeaponsModule module) {
        this.module = module;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Admin Armi", NamedTextColor.DARK_RED, TextDecoration.BOLD));

        int slot = 0;
        
        // Weapons
        for (WeaponDefinition weapon : module.getWeaponRegistry().getAll()) {
            if (slot >= 54) break;
            inv.setItem(slot++, module.getWeaponRegistry().createItemStack(weapon.getId()));
        }

        // Ammo
        for (AmmoDefinition ammo : module.getAmmoRegistry().getAll()) {
            if (slot >= 54) break;
            inv.setItem(slot++, module.getAmmoRegistry().createItemStack(ammo.getId(), ammo.getMaxStack()));
        }

        // Balaclava
        if (slot < 54) inv.setItem(slot++, module.getBalaclavaManager().createBalaclava());

        // Bulletproof vests
        for (ArmorDefinition armor : module.getArmorManager().getAll()) {
            if (slot >= 54) break;
            inv.setItem(slot++, module.getArmorManager().createItemStack(armor.getId()));
        }

        // Grenades
        for (GrenadeDefinition grenade : module.getGrenadeManager().getAll()) {
            if (slot >= 54) break;
            inv.setItem(slot++, module.getGrenadeManager().createItemStack(grenade.getId()));
        }

        // Handcuffs and Bolt Cutters
        if (slot < 54) inv.setItem(slot++, module.getHandcuffManager().createHandcuffs());
        if (slot < 54) inv.setItem(slot++, module.getHandcuffManager().createBoltCutters());

        // Fill empty spaces with glass
        ItemStack filler = ItemBuilder.filler();
        for (int i = slot; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Admin Armi", NamedTextColor.DARK_RED, TextDecoration.BOLD))) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Clone item to give to player
        ItemStack giveItem = clicked.clone();
        
        // Let's ensure it's one of our items
        if (module.getWeaponRegistry().getWeapon(clicked) != null || 
            module.getAmmoRegistry().getAmmo(clicked) != null || 
            module.getBalaclavaManager().isBalaclava(clicked) ||
            module.getArmorManager().getArmor(clicked) != null ||
            module.getGrenadeManager().getGrenade(clicked) != null ||
            module.getHandcuffManager().isHandcuffItem(clicked) ||
            module.getHandcuffManager().isBoltCutterItem(clicked)) {
            
            player.getInventory().addItem(giveItem);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }
}
