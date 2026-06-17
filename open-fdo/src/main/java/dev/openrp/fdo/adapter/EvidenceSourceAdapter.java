package dev.openrp.fdo.adapter;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/**
 * Optional forensic source detection (e.g. "UV fingerprints"). Kept out of the core so seizing
 * evidence never assumes a weapons module: if no adapter is present, evidence is still collected
 * manually, just without the trace sub-system.
 */
public interface EvidenceSourceAdapter {

    String id();

    /** A trace label for the item (e.g. the last handler), or empty if nothing is detected. */
    Optional<String> sourceOf(ItemStack item);
}
