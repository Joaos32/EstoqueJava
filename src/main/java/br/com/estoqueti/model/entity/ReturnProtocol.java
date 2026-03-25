package br.com.estoqueti.model.entity;

import br.com.estoqueti.model.enums.ReturnProtocolReason;
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
@Table(name = "return_protocol", schema = "estoque_ti")
public class ReturnProtocol {

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

    @Column(name = "employee_name", nullable = false, length = 120)
    private String employeeName;

    @Column(name = "employee_cpf", nullable = false, length = 14)
    private String employeeCpf;

    @Column(name = "company_receiver_name", nullable = false, length = 120)
    private String companyReceiverName;

    @Column(name = "company_receiver_role", nullable = false, length = 120)
    private String companyReceiverRole;

    @Column(name = "company_receiver_cpf", nullable = false, length = 14)
    private String companyReceiverCpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_reason", nullable = false, length = 40)
    private ReturnProtocolReason returnReason;

    @Column(name = "other_reason", columnDefinition = "TEXT")
    private String otherReason;

    @Column(name = "item_quantity", nullable = false)
    private int itemQuantity;

    @Column(name = "item_description", nullable = false, length = 250)
    private String itemDescription;

    @Column(name = "item_identifier", length = 180)
    private String itemIdentifier;

    @Column(name = "item_observations", columnDefinition = "TEXT")
    private String itemObservations;

    @Column(name = "returned_at", nullable = false)
    private OffsetDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_user_id", nullable = false)
    private User generatedByUser;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ReturnProtocol() {
    }

    public static ReturnProtocol of(
            String protocolNumber,
            StockMovement stockMovement,
            Equipment equipment,
            String employeeName,
            String employeeCpf,
            String companyReceiverName,
            String companyReceiverRole,
            String companyReceiverCpf,
            ReturnProtocolReason returnReason,
            String otherReason,
            int itemQuantity,
            String itemDescription,
            String itemIdentifier,
            String itemObservations,
            OffsetDateTime returnedAt,
            User generatedByUser
    ) {
        ReturnProtocol returnProtocol = new ReturnProtocol();
        returnProtocol.protocolNumber = protocolNumber;
        returnProtocol.stockMovement = stockMovement;
        returnProtocol.equipment = equipment;
        returnProtocol.employeeName = employeeName;
        returnProtocol.employeeCpf = employeeCpf;
        returnProtocol.companyReceiverName = companyReceiverName;
        returnProtocol.companyReceiverRole = companyReceiverRole;
        returnProtocol.companyReceiverCpf = companyReceiverCpf;
        returnProtocol.returnReason = returnReason;
        returnProtocol.otherReason = otherReason;
        returnProtocol.itemQuantity = itemQuantity;
        returnProtocol.itemDescription = itemDescription;
        returnProtocol.itemIdentifier = itemIdentifier;
        returnProtocol.itemObservations = itemObservations;
        returnProtocol.returnedAt = returnedAt;
        returnProtocol.generatedByUser = generatedByUser;
        return returnProtocol;
    }

    public Long getId() {
        return id;
    }
}
