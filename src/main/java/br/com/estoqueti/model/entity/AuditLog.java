package br.com.estoqueti.model.entity;

import br.com.estoqueti.model.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log", schema = "estoque_ti")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "entity_name", length = 80)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_or_station", length = 120)
    private String ipOrStation;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public static AuditLog of(User user, AuditAction action, String entityName, Long entityId, String description, String ipOrStation) {
        AuditLog auditLog = new AuditLog();
        auditLog.user = user;
        auditLog.action = action;
        auditLog.entityName = entityName;
        auditLog.entityId = entityId;
        auditLog.description = description;
        auditLog.ipOrStation = ipOrStation;
        return auditLog;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public String getIpOrStation() {
        return ipOrStation;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
