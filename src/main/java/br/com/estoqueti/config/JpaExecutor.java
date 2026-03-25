package br.com.estoqueti.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

public final class JpaExecutor {

    private JpaExecutor() {
    }

    public static <T> T query(Function<EntityManager, T> callback) {
        EntityManager entityManager = EntityManagerFactoryProvider.createEntityManager();
        try {
            return callback.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    public static <T> T transaction(Function<EntityManager, T> callback) {
        EntityManager entityManager = EntityManagerFactoryProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();
            T result = callback.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
