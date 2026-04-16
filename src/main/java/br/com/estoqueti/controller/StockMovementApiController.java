package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.movement.MovementReferenceDataDto;
import br.com.estoqueti.dto.movement.StockMovementCreateRequest;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.service.ApiAuthenticatedUserService;
import br.com.estoqueti.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@Tag(name = "Movimentacoes")
public class StockMovementApiController {

    private final StockMovementService stockMovementService;
    private final ApiAuthenticatedUserService authenticatedUserService;

    public StockMovementApiController(StockMovementService stockMovementService, ApiAuthenticatedUserService authenticatedUserService) {
        this.stockMovementService = stockMovementService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/reference-data")
    @Operation(summary = "Lista dados de apoio para movimentacoes")
    public MovementReferenceDataDto listReferenceData() {
        return stockMovementService.listReferenceData();
    }

    @GetMapping
    @Operation(summary = "Lista movimentacoes com filtros opcionais")
    public List<StockMovementListItemDto> searchMovements(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) OffsetDateTime movementFrom,
            @RequestParam(required = false) OffsetDateTime movementTo
    ) {
        return stockMovementService.searchMovements(new StockMovementSearchFilter(
                equipmentId,
                movementType,
                movementFrom,
                movementTo
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma movimentacao de estoque")
    public StockMovementListItemDto registerMovement(
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestBody StockMovementCreateRequest request
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return stockMovementService.registerMovement(request, authenticatedUser);
    }
}
