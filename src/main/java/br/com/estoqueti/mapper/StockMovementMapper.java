package br.com.estoqueti.mapper;

import br.com.estoqueti.dto.movement.MovementEquipmentOptionDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.StockMovement;

public final class StockMovementMapper {

    private StockMovementMapper() {
    }

    public static MovementEquipmentOptionDto toEquipmentOptionDto(Equipment equipment) {
        return new MovementEquipmentOptionDto(
                equipment.getId(),
                equipment.getInternalCode(),
                equipment.getName(),
                equipment.getQuantity(),
                equipment.getStatus(),
                equipment.getLocation().getId(),
                equipment.getLocation().getName(),
                equipment.getResponsibleName()
        );
    }

    public static StockMovementListItemDto toListItemDto(StockMovement stockMovement) {
        return new StockMovementListItemDto(
                stockMovement.getId(),
                stockMovement.getEquipment().getInternalCode(),
                stockMovement.getEquipment().getName(),
                stockMovement.getMovementType(),
                stockMovement.getQuantity(),
                stockMovement.getSourceLocation() == null ? null : stockMovement.getSourceLocation().getName(),
                stockMovement.getDestinationLocation() == null ? null : stockMovement.getDestinationLocation().getName(),
                stockMovement.getResponsibleName(),
                stockMovement.getMovementAt(),
                stockMovement.getPerformedByUser().getUsername()
        );
    }
}