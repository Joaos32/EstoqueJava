package br.com.estoqueti.dto.equipment;

import br.com.estoqueti.model.enums.EquipmentStatus;

public record EquipmentSearchFilter(
        String name,
        String internalCode,
        String patrimonyNumber,
        String serialNumber,
        Long categoryId,
        EquipmentStatus status,
        Long locationId,
        String responsibleName
) {
}