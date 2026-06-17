package dev.openrp.jobs.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Licence rules for a job. {@code autoIssue} mints the licence on the worker's first session; when off,
 * an admin or NPC must issue it manually (the trade is learned from someone). The item fields only
 * matter when an Identity adapter is present and turn the licence into a physical NBT document.
 */
public final class LicenseSpec {

    private final boolean autoIssue;
    private final boolean revocable;
    private final String displayName;
    private final String itemMaterial;
    private final int itemCustomModelData;

    public LicenseSpec(boolean autoIssue, boolean revocable, String displayName,
                       String itemMaterial, int itemCustomModelData) {
        this.autoIssue = autoIssue;
        this.revocable = revocable;
        this.displayName = displayName;
        this.itemMaterial = itemMaterial;
        this.itemCustomModelData = itemCustomModelData;
    }

    public static LicenseSpec from(ConfigurationSection section) {
        if (section == null) {
            return new LicenseSpec(true, true, "", "PAPER", 0);
        }
        return new LicenseSpec(
                section.getBoolean("auto_issue", true),
                section.getBoolean("revocable", true),
                section.getString("display_name", ""),
                section.getString("item_material", "PAPER"),
                section.getInt("item_custom_model_data", 0));
    }

    public boolean autoIssue() {
        return autoIssue;
    }

    public boolean revocable() {
        return revocable;
    }

    public String displayName() {
        return displayName;
    }

    public String itemMaterial() {
        return itemMaterial;
    }

    public int itemCustomModelData() {
        return itemCustomModelData;
    }
}
