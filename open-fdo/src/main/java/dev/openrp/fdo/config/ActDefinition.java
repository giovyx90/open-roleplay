package dev.openrp.fdo.config;

import dev.openrp.fdo.capability.Capability;

/**
 * A configured act from {@code acts.yml}. An act is available to a member only when their rank holds
 * {@link #capability()} AND - if {@link #requiresAdapter()} is set - that adapter is registered at
 * runtime. The remaining flags describe the act's effect on the core's records; the plugin never
 * writes the act's <em>content</em> (the member writes that in the stamped book), it only registers,
 * enables and (for detention) executes through the adapter.
 *
 * @param custodyHours hours of cautionary custody to open, or {@code -1} to use the global default
 * @param wantedLevel  wanted level to apply when {@link #flagsWanted()} is set
 * @param requiresAdapter adapter id this act needs, or {@code null} when self-contained
 */
public record ActDefinition(String id, String displayName, Capability capability, String requiresAdapter,
                            boolean opensDossier, boolean startsCustody, int custodyHours,
                            boolean seizesEvidence, boolean flagsWanted, int wantedLevel,
                            boolean issuesFine, String bookTemplate, String icon) {

    /** The adapter id this act needs at runtime, combining its own and its capability's requirement. */
    public String effectiveRequiredAdapter() {
        if (requiresAdapter != null && !requiresAdapter.isBlank()) {
            return requiresAdapter;
        }
        return capability == null ? null : capability.requiredAdapter().orElse(null);
    }
}
