package br.com.estoqueti.controller;

import br.com.estoqueti.dto.report.ReportDocumentDto;
import br.com.estoqueti.dto.report.ReportRequestDto;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.model.enums.ReportFormat;
import br.com.estoqueti.model.enums.ReportType;
import br.com.estoqueti.service.report.ReportService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

public class ReportController {

    private final ReportService reportService = new ReportService();

    private ReportDocumentDto currentDocument;

    @FXML
    private ComboBox<ReportType> reportTypeComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label reportDescriptionLabel;

    @FXML
    private Label periodHintLabel;

    @FXML
    private Label previewTitleLabel;

    @FXML
    private Label previewRowsLabel;

    @FXML
    private Label previewColumnsLabel;

    @FXML
    private Button exportCsvButton;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Label reportStatusLabel;

    @FXML
    public void initialize() {
        reportTypeComboBox.setItems(FXCollections.observableArrayList(ReportType.values()));
        reportTypeComboBox.getSelectionModel().select(ReportType.EQUIPAMENTOS_CADASTRADOS);
        reportTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentDocument = null;
            configureDateFields();
            refreshPreview();
        });
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        configureDateFields();
        refreshPreview();
    }

    @FXML
    private void handleRefreshPreview() {
        refreshPreview();
    }

    @FXML
    private void handleExportCsv() {
        exportReport(ReportFormat.CSV);
    }

    @FXML
    private void handleExportPdf() {
        exportReport(ReportFormat.PDF);
    }

    private void configureDateFields() {
        ReportType selectedReport = reportTypeComboBox.getValue();
        boolean requiresPeriod = selectedReport != null && selectedReport.requiresPeriod();

        startDatePicker.setDisable(!requiresPeriod);
        endDatePicker.setDisable(!requiresPeriod);

        reportDescriptionLabel.setText(selectedReport == null ? "" : selectedReport.getDescription());
        periodHintLabel.setText(requiresPeriod
                ? "Esse relatorio exige data inicial e data final para filtrar as movimentacoes."
                : "Esse relatorio nao exige periodo. Os filtros de data ficam desabilitados para evitar inconsistencias.");
    }

    private void refreshPreview() {
        try {
            ReportDocumentDto document = reportService.generateReport(buildRequest());
            currentDocument = document;
            previewTitleLabel.setText(document.title());
            previewRowsLabel.setText(document.rows().size() + " linha(s) serao exportadas com os filtros atuais.");
            previewColumnsLabel.setText(String.join(" | ", document.headers()));
            reportStatusLabel.setText("Resumo carregado com sucesso. Selecione CSV ou PDF para exportar.");
            applyStatusStyle("form-status-neutral");
        } catch (BusinessException exception) {
            currentDocument = null;
            previewTitleLabel.setText("Resumo indisponivel");
            previewRowsLabel.setText(exception.getMessage());
            previewColumnsLabel.setText("Ajuste os filtros do relatorio e tente novamente.");
            reportStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        }
    }

    private void exportReport(ReportFormat reportFormat) {
        try {
            ReportDocumentDto document = ensureCurrentDocument();
            File selectedFile = chooseOutputFile(reportFormat, document.reportType());
            if (selectedFile == null) {
                reportStatusLabel.setText("Exportacao cancelada pelo usuario.");
                applyStatusStyle("form-status-neutral");
                return;
            }

            Path outputPath = selectedFile.toPath();
            reportService.exportReport(document, reportFormat, outputPath);
            reportStatusLabel.setText(reportFormat.getDisplayName() + " gerado com sucesso em: " + outputPath.toAbsolutePath());
            applyStatusStyle("form-status-success");
        } catch (BusinessException exception) {
            reportStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        } catch (RuntimeException exception) {
            reportStatusLabel.setText("Nao foi possivel exportar o relatorio. Verifique o local escolhido e tente novamente.");
            applyStatusStyle("form-status-error");
        }
    }

    private ReportDocumentDto ensureCurrentDocument() {
        ReportRequestDto request = buildRequest();
        if (currentDocument == null || currentDocument.reportType() != request.reportType()) {
            currentDocument = reportService.generateReport(request);
            return currentDocument;
        }

        if (request.reportType().requiresPeriod()) {
            currentDocument = reportService.generateReport(request);
        }
        return currentDocument;
    }

    private ReportRequestDto buildRequest() {
        return new ReportRequestDto(
                reportTypeComboBox.getValue(),
                startDatePicker.getValue(),
                endDatePicker.getValue()
        );
    }

    private File chooseOutputFile(ReportFormat reportFormat, ReportType reportType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar relatorio em " + reportFormat.getDisplayName());
        fileChooser.setInitialFileName(reportService.buildDefaultFileName(reportType, reportFormat));
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter(reportFormat.getDisplayName() + " (*." + reportFormat.getExtension() + ")", "*." + reportFormat.getExtension())
        );
        return fileChooser.showSaveDialog(exportCsvButton.getScene().getWindow());
    }

    private void applyStatusStyle(String styleClass) {
        reportStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        reportStatusLabel.getStyleClass().add(styleClass);
    }
}