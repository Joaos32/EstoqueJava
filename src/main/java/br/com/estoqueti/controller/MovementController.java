package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.movement.MovementEquipmentOptionDto;
import br.com.estoqueti.dto.movement.MovementReferenceDataDto;
import br.com.estoqueti.dto.movement.StockMovementCreateRequest;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.service.StockMovementService;
import br.com.estoqueti.session.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.UnaryOperator;

public class MovementController {

    private static final DateTimeFormatter MOVEMENT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StockMovementService stockMovementService = new StockMovementService();

    private List<MovementEquipmentOptionDto> equipmentOptions = List.of();
    private List<LookupOptionDto> locationOptions = List.of();

    @FXML
    private TableView<StockMovementListItemDto> movementTable;

    @FXML
    private TableColumn<StockMovementListItemDto, String> movementAtColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> movementTypeColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> equipmentColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> quantityColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> sourceColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> destinationColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> responsibleColumn;

    @FXML
    private TableColumn<StockMovementListItemDto, String> performedByColumn;

    @FXML
    private ComboBox<MovementEquipmentOptionDto> equipmentFilterComboBox;

    @FXML
    private ComboBox<MovementType> movementTypeFilterComboBox;

    @FXML
    private DatePicker movementFromDatePicker;

    @FXML
    private DatePicker movementToDatePicker;

    @FXML
    private Label resultsSummaryLabel;

    @FXML
    private Label accessHintLabel;

    @FXML
    private Label equipmentSummaryLabel;

    @FXML
    private Label rulesHintLabel;

    @FXML
    private VBox formFieldsContainer;

    @FXML
    private ComboBox<MovementEquipmentOptionDto> movementEquipmentComboBox;

    @FXML
    private ComboBox<MovementType> movementTypeComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<LookupOptionDto> sourceLocationComboBox;

    @FXML
    private ComboBox<LookupOptionDto> destinationLocationComboBox;

    @FXML
    private TextField responsibleNameField;

    @FXML
    private DatePicker movementDatePicker;

    @FXML
    private TextField movementTimeField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button registerMovementButton;

    @FXML
    private Label formStatusLabel;

    @FXML
    public void initialize() {
        configureTable();
        configureQuantityField();
        configureTimeField();
        movementTypeComboBox.setItems(FXCollections.observableArrayList(MovementType.values()));
        movementTypeFilterComboBox.setItems(FXCollections.observableArrayList(MovementType.values()));
        movementTypeComboBox.getSelectionModel().select(MovementType.ENTRADA);
        movementDatePicker.setValue(LocalDate.now());
        movementTimeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER));
        formStatusLabel.setText("Registre entradas, saidas, transferencias e manutencoes com impacto real no estoque.");
        applyStatusStyle("form-status-neutral");
        equipmentSummaryLabel.setText("Selecione um equipamento para ver saldo, status e localizacao atual.");
        loadReferenceData();
        configurePermissions();
        attachListeners();
        applyMovementTypeRules();
        updateEquipmentSummary();
        loadMovements();
    }

    @FXML
    private void handleSearch() {
        loadMovements();
    }

    @FXML
    private void handleClearFilters() {
        equipmentFilterComboBox.getSelectionModel().clearSelection();
        movementTypeFilterComboBox.getSelectionModel().clearSelection();
        movementFromDatePicker.setValue(null);
        movementToDatePicker.setValue(null);
        loadMovements();
    }

    @FXML
    private void handleRegisterMovement() {
        try {
            StockMovementListItemDto registeredMovement = stockMovementService.registerMovement(
                    buildCreateRequest(),
                    UserSession.requireAuthenticatedUser()
            );

            formStatusLabel.setText("Movimentacao registrada com sucesso para o equipamento " + registeredMovement.equipmentInternalCode() + ".");
            applyStatusStyle("form-status-success");
            Long selectedEquipmentId = movementEquipmentComboBox.getValue() == null ? null : movementEquipmentComboBox.getValue().id();
            reloadReferenceData();
            clearForm();
            restoreFormEquipmentSelection(selectedEquipmentId);
            updateEquipmentSummary();
            loadMovements();
        } catch (BusinessException exception) {
            formStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
        updateEquipmentSummary();
        applyMovementTypeRules();
        formStatusLabel.setText("Formulario limpo. Informe os dados da nova movimentacao.");
        applyStatusStyle("form-status-neutral");
    }

    private void attachListeners() {
        movementEquipmentComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateEquipmentSummary();
            applyMovementTypeRules();
        });
        movementTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyMovementTypeRules());
    }

    private void configureTable() {
        movementAtColumn.setCellValueFactory(data -> new SimpleStringProperty(MOVEMENT_FORMATTER.format(data.getValue().movementAt())));
        movementTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().movementType().getDisplayName()));
        equipmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().equipmentInternalCode() + " - " + data.getValue().equipmentName()));
        quantityColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().quantity())));
        sourceColumn.setCellValueFactory(data -> new SimpleStringProperty(defaultValue(data.getValue().sourceLocationName())));
        destinationColumn.setCellValueFactory(data -> new SimpleStringProperty(defaultValue(data.getValue().destinationLocationName())));
        responsibleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().responsibleName()));
        performedByColumn.setCellValueFactory(data -> new SimpleStringProperty("@" + data.getValue().performedByUsername()));
    }

    private void configureQuantityField() {
        UnaryOperator<TextFormatter.Change> numericFilter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        quantityField.setTextFormatter(new TextFormatter<>(numericFilter));
    }

    private void configureTimeField() {
        UnaryOperator<TextFormatter.Change> timeFilter = change -> change.getControlNewText().matches("[0-9:]{0,5}") ? change : null;
        movementTimeField.setTextFormatter(new TextFormatter<>(timeFilter));
    }

    private void configurePermissions() {
        AuthenticatedUserDto authenticatedUser = UserSession.requireAuthenticatedUser();
        boolean canManageInventory = authenticatedUser.canManageInventory();

        accessHintLabel.setText(canManageInventory
                ? "Seu perfil pode registrar movimentacoes com impacto imediato no estoque corporativo."
                : "Seu perfil possui acesso somente de consulta para movimentacoes de estoque.");

        formFieldsContainer.setDisable(!canManageInventory);
        registerMovementButton.setDisable(!canManageInventory);
    }

    private void loadReferenceData() {
        MovementReferenceDataDto referenceData = stockMovementService.listReferenceData();
        equipmentOptions = referenceData.equipments();
        locationOptions = referenceData.locations();

        movementEquipmentComboBox.setItems(FXCollections.observableArrayList(equipmentOptions));
        equipmentFilterComboBox.setItems(FXCollections.observableArrayList(equipmentOptions));
        sourceLocationComboBox.setItems(FXCollections.observableArrayList(locationOptions));
        destinationLocationComboBox.setItems(FXCollections.observableArrayList(locationOptions));
    }

    private void reloadReferenceData() {
        Long filterEquipmentId = equipmentFilterComboBox.getValue() == null ? null : equipmentFilterComboBox.getValue().id();
        loadReferenceData();
        restoreFilterEquipmentSelection(filterEquipmentId);
    }

    private void loadMovements() {
        List<StockMovementListItemDto> movements = stockMovementService.searchMovements(buildSearchFilter());
        movementTable.setItems(FXCollections.observableArrayList(movements));
        resultsSummaryLabel.setText(movements.size() + " movimentacao(oes) encontradas para os filtros informados.");
    }

    private StockMovementSearchFilter buildSearchFilter() {
        return new StockMovementSearchFilter(
                equipmentFilterComboBox.getValue() == null ? null : equipmentFilterComboBox.getValue().id(),
                movementTypeFilterComboBox.getValue(),
                atStartOfDay(movementFromDatePicker.getValue()),
                atEndOfDay(movementToDatePicker.getValue())
        );
    }

    private StockMovementCreateRequest buildCreateRequest() {
        return new StockMovementCreateRequest(
                movementEquipmentComboBox.getValue() == null ? null : movementEquipmentComboBox.getValue().id(),
                movementTypeComboBox.getValue(),
                parseRequiredInteger(quantityField.getText()),
                sourceLocationComboBox.getValue() == null ? null : sourceLocationComboBox.getValue().id(),
                destinationLocationComboBox.getValue() == null ? null : destinationLocationComboBox.getValue().id(),
                responsibleNameField.getText(),
                buildMovementAt(),
                notesArea.getText()
        );
    }

    private Integer parseRequiredInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException("Informe uma quantidade numerica valida.");
        }
    }

    private OffsetDateTime buildMovementAt() {
        if (movementDatePicker.getValue() == null) {
            return null;
        }

        try {
            LocalTime time = LocalTime.parse(movementTimeField.getText(), TIME_FORMATTER);
            return ZonedDateTime.of(movementDatePicker.getValue(), time, ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException exception) {
            throw new ValidationException("Informe o horario da movimentacao no formato HH:mm.");
        }
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.of(date, LocalTime.MIN, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private OffsetDateTime atEndOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.of(date, LocalTime.MAX, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void applyMovementTypeRules() {
        MovementType movementType = movementTypeComboBox.getValue();
        MovementEquipmentOptionDto selectedEquipment = movementEquipmentComboBox.getValue();

        boolean sourceRequired = movementType != null && movementType.requiresSourceLocation();
        boolean destinationRequired = movementType != null && movementType.requiresDestinationLocation();
        LookupOptionDto currentLocation = selectedEquipment == null ? null : findLocationById(selectedEquipment.locationId());
        boolean lockDestinationToCurrentLocation = movementType == MovementType.ENTRADA
                && selectedEquipment != null
                && selectedEquipment.quantity() > 0
                && currentLocation != null;

        sourceLocationComboBox.setDisable(!sourceRequired);
        destinationLocationComboBox.setDisable(!destinationRequired || lockDestinationToCurrentLocation);

        if (sourceRequired && currentLocation != null) {
            sourceLocationComboBox.getSelectionModel().select(currentLocation);
        } else {
            sourceLocationComboBox.getSelectionModel().clearSelection();
        }

        if (!destinationRequired) {
            destinationLocationComboBox.getSelectionModel().clearSelection();
        } else if (lockDestinationToCurrentLocation) {
            destinationLocationComboBox.getSelectionModel().select(currentLocation);
        }

        rulesHintLabel.setText(resolveRulesHint(movementType));
    }

    private String resolveRulesHint(MovementType movementType) {
        if (movementType == null) {
            return "Selecione o tipo de movimentacao para ver as regras aplicadas ao estoque.";
        }

        return switch (movementType) {
            case ENTRADA -> "Entradas aumentam o saldo. Se o registro ja tem saldo, a entrada deve permanecer na localizacao atual.";
            case SAIDA -> "Saidas reduzem o saldo e usam apenas a origem atual do equipamento.";
            case TRANSFERENCIA -> "Transferencias movem o saldo total do registro para outra localizacao, sem alterar a quantidade.";
            case ENVIO_MANUTENCAO -> "Envio para manutencao move o saldo total para outro local e altera o status para Em manutencao.";
            case RETORNO_MANUTENCAO -> "Retorno de manutencao exige item em manutencao, move o saldo total e volta o status para Disponivel.";
            case BAIXA_DESCARTE -> "Baixa reduz o saldo. Quando o saldo chega a zero, o status do registro passa para Descartado.";
        };
    }

    private void updateEquipmentSummary() {
        MovementEquipmentOptionDto selectedEquipment = movementEquipmentComboBox.getValue();
        if (selectedEquipment == null) {
            equipmentSummaryLabel.setText("Selecione um equipamento para ver saldo, status e localizacao atual.");
            return;
        }

        equipmentSummaryLabel.setText(
                "Saldo atual: " + selectedEquipment.quantity()
                        + " | Status: " + selectedEquipment.status().getDisplayName()
                        + " | Local atual: " + selectedEquipment.locationName()
                        + " | Responsavel atual: " + defaultValue(selectedEquipment.responsibleName())
        );
    }

    private LookupOptionDto findLocationById(Long locationId) {
        if (locationId == null) {
            return null;
        }

        return locationOptions.stream()
                .filter(option -> option.id().equals(locationId))
                .findFirst()
                .orElse(null);
    }

    private void clearForm() {
        movementEquipmentComboBox.getSelectionModel().clearSelection();
        movementTypeComboBox.getSelectionModel().select(MovementType.ENTRADA);
        quantityField.clear();
        sourceLocationComboBox.getSelectionModel().clearSelection();
        destinationLocationComboBox.getSelectionModel().clearSelection();
        responsibleNameField.clear();
        movementDatePicker.setValue(LocalDate.now());
        movementTimeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER));
        notesArea.clear();
    }

    private void restoreFormEquipmentSelection(Long equipmentId) {
        if (equipmentId == null) {
            return;
        }

        equipmentOptions.stream()
                .filter(option -> option.id().equals(equipmentId))
                .findFirst()
                .ifPresent(option -> movementEquipmentComboBox.getSelectionModel().select(option));
    }

    private void restoreFilterEquipmentSelection(Long equipmentId) {
        if (equipmentId == null) {
            return;
        }

        equipmentOptions.stream()
                .filter(option -> option.id().equals(equipmentId))
                .findFirst()
                .ifPresent(option -> equipmentFilterComboBox.getSelectionModel().select(option));
    }

    private void applyStatusStyle(String styleClass) {
        formStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        formStatusLabel.getStyleClass().add(styleClass);
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "Nao informado" : value;
    }
}