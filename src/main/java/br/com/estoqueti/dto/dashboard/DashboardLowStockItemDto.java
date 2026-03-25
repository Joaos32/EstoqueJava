package br.com.estoqueti.dto.dashboard;

public record DashboardLowStockItemDto(
        Long id,
        String internalCode,
        String name,
        String locationName,
        int quantity,
        int minimumStock,
        int stockGap
) {
}