package br.com.estoqueti.repository.impl;

import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;

public class JpaAuditLogRepository implements AuditLogRepository {

    private final EntityManager entityManager;

    public JpaAuditLogRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            entityManager.persist(auditLog);
            return auditLog;
        }

        return entityManager.merge(auditLog);
    }
}
