package dev.openrp.vending.listener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.model.VendingMachine;

/**
 * Optional bridge that lets a machine "be" a custom furniture model. ItemsAdder, Oraxen and Nexo
 * furniture (and vanilla item frames / armor stands) are <em>entities</em>, not blocks, so the block
 * interaction listener never sees them. This listener catches a right-click on any entity, resolves
 * the machine sitting on that block (the entity's block, or the block just below it - furniture often
 * floats one block up), and opens the same GUI.
 *
 * <p>Always registered but gated by {@code interaction.furniture-entities} so it can be toggled with
 * {@code /ovm reload}. It needs no compile-time dependency on any furniture plugin: bind a machine to
 * the block under the furniture (place the furniture, then {@code /ovm create} looking at that block,
 * with {@code machines.place-icon-block: false}).</p>
 */
public final class FurnitureInteractionListener implements Listener {

    private final OpenVendingMachinesPlugin plugin;

    public FurnitureInteractionListener(OpenVendingMachinesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.settings().furnitureEntities() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        VendingMachine machine = resolve(event.getRightClicked());
        if (machine == null) {
            return;
        }
        event.setCancelled(true);
        MachineInteractionListener.openMachine(plugin, event.getPlayer(), machine);
    }

    private VendingMachine resolve(Entity entity) {
        Location location = entity.getLocation();
        VendingMachine machine = plugin.machines().getAt(location);
        if (machine != null) {
            return machine;
        }
        return plugin.machines().getAt(location.subtract(0, 1, 0));
    }
}
