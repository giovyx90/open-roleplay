package dev.openrp.vending.adapter.defaults;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import dev.openrp.vending.adapter.BusinessAdapter;
import dev.openrp.vending.adapter.BusinessCapability;

/**
 * Default business adapter driven entirely by the {@code businesses} section of config.yml. It reads
 * the live plugin config on each call, so {@code /ovm reload} immediately reflects edits. Swap this
 * adapter to source companies, members and roles from a real company plugin instead.
 */
public final class ConfigBusinessAdapter implements BusinessAdapter {

    private final Plugin plugin;

    public ConfigBusinessAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "config";
    }

    @Override
    public boolean companyExists(String companyId) {
        return company(companyId) != null;
    }

    @Override
    public Optional<String> companyDisplayName(String companyId) {
        ConfigurationSection company = company(companyId);
        if (company == null) {
            return Optional.empty();
        }
        return Optional.of(company.getString("display-name", companyId));
    }

    @Override
    public boolean isMember(OfflinePlayer player, String companyId) {
        return roleOf(player, companyId).isPresent();
    }

    @Override
    public Optional<String> roleOf(OfflinePlayer player, String companyId) {
        ConfigurationSection company = company(companyId);
        if (company == null) {
            return Optional.empty();
        }
        ConfigurationSection members = company.getConfigurationSection("members");
        if (members == null) {
            return Optional.empty();
        }
        String role = members.getString(player.getUniqueId().toString());
        return Optional.ofNullable(role);
    }

    @Override
    public boolean hasCapability(OfflinePlayer player, String companyId, BusinessCapability capability) {
        Optional<String> role = roleOf(player, companyId);
        if (role.isEmpty()) {
            return false;
        }
        ConfigurationSection company = company(companyId);
        ConfigurationSection roles = company == null ? null : company.getConfigurationSection("roles");
        if (roles == null) {
            return false;
        }
        List<String> granted = roles.getStringList(role.get());
        return granted.stream().anyMatch(value -> value.equalsIgnoreCase(capability.key()));
    }

    @Override
    public int machineLimit(String companyId) {
        ConfigurationSection company = company(companyId);
        int fallback = plugin.getConfig().getInt("machines.default-company-limit", 5);
        return company == null ? fallback : company.getInt("machine-limit", fallback);
    }

    @Override
    public Optional<String> companyAccount(String companyId) {
        ConfigurationSection company = company(companyId);
        if (company == null) {
            return Optional.empty();
        }
        return Optional.of(company.getString("account", companyId));
    }

    private ConfigurationSection company(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            return null;
        }
        ConfigurationSection businesses = plugin.getConfig().getConfigurationSection("businesses");
        if (businesses == null || !businesses.getBoolean("enabled", true)) {
            return null;
        }
        ConfigurationSection companies = businesses.getConfigurationSection("companies");
        return companies == null ? null : companies.getConfigurationSection(companyId.toLowerCase(Locale.ROOT));
    }
}
