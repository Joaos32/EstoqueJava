package br.com.estoqueti.dto.equipment;

import br.com.estoqueti.model.enums.EquipmentStatus;

import java.time.LocalDate;

public record EquipmentCreateRequest(
        String internalCode,
        String name,
        Long categoryId,
        String brand,
        String model,
        String serialNumber,
        String patrimonyNumber,
        Integer quantity,
        Integer minimumStock,
        EquipmentStatus status,
        Long locationId,
        String responsibleName,
        Long supplierId,
        LocalDate entryDate,
        String notes
) {
}