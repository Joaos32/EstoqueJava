package br.com.estoqueti.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "delivery_protocol", schema = "estoque_ti")
public class DeliveryProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protocol_number", nullable = false, length = 40)
    private String protocolNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_movement_id", nullable = false, unique = true)
    private StockMovement stockMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "recipient_cpf", nullable = false, length = 14)
    private String recipientCpf;

    @Column(name = "recipient_role", nullable = false, length = 120)
    private String recipientRole;

    @Column(name = "item_quantity", nullable = false)
    private int itemQuantity;

    @Column(name = "item_description", nullable = false, length = 250)
    private String itemDescription;

    @Column(name = "item_identifier", length = 180)
    private String itemIdentifier;

    @Column(name = "item_observations", columnDefinition = "TEXT")
    private String itemObservations;

    @Column(name = "delivery_at", nullable = false)
    private OffsetDateTime deliveryAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_user_id", nullable = false)
    private User generatedByUser;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected DeliveryProtocol() {
    }

    public static DeliveryProtocol of(
            String protocolNumber,
            StockMovement stockMovement,
            Equipment equipment,
            String recipientName,
            String recipientCpf,
            String recipientRole,
            int itemQuantity,
            String itemDescription,
            String itemIdentifier,
            String itemObservations,
            OffsetDateTime deliveryAt,
            User generatedByUser
    ) {
        DeliveryProtocol deliveryProtocol = new DeliveryProtocol();
        deliveryProtocol.protocolNumber = protocolNumber;
        deliveryProtocol.stockMovement = stockMovement;
        deliveryProtocol.equipment = equipment;
        deliveryProtocol.recipientName = recipientName;
        deliveryProtocol.recipientCpf = recipientCpf;
        deliveryProtocol.recipientRole = recipientRole;
        deliveryProtocol.itemQuantity = itemQuantity;
        deliveryProtocol.itemDescription = itemDescription;
        deliveryProtocol.itemIdentifier = itemIdentifier;
        deliveryProtocol.itemObservations = itemObservations;
        deliveryProtocol.deliveryAt = deliveryAt;
        deliveryProtocol.generatedByUser = generatedByUser;
        return deliveryProtocol;
    }

    public Long getId() {
        return id;
    }

    public String getProtocolNumber() {
        return protocolNumber;
    }

    public StockMovement getStockMovement() {
        return stockMovement;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientCpf() {
        return recipientCpf;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public int getItemQuantity() {
        return itemQuantity;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public String getItemIdentifier() {
        return itemIdentifier;
    }

    public String getItemObservations() {
        return itemObservations;
    }

    public OffsetDateTime getDeliveryAt() {
        return deliveryAt;
    }

    public User getGeneratedByUser() {
        return generatedByUser;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}