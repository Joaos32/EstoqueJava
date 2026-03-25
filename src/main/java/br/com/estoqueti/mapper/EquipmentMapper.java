package br.com.estoqueti.mapper;

import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.EquipmentCategory;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.entity.Supplier;

public final class EquipmentMapper {

    private EquipmentMapper() {
    }

    public static EquipmentListItemDto toListItemDto(Equipment equipment) {
        return new EquipmentListItemDto(
                equipment.getId(),
                equipment.getInternalCode(),
                equipment.getName(),
                equipment.getCategory().getName(),
                equipment.getBrand(),
                equipment.getModel(),
                equipment.getSerialNumber(),
                equipment.getPatrimonyNumber(),
                equipment.getQuantity(),
                equipment.getMinimumStock(),
                equipment.getStatus(),
                equipment.getLocation().getName(),
                equipment.getResponsibleName(),
                equipment.getSupplier() == null ? null : equipment.getSupplier().getCorporateName(),
                equipment.getEntryDate()
        );
    }

    public static LookupOptionDto toLookupOptionDto(EquipmentCategory category) {
        return new LookupOptionDto(category.getId(), category.getName());
    }

    public static LookupOptionDto toLookupOptionDto(Location location) {
        return new LookupOptionDto(location.getId(), location.getName());
    }

    public static LookupOptionDto toLookupOptionDto(Supplier supplier) {
        return new LookupOptionDto(supplier.getId(), supplier.getCorporateName());
    }
}