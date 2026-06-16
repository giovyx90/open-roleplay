package dev.openrp.companies.integration.vending;

import java.util.Optional;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import dev.openrp.companies.OpenCompaniesPlugin;
import dev.openrp.companies.model.CompanyRole;
import dev.openrp.vending.adapter.BusinessAdapter;
import dev.openrp.vending.adapter.BusinessCapability;
import dev.openrp.vending.adapter.defaults.ConfigBusinessAdapter;
import dev.openrp.vending.api.OpenVendingMachinesApi;

/**
 * Bridges Open Companies into Open Vending Machines' {@code BusinessAdapter} so vending machines can
 * belong to companies and respect company roles. This class references Open Vending Machines types,
 * so it is only ever loaded when that plugin is present (the registration is guarded in
 * {@code OpenCompaniesPlugin}); Open Companies has no hard dependency on it.
 *
 * <p>Vending capabilities map to a minimum company role <em>level</em> (CEO=6 ... TRAINING=1):</p>
 * <ul>
 *   <li>{@code USE} &rarr; Employee+ (level&nbsp;2)</li>
 *   <li>{@code RESTOCK} &rarr; Employee+ or Manager+ ({@code integration.vending.restock-requires-manager})</li>
 *   <li>{@code WITHDRAW} &rarr; Manager+ (level&nbsp;3)</li>
 *   <li>{@code EDIT_PRICE} &rarr; Manager+ (level&nbsp;3)</li>
 *   <li>{@code MANAGE} &rarr; Director+ (level&nbsp;5)</li>
 * </ul>
 */
public final class OpenCompaniesBusinessAdapter implements BusinessAdapter {

    private final OpenCompaniesPlugin plugin;

    public OpenCompaniesBusinessAdapter(OpenCompaniesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Installs this adapter into a running Open Vending Machines instance. Confined here (the only
     * class that references Open Vending types) so the rest of the plugin never touches them and loads
     * fine when Open Vending Machines is absent. Returns {@code false} if the vending API is unavailable.
     */
    public static boolean register(OpenCompaniesPlugin plugin) {
        RegisteredServiceProvider<OpenVendingMachinesApi> registration =
                plugin.getServer().getServicesManager().getRegistration(OpenVendingMachinesApi.class);
        if (registration == null || registration.getProvider() == null) {
            return false;
        }
        registration.getProvider().adapters().setBusiness(new OpenCompaniesBusinessAdapter(plugin));
        return true;
    }

    /** Restores Open Vending Machines' default config-driven business adapter when we unload. */
    public static void unregister(OpenCompaniesPlugin plugin) {
        RegisteredServiceProvider<OpenVendingMachinesApi> registration =
                plugin.getServer().getServicesManager().getRegistration(OpenVendingMachinesApi.class);
        Plugin vending = plugin.getServer().getPluginManager().getPlugin("OpenVendingMachines");
        if (registration != null && registration.getProvider() != null && vending != null) {
            registration.getProvider().adapters().setBusiness(new ConfigBusinessAdapter(vending));
        }
    }

    @Override
    public String id() {
        return "open-companies";
    }

    @Override
    public boolean companyExists(String companyId) {
        return plugin.companies().exists(companyId);
    }

    @Override
    public Optional<String> companyDisplayName(String companyId) {
        return plugin.companies().findById(companyId).map(company -> company.displayName());
    }

    @Override
    public boolean isMember(OfflinePlayer player, String companyId) {
        return player != null && plugin.companies().roleOf(player.getUniqueId(), companyId).isPresent();
    }

    @Override
    public Optional<String> roleOf(OfflinePlayer player, String companyId) {
        if (player == null) {
            return Optional.empty();
        }
        return plugin.companies().roleOf(player.getUniqueId(), companyId).map(Enum::name);
    }

    @Override
    public boolean hasCapability(OfflinePlayer player, String companyId, BusinessCapability capability) {
        if (player == null || capability == null) {
            return false;
        }
        return plugin.companies().roleOf(player.getUniqueId(), companyId)
                .map(role -> role.level() >= minimumLevel(capability))
                .orElse(false);
    }

    @Override
    public int machineLimit(String companyId) {
        return plugin.settings().vendingDefaultMachineLimit();
    }

    @Override
    public Optional<String> companyAccount(String companyId) {
        return plugin.companies().findById(companyId).map(company -> company.id());
    }

    private int minimumLevel(BusinessCapability capability) {
        return switch (capability) {
            case USE -> CompanyRole.EMPLOYEE.level();
            case RESTOCK -> plugin.settings().vendingRestockRequiresManager()
                    ? CompanyRole.MANAGER.level()
                    : CompanyRole.EMPLOYEE.level();
            case WITHDRAW, EDIT_PRICE -> CompanyRole.MANAGER.level();
            case MANAGE -> CompanyRole.DIRECTOR.level();
        };
    }
}
