package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.repository.UserRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaUserRepository implements UserRepository {

    private final EntityManager entityManager;

    public JpaUserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<User> findByUsernameIgnoreCase(String username) {
        List<User> result = entityManager.createQuery(
                        "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)",
                        User.class
                )
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }

    @Override
    public List<User> findAllOrderedByName() {
        return entityManager.createQuery(
                        "SELECT u FROM User u ORDER BY LOWER(u.fullName), LOWER(u.username)",
                        User.class
                )
                .getResultList();
    }

    @Override
    public boolean existsByUsernameIgnoreCase(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE LOWER(u.username) = LOWER(:username)",
                        Long.class
                )
                .setParameter("username", username)
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }

        return entityManager.merge(user);
    }
}
