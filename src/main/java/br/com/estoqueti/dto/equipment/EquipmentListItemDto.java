package br.com.estoqueti.dto.equipment;

import br.com.estoqueti.model.enums.EquipmentStatus;

import java.time.LocalDate;

public record EquipmentListItemDto(
        Long id,
        String internalCode,
        String name,
        String categoryName,
        String brand,
        String model,
        String serialNumber,
        String patrimonyNumber,
        int quantity,
        int minimumStock,
        EquipmentStatus status,
        String locationName,
        String responsibleName,
        String supplierName,
        LocalDate entryDate
) {
}