package br.com.estoqueti.service;

import br.com.estoqueti.config.DatabaseConnectionStatus;
import br.com.estoqueti.config.DatabaseProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public final class DatabaseConnectivityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnectivityService.class);
    private static final String HEALTH_QUERY = "SELECT current_database(), current_schema(), current_user, version()";

    private DatabaseConnectivityService() {
    }

    public static DatabaseConnectionStatus checkConnection() {
        DatabaseProperties properties = DatabaseProperties.load();
        LocalDateTime checkedAt = LocalDateTime.now();

        try {
            DataSource dataSource = DataSourceFactory.getDataSource();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(HEALTH_QUERY);
                 ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return new DatabaseConnectionStatus(
                            false,
                            properties.displayUrl(),
                            "Falha ao validar conexao",
                            "A consulta de saude do PostgreSQL nao retornou resultado.",
                            checkedAt
                    );
                }

                String databaseName = resultSet.getString(1);
                String schemaName = resultSet.getString(2);
                String username = resultSet.getString(3);
                String version = resultSet.getString(4);

                validateJpaBootstrap();

                String details = String.format(
                        "Banco: %s | Schema: %s | Usuario: %s | Hibernate/JPA inicializado | %s",
                        databaseName,
                        schemaName,
                        username,
                        compactVersion(version)
                );

                return new DatabaseConnectionStatus(
                        true,
                        properties.displayUrl(),
                        "PostgreSQL conectado",
                        details,
                        checkedAt
                );
            }
        } catch (Exception exception) {
            LOGGER.warn("Falha ao verificar conectividade com o PostgreSQL.", exception);
            return new DatabaseConnectionStatus(
                    false,
                    properties.displayUrl(),
                    "Falha de conexao",
                    sanitizeMessage(exception.getMessage()),
                    checkedAt
            );
        }
    }

    private static void validateJpaBootstrap() {
        try (EntityManager entityManager = EntityManagerFactoryProvider.createEntityManager()) {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
        }
    }

    private static String compactVersion(String version) {
        if (version == null || version.isBlank()) {
            return "Versao do servidor nao identificada";
        }

        int separatorIndex = version.indexOf(',');
        if (separatorIndex > 0) {
            return version.substring(0, separatorIndex);
        }

        return version;
    }

    private static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Nao foi possivel estabelecer a conexao com o banco de dados.";
        }

        return message.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
