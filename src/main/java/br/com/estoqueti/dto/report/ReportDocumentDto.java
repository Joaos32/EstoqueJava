package br.com.estoqueti.dto.report;

import br.com.estoqueti.model.enums.ReportType;

import java.util.List;

public record ReportDocumentDto(
        ReportType reportType,
        String title,
        String subtitle,
        List<String> headers,
        List<List<String>> rows
) {
}