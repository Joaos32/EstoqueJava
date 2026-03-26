package br.com.estoqueti.service.report;

import br.com.estoqueti.dto.report.ReportDocumentDto;
import br.com.estoqueti.model.enums.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvReportExporterTest {

    private final CsvReportExporter csvReportExporter = new CsvReportExporter();

    @TempDir
    Path tempDir;

    @Test
    void shouldPrefixDangerousSpreadsheetCells() throws Exception {
        Path outputPath = tempDir.resolve("relatorio.csv");
        ReportDocumentDto document = new ReportDocumentDto(
                ReportType.EQUIPAMENTOS_CADASTRADOS,
                "Relatorio",
                "Teste",
                List.of("A", "B", "C", "D", "E", "F"),
                List.of(List.of("=2+2", "+SOMA(A1:A2)", "-10", "@cmd", "\tvalor", "Normal"))
        );

        csvReportExporter.export(document, outputPath);

        String content = Files.readString(outputPath);
        assertTrue(content.contains("'=2+2"));
        assertTrue(content.contains("'+SOMA(A1:A2)"));
        assertTrue(content.contains("'-10"));
        assertTrue(content.contains("'@cmd"));
        assertTrue(content.contains("'\tvalor"));
        assertTrue(content.contains("Normal"));
    }
}