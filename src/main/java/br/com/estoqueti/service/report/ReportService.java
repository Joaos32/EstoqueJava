package br.com.estoqueti.service.report;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.dto.report.ReportDocumentDto;
import br.com.estoqueti.dto.report.ReportRequestDto;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.ReportFormat;
import br.com.estoqueti.model.enums.ReportType;
import br.com.estoqueti.repository.EquipmentRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentRepository;
import br.com.estoqueti.service.DashboardService;
import br.com.estoqueti.service.EquipmentService;
import br.com.estoqueti.service.StockMovementService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EquipmentService equipmentService = new EquipmentService();
    private final StockMovementService stockMovementService = new StockMovementService();
    private final DashboardService dashboardService = new DashboardService();
    private final CsvReportExporter csvReportExporter = new CsvReportExporter();
    private final PdfReportExporter pdfReportExporter = new PdfReportExporter();

    public ReportDocumentDto generateReport(ReportRequestDto request) {
        validateRequest(request);
        return switch (request.reportType()) {
            case EQUIPAMENTOS_CADASTRADOS -> buildRegisteredEquipmentReport();
            case ESTOQUE_BAIXO -> buildLowStockReport();
            case EQUIPAMENTOS_POR_CATEGORIA -> buildEquipmentByCategoryReport();
            case EQUIPAMENTOS_POR_LOCALIZACAO -> buildEquipmentByLocationReport();
            case MOVIMENTACOES_POR_PERIODO -> buildMovementsByPeriodReport(request.startDate(), request.endDate());
            case EQUIPAMENTOS_EM_MANUTENCAO -> buildMaintenanceEquipmentReport();
        };
    }

    public void exportReport(ReportRequestDto request, ReportFormat reportFormat, Path outputPath) {
        exportReport(generateReport(request), reportFormat, outputPath);
    }

    public void exportReport(ReportDocumentDto document, ReportFormat reportFormat, Path outputPath) {
        if (reportFormat == ReportFormat.CSV) {
            csvReportExporter.export(document, outputPath);
            return;
        }

        pdfReportExporter.export(document, outputPath);
    }

    public String buildDefaultFileName(ReportType reportType, ReportFormat reportFormat) {
        return reportType.getFileSlug() + "-" + LocalDate.now() + "." + reportFormat.getExtension();
    }

    private ReportDocumentDto buildRegisteredEquipmentReport() {
        List<EquipmentListItemDto> equipments = equipmentService.searchEquipment(new EquipmentSearchFilter(null, null, null, null, null, null, null, null));
        List<List<String>> rows = equipments.stream()
                .map(this::toRegisteredEquipmentRow)
                .toList();

        return new ReportDocumentDto(
                ReportType.EQUIPAMENTOS_CADASTRADOS,
                ReportType.EQUIPAMENTOS_CADASTRADOS.getDisplayName(),
                "Lista completa dos equipamentos ativos cadastrados no estoque.",
                List.of("Codigo", "Nome", "Categoria", "Marca", "Modelo", "Serie", "Patrimonio", "Quantidade", "Minimo", "Status", "Localizacao", "Responsavel", "Fornecedor", "Entrada"),
                rows
        );
    }

    private ReportDocumentDto buildLowStockReport() {
        DashboardSummaryDto dashboard = dashboardService.loadDashboard();
        List<List<String>> rows = dashboard.lowStockItems().stream()
                .map(this::toLowStockRow)
                .toList();

        return new ReportDocumentDto(
                ReportType.ESTOQUE_BAIXO,
                ReportType.ESTOQUE_BAIXO.getDisplayName(),
                "Itens abaixo do estoque minimo configurado.",
                List.of("Codigo", "Equipamento", "Localizacao", "Quantidade Atual", "Estoque Minimo", "Deficit"),
                rows
        );
    }

    private ReportDocumentDto buildEquipmentByCategoryReport() {
        List<Equipment> equipments = loadAllActiveEquipments();
        Map<String, AggregateRow> aggregates = new LinkedHashMap<>();

        for (Equipment equipment : equipments) {
            AggregateRow aggregate = aggregates.computeIfAbsent(equipment.getCategory().getName(), key -> new AggregateRow());
            aggregate.record(equipment);
        }

        List<List<String>> rows = aggregates.entrySet().stream()
                .map(entry -> entry.getValue().toAggregateRow(entry.getKey()))
                .toList();

        return new ReportDocumentDto(
                ReportType.EQUIPAMENTOS_POR_CATEGORIA,
                ReportType.EQUIPAMENTOS_POR_CATEGORIA.getDisplayName(),
                "Resumo consolidado por categoria de equipamento.",
                List.of("Categoria", "Registros", "Quantidade Total", "Disponivel", "Em Uso", "Em Manutencao"),
                rows
        );
    }

    private ReportDocumentDto buildEquipmentByLocationReport() {
        List<Equipment> equipments = loadAllActiveEquipments();
        Map<String, AggregateRow> aggregates = new LinkedHashMap<>();

        for (Equipment equipment : equipments) {
            AggregateRow aggregate = aggregates.computeIfAbsent(equipment.getLocation().getName(), key -> new AggregateRow());
            aggregate.record(equipment);
        }

        List<List<String>> rows = aggregates.entrySet().stream()
                .map(entry -> entry.getValue().toAggregateRow(entry.getKey()))
                .toList();

        return new ReportDocumentDto(
                ReportType.EQUIPAMENTOS_POR_LOCALIZACAO,
                ReportType.EQUIPAMENTOS_POR_LOCALIZACAO.getDisplayName(),
                "Resumo consolidado por localizacao do estoque.",
                List.of("Localizacao", "Registros", "Quantidade Total", "Disponivel", "Em Uso", "Em Manutencao"),
                rows
        );
    }

    private ReportDocumentDto buildMovementsByPeriodReport(LocalDate startDate, LocalDate endDate) {
        List<StockMovementListItemDto> movements = stockMovementService.searchMovements(
                new StockMovementSearchFilter(
                        null,
                        null,
                        atStartOfDay(startDate),
                        atEndOfDay(endDate)
                )
        );

        List<List<String>> rows = movements.stream()
                .map(this::toMovementRow)
                .toList();

        return new ReportDocumentDto(
                ReportType.MOVIMENTACOES_POR_PERIODO,
                ReportType.MOVIMENTACOES_POR_PERIODO.getDisplayName(),
                "Periodo de " + DATE_FORMATTER.format(startDate) + " ate " + DATE_FORMATTER.format(endDate) + ".",
                List.of("Data/Hora", "Tipo", "Codigo", "Equipamento", "Quantidade", "Origem", "Destino", "Responsavel", "Registrado por"),
                rows
        );
    }

    private ReportDocumentDto buildMaintenanceEquipmentReport() {
        List<EquipmentListItemDto> equipments = equipmentService.searchEquipment(
                new EquipmentSearchFilter(null, null, null, null, null, EquipmentStatus.EM_MANUTENCAO, null, null)
        );
        List<List<String>> rows = equipments.stream()
                .map(this::toMaintenanceRow)
                .toList();

        return new ReportDocumentDto(
                ReportType.EQUIPAMENTOS_EM_MANUTENCAO,
                ReportType.EQUIPAMENTOS_EM_MANUTENCAO.getDisplayName(),
                "Equipamentos com status atual em manutencao.",
                List.of("Codigo", "Nome", "Categoria", "Localizacao", "Quantidade", "Responsavel", "Fornecedor", "Entrada"),
                rows
        );
    }

    private List<Equipment> loadAllActiveEquipments() {
        return JpaExecutor.query(entityManager -> {
            EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
            return new ArrayList<>(equipmentRepository.findAllActiveOrderedByName());
        });
    }

    private void validateRequest(ReportRequestDto request) {
        if (request == null || request.reportType() == null) {
            throw new ValidationException("Selecione o tipo de relatorio que deseja exportar.");
        }
        if (request.reportType().requiresPeriod()) {
            if (request.startDate() == null || request.endDate() == null) {
                throw new ValidationException("Informe a data inicial e a data final para esse relatorio.");
            }
            if (request.endDate().isBefore(request.startDate())) {
                throw new ValidationException("A data final do relatorio nao pode ser anterior a data inicial.");
            }
        }
    }

    private List<String> toRegisteredEquipmentRow(EquipmentListItemDto equipment) {
        return List.of(
                equipment.internalCode(),
                equipment.name(),
                equipment.categoryName(),
                defaultValue(equipment.brand()),
                defaultValue(equipment.model()),
                defaultValue(equipment.serialNumber()),
                defaultValue(equipment.patrimonyNumber()),
                String.valueOf(equipment.quantity()),
                String.valueOf(equipment.minimumStock()),
                equipment.status().getDisplayName(),
                equipment.locationName(),
                defaultValue(equipment.responsibleName()),
                defaultValue(equipment.supplierName()),
                DATE_FORMATTER.format(equipment.entryDate())
        );
    }

    private List<String> toLowStockRow(DashboardLowStockItemDto item) {
        return List.of(
                item.internalCode(),
                item.name(),
                item.locationName(),
                String.valueOf(item.quantity()),
                String.valueOf(item.minimumStock()),
                String.valueOf(item.stockGap())
        );
    }

    private List<String> toMovementRow(StockMovementListItemDto movement) {
        return List.of(
                DATE_TIME_FORMATTER.format(movement.movementAt().toLocalDateTime()),
                movement.movementType().getDisplayName(),
                movement.equipmentInternalCode(),
                movement.equipmentName(),
                String.valueOf(movement.quantity()),
                defaultValue(movement.sourceLocationName()),
                defaultValue(movement.destinationLocationName()),
                movement.responsibleName(),
                movement.performedByUsername()
        );
    }

    private List<String> toMaintenanceRow(EquipmentListItemDto equipment) {
        return List.of(
                equipment.internalCode(),
                equipment.name(),
                equipment.categoryName(),
                equipment.locationName(),
                String.valueOf(equipment.quantity()),
                defaultValue(equipment.responsibleName()),
                defaultValue(equipment.supplierName()),
                DATE_FORMATTER.format(equipment.entryDate())
        );
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIN, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private OffsetDateTime atEndOfDay(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MAX, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "Nao informado" : value;
    }

    private static final class AggregateRow {
        private int records;
        private int totalQuantity;
        private int availableQuantity;
        private int inUseQuantity;
        private int maintenanceQuantity;

        private void record(Equipment equipment) {
            records++;
            totalQuantity += equipment.getQuantity();
            if (equipment.getStatus() == EquipmentStatus.DISPONIVEL) {
                availableQuantity += equipment.getQuantity();
            }
            if (equipment.getStatus() == EquipmentStatus.EM_USO) {
                inUseQuantity += equipment.getQuantity();
            }
            if (equipment.getStatus() == EquipmentStatus.EM_MANUTENCAO) {
                maintenanceQuantity += equipment.getQuantity();
            }
        }

        private List<String> toAggregateRow(String label) {
            return List.of(
                    label,
                    String.valueOf(records),
                    String.valueOf(totalQuantity),
                    String.valueOf(availableQuantity),
                    String.valueOf(inUseQuantity),
                    String.valueOf(maintenanceQuantity)
            );
        }
    }
}