package br.com.estoqueti.service;

import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.support.IntegrationTestDatabaseSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardServiceIntegrationTest {

    private final DashboardService dashboardService = new DashboardService();

    @BeforeAll
    static void prepareBaseline() {
        IntegrationTestDatabaseSupport.ensureBaselineData();
    }

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldLoadDashboardSummary() {
        DashboardSummaryDto summary = dashboardService.loadDashboard();

        assertNotNull(summary);
        assertTrue(summary.totalRegisteredEquipments() >= 5);
        assertTrue(summary.totalActiveQuantity() >= 0);
        assertTrue(summary.totalActiveQuantity() >= summary.totalAvailableQuantity() + summary.totalInUseQuantity() + summary.totalInMaintenanceQuantity());
        assertNotNull(summary.lowStockItems());
        assertNotNull(summary.recentMovements());
    }

    @Test
    void shouldReturnOnlyItemsBelowMinimumStockInLowStockList() {
        DashboardSummaryDto summary = dashboardService.loadDashboard();
        List<DashboardLowStockItemDto> lowStockItems = summary.lowStockItems();

        for (DashboardLowStockItemDto item : lowStockItems) {
            assertTrue(item.minimumStock() > 0);
            assertTrue(item.quantity() < item.minimumStock());
            assertEquals(item.minimumStock() - item.quantity(), item.stockGap());
        }
    }

    @Test
    void shouldReturnRecentMovementsOrderedFromNewestToOldest() {
        DashboardSummaryDto summary = dashboardService.loadDashboard();
        List<StockMovementListItemDto> recentMovements = summary.recentMovements();

        assertFalse(recentMovements.isEmpty());
        assertTrue(recentMovements.size() <= 8);

        for (int index = 1; index < recentMovements.size(); index++) {
            assertTrue(!recentMovements.get(index).movementAt().isAfter(recentMovements.get(index - 1).movementAt()));
        }
    }
}