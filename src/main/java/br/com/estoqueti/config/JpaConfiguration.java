package br.com.estoqueti.config;

import org.hibernate.cfg.AvailableSettings;

import java.util.HashMap;
import java.util.Map;

public final class JpaConfiguration {

    private JpaConfiguration() {
    }

    public static Map<String, Object> buildProperties() {
        DatabaseProperties properties = DatabaseProperties.load();
        Map<String, Object> configuration = new HashMap<>();

        configuration.put("jakarta.persistence.nonJtaDataSource", DataSourceFactory.getDataSource());
        configuration.put(AvailableSettings.DEFAULT_SCHEMA, properties.schema());
        configuration.put(AvailableSettings.HBM2DDL_AUTO, properties.hbm2ddlAuto());
        configuration.put(AvailableSettings.SHOW_SQL, properties.showSql());
        configuration.put(AvailableSettings.FORMAT_SQL, properties.formatSql());
        configuration.put(AvailableSettings.HIGHLIGHT_SQL, properties.highlightSql());
        configuration.put(AvailableSettings.GENERATE_STATISTICS, properties.generateStatistics());
        configuration.put(AvailableSettings.JDBC_TIME_ZONE, properties.jdbcTimeZone());
        configuration.put(AvailableSettings.STATEMENT_BATCH_SIZE, properties.statementBatchSize());
        configuration.put(AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, true);
        configuration.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, true);

        if (!properties.dialect().isBlank()) {
            configuration.put(AvailableSettings.DIALECT, properties.dialect());
        }

        return configuration;
    }
}
