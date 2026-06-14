package dev.openrp.access.storage;

final class MySqlAccessSqlDialect implements AccessSqlDialect {
    @Override
    public String createProfilesSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_profiles (
                    id VARCHAR(64) PRIMARY KEY,
                    type VARCHAR(32) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    region_id VARCHAR(128) NOT NULL,
                    owner_uuid VARCHAR(64) NULL,
                    owner_name VARCHAR(64) NULL,
                    owner_key VARCHAR(128) NULL,
                    display_name VARCHAR(128) NULL,
                    default_preset VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
                    enabled TINYINT(1) DEFAULT 1,
                    created_at VARCHAR(64),
                    updated_at VARCHAR(64),
                    UNIQUE KEY uk_access_profile_region (world, region_id),
                    INDEX idx_access_profile_type (type),
                    INDEX idx_access_profile_owner_key (owner_key)
                )""";
    }

    @Override
    public String createRulesSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_rules (
                    id VARCHAR(64) PRIMARY KEY,
                    profile_id VARCHAR(64) NOT NULL,
                    scope VARCHAR(16) NOT NULL,
                    world VARCHAR(64) NULL,
                    x INT NULL,
                    y INT NULL,
                    z INT NULL,
                    principal_type VARCHAR(32) NOT NULL,
                    principal_value VARCHAR(128) NULL,
                    actions_csv TEXT NOT NULL,
                    allow_rule TINYINT(1) DEFAULT 1,
                    created_at VARCHAR(64),
                    INDEX idx_access_rule_profile (profile_id),
                    INDEX idx_access_rule_location (world, x, y, z),
                    INDEX idx_access_rule_principal (principal_type, principal_value)
                )""";
    }

    @Override
    public String createAuditSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_audit_logs (
                    id VARCHAR(64) PRIMARY KEY,
                    profile_id VARCHAR(64) NULL,
                    action VARCHAR(64) NOT NULL,
                    actor_uuid VARCHAR(64) NULL,
                    actor_name VARCHAR(64) NULL,
                    world VARCHAR(64) NULL,
                    x INT NULL,
                    y INT NULL,
                    z INT NULL,
                    details_json TEXT,
                    created_at VARCHAR(64),
                    INDEX idx_access_audit_profile (profile_id),
                    INDEX idx_access_audit_actor (actor_uuid),
                    INDEX idx_access_audit_action (action),
                    INDEX idx_access_audit_created (created_at)
                )""";
    }

    @Override
    public Iterable<String> createIndexSql() {
        return java.util.List.of();
    }

    @Override
    public String saveProfileSql() {
        return """
                INSERT INTO access_profiles (
                    id, type, world, region_id, owner_uuid, owner_name, owner_key,
                    display_name, default_preset, enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    type = VALUES(type),
                    owner_uuid = VALUES(owner_uuid),
                    owner_name = VALUES(owner_name),
                    owner_key = VALUES(owner_key),
                    display_name = VALUES(display_name),
                    default_preset = VALUES(default_preset),
                    enabled = VALUES(enabled),
                    updated_at = VALUES(updated_at)
                """;
    }

    @Override
    public String saveRuleSql() {
        return """
                INSERT INTO access_rules (
                    id, profile_id, scope, world, x, y, z, principal_type,
                    principal_value, actions_csv, allow_rule, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    scope = VALUES(scope),
                    world = VALUES(world),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    principal_type = VALUES(principal_type),
                    principal_value = VALUES(principal_value),
                    actions_csv = VALUES(actions_csv),
                    allow_rule = VALUES(allow_rule)
                """;
    }
}
