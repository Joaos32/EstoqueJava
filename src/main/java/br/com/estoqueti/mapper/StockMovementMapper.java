package br.com.estoqueti.mapper;

import br.com.estoqueti.dto.movement.MovementEquipmentOptionDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.enums.MovementType;

public final class StockMovementMapper {

    private static final String DELIVERY_DESTINATION_PREFIX = "Destino informado: ";
    private static final String NOTES_SEPARATOR = " | ";

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
                resolveDestinationName(stockMovement),
                stockMovement.getResponsibleName(),
                stockMovement.getMovementAt(),
                stockMovement.getPerformedByUser().getUsername()
        );
    }

    private static String resolveDestinationName(StockMovement stockMovement) {
        if (stockMovement.getMovementType() == MovementType.ENTREGA_FUNCIONARIO) {
            String destinationFromNotes = extractDeliveryDestination(stockMovement.getNotes());
            if (destinationFromNotes != null) {
                return destinationFromNotes;
            }
        }
        return stockMovement.getDestinationLocation() == null ? null : stockMovement.getDestinationLocation().getName();
    }

    private static String extractDeliveryDestination(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }

        int startIndex = notes.indexOf(DELIVERY_DESTINATION_PREFIX);
        if (startIndex < 0) {
            return null;
        }

        int valueStartIndex = startIndex + DELIVERY_DESTINATION_PREFIX.length();
        int valueEndIndex = notes.indexOf(NOTES_SEPARATOR, valueStartIndex);
        String destination = valueEndIndex >= 0
                ? notes.substring(valueStartIndex, valueEndIndex)
                : notes.substring(valueStartIndex);
        return destination.isBlank() ? null : destination;
    }
}