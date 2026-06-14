package dev.openrp.access.storage;

final class SqliteAccessSqlDialect implements AccessSqlDialect {
    @Override
    public String createProfilesSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_profiles (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    world TEXT NOT NULL,
                    region_id TEXT NOT NULL,
                    owner_uuid TEXT NULL,
                    owner_name TEXT NULL,
                    owner_key TEXT NULL,
                    display_name TEXT NULL,
                    default_preset TEXT NOT NULL DEFAULT 'PRIVATE',
                    enabled INTEGER DEFAULT 1,
                    created_at TEXT,
                    updated_at TEXT,
                    UNIQUE (world, region_id)
                )""";
    }

    @Override
    public String createRulesSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_rules (
                    id TEXT PRIMARY KEY,
                    profile_id TEXT NOT NULL,
                    scope TEXT NOT NULL,
                    world TEXT NULL,
                    x INTEGER NULL,
                    y INTEGER NULL,
                    z INTEGER NULL,
                    principal_type TEXT NOT NULL,
                    principal_value TEXT NULL,
                    actions_csv TEXT NOT NULL,
                    allow_rule INTEGER DEFAULT 1,
                    created_at TEXT
                )""";
    }

    @Override
    public String createAuditSql() {
        return """
                CREATE TABLE IF NOT EXISTS access_audit_logs (
                    id TEXT PRIMARY KEY,
                    profile_id TEXT NULL,
                    action TEXT NOT NULL,
                    actor_uuid TEXT NULL,
                    actor_name TEXT NULL,
                    world TEXT NULL,
                    x INTEGER NULL,
                    y INTEGER NULL,
                    z INTEGER NULL,
                    details_json TEXT,
                    created_at TEXT
                )""";
    }

    @Override
    public Iterable<String> createIndexSql() {
        return java.util.List.of(
                "CREATE INDEX IF NOT EXISTS idx_access_rule_profile ON access_rules (profile_id)",
                "CREATE INDEX IF NOT EXISTS idx_access_rule_location ON access_rules (world, x, y, z)",
                "CREATE INDEX IF NOT EXISTS idx_access_rule_principal ON access_rules (principal_type, principal_value)",
                "CREATE INDEX IF NOT EXISTS idx_access_audit_profile ON access_audit_logs (profile_id)",
                "CREATE INDEX IF NOT EXISTS idx_access_audit_actor ON access_audit_logs (actor_uuid)",
                "CREATE INDEX IF NOT EXISTS idx_access_audit_action ON access_audit_logs (action)",
                "CREATE INDEX IF NOT EXISTS idx_access_audit_created ON access_audit_logs (created_at)"
        );
    }

    @Override
    public String saveProfileSql() {
        return """
                INSERT INTO access_profiles (
                    id, type, world, region_id, owner_uuid, owner_name, owner_key,
                    display_name, default_preset, enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    type = excluded.type,
                    owner_uuid = excluded.owner_uuid,
                    owner_name = excluded.owner_name,
                    owner_key = excluded.owner_key,
                    display_name = excluded.display_name,
                    default_preset = excluded.default_preset,
                    enabled = excluded.enabled,
                    updated_at = excluded.updated_at
                """;
    }

    @Override
    public String saveRuleSql() {
        return """
                INSERT INTO access_rules (
                    id, profile_id, scope, world, x, y, z, principal_type,
                    principal_value, actions_csv, allow_rule, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    scope = excluded.scope,
                    world = excluded.world,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    principal_type = excluded.principal_type,
                    principal_value = excluded.principal_value,
                    actions_csv = excluded.actions_csv,
                    allow_rule = excluded.allow_rule
                """;
    }
}
