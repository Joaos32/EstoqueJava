package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.repository.LocationRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaLocationRepository implements LocationRepository {

    private final EntityManager entityManager;

    public JpaLocationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Location> findAllActiveOrderByName() {
        return entityManager.createQuery(
                        "SELECT l FROM Location l WHERE l.active = true ORDER BY LOWER(l.name)",
                        Location.class
                )
                .getResultList();
    }

    @Override
    public Optional<Location> findActiveById(Long id) {
        List<Location> result = entityManager.createQuery(
                        "SELECT l FROM Location l WHERE l.id = :id AND l.active = true",
                        Location.class
                )
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }
}