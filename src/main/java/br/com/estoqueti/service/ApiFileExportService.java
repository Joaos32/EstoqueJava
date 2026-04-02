package br.com.estoqueti.service;

import br.com.estoqueti.dto.DownloadedFile;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.delivery.DeliveryProtocolCreateRequest;
import br.com.estoqueti.dto.delivery.DeliveryProtocolResultDto;
import br.com.estoqueti.dto.report.ReportRequestDto;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolCreateRequest;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolResultDto;
import br.com.estoqueti.model.enums.ReportFormat;
import br.com.estoqueti.service.report.ReportService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiFileExportService {

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DeliveryProtocolService deliveryProtocolService;
    private final ReturnProtocolService returnProtocolService;
    private final ReportService reportService;

    public ApiFileExportService(
            DeliveryProtocolService deliveryProtocolService,
            ReturnProtocolService returnProtocolService,
            ReportService reportService
    ) {
        this.deliveryProtocolService = deliveryProtocolService;
        this.returnProtocolService = returnProtocolService;
        this.reportService = reportService;
    }

    public DownloadedFile exportDeliveryProtocol(DeliveryProtocolCreateRequest request, AuthenticatedUserDto authenticatedUser) {
        Path outputPath = createTemporaryFile("delivery-protocol-", ".docx");
        try {
            DeliveryProtocolResultDto result = deliveryProtocolService.registerDelivery(request, outputPath, authenticatedUser);
            String fileName = deliveryProtocolService.buildDefaultFileName(
                    result.equipmentInternalCode(),
                    result.recipientName(),
                    resolveLocalDate(request.deliveryAt())
            );
            return buildDownloadedFile(result.outputPath(), fileName, DOCX_CONTENT_TYPE, Map.of(
                    "protocolNumber", result.protocolNumber(),
                    "equipmentInternalCode", result.equipmentInternalCode(),
                    "recipientName", result.recipientName()
            ));
        } catch (RuntimeException exception) {
            deleteSilently(outputPath);
            throw exception;
        }
    }

    public DownloadedFile exportReturnProtocol(ReturnProtocolCreateRequest request, AuthenticatedUserDto authenticatedUser) {
        Path outputPath = createTemporaryFile("return-protocol-", ".docx");
        try {
            ReturnProtocolResultDto result = returnProtocolService.registerReturn(request, outputPath, authenticatedUser);
            String fileName = returnProtocolService.buildDefaultFileName(
                    result.equipmentInternalCode(),
                    result.employeeName(),
                    resolveLocalDate(request.returnedAt())
            );
            return buildDownloadedFile(result.outputPath(), fileName, DOCX_CONTENT_TYPE, Map.of(
                    "protocolNumber", result.protocolNumber(),
                    "equipmentInternalCode", result.equipmentInternalCode(),
                    "employeeName", result.employeeName()
            ));
        } catch (RuntimeException exception) {
            deleteSilently(outputPath);
            throw exception;
        }
    }

    public DownloadedFile exportReport(ReportRequestDto request, ReportFormat reportFormat) {
        Path outputPath = createTemporaryFile("report-", "." + reportFormat.getExtension());
        try {
            reportService.exportReport(request, reportFormat, outputPath);
            String fileName = reportService.buildDefaultFileName(request.reportType(), reportFormat);
            String contentType = reportFormat == ReportFormat.CSV ? CSV_CONTENT_TYPE : PDF_CONTENT_TYPE;
            return buildDownloadedFile(outputPath, fileName, contentType, Map.of(
                    "reportType", request.reportType().name(),
                    "reportFormat", reportFormat.name()
            ));
        } catch (RuntimeException exception) {
            deleteSilently(outputPath);
            throw exception;
        }
    }

    private DownloadedFile buildDownloadedFile(Path outputPath, String fileName, String contentType, Map<String, String> metadata) {
        try {
            byte[] content = Files.readAllBytes(outputPath);
            return new DownloadedFile(fileName, contentType, content, new LinkedHashMap<>(metadata));
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel ler o arquivo gerado para download.", exception);
        } finally {
            deleteSilently(outputPath);
        }
    }

    private Path createTemporaryFile(String prefix, String suffix) {
        try {
            Path directory = Path.of("target", "api-output");
            Files.createDirectories(directory);
            return Files.createTempFile(directory, prefix, suffix);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel preparar o diretorio temporario de exportacao.", exception);
        }
    }

    private LocalDate resolveLocalDate(OffsetDateTime value) {
        return value == null ? LocalDate.now() : value.toLocalDate();
    }

    private void deleteSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
