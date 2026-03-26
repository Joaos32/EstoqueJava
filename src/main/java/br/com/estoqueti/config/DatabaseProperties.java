package br.com.estoqueti.config;

public record DatabaseProperties(
        String driverClassName,
        String url,
        String username,
        String password,
        String schema,
        String poolName,
        int minimumIdle,
        int maximumPoolSize,
        long connectionTimeoutMs,
        long idleTimeoutMs,
        long maxLifetimeMs,
        long validationTimeoutMs,
        String dialect,
        boolean showSql,
        boolean formatSql,
        boolean highlightSql,
        String hbm2ddlAuto,
        String jdbcTimeZone,
        boolean generateStatistics,
        int statementBatchSize
) {

    public static DatabaseProperties load() {
        return new DatabaseProperties(
                ApplicationProperties.getRequired("database.driver-class-name"),
                ApplicationProperties.getRequired("database.url"),
                ApplicationProperties.getRequired("database.username"),
                ApplicationProperties.get("database.password", ""),
                resolveSchema(),
                ApplicationProperties.get("database.hikari.pool-name", "EstoqueTIHikariPool"),
                ApplicationProperties.getInt("database.hikari.minimum-idle", 2),
                ApplicationProperties.getInt("database.hikari.maximum-pool-size", 10),
                ApplicationProperties.getLong("database.hikari.connection-timeout-ms", 30000L),
                ApplicationProperties.getLong("database.hikari.idle-timeout-ms", 600000L),
                ApplicationProperties.getLong("database.hikari.max-lifetime-ms", 1800000L),
                ApplicationProperties.getLong("database.hikari.validation-timeout-ms", 5000L),
                ApplicationProperties.get("hibernate.dialect", ""),
                ApplicationProperties.getBoolean("hibernate.show-sql", false),
                ApplicationProperties.getBoolean("hibernate.format-sql", true),
                ApplicationProperties.getBoolean("hibernate.highlight-sql", false),
                ApplicationProperties.get("hibernate.hbm2ddl.auto", "none"),
                ApplicationProperties.get("hibernate.jdbc.time-zone", "America/Sao_Paulo"),
                ApplicationProperties.getBoolean("hibernate.generate-statistics", false),
                ApplicationProperties.getInt("hibernate.jdbc.batch-size", 25)
        );
    }

    public String displayUrl() {
        return url;
    }

    private static String resolveSchema() {
        String configuredSchema = ApplicationProperties.get("database.schema", "public");
        if (configuredSchema == null || configuredSchema.isBlank()) {
            return "public";
        }
        return SqlIdentifierValidator.requireSimpleIdentifier(configuredSchema, "database.schema");
    }
}