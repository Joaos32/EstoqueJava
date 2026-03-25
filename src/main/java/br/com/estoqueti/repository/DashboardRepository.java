package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.enums.EquipmentStatus;

import java.util.List;

public interface DashboardRepository {

    int sumActiveQuantity();

    long countActiveEquipmentRecords();

    int sumQuantityByStatus(EquipmentStatus status);

    long countLowStockItems();

    List<Equipment> findLowStockItems(int maxResults);

    List<StockMovement> findRecentMovements(int maxResults);
}