package dev.openrp.weapons.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class OpenCompanyBridge {
    public boolean isCompanyEmployeeOfType(UUID uuid, String... companyTypes) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return false;
        }
        for (String companyType : normalize(companyTypes)) {
            if (hasTypePermission(player, companyType)) {
                return true;
            }
        }
        return false;
    }

    public List<Player> onlineCompanyEmployees(String... companyTypes) {
        Set<String> types = normalize(companyTypes);
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String type : types) {
                if (hasTypePermission(player, type)) {
                    players.add(player);
                    break;
                }
            }
        }
        return players;
    }

    private boolean hasTypePermission(Player player, String companyType) {
        String key = companyType.toLowerCase(Locale.ROOT).replace('_', '.');
        return player.hasPermission("openrp.company." + key);
    }

    private Set<String> normalize(String... companyTypes) {
        if (companyTypes == null || companyTypes.length == 0) {
            return Set.of();
        }
        java.util.LinkedHashSet<String> normalized = new java.util.LinkedHashSet<>();
        for (String type : companyTypes) {
            if (type != null && !type.isBlank()) {
                normalized.add(type.trim().toUpperCase(Locale.ROOT));
            }
        }
        return Set.copyOf(normalized);
    }
}
