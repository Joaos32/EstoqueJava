package br.com.estoqueti.repository;

import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.model.entity.StockMovement;

import java.util.List;

public interface StockMovementRepository {

    List<StockMovement> search(StockMovementSearchFilter filter);

    StockMovement save(StockMovement stockMovement);
}