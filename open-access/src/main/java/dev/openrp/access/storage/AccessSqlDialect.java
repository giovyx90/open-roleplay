package dev.openrp.access.storage;

interface AccessSqlDialect {
    String createProfilesSql();

    String createRulesSql();

    String createAuditSql();

    Iterable<String> createIndexSql();

    String saveProfileSql();

    String saveRuleSql();
}
