package dev.openrp.jobs.model;

/**
 * One tracked action inside a {@link WorkSession}: what kind of work it was, the material involved, how
 * many units, and where. Granular by design so payment is accurate, progression is honest and disputes
 * are auditable - the plugin records the work that was physically done, never the time merely spent.
 */
public final class ActivityEntry {

    private final long timestamp;
    private final String actionType;
    private final String material;
    private final int quantity;
    private final int x;
    private final int y;
    private final int z;

    public ActivityEntry(long timestamp, String actionType, String material, int quantity, int x, int y, int z) {
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.material = material;
        this.quantity = Math.max(0, quantity);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long timestamp() {
        return timestamp;
    }

    public String actionType() {
        return actionType;
    }

    public String material() {
        return material;
    }

    public int quantity() {
        return quantity;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }
}
