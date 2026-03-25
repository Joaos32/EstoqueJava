package br.com.estoqueti.service;

import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.dto.report.ReportDocumentDto;
import br.com.estoqueti.dto.report.ReportRequestDto;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.ReportFormat;
import br.com.estoqueti.model.enums.ReportType;
import br.com.estoqueti.service.report.ReportService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceIntegrationTest {

    private final ReportService reportService = new ReportService();

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldGenerateRegisteredEquipmentReport() {
        ReportDocumentDto document = reportService.generateReport(
                new ReportRequestDto(ReportType.EQUIPAMENTOS_CADASTRADOS, null, null)
        );

        assertTrue(document.rows().size() >= 5);
        assertTrue(document.headers().contains("Codigo"));
        assertTrue(document.title().contains("Equipamentos"));
    }

    @Test
    void shouldRequirePeriodForMovementsReport() {
        assertThrows(ValidationException.class, () -> reportService.generateReport(
                new ReportRequestDto(ReportType.MOVIMENTACOES_POR_PERIODO, null, null)
        ));
    }

    @Test
    void shouldExportLowStockReportAsCsv(@TempDir Path tempDir) throws Exception {
        Path csvPath = tempDir.resolve("estoque-baixo.csv");
        ReportRequestDto request = new ReportRequestDto(ReportType.ESTOQUE_BAIXO, null, null);

        reportService.exportReport(request, ReportFormat.CSV, csvPath);

        String content = Files.readString(csvPath);
        assertTrue(Files.exists(csvPath));
        assertTrue(content.contains("Codigo"));
        assertTrue(content.contains("SSD-0012"));
    }

    @Test
    void shouldExportMovementsReportAsPdf(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("movimentacoes.pdf");
        ReportRequestDto request = new ReportRequestDto(
                ReportType.MOVIMENTACOES_POR_PERIODO,
                LocalDate.of(2025, 11, 1),
                LocalDate.of(2026, 12, 31)
        );

        reportService.exportReport(request, ReportFormat.PDF, pdfPath);

        assertTrue(Files.exists(pdfPath));
        assertTrue(Files.size(pdfPath) > 0L);
    }

    @Test
    void shouldGenerateMaintenanceReportWithOnlyMaintenanceItems() {
        ReportDocumentDto document = reportService.generateReport(
                new ReportRequestDto(ReportType.EQUIPAMENTOS_EM_MANUTENCAO, null, null)
        );

        assertFalse(document.rows().isEmpty());
        assertTrue(document.title().contains("manutencao"));
    }
}