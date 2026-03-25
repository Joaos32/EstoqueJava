package br.com.estoqueti.dto.movement;

import br.com.estoqueti.model.enums.MovementType;

import java.time.OffsetDateTime;

public record StockMovementListItemDto(
        Long id,
        String equipmentInternalCode,
        String equipmentName,
        MovementType movementType,
        int quantity,
        String sourceLocationName,
        String destinationLocationName,
        String responsibleName,
        OffsetDateTime movementAt,
        String performedByUsername
) {
}