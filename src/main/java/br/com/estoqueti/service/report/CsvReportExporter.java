package br.com.estoqueti.service.report;

import br.com.estoqueti.dto.report.ReportDocumentDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvReportExporter {

    public void export(ReportDocumentDto document, Path outputPath) {
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(document.headers().toArray(String[]::new))
                    .build();

            try (Writer writer = Files.newBufferedWriter(outputPath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (var row : document.rows()) {
                    csvPrinter.printRecord(row.stream()
                            .map(this::sanitizeCell)
                            .toList());
                }
                csvPrinter.flush();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel exportar o relatorio em CSV.", exception);
        }
    }

    private String sanitizeCell(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        char firstCharacter = value.charAt(0);
        if (firstCharacter == '='
                || firstCharacter == '+'
                || firstCharacter == '-'
                || firstCharacter == '@'
                || firstCharacter == '\t'
                || firstCharacter == '\r') {
            return "'" + value;
        }

        return value;
    }
}