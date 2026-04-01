package br.com.estoqueti.service;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.support.IntegrationTestDatabaseSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceDataIntegrationTest {

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldEnsureRequestedCategoriesAndLocationsExist() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");

        IntegrationTestDatabaseSupport.ensureBaselineData();

        long categoryCount = JpaExecutor.query(entityManager -> countByName(entityManager,
                "estoque_ti.equipment_category",
                "Escritorio",
                "Equipamento"));
        long locationCount = JpaExecutor.query(entityManager -> countByName(entityManager,
                "estoque_ti.location",
                "Almoxarifado TI",
                "Estoque Escritorio"));

        assertTrue(categoryCount >= 2L);
        assertTrue(locationCount >= 2L);
    }

    private long countByName(jakarta.persistence.EntityManager entityManager, String tableName, String firstName, String secondName) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM " + tableName + " WHERE LOWER(name) IN (LOWER(:firstName), LOWER(:secondName))")
                .setParameter("firstName", firstName)
                .setParameter("secondName", secondName)
                .getSingleResult();
        return count == null ? 0L : count.longValue();
    }
}
