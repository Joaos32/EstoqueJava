package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.service.DashboardService;
import br.com.estoqueti.session.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardController {

    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label dashboardSubtitleLabel;

    @FXML
    private Label totalItemsValueLabel;

    @FXML
    private Label totalItemsMetaLabel;

    @FXML
    private Label availableValueLabel;

    @FXML
    private Label availableMetaLabel;

    @FXML
    private Label inUseValueLabel;

    @FXML
    private Label inUseMetaLabel;

    @FXML
    private Label maintenanceValueLabel;

    @FXML
    private Label maintenanceMetaLabel;

    @FXML
    private Label lowStockValueLabel;

    @FXML
    private Label lowStockMetaLabel;

    @FXML
    private TableView<DashboardLowStockItemDto> lowStockTable;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockCodeColumn;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockNameColumn;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockLocationColumn;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockQuantityColumn;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockMinimumColumn;

    @FXML
    private TableColumn<DashboardLowStockItemDto, String> lowStockGapColumn;

    @FXML
    private TableView<StockMovementListItemDto> recentMovementsTable;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementDateColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementTypeColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementEquipmentColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementQuantityColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementRouteColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> recentMovementResponsibleColumn;

    @FXML
    private Label dashboardStatusLabel;

    @FXML
    public void initialize() {
        configureTables();
        configureStaticTexts();
        loadDashboard();
    }

    @FXML
    private void handleRefreshDashboard() {
        loadDashboard();
    }

    private void configureTables() {
        lowStockCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().internalCode()));
        lowStockNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        lowStockLocationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().locationName()));
        lowStockQuantityColumn.setCellValueFactory(data -> new SimpleStringProperty(formatInteger(data.getValue().quantity())));
        lowStockMinimumColumn.setCellValueFactory(data -> new SimpleStringProperty(formatInteger(data.getValue().minimumStock())));
        lowStockGapColumn.setCellValueFactory(data -> new SimpleStringProperty(formatInteger(data.getValue().stockGap())));

        recentMovementDateColumn.setCellValueFactory(data -> new SimpleStringProperty(DATE_TIME_FORMATTER.format(data.getValue().movementAt().toLocalDateTime())));
        recentMovementTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().movementType().getDisplayName()));
        recentMovementEquipmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().equipmentInternalCode() + " - " + data.getValue().equipmentName()));
        recentMovementQuantityColumn.setCellValueFactory(data -> new SimpleStringProperty(formatInteger(data.getValue().quantity())));
        recentMovementRouteColumn.setCellValueFactory(data -> new SimpleStringProperty(buildRouteLabel(data.getValue())));
        recentMovementResponsibleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().responsibleName()));

        configureInformationalTable(lowStockTable);
        configureInformationalTable(recentMovementsTable);

        lowStockTable.setPlaceholder(new Label("Nenhum item abaixo do estoque minimo no momento."));
        recentMovementsTable.setPlaceholder(new Label("Nenhuma movimentacao recente encontrada."));
    }

    private <T> void configureInformationalTable(TableView<T> tableView) {
        tableView.setEditable(false);
        tableView.setFocusTraversable(false);
        tableView.setRowFactory(ignored -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMousePressed(event -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().clearSelection();
                    tableView.getFocusModel().focus(-1);
                    event.consume();
                }
            });
            return row;
        });
    }

    private void configureStaticTexts() {
        AuthenticatedUserDto authenticatedUser = UserSession.requireAuthenticatedUser();
        welcomeLabel.setText("Painel operacional de " + authenticatedUser.fullName());
        dashboardSubtitleLabel.setText("Visao consolidada do estoque ativo, alertas de saldo e ultimas movimentacoes registradas.");
    }

    private void loadDashboard() {
        try {
            DashboardSummaryDto summary = dashboardService.loadDashboard();
            applySummary(summary);
            dashboardStatusLabel.setText("Dashboard atualizado em " + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ".");
            applyStatusStyle("form-status-neutral");
        } catch (RuntimeException exception) {
            totalItemsValueLabel.setText("-");
            availableValueLabel.setText("-");
            inUseValueLabel.setText("-");
            maintenanceValueLabel.setText("-");
            lowStockValueLabel.setText("-");
            lowStockTable.setItems(FXCollections.observableArrayList());
            recentMovementsTable.setItems(FXCollections.observableArrayList());
            dashboardStatusLabel.setText("Nao foi possivel carregar o dashboard. Verifique a conexao e tente novamente.");
            applyStatusStyle("form-status-error");
        }
    }

    private void applySummary(DashboardSummaryDto summary) {
        totalItemsValueLabel.setText(formatInteger(summary.totalActiveQuantity()));
        totalItemsMetaLabel.setText(summary.totalRegisteredEquipments() + " cadastro(s) ativos no estoque.");

        availableValueLabel.setText(formatInteger(summary.totalAvailableQuantity()));
        availableMetaLabel.setText("Itens com status Disponivel.");

        inUseValueLabel.setText(formatInteger(summary.totalInUseQuantity()));
        inUseMetaLabel.setText("Itens atualmente em uso operacional.");

        maintenanceValueLabel.setText(formatInteger(summary.totalInMaintenanceQuantity()));
        maintenanceMetaLabel.setText("Itens alocados em manutencao.");

        lowStockValueLabel.setText(String.valueOf(summary.lowStockItemsCount()));
        lowStockMetaLabel.setText(summary.lowStockItemsCount() == 1
                ? "1 item abaixo do estoque minimo."
                : summary.lowStockItemsCount() + " itens abaixo do estoque minimo.");

        lowStockTable.setItems(FXCollections.observableArrayList(summary.lowStockItems()));
        recentMovementsTable.setItems(FXCollections.observableArrayList(summary.recentMovements()));
    }

    private String buildRouteLabel(StockMovementListItemDto item) {
        String source = item.sourceLocationName() == null ? "Sem origem" : item.sourceLocationName();
        String destination = item.destinationLocationName() == null ? "Sem destino" : item.destinationLocationName();
        return source + " -> " + destination;
    }

    private String formatInteger(long value) {
        return INTEGER_FORMAT.format(value);
    }

    private void applyStatusStyle(String styleClass) {
        dashboardStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        dashboardStatusLabel.getStyleClass().add(styleClass);
    }
}