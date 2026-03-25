package br.com.estoqueti.model.entity;

import br.com.estoqueti.model.enums.MovementType;
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
@Table(name = "stock_movement", schema = "estoque_ti")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private Location sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    @Column(name = "responsible_name", nullable = false, length = 120)
    private String responsibleName;

    @Column(name = "movement_at", nullable = false)
    private OffsetDateTime movementAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private User performedByUser;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StockMovement() {
    }

    public static StockMovement of(
            Equipment equipment,
            MovementType movementType,
            int quantity,
            Location sourceLocation,
            Location destinationLocation,
            String responsibleName,
            OffsetDateTime movementAt,
            String notes,
            User performedByUser
    ) {
        StockMovement movement = new StockMovement();
        movement.equipment = equipment;
        movement.movementType = movementType;
        movement.quantity = quantity;
        movement.sourceLocation = sourceLocation;
        movement.destinationLocation = destinationLocation;
        movement.responsibleName = responsibleName;
        movement.movementAt = movementAt;
        movement.notes = notes;
        movement.performedByUser = performedByUser;
        return movement;
    }

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public OffsetDateTime getMovementAt() {
        return movementAt;
    }

    public String getNotes() {
        return notes;
    }

    public User getPerformedByUser() {
        return performedByUser;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}