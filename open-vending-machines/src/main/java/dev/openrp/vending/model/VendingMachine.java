package dev.openrp.vending.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A live vending machine instance.
 *
 * <p>Mutable by design: stock, cash and state change at runtime. All field mutation is expected to
 * happen while holding the machine's lock (see {@code core.MachineLocks}) so concurrent purchase /
 * restock / withdraw operations cannot duplicate items or money.</p>
 */
public final class VendingMachine {

    private final UUID id;
    private final String typeId;
    private final MachineLocation location;
    private final Map<String, MachineProduct> products = new LinkedHashMap<>();

    private String ownerCompanyId;
    private double cashBalance;
    private VendingMachineState state;

    public VendingMachine(UUID id, String typeId, MachineLocation location, String ownerCompanyId) {
        this.id = id;
        this.typeId = typeId;
        this.location = location;
        this.ownerCompanyId = (ownerCompanyId == null || ownerCompanyId.isBlank()) ? null : ownerCompanyId;
        this.cashBalance = 0.0;
        this.state = VendingMachineState.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    /** Short, human-friendly id (first 8 chars) used in chat and command output. */
    public String shortId() {
        return id.toString().substring(0, 8);
    }

    public String typeId() {
        return typeId;
    }

    public MachineLocation location() {
        return location;
    }

    public Optional<String> ownerCompanyId() {
        return Optional.ofNullable(ownerCompanyId);
    }

    public boolean hasOwner() {
        return ownerCompanyId != null;
    }

    public void setOwnerCompanyId(String ownerCompanyId) {
        this.ownerCompanyId = (ownerCompanyId == null || ownerCompanyId.isBlank()) ? null : ownerCompanyId;
    }

    public double cashBalance() {
        return cashBalance;
    }

    public void depositCash(double amount) {
        if (amount > 0) {
            this.cashBalance += amount;
        }
    }

    /** Sets the cash box to exactly {@code value} (used by storage loading and withdrawals). */
    public void setCashBalance(double value) {
        this.cashBalance = Math.max(0.0, value);
    }

    /** Empties the cash box and returns the amount that was inside. */
    public double drainCash() {
        double drained = cashBalance;
        cashBalance = 0.0;
        return drained;
    }

    public VendingMachineState state() {
        return state;
    }

    public void setState(VendingMachineState state) {
        this.state = state == null ? VendingMachineState.ACTIVE : state;
    }

    public Collection<MachineProduct> products() {
        return products.values();
    }

    public MachineProduct product(String productId) {
        return productId == null ? null : products.get(productId.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean hasProduct(String productId) {
        return product(productId) != null;
    }

    public void putProduct(MachineProduct product) {
        if (product != null) {
            products.put(product.productId(), product);
        }
    }

    public void removeProduct(String productId) {
        if (productId != null) {
            products.remove(productId.toLowerCase(java.util.Locale.ROOT));
        }
    }

    public int productCount() {
        return products.size();
    }

    /** True when every stocked product is at zero. An empty machine (no products) also counts. */
    public boolean isOutOfStock() {
        for (MachineProduct product : products.values()) {
            if (product.inStock()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recomputes the automatic ACTIVE/EMPTY transition while preserving the manual BROKEN and
     * DISABLED states (those only change through explicit admin/repair actions). Returns the
     * resulting state for convenience.
     */
    public VendingMachineState refreshAutomaticState() {
        if (state == VendingMachineState.BROKEN || state == VendingMachineState.DISABLED) {
            return state;
        }
        state = isOutOfStock() ? VendingMachineState.EMPTY : VendingMachineState.ACTIVE;
        return state;
    }
}
