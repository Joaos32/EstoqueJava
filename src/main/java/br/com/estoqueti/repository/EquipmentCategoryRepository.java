package br.com.estoqueti.repository;

import br.com.estoqueti.model.entity.EquipmentCategory;

import java.util.List;
import java.util.Optional;

public interface EquipmentCategoryRepository {

    List<EquipmentCategory> findAllActiveOrderByName();

    Optional<EquipmentCategory> findActiveById(Long id);
}