package dev.openrp.vending.api;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import dev.openrp.vending.model.VendingMachine;

/** Read-only query surface over the live machine set. */
public interface VendingMachineService {

    Collection<VendingMachine> all();

    VendingMachine byId(UUID id);

    /** The machine occupying the given block, or {@code null}. */
    VendingMachine at(Location location);

    List<VendingMachine> nearby(Location location, double radius);

    int count();

    long countOwnedBy(String companyId);
}
