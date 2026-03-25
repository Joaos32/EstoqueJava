package br.com.estoqueti.dto.dashboard;

import br.com.estoqueti.dto.movement.StockMovementListItemDto;

import java.util.List;

public record DashboardSummaryDto(
        int totalActiveQuantity,
        long totalRegisteredEquipments,
        int totalAvailableQuantity,
        int totalInUseQuantity,
        int totalInMaintenanceQuantity,
        long lowStockItemsCount,
        List<DashboardLowStockItemDto> lowStockItems,
        List<StockMovementListItemDto> recentMovements
) {
}