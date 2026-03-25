package br.com.estoqueti.config;

import br.com.estoqueti.service.DatabaseConnectivityService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectivitySmokeTest {

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldConnectToConfiguredPostgresqlInstance() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");

        DatabaseConnectionStatus status = DatabaseConnectivityService.checkConnection();

        assertTrue(status.successful(), status.details());
    }

    @Test
    void shouldBootstrapJpaAndExecuteNativeQuery() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");

        try (EntityManager entityManager = EntityManagerFactoryProvider.createEntityManager()) {
            Object currentDatabase = entityManager.createNativeQuery("SELECT current_database()").getSingleResult();
            assertEquals("estoqueti", currentDatabase);
        }
    }
}
