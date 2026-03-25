package br.com.estoqueti.repository.impl;

import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.repository.EquipmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JpaEquipmentRepository implements EquipmentRepository {

    private final EntityManager entityManager;

    public JpaEquipmentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Equipment> searchActive(EquipmentSearchFilter filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT e
                FROM Equipment e
                JOIN FETCH e.category c
                JOIN FETCH e.location l
                LEFT JOIN FETCH e.supplier s
                WHERE e.active = true
                """);

        List<QueryParameter> parameters = new ArrayList<>();

        if (hasText(filter.name())) {
            jpql.append(" AND LOWER(e.name) LIKE :name");
            parameters.add(new QueryParameter("name", "%" + filter.name().trim().toLowerCase() + "%"));
        }
        if (hasText(filter.internalCode())) {
            jpql.append(" AND LOWER(e.internalCode) LIKE :internalCode");
            parameters.add(new QueryParameter("internalCode", "%" + filter.internalCode().trim().toLowerCase() + "%"));
        }
        if (hasText(filter.patrimonyNumber())) {
            jpql.append(" AND LOWER(e.patrimonyNumber) LIKE :patrimonyNumber");
            parameters.add(new QueryParameter("patrimonyNumber", "%" + filter.patrimonyNumber().trim().toLowerCase() + "%"));
        }
        if (hasText(filter.serialNumber())) {
            jpql.append(" AND LOWER(e.serialNumber) LIKE :serialNumber");
            parameters.add(new QueryParameter("serialNumber", "%" + filter.serialNumber().trim().toLowerCase() + "%"));
        }
        if (filter.categoryId() != null) {
            jpql.append(" AND c.id = :categoryId");
            parameters.add(new QueryParameter("categoryId", filter.categoryId()));
        }
        if (filter.status() != null) {
            jpql.append(" AND e.status = :status");
            parameters.add(new QueryParameter("status", filter.status()));
        }
        if (filter.locationId() != null) {
            jpql.append(" AND l.id = :locationId");
            parameters.add(new QueryParameter("locationId", filter.locationId()));
        }
        if (hasText(filter.responsibleName())) {
            jpql.append(" AND LOWER(e.responsibleName) LIKE :responsibleName");
            parameters.add(new QueryParameter("responsibleName", "%" + filter.responsibleName().trim().toLowerCase() + "%"));
        }

        jpql.append(" ORDER BY LOWER(e.name), LOWER(e.internalCode)");

        TypedQuery<Equipment> query = entityManager.createQuery(jpql.toString(), Equipment.class);
        parameters.forEach(parameter -> query.setParameter(parameter.name(), parameter.value()));
        return query.getResultList();
    }

    @Override
    public List<Equipment> findAllActiveOrderedByName() {
        return entityManager.createQuery("""
                        SELECT e
                        FROM Equipment e
                        JOIN FETCH e.category c
                        JOIN FETCH e.location l
                        LEFT JOIN FETCH e.supplier s
                        WHERE e.active = true
                        ORDER BY LOWER(e.name), LOWER(e.internalCode)
                        """, Equipment.class)
                .getResultList();
    }

    @Override
    public Optional<Equipment> findActiveById(Long id) {
        List<Equipment> result = entityManager.createQuery("""
                        SELECT e
                        FROM Equipment e
                        JOIN FETCH e.location l
                        JOIN FETCH e.category c
                        LEFT JOIN FETCH e.supplier s
                        WHERE e.id = :id
                          AND e.active = true
                        """, Equipment.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }

    @Override
    public Optional<Equipment> findActiveByIdForUpdate(Long id) {
        List<Equipment> result = entityManager.createQuery("""
                        SELECT e
                        FROM Equipment e
                        JOIN FETCH e.location l
                        JOIN FETCH e.category c
                        LEFT JOIN FETCH e.supplier s
                        WHERE e.id = :id
                          AND e.active = true
                        """, Equipment.class)
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultList();

        return result.stream().findFirst();
    }

    @Override
    public boolean existsByInternalCodeIgnoreCase(String internalCode) {
        return countByExpression("e.internalCode", internalCode);
    }

    @Override
    public boolean existsBySerialNumberIgnoreCase(String serialNumber) {
        return countByExpression("e.serialNumber", serialNumber);
    }

    @Override
    public boolean existsByPatrimonyNumberIgnoreCase(String patrimonyNumber) {
        return countByExpression("e.patrimonyNumber", patrimonyNumber);
    }

    @Override
    public Equipment save(Equipment equipment) {
        if (equipment.getId() == null) {
            entityManager.persist(equipment);
            return equipment;
        }

        return entityManager.merge(equipment);
    }

    private boolean countByExpression(String expression, String value) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(e) FROM Equipment e WHERE LOWER(" + expression + ") = LOWER(:value)",
                        Long.class
                )
                .setParameter("value", value)
                .getSingleResult();

        return count != null && count > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QueryParameter(String name, Object value) {
    }
}