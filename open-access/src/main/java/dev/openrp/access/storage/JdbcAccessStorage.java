package dev.openrp.access.storage;

import dev.openrp.access.model.AccessAction;
import dev.openrp.access.model.AccessPreset;
import dev.openrp.access.model.AccessPrincipal;
import dev.openrp.access.model.AccessPrincipalType;
import dev.openrp.access.model.AccessProfile;
import dev.openrp.access.model.AccessProfileType;
import dev.openrp.access.model.AccessRule;
import dev.openrp.access.model.AccessRuleScope;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JdbcAccessStorage implements AccessStorage {
    private final DataSource dataSource;
    private final AccessSqlDialect dialect;
    private final Runnable closeHook;

    public JdbcAccessStorage(DataSource dataSource, AccessSqlDialect dialect, Runnable closeHook) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.closeHook = closeHook == null ? () -> { } : closeHook;
    }

    @Override
    public void createTables() {
        execute(dialect.createProfilesSql());
        execute(dialect.createRulesSql());
        execute(dialect.createAuditSql());
        for (String sql : dialect.createIndexSql()) {
            execute(sql);
        }
    }

    @Override
    public List<AccessProfile> loadProfiles() {
        List<AccessProfile> profiles = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM access_profiles WHERE enabled = 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                profiles.add(profileFrom(rs));
            }
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile caricare i profili accesso.", e);
        }
        return profiles;
    }

    @Override
    public List<AccessRule> loadRules() {
        List<AccessRule> rules = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM access_rules");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rules.add(ruleFrom(rs));
            }
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile caricare le regole accesso.", e);
        }
        return rules;
    }

    @Override
    public void saveProfile(AccessProfile profile) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.saveProfileSql())) {
            ps.setString(1, profile.getId());
            ps.setString(2, profile.getType().name());
            ps.setString(3, profile.getWorld());
            ps.setString(4, profile.getRegionId());
            ps.setString(5, profile.getOwnerUuid() == null ? null : profile.getOwnerUuid().toString());
            ps.setString(6, profile.getOwnerName());
            ps.setString(7, profile.getOwnerKey());
            ps.setString(8, profile.getDisplayName());
            ps.setString(9, profile.getDefaultPreset().name());
            ps.setBoolean(10, profile.isEnabled());
            ps.setString(11, profile.getCreatedAt().toString());
            ps.setString(12, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile salvare il profilo accesso.", e);
        }
    }

    @Override
    public void deleteProfile(String profileId) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM access_rules WHERE profile_id = ?")) {
                ps.setString(1, profileId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM access_profiles WHERE id = ?")) {
                ps.setString(1, profileId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile eliminare il profilo accesso.", e);
        }
    }

    @Override
    public void saveRule(AccessRule rule) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.saveRuleSql())) {
            ps.setString(1, rule.getId());
            ps.setString(2, rule.getProfileId());
            ps.setString(3, rule.getScope().name());
            ps.setString(4, rule.getWorld());
            setNullableInt(ps, 5, rule.getX());
            setNullableInt(ps, 6, rule.getY());
            setNullableInt(ps, 7, rule.getZ());
            ps.setString(8, rule.getPrincipal().type().name());
            ps.setString(9, rule.getPrincipal().value());
            ps.setString(10, actionsCsv(rule.getActions()));
            ps.setBoolean(11, rule.isAllow());
            ps.setString(12, rule.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile salvare la regola accesso.", e);
        }
    }

    @Override
    public void deleteBlockRules(String profileId, String world, int x, int y, int z) {
        String sql = "DELETE FROM access_rules WHERE profile_id = ? AND scope = 'BLOCK' AND world = ? AND x = ? AND y = ? AND z = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profileId);
            ps.setString(2, world);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile eliminare le regole del blocco.", e);
        }
    }

    @Override
    public void deletePlayerRules(String profileId, String playerUuid) {
        String sql = "DELETE FROM access_rules WHERE profile_id = ? AND principal_type = 'PLAYER' AND principal_value = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profileId);
            ps.setString(2, playerUuid == null ? "" : playerUuid.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile eliminare le regole del player.", e);
        }
    }

    @Override
    public void audit(String profileId, String action, UUID actorUuid, String actorName,
                      String world, Integer x, Integer y, Integer z, String detailsJson) {
        String sql = """
                INSERT INTO access_audit_logs (
                    id, profile_id, action, actor_uuid, actor_name, world, x, y, z, details_json, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "audit-" + UUID.randomUUID());
            ps.setString(2, profileId);
            ps.setString(3, action);
            ps.setString(4, actorUuid == null ? null : actorUuid.toString());
            ps.setString(5, actorName);
            ps.setString(6, world);
            setNullableInt(ps, 7, x);
            setNullableInt(ps, 8, y);
            setNullableInt(ps, 9, z);
            ps.setString(10, detailsJson == null || detailsJson.isBlank() ? "{}" : detailsJson);
            ps.setString(11, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile scrivere l'audit accesso.", e);
        }
    }

    @Override
    public long countAuditLogs() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM access_audit_logs");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new AccessStorageException("Impossibile contare gli audit accesso.", e);
        }
    }

    @Override
    public void close() {
        closeHook.run();
    }

    private void execute(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new AccessStorageException("Errore SQL: " + sql, e);
        }
    }

    private AccessProfile profileFrom(ResultSet rs) throws SQLException {
        return new AccessProfile(
                rs.getString("id"),
                AccessProfileType.parse(rs.getString("type")),
                rs.getString("world"),
                rs.getString("region_id"),
                uuidOrNull(rs.getString("owner_uuid")),
                rs.getString("owner_name"),
                rs.getString("owner_key"),
                rs.getString("display_name"),
                AccessPreset.parse(rs.getString("default_preset")),
                rs.getBoolean("enabled"),
                instantOrNow(rs.getString("created_at")),
                instantOrNow(rs.getString("updated_at"))
        );
    }

    private AccessRule ruleFrom(ResultSet rs) throws SQLException {
        AccessRuleScope scope = AccessRuleScope.valueOf(rs.getString("scope"));
        Integer x = nullableInt(rs, "x");
        Integer y = nullableInt(rs, "y");
        Integer z = nullableInt(rs, "z");
        AccessPrincipal principal = new AccessPrincipal(
                AccessPrincipalType.parse(rs.getString("principal_type")),
                rs.getString("principal_value"));
        return new AccessRule(
                rs.getString("id"),
                rs.getString("profile_id"),
                scope,
                rs.getString("world"),
                x,
                y,
                z,
                principal,
                parseActions(rs.getString("actions_csv")),
                rs.getBoolean("allow_rule"),
                instantOrNow(rs.getString("created_at"))
        );
    }

    private Set<AccessAction> parseActions(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("ALL")) {
            return EnumSet.copyOf(AccessAction.ALL_ACTIONS);
        }
        EnumSet<AccessAction> actions = EnumSet.noneOf(AccessAction.class);
        for (String token : raw.split(",")) {
            if (!token.isBlank()) {
                actions.add(AccessAction.parse(token));
            }
        }
        return actions.isEmpty() ? EnumSet.copyOf(AccessAction.ALL_ACTIONS) : actions;
    }

    private String actionsCsv(Set<AccessAction> actions) {
        if (actions == null || actions.isEmpty() || actions.containsAll(AccessAction.ALL_ACTIONS)) {
            return "ALL";
        }
        return actions.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private UUID uuidOrNull(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Instant instantOrNow(String raw) {
        try {
            return raw == null || raw.isBlank() ? Instant.now() : Instant.parse(raw);
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
