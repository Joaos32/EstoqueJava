package br.com.estoqueti.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityManagerFactoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerFactoryProvider.class);
    private static final String PERSISTENCE_UNIT_NAME = "estoqueti-persistence-unit";
    private static volatile EntityManagerFactory entityManagerFactory;

    private EntityManagerFactoryProvider() {
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory localEntityManagerFactory = entityManagerFactory;
        if (localEntityManagerFactory == null) {
            synchronized (EntityManagerFactoryProvider.class) {
                localEntityManagerFactory = entityManagerFactory;
                if (localEntityManagerFactory == null) {
                    LOGGER.info("Inicializando EntityManagerFactory da aplicacao.");
                    localEntityManagerFactory = Persistence.createEntityManagerFactory(
                            PERSISTENCE_UNIT_NAME,
                            JpaConfiguration.buildProperties()
                    );
                    entityManagerFactory = localEntityManagerFactory;
                }
            }
        }
        return localEntityManagerFactory;
    }

    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            LOGGER.info("Encerrando EntityManagerFactory da aplicacao.");
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }
}
