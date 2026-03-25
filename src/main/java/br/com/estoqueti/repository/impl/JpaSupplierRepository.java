package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.Supplier;
import br.com.estoqueti.repository.SupplierRepository;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaSupplierRepository implements SupplierRepository {

    private final EntityManager entityManager;

    public JpaSupplierRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Supplier> findAllActiveOrderByCorporateName() {
        return entityManager.createQuery(
                        "SELECT s FROM Supplier s WHERE s.active = true ORDER BY LOWER(s.corporateName)",
                        Supplier.class
                )
                .getResultList();
    }

    @Override
    public Optional<Supplier> findActiveById(Long id) {
        List<Supplier> result = entityManager.createQuery(
                        "SELECT s FROM Supplier s WHERE s.id = :id AND s.active = true",
                        Supplier.class
                )
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }
}