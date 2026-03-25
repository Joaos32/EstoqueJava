package br.com.estoqueti.repository;

import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.model.entity.Equipment;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository {

    List<Equipment> searchActive(EquipmentSearchFilter filter);

    List<Equipment> findAllActiveOrderedByName();

    Optional<Equipment> findActiveById(Long id);

    Optional<Equipment> findActiveByIdForUpdate(Long id);

    boolean existsByInternalCodeIgnoreCase(String internalCode);

    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    boolean existsByPatrimonyNumberIgnoreCase(String patrimonyNumber);

    Equipment save(Equipment equipment);
}