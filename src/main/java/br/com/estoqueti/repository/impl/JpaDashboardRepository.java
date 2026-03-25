package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.repository.DashboardRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaDashboardRepository implements DashboardRepository {

    private final EntityManager entityManager;

    public JpaDashboardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int sumActiveQuantity() {
        Number result = entityManager.createQuery(
                        "SELECT COALESCE(SUM(e.quantity), 0) FROM Equipment e WHERE e.active = true",
                        Number.class
                )
                .getSingleResult();

        return result == null ? 0 : result.intValue();
    }

    @Override
    public long countActiveEquipmentRecords() {
        Long result = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Equipment e WHERE e.active = true",
                        Long.class
                )
                .getSingleResult();

        return result == null ? 0L : result;
    }

    @Override
    public int sumQuantityByStatus(EquipmentStatus status) {
        Number result = entityManager.createQuery(
                        "SELECT COALESCE(SUM(e.quantity), 0) FROM Equipment e WHERE e.active = true AND e.status = :status",
                        Number.class
                )
                .setParameter("status", status)
                .getSingleResult();

        return result == null ? 0 : result.intValue();
    }

    @Override
    public long countLowStockItems() {
        Long result = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Equipment e WHERE e.active = true AND e.minimumStock > 0 AND e.quantity < e.minimumStock",
                        Long.class
                )
                .getSingleResult();

        return result == null ? 0L : result;
    }

    @Override
    public List<Equipment> findLowStockItems(int maxResults) {
        return entityManager.createQuery("""
                        SELECT e
                        FROM Equipment e
                        JOIN FETCH e.location l
                        WHERE e.active = true
                          AND e.minimumStock > 0
                          AND e.quantity < e.minimumStock
                        ORDER BY (e.minimumStock - e.quantity) DESC, LOWER(e.name)
                        """, Equipment.class)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public List<StockMovement> findRecentMovements(int maxResults) {
        return entityManager.createQuery("""
                        SELECT m
                        FROM StockMovement m
                        JOIN FETCH m.equipment e
                        LEFT JOIN FETCH m.sourceLocation sl
                        LEFT JOIN FETCH m.destinationLocation dl
                        JOIN FETCH m.performedByUser u
                        ORDER BY m.movementAt DESC, m.id DESC
                        """, StockMovement.class)
                .setMaxResults(maxResults)
                .getResultList();
    }
}