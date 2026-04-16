package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.equipment.EquipmentCreateRequest;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentReferenceDataDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.service.ApiAuthenticatedUserService;
import br.com.estoqueti.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipments")
@Tag(name = "Equipamentos")
public class EquipmentApiController {

    private final EquipmentService equipmentService;
    private final ApiAuthenticatedUserService authenticatedUserService;

    public EquipmentApiController(EquipmentService equipmentService, ApiAuthenticatedUserService authenticatedUserService) {
        this.equipmentService = equipmentService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/reference-data")
    @Operation(summary = "Lista dados de apoio para cadastro de equipamentos")
    public EquipmentReferenceDataDto listReferenceData() {
        return equipmentService.listReferenceData();
    }

    @GetMapping
    @Operation(summary = "Lista equipamentos ativos com filtros opcionais")
    public List<EquipmentListItemDto> searchEquipments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String internalCode,
            @RequestParam(required = false) String patrimonyNumber,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String responsibleName
    ) {
        return equipmentService.searchEquipment(new EquipmentSearchFilter(
                name,
                internalCode,
                patrimonyNumber,
                serialNumber,
                categoryId,
                status,
                locationId,
                responsibleName
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo equipamento")
    public EquipmentListItemDto createEquipment(
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestBody EquipmentCreateRequest request
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return equipmentService.createEquipment(request, authenticatedUser);
    }

    @DeleteMapping("/{equipmentId}")
    @Operation(summary = "Inativa um equipamento com quantidade zerada")
    public EquipmentListItemDto deactivateEquipment(
            @PathVariable Long equipmentId,
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return equipmentService.deactivateEquipment(equipmentId, authenticatedUser);
    }
}
