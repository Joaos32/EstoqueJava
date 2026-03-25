package br.com.estoqueti.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public final class DataSourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);
    private static volatile HikariDataSource dataSource;

    private DataSourceFactory() {
    }

    public static DataSource getDataSource() {
        HikariDataSource localDataSource = dataSource;
        if (localDataSource == null) {
            synchronized (DataSourceFactory.class) {
                localDataSource = dataSource;
                if (localDataSource == null) {
                    localDataSource = createDataSource();
                    dataSource = localDataSource;
                }
            }
        }
        return localDataSource;
    }

    public static synchronized void close() {
        if (dataSource != null) {
            LOGGER.info("Encerrando pool de conexoes HikariCP.");
            dataSource.close();
            dataSource = null;
        }
    }

    private static HikariDataSource createDataSource() {
        DatabaseProperties properties = DatabaseProperties.load();

        HikariConfig config = new HikariConfig();
        config.setPoolName(properties.poolName());
        config.setDriverClassName(properties.driverClassName());
        config.setJdbcUrl(properties.url());
        config.setUsername(properties.username());
        config.setPassword(properties.password());
        config.setMinimumIdle(properties.minimumIdle());
        config.setMaximumPoolSize(properties.maximumPoolSize());
        config.setConnectionTimeout(properties.connectionTimeoutMs());
        config.setIdleTimeout(properties.idleTimeoutMs());
        config.setMaxLifetime(properties.maxLifetimeMs());
        config.setValidationTimeout(properties.validationTimeoutMs());
        config.setConnectionTestQuery("SELECT 1");
        config.setInitializationFailTimeout(-1);
        config.setAutoCommit(false);
        config.addDataSourceProperty("ApplicationName", ApplicationProperties.get("app.name", "EstoqueTI Desktop"));
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        if (!properties.schema().isBlank()) {
            config.setSchema(properties.schema());
            config.setConnectionInitSql("SET search_path TO " + properties.schema() + ", public");
        }

        LOGGER.info("Inicializando DataSource para {}", properties.displayUrl());
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        DatabaseMigrationService.ensureProtocolSupport(hikariDataSource, properties.schema());
        return hikariDataSource;
    }
}
