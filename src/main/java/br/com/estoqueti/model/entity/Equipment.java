package br.com.estoqueti.model.entity;

import br.com.estoqueti.model.enums.EquipmentStatus;
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
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment", schema = "estoque_ti")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_code", nullable = false, length = 50)
    private String internalCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EquipmentCategory category;

    @Column(name = "brand", length = 80)
    private String brand;

    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "patrimony_number", length = 120)
    private String patrimonyNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "minimum_stock", nullable = false)
    private int minimumStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EquipmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "responsible_name", length = 120)
    private String responsibleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Equipment() {
    }

    public Equipment(
            String internalCode,
            String name,
            EquipmentCategory category,
            String brand,
            String model,
            String serialNumber,
            String patrimonyNumber,
            int quantity,
            int minimumStock,
            EquipmentStatus status,
            Location location,
            String responsibleName,
            Supplier supplier,
            LocalDate entryDate,
            String notes,
            boolean active
    ) {
        this.internalCode = internalCode;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.patrimonyNumber = patrimonyNumber;
        this.quantity = quantity;
        this.minimumStock = minimumStock;
        this.status = status;
        this.location = location;
        this.responsibleName = responsibleName;
        this.supplier = supplier;
        this.entryDate = entryDate;
        this.notes = notes;
        this.active = active;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public void removeQuantity(int amount) {
        this.quantity -= amount;
    }

    public void changeLocation(Location location) {
        this.location = location;
    }

    public void changeStatus(EquipmentStatus status) {
        this.status = status;
    }

    public void changeResponsibleName(String responsibleName) {
        this.responsibleName = responsibleName;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public String getName() {
        return name;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getPatrimonyNumber() {
        return patrimonyNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public Location getLocation() {
        return location;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
