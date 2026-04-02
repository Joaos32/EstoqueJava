package br.com.estoqueti.controller;

import br.com.estoqueti.dto.DownloadedFile;
import br.com.estoqueti.dto.report.ReportDocumentDto;
import br.com.estoqueti.dto.report.ReportRequestDto;
import br.com.estoqueti.model.enums.ReportFormat;
import br.com.estoqueti.service.ApiFileExportService;
import br.com.estoqueti.service.report.ReportService;
import br.com.estoqueti.util.ApiFileResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatorios")
public class ReportApiController {

    private final ReportService reportService;
    private final ApiFileExportService apiFileExportService;

    public ReportApiController(ReportService reportService, ApiFileExportService apiFileExportService) {
        this.reportService = reportService;
        this.apiFileExportService = apiFileExportService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Gera a estrutura de um relatorio em JSON")
    public ReportDocumentDto generateReport(@RequestBody ReportRequestDto request) {
        return reportService.generateReport(request);
    }

    @PostMapping("/export/{reportFormat}")
    @Operation(summary = "Exporta um relatorio em CSV ou PDF")
    @ApiResponse(responseCode = "200", description = "Arquivo exportado com sucesso",
            content = @Content(schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<ByteArrayResource> exportReport(
            @PathVariable ReportFormat reportFormat,
            @RequestBody ReportRequestDto request
    ) {
        DownloadedFile downloadedFile = apiFileExportService.exportReport(request, reportFormat);
        return ApiFileResponseFactory.createAttachmentResponse(downloadedFile);
    }
}
