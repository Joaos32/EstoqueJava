package br.com.estoqueti.dto.movement;

import br.com.estoqueti.model.enums.MovementType;

import java.time.OffsetDateTime;

public record StockMovementCreateRequest(
        Long equipmentId,
        MovementType movementType,
        Integer quantity,
        Long sourceLocationId,
        Long destinationLocationId,
        String responsibleName,
        OffsetDateTime movementAt,
        String notes
) {
}