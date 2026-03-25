package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.AuditLog;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);
}
