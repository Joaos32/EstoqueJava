package br.com.estoqueti.dto.movement;

import br.com.estoqueti.model.enums.EquipmentStatus;

public record MovementEquipmentOptionDto(
        Long id,
        String internalCode,
        String name,
        int quantity,
        EquipmentStatus status,
        Long locationId,
        String locationName,
        String responsibleName
) {

    @Override
    public String toString() {
        return "[" + internalCode + "] " + name;
    }
}