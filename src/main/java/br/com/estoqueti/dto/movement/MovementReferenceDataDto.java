package br.com.estoqueti.dto.movement;

import br.com.estoqueti.dto.common.LookupOptionDto;

import java.util.List;

public record MovementReferenceDataDto(
        List<MovementEquipmentOptionDto> equipments,
        List<LookupOptionDto> locations
) {
}