package br.com.estoqueti.dto.equipment;

import br.com.estoqueti.dto.common.LookupOptionDto;

import java.util.List;

public record EquipmentReferenceDataDto(
        List<LookupOptionDto> categories,
        List<LookupOptionDto> locations,
        List<LookupOptionDto> suppliers
) {
}