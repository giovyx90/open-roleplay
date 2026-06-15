package dev.openrp.vending.core;

import org.bukkit.command.CommandSender;
import dev.openrp.vending.OpenVendingMachinesPlugin;
import dev.openrp.vending.model.MachineProduct;
import dev.openrp.vending.model.MachineType;
import dev.openrp.vending.model.ProductDefinition;
import dev.openrp.vending.model.VendingMachine;

/** Renders a machine's status as chat lines. Shared by {@code /ovm info} and the GUI info button. */
public final class MachineInfoPresenter {

    private MachineInfoPresenter() {
    }

    public static void send(OpenVendingMachinesPlugin plugin, CommandSender sender, VendingMachine machine) {
        String symbol = plugin.settings().currencySymbol();
        MachineType type = plugin.machineTypes().get(machine.typeId());
        String typeName = type == null ? machine.typeId() : type.plainName();
        String owner = machine.ownerCompanyId()
                .map(id -> plugin.adapters().business().companyDisplayName(id).orElse(id))
                .orElse("-");
        String stateLabel = plugin.messages().text(sender, machine.state().messageKey());

        plugin.messages().info(sender, "machine.info_header", "id", machine.shortId());
        plugin.messages().info(sender, "machine.info_type", "type", typeName);
        plugin.messages().info(sender, "machine.info_owner", "owner", owner);
        plugin.messages().info(sender, "machine.info_state", "state", stateLabel);
        plugin.messages().info(sender, "machine.info_cash", "symbol", symbol, "cash", Money.format(machine.cashBalance()));
        plugin.messages().info(sender, "machine.info_products");
        for (MachineProduct product : machine.products()) {
            ProductDefinition definition = plugin.products().get(product.productId());
            String productName = definition == null ? product.productId() : definition.plainName();
            plugin.messages().info(sender, "machine.info_product_line",
                    "product", productName,
                    "stock", product.stock(),
                    "capacity", product.capacity(),
                    "symbol", symbol,
                    "price", Money.format(product.price()));
        }
    }
}
