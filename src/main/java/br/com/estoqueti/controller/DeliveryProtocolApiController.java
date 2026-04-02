package br.com.estoqueti.controller;

import br.com.estoqueti.dto.DownloadedFile;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.delivery.DeliveryProtocolCreateRequest;
import br.com.estoqueti.service.ApiAuthenticatedUserService;
import br.com.estoqueti.service.ApiFileExportService;
import br.com.estoqueti.util.ApiFileResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-protocols")
@Tag(name = "Protocolos de Entrega")
public class DeliveryProtocolApiController {

    private final ApiFileExportService apiFileExportService;
    private final ApiAuthenticatedUserService authenticatedUserService;

    public DeliveryProtocolApiController(ApiFileExportService apiFileExportService, ApiAuthenticatedUserService authenticatedUserService) {
        this.apiFileExportService = apiFileExportService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/export")
    @Operation(summary = "Gera e baixa um protocolo de entrega em DOCX")
    @ApiResponse(responseCode = "200", description = "Arquivo DOCX gerado com sucesso",
            content = @Content(schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<ByteArrayResource> exportProtocol(
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestBody DeliveryProtocolCreateRequest request
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        DownloadedFile downloadedFile = apiFileExportService.exportDeliveryProtocol(request, authenticatedUser);
        return ApiFileResponseFactory.createAttachmentResponse(downloadedFile);
    }
}
