package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.dashboard.DashboardLowStockItemDto;
import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.service.DashboardService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ResponsiveLayoutSupport;
import br.com.estoqueti.util.UiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;

public class DashboardController {

    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DashboardService dashboardService = new DashboardService();
    @FXML
    private FlowPane metricsFlowPane;

    @FXML
    private VBox totalMetricCard;

    @FXML
    private VBox availableMetricCard;

    @FXML
    private VBox inUseMetricCard;

    @FXML
    private VBox maintenanceMetricCard;

    @FXML
    private VBox lowStockMetricCard;

    @FXML
    private FlowPane dashboardPanelsFlowPane;

    @FXML
    private VBox lowStockPanelCard;

    @FXML
    private VBox recentMovementsPanelCard;

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
        ResponsiveLayoutSupport.configureResponsiveTiles(metricsFlowPane, 192, 360, totalMetricCard, availableMetricCard, inUseMetricCard, maintenanceMetricCard, lowStockMetricCard);
        ResponsiveLayoutSupport.configureResponsiveSplit(dashboardPanelsFlowPane, lowStockPanelCard, 42, 400, recentMovementsPanelCard, 58, 460);
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
        recentMovementTypeColumn.setCellFactory(UiSupport.badgeCellFactory(this::resolveMovementBadgeStyle));
        recentMovementEquipmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().equipmentInternalCode() + " - " + data.getValue().equipmentName()));
        recentMovementQuantityColumn.setCellValueFactory(data -> new SimpleStringProperty(formatInteger(data.getValue().quantity())));
        recentMovementRouteColumn.setCellValueFactory(data -> new SimpleStringProperty(buildRouteLabel(data.getValue())));
        recentMovementResponsibleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().responsibleName()));

        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentMovementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configureInformationalTable(lowStockTable, this::applyLowStockRowStyle);
        configureInformationalTable(recentMovementsTable, this::applyRecentMovementRowStyle);

        lowStockTable.setPlaceholder(UiSupport.createTablePlaceholder(
                "Sem alertas criticos no momento",
                "Quando houver itens abaixo do minimo configurado, eles aparecerao aqui."
        ));
        recentMovementsTable.setPlaceholder(UiSupport.createTablePlaceholder(
                "Sem movimentacoes recentes",
                "O historico operacional mais recente sera exibido neste painel assim que houver registros."
        ));
    }

    private <T> void configureInformationalTable(TableView<T> tableView, BiConsumer<TableRow<T>, T> rowStyler) {
        tableView.setEditable(false);
        tableView.setFocusTraversable(false);
        tableView.setRowFactory(ignored -> {
            TableRow<T> row = new TableRow<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("table-row-warning", "table-row-danger", "table-row-info", "table-row-muted");

                    if (!empty && item != null) {
                        rowStyler.accept(this, item);
                    }
                }
            };
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

    private void applyLowStockRowStyle(TableRow<DashboardLowStockItemDto> row, DashboardLowStockItemDto item) {
        if (item.quantity() == 0) {
            row.getStyleClass().add("table-row-danger");
        } else {
            row.getStyleClass().add("table-row-warning");
        }
    }

    private void applyRecentMovementRowStyle(TableRow<StockMovementListItemDto> row, StockMovementListItemDto item) {
        switch (item.movementType()) {
            case BAIXA_DESCARTE -> row.getStyleClass().add("table-row-danger");
            case ENVIO_MANUTENCAO, RETORNO_MANUTENCAO -> row.getStyleClass().add("table-row-warning");
            case TRANSFERENCIA, ENTREGA_FUNCIONARIO, DEVOLUCAO_FUNCIONARIO -> row.getStyleClass().add("table-row-info");
            case SAIDA -> row.getStyleClass().add("table-row-muted");
            default -> {
            }
        }
    }

    private String buildRouteLabel(StockMovementListItemDto item) {
        String source = item.sourceLocationName() == null ? "Sem origem" : item.sourceLocationName();
        String destination = item.destinationLocationName() == null ? "Sem destino" : item.destinationLocationName();
        return source + " -> " + destination;
    }

    private String formatInteger(long value) {
        return INTEGER_FORMAT.format(value);
    }

    private String resolveMovementBadgeStyle(String movementType) {
        return switch (movementType) {
            case "Entrada de estoque", "Retorno de manutencao", "Devolucao com protocolo" -> "badge-success";
            case "Transferencia entre locais", "Entrega com protocolo" -> "badge-info";
            case "Envio para manutencao" -> "badge-warning";
            case "Saida de estoque", "Baixa ou descarte" -> "badge-danger";
            default -> "badge-neutral";
        };
    }

    private void applyStatusStyle(String styleClass) {
        dashboardStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        dashboardStatusLabel.getStyleClass().add(styleClass);
    }
}

