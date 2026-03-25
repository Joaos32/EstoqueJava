package br.com.estoqueti.mapper;

import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.model.entity.Equipment;

public final class DashboardMapper {

    private DashboardMapper() {
    }

    public static DashboardLowStockItemDto toLowStockItemDto(Equipment equipment) {
        return new DashboardLowStockItemDto(
                equipment.getId(),
                equipment.getInternalCode(),
                equipment.getName(),
                equipment.getLocation().getName(),
                equipment.getQuantity(),
                equipment.getMinimumStock(),
                equipment.getMinimumStock() - equipment.getQuantity()
        );
    }
}