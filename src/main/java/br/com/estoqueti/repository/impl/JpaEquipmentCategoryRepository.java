package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.EquipmentCategory;
import br.com.estoqueti.repository.EquipmentCategoryRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaEquipmentCategoryRepository implements EquipmentCategoryRepository {

    private final EntityManager entityManager;

    public JpaEquipmentCategoryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<EquipmentCategory> findAllActiveOrderByName() {
        return entityManager.createQuery(
                        "SELECT c FROM EquipmentCategory c WHERE c.active = true ORDER BY LOWER(c.name)",
                        EquipmentCategory.class
                )
                .getResultList();
    }

    @Override
    public Optional<EquipmentCategory> findActiveById(Long id) {
        List<EquipmentCategory> result = entityManager.createQuery(
                        "SELECT c FROM EquipmentCategory c WHERE c.id = :id AND c.active = true",
                        EquipmentCategory.class
                )
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }
}