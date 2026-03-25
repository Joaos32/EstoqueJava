package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.mapper.DashboardMapper;
import br.com.estoqueti.mapper.StockMovementMapper;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.repository.DashboardRepository;
import br.com.estoqueti.repository.impl.JpaDashboardRepository;

import java.util.List;

public class DashboardService {

    private static final int LOW_STOCK_LIMIT = 6;
    private static final int RECENT_MOVEMENTS_LIMIT = 8;

    public DashboardSummaryDto loadDashboard() {
        return JpaExecutor.query(entityManager -> {
            DashboardRepository dashboardRepository = new JpaDashboardRepository(entityManager);

            List<DashboardLowStockItemDto> lowStockItems = dashboardRepository.findLowStockItems(LOW_STOCK_LIMIT)
                    .stream()
                    .map(DashboardMapper::toLowStockItemDto)
                    .toList();

            List<StockMovementListItemDto> recentMovements = dashboardRepository.findRecentMovements(RECENT_MOVEMENTS_LIMIT)
                    .stream()
                    .map(StockMovementMapper::toListItemDto)
                    .toList();

            return new DashboardSummaryDto(
                    dashboardRepository.sumActiveQuantity(),
                    dashboardRepository.countActiveEquipmentRecords(),
                    dashboardRepository.sumQuantityByStatus(EquipmentStatus.DISPONIVEL),
                    dashboardRepository.sumQuantityByStatus(EquipmentStatus.EM_USO),
                    dashboardRepository.sumQuantityByStatus(EquipmentStatus.EM_MANUTENCAO),
                    dashboardRepository.countLowStockItems(),
                    lowStockItems,
                    recentMovements
            );
        });
    }
}