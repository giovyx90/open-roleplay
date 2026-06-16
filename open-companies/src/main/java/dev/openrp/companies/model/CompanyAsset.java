package dev.openrp.companies.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A physical company asset placed in the world (a terminal, POS, printer, safe, ...). The company
 * core deliberately stores only the minimum: a stable id, the owning company, the asset type, a
 * block position and a free-form metadata map that vertical modules can use for their own state.
 * Rendering and behaviour live in those modules, not here.
 */
public final class CompanyAsset {

    private final UUID id;
    private final String companyId;
    private final CompanyAssetType type;
    private final BlockPosition position;
    private final long createdAt;
    private final Map<String, String> metadata = new LinkedHashMap<>();

    public CompanyAsset(UUID id, String companyId, CompanyAssetType type, BlockPosition position, long createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.type = Objects.requireNonNull(type, "type");
        this.position = Objects.requireNonNull(position, "position");
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    /** First 8 characters of the id; handy for command output. */
    public String shortId() {
        return id.toString().substring(0, 8);
    }

    public String companyId() {
        return companyId;
    }

    public CompanyAssetType type() {
        return type;
    }

    public BlockPosition position() {
        return position;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    /** Block coordinates of an asset, with a compact string key used for fast location lookups. */
    public record BlockPosition(String world, int x, int y, int z) {

        public BlockPosition {
            world = world == null ? "world" : world;
        }

        /** Stable key for indexing assets by location, e.g. {@code "world:10:64:-3"}. */
        public String key() {
            return world.toLowerCase(Locale.ROOT) + ":" + x + ":" + y + ":" + z;
        }
    }
}
