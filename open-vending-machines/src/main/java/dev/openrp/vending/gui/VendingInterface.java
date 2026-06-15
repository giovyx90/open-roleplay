package dev.openrp.vending.gui;

import org.bukkit.entity.Player;
import dev.openrp.vending.model.VendingMachine;

/**
 * The swappable user-interface contract. The core (commands, interaction listener) only ever calls
 * these two methods, so a server can replace the bundled chest GUI with anything - a custom menu
 * library, a web panel trigger, a dialog plugin - by providing its own implementation and setting it
 * via {@code OpenVendingMachinesPlugin#setUserInterface}. The default implementation is
 * {@link DefaultVendingInterface}.
 */
public interface VendingInterface {

    /** Opens the buy view for the machine. */
    void openPurchase(Player player, VendingMachine machine);

    /** Opens the staff management view (restock / withdraw / prices) for the machine. */
    void openManagement(Player player, VendingMachine machine);
}
