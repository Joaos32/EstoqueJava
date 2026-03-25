package br.com.estoqueti.dto.movement;

import br.com.estoqueti.model.enums.MovementType;

import java.time.OffsetDateTime;

public record StockMovementSearchFilter(
        Long equipmentId,
        MovementType movementType,
        OffsetDateTime movementFrom,
        OffsetDateTime movementTo
) {
}