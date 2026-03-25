package br.com.estoqueti.repository.impl;

import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;

public class JpaStockMovementRepository implements StockMovementRepository {

    private final EntityManager entityManager;

    public JpaStockMovementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<StockMovement> search(StockMovementSearchFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT m
                FROM StockMovement m
                JOIN FETCH m.equipment e
                LEFT JOIN FETCH m.sourceLocation sl
                LEFT JOIN FETCH m.destinationLocation dl
                JOIN FETCH m.performedByUser u
                WHERE 1 = 1
                """);

        List<QueryParameter> parameters = new ArrayList<>();

        if (filter != null && filter.equipmentId() != null) {
            jpql.append(" AND e.id = :equipmentId");
            parameters.add(new QueryParameter("equipmentId", filter.equipmentId()));
        }
        if (filter != null && filter.movementType() != null) {
            jpql.append(" AND m.movementType = :movementType");
            parameters.add(new QueryParameter("movementType", filter.movementType()));
        }
        if (filter != null && filter.movementFrom() != null) {
            jpql.append(" AND m.movementAt >= :movementFrom");
            parameters.add(new QueryParameter("movementFrom", filter.movementFrom()));
        }
        if (filter != null && filter.movementTo() != null) {
            jpql.append(" AND m.movementAt <= :movementTo");
            parameters.add(new QueryParameter("movementTo", filter.movementTo()));
        }

        jpql.append(" ORDER BY m.movementAt DESC, m.id DESC");

        TypedQuery<StockMovement> query = entityManager.createQuery(jpql.toString(), StockMovement.class);
        parameters.forEach(parameter -> query.setParameter(parameter.name(), parameter.value()));
        query.setMaxResults(200);
        return query.getResultList();
    }

    @Override
    public StockMovement save(StockMovement stockMovement) {
        if (stockMovement.getId() == null) {
            entityManager.persist(stockMovement);
            return stockMovement;
        }

        return entityManager.merge(stockMovement);
    }

    private record QueryParameter(String name, Object value) {
    }
}