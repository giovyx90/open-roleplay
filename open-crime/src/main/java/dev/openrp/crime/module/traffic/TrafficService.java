package dev.openrp.crime.module.traffic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.openrp.crime.OpenCrimePlugin;
import dev.openrp.crime.capability.Capability;
import dev.openrp.crime.config.Good;
import dev.openrp.crime.config.Route;
import dev.openrp.crime.core.CrimeResult;
import dev.openrp.crime.core.Ids;
import dev.openrp.crime.model.CrimeEvent;
import dev.openrp.crime.model.CrimeEventType;
import dev.openrp.crime.model.IllegalOrg;
import dev.openrp.crime.model.Shipment;
import dev.openrp.crime.model.ShipmentStatus;
import dev.openrp.crime.model.TrackedGoodStatus;

/**
 * Drives the distribution of illegal goods along physical routes. The carrier really carries the
 * marked items; an interception is the authorities physically searching them. Delivery converts the
 * goods to dirty proceeds in the org treasury. The core never auto-notifies anyone about a shipment.
 */
public final class TrafficService {

    private final OpenCrimePlugin plugin;
    private final Map<String, Shipment> byId = new ConcurrentHashMap<>();

    public TrafficService(OpenCrimePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        byId.clear();
        for (Shipment shipment : plugin.adapters().storage().loadShipments()) {
            byId.put(shipment.id(), shipment);
        }
    }

    public CrimeResult start(Player player, String routeId) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null || !org.isActive()) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(player.getUniqueId(), Capability.TRAFFIC)) {
            return CrimeResult.fail("general.no_capability");
        }
        Route route = plugin.config().routes().get(routeId).orElse(null);
        if (route == null) {
            return CrimeResult.fail("traffic.unknown_route", "route", String.valueOf(routeId));
        }
        if (carrierBusy(player.getUniqueId())) {
            return CrimeResult.fail("traffic.already_carrying");
        }
        if (!inRegion(player, route.originRegion())) {
            return CrimeResult.fail("traffic.not_at_origin", "region", route.originRegion());
        }
        Map<String, Integer> goods = new LinkedHashMap<>();
        List<String> itemUuids = new ArrayList<>();
        tallyCarriedGoods(player, goods, itemUuids);
        if (goods.isEmpty()) {
            return CrimeResult.fail("traffic.no_goods");
        }

        long started = System.currentTimeMillis();
        long expected = started + plugin.config().settings().realMillisFromMinutes(route.durationMinutes());
        Shipment shipment = new Shipment(Ids.prefixed("ship"), org.id(), route.id(),
                player.getUniqueId(), started, expected);
        goods.forEach(shipment::addGood);
        byId.put(shipment.id(), shipment);
        plugin.adapters().storage().saveShipment(shipment);

        itemUuids.forEach(uuid -> plugin.goods().setStatus(uuid, TrackedGoodStatus.IN_TRANSIT));
        CrimeEvent event = new CrimeEvent(Ids.prefixed("evt"), CrimeEventType.TRAFFIC, org.id(),
                List.of(player.getUniqueId()), itemUuids, player.getWorld().getName(),
                player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                player.getLocation().getBlockZ(), started, null);
        plugin.events().register(event);
        return CrimeResult.ok("traffic.started", "route", route.name());
    }

    public CrimeResult deliver(Player player) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        Shipment shipment = activeOf(player.getUniqueId());
        if (shipment == null) {
            return CrimeResult.fail("traffic.no_active");
        }
        Route route = plugin.config().routes().get(shipment.routeId()).orElse(null);
        if (route != null && !inRegion(player, route.destinationRegion())) {
            return CrimeResult.fail("traffic.not_at_destination", "region", route.destinationRegion());
        }
        long proceeds = 0L;
        for (Map.Entry<String, Integer> entry : shipment.goods().entrySet()) {
            // Resolve the good FIRST: if its id was removed from config mid-transit, leave the cargo in
            // the carrier's inventory rather than destroying it for no payment.
            Good good = plugin.config().goods().get(entry.getKey()).orElse(null);
            if (good == null) {
                continue;
            }
            int removed = removeCarriedGood(player, entry.getKey(), entry.getValue());
            proceeds += (long) good.streetValue() * removed;
        }
        plugin.adapters().economy().deposit(org.treasury(), proceeds, true);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(System.currentTimeMillis());
        plugin.adapters().storage().saveShipment(shipment);
        return CrimeResult.ok("traffic.delivered", "route", route == null ? shipment.routeId() : route.name(),
                "proceeds", String.valueOf(proceeds));
    }

    public CrimeResult agreement(Player player, String otherOrgId) {
        IllegalOrg org = plugin.orgs().byMember(player.getUniqueId()).orElse(null);
        if (org == null) {
            return CrimeResult.fail("syndicate.not_in_org");
        }
        if (!plugin.orgs().has(player.getUniqueId(), Capability.TRAFFIC_AGREEMENT)) {
            return CrimeResult.fail("general.no_capability");
        }
        IllegalOrg other = plugin.orgs().get(otherOrgId).orElse(null);
        if (other == null || other.id().equals(org.id())) {
            return CrimeResult.fail("traffic.unknown_org", "id", String.valueOf(otherOrgId));
        }
        // The plugin registers the intent; the actual terms are negotiated in RP.
        return CrimeResult.ok("traffic.agreement_proposed", "org", other.name());
    }

    public List<Shipment> ofOrg(String orgId) {
        List<Shipment> result = new ArrayList<>();
        for (Shipment shipment : byId.values()) {
            if (shipment.orgId().equals(orgId)) {
                result.add(shipment);
            }
        }
        return result;
    }

    public List<Shipment> activeOfOrg(String orgId) {
        List<Shipment> result = new ArrayList<>();
        for (Shipment shipment : byId.values()) {
            if (shipment.orgId().equals(orgId) && shipment.status() == ShipmentStatus.IN_TRANSIT) {
                result.add(shipment);
            }
        }
        return result;
    }

    private Shipment activeOf(java.util.UUID carrier) {
        for (Shipment shipment : byId.values()) {
            if (carrier.equals(shipment.carrier()) && shipment.status() == ShipmentStatus.IN_TRANSIT) {
                return shipment;
            }
        }
        return null;
    }

    private boolean carrierBusy(java.util.UUID carrier) {
        return activeOf(carrier) != null;
    }

    private boolean inRegion(Player player, String regionId) {
        if (regionId == null || regionId.isBlank() || !plugin.adapters().region().available()) {
            return true;
        }
        return plugin.adapters().region().regionAt(player.getLocation())
                .map(regionId::equals).orElse(false);
    }

    private void tallyCarriedGoods(Player player, Map<String, Integer> goods, List<String> itemUuids) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            Optional<String> goodId = plugin.goods().goodId(item);
            if (goodId.isEmpty()) {
                continue;
            }
            goods.merge(goodId.get(), item.getAmount(), Integer::sum);
            plugin.goods().itemUuid(item).ifPresent(itemUuids::add);
        }
    }

    /** Removes up to {@code amount} of the carried good from the inventory; returns how many were removed. */
    private int removeCarriedGood(Player player, String goodId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !plugin.goods().goodId(item).map(goodId::equals).orElse(false)) {
                continue;
            }
            int take = Math.min(remaining, item.getAmount());
            plugin.goods().itemUuid(item).ifPresent(uuid -> plugin.goods().setStatus(uuid, TrackedGoodStatus.SOLD));
            if (take >= item.getAmount()) {
                player.getInventory().setItem(slot, null);
            } else {
                // Write back through setItem so we never rely on getContents() returning live references.
                ItemStack reduced = item.clone();
                reduced.setAmount(item.getAmount() - take);
                player.getInventory().setItem(slot, reduced);
            }
            remaining -= take;
        }
        return amount - remaining;
    }
}
