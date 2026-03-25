package br.com.estoqueti.dto.report;

import br.com.estoqueti.model.enums.ReportType;

import java.time.LocalDate;

public record ReportRequestDto(
        ReportType reportType,
        LocalDate startDate,
        LocalDate endDate
) {
}