package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.delivery.DeliveryProtocolCreateRequest;
import br.com.estoqueti.dto.delivery.DeliveryProtocolResultDto;
import br.com.estoqueti.dto.movement.MovementEquipmentOptionDto;
import br.com.estoqueti.dto.movement.MovementReferenceDataDto;
import br.com.estoqueti.dto.movement.StockMovementCreateRequest;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolCreateRequest;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolResultDto;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.model.enums.ReturnProtocolReason;
import br.com.estoqueti.service.DeliveryProtocolService;
import br.com.estoqueti.service.ReturnProtocolService;
import br.com.estoqueti.service.StockMovementService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ResponsiveLayoutSupport;
import br.com.estoqueti.util.UiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final DeliveryProtocolService deliveryProtocolService = new DeliveryProtocolService();
    private final ReturnProtocolService returnProtocolService = new ReturnProtocolService();

    private List<MovementEquipmentOptionDto> equipmentOptions = List.of();
    private List<LookupOptionDto> locationOptions = List.of();
    private Path lastGeneratedProtocolPath;

    @FXML
    private GridPane movementLayoutPane;

    @FXML
    private VBox movementHistoryCard;

    @FXML
    private VBox movementFormCard;

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
    private Label responsibleNameLabel;

    @FXML
    private Label protocolFlowHintLabel;

    @FXML
    private VBox protocolFieldsContainer;

    @FXML
    private Label protocolSectionCaptionLabel;

    @FXML
    private VBox deliveryProtocolFieldsContainer;

    @FXML
    private VBox returnProtocolFieldsContainer;

    @FXML
    private VBox protocolActionsContainer;

    @FXML
    private Label generatedProtocolPathLabel;

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
    private TextField deliveryDestinationField;

    @FXML
    private TextField responsibleNameField;

    @FXML
    private TextField recipientCpfField;

    @FXML
    private TextField recipientRoleField;

    @FXML
    private TextField returnEmployeeNameField;

    @FXML
    private TextField returnEmployeeCpfField;

    @FXML
    private TextField companyReceiverRoleField;

    @FXML
    private TextField companyReceiverCpfField;

    @FXML
    private ComboBox<ReturnProtocolReason> returnReasonComboBox;

    @FXML
    private TextField returnReasonOtherField;

    @FXML
    private DatePicker movementDatePicker;

    @FXML
    private TextField movementTimeField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button registerMovementButton;

    @FXML
    private Button openProtocolButton;

    @FXML
    private Button printProtocolButton;

    @FXML
    private Label formStatusLabel;

    @FXML
    public void initialize() {
        configureTable();
        configureQuantityField();
        configureCpfFields();
        configureTimeField();
        movementTypeComboBox.setItems(FXCollections.observableArrayList(MovementType.values()));
        movementTypeFilterComboBox.setItems(FXCollections.observableArrayList(MovementType.values()));
        returnReasonComboBox.setItems(FXCollections.observableArrayList(ReturnProtocolReason.values()));
        movementTypeComboBox.getSelectionModel().select(MovementType.ENTRADA);
        movementDatePicker.setValue(LocalDate.now());
        movementTimeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMATTER));
        formStatusLabel.setText("Registre entradas, saidas, transferencias, manutencoes, entregas e devolucoes com protocolo.");
        protocolFlowHintLabel.setText("Para gerar protocolo, escolha 'Entrega com protocolo' ou 'Devolucao com protocolo'. Os campos do documento aparecem automaticamente.");
        protocolSectionCaptionLabel.setText("Os campos do protocolo aparecem de acordo com o tipo selecionado.");
        applyStatusStyle("form-status-neutral");
        ResponsiveLayoutSupport.configureResponsiveSplit(movementLayoutPane, movementHistoryCard, 60, 540, movementFormCard, 40, 370, 1260);
        equipmentSummaryLabel.setText("Selecione um equipamento para ver saldo, status e localizacao atual.");
        hideProtocolActions();
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
            MovementType selectedMovementType = movementTypeComboBox.getValue();
            Long selectedEquipmentId = movementEquipmentComboBox.getValue() == null ? null : movementEquipmentComboBox.getValue().id();

            if (selectedMovementType == MovementType.ENTREGA_FUNCIONARIO) {
                DeliveryProtocolResultDto deliveryResult = registerDeliveryWithProtocol();
                if (deliveryResult == null) {
                    return;
                }

                showProtocolActions(deliveryResult.outputPath());
                formStatusLabel.setText("Entrega registrada com sucesso. Protocolo " + deliveryResult.protocolNumber() + " salvo em: " + deliveryResult.outputPath());
            } else if (selectedMovementType == MovementType.DEVOLUCAO_FUNCIONARIO) {
                ReturnProtocolResultDto returnResult = registerReturnWithProtocol();
                if (returnResult == null) {
                    return;
                }

                showProtocolActions(returnResult.outputPath());
                formStatusLabel.setText("Devolucao registrada com sucesso. Protocolo " + returnResult.protocolNumber() + " salvo em: " + returnResult.outputPath());
            } else {
                StockMovementListItemDto registeredMovement = stockMovementService.registerMovement(
                        buildCreateRequest(),
                        UserSession.requireAuthenticatedUser()
                );
                formStatusLabel.setText("Movimentacao registrada com sucesso para o equipamento " + registeredMovement.equipmentInternalCode() + ".");
            }

            applyStatusStyle("form-status-success");
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

    @FXML
    private void handleOpenProtocol() {
        if (lastGeneratedProtocolPath == null || !Files.exists(lastGeneratedProtocolPath)) {
            formStatusLabel.setText("Nenhum protocolo gerado foi encontrado para abrir.");
            applyStatusStyle("form-status-error");
            hideProtocolActions();
            return;
        }

        try {
            Desktop.getDesktop().open(lastGeneratedProtocolPath.toFile());
            formStatusLabel.setText("Protocolo aberto com sucesso.");
            applyStatusStyle("form-status-neutral");
        } catch (IOException | UnsupportedOperationException exception) {
            formStatusLabel.setText("Nao foi possivel abrir o protocolo no aplicativo padrao do Windows.");
            applyStatusStyle("form-status-error");
        }
    }

    @FXML
    private void handlePrintProtocol() {
        if (lastGeneratedProtocolPath == null || !Files.exists(lastGeneratedProtocolPath)) {
            formStatusLabel.setText("Nenhum protocolo gerado foi encontrado para imprimir.");
            applyStatusStyle("form-status-error");
            hideProtocolActions();
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.PRINT)) {
                desktop.print(lastGeneratedProtocolPath.toFile());
                formStatusLabel.setText("Comando de impressao enviado para o Windows.");
            } else {
                desktop.open(lastGeneratedProtocolPath.toFile());
                formStatusLabel.setText("A impressao direta nao esta disponivel. O protocolo foi aberto para impressao manual.");
            }
            applyStatusStyle("form-status-neutral");
        } catch (IOException | UnsupportedOperationException exception) {
            formStatusLabel.setText("Nao foi possivel imprimir o protocolo pelo Windows.");
            applyStatusStyle("form-status-error");
        }
    }

    private void attachListeners() {
        movementEquipmentComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateEquipmentSummary();
            applyMovementTypeRules();
        });
        movementTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyMovementTypeRules());
        returnReasonComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateReturnReasonFieldState());
    }

    private void configureTable() {
        movementAtColumn.setCellValueFactory(data -> new SimpleStringProperty(MOVEMENT_FORMATTER.format(data.getValue().movementAt())));
        movementTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().movementType().getDisplayName()));
        movementTypeColumn.setCellFactory(UiSupport.badgeCellFactory(this::resolveMovementBadgeStyle));
        equipmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().equipmentInternalCode() + " - " + data.getValue().equipmentName()));
        quantityColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().quantity())));
        sourceColumn.setCellValueFactory(data -> new SimpleStringProperty(defaultValue(data.getValue().sourceLocationName())));
        destinationColumn.setCellValueFactory(data -> new SimpleStringProperty(defaultValue(data.getValue().destinationLocationName())));
        responsibleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().responsibleName()));
        performedByColumn.setCellValueFactory(data -> new SimpleStringProperty("@" + data.getValue().performedByUsername()));
        movementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        movementTable.setPlaceholder(UiSupport.createTablePlaceholder(
                "Nenhuma movimentacao encontrada",
                "Experimente ampliar o periodo ou limpar os filtros para revisar o historico recente."
        ));
        movementTable.setRowFactory(ignored -> new TableRow<>() {
            @Override
            protected void updateItem(StockMovementListItemDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("table-row-warning", "table-row-danger", "table-row-info", "table-row-muted");

                if (empty || item == null) {
                    return;
                }

                switch (item.movementType()) {
                    case BAIXA_DESCARTE -> getStyleClass().add("table-row-danger");
                    case ENVIO_MANUTENCAO, RETORNO_MANUTENCAO -> getStyleClass().add("table-row-warning");
                    case TRANSFERENCIA, ENTREGA_FUNCIONARIO, DEVOLUCAO_FUNCIONARIO -> getStyleClass().add("table-row-info");
                    case SAIDA -> getStyleClass().add("table-row-muted");
                    default -> {
                    }
                }
            }
        });
    }

    private void configureQuantityField() {
        UnaryOperator<TextFormatter.Change> numericFilter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        quantityField.setTextFormatter(new TextFormatter<>(numericFilter));
    }

    private void configureCpfFields() {
        configureCpfField(recipientCpfField);
        configureCpfField(returnEmployeeCpfField);
        configureCpfField(companyReceiverCpfField);
    }

    private void configureCpfField(TextField field) {
        UnaryOperator<TextFormatter.Change> cpfFilter = change -> change.getControlNewText().matches("[0-9.\\-]{0,14}") ? change : null;
        field.setTextFormatter(new TextFormatter<>(cpfFilter));
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
        openProtocolButton.setDisable(!canManageInventory);
        printProtocolButton.setDisable(!canManageInventory);
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
        updateResultsSummary(movements.size());
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

    private DeliveryProtocolCreateRequest buildDeliveryRequest() {
        return new DeliveryProtocolCreateRequest(
                movementEquipmentComboBox.getValue() == null ? null : movementEquipmentComboBox.getValue().id(),
                parseRequiredInteger(quantityField.getText()),
                deliveryDestinationField.getText(),
                responsibleNameField.getText(),
                recipientCpfField.getText(),
                recipientRoleField.getText(),
                buildMovementAt(),
                notesArea.getText()
        );
    }

    private ReturnProtocolCreateRequest buildReturnRequest() {
        return new ReturnProtocolCreateRequest(
                movementEquipmentComboBox.getValue() == null ? null : movementEquipmentComboBox.getValue().id(),
                parseRequiredInteger(quantityField.getText()),
                destinationLocationComboBox.getValue() == null ? null : destinationLocationComboBox.getValue().id(),
                returnEmployeeNameField.getText(),
                returnEmployeeCpfField.getText(),
                responsibleNameField.getText(),
                companyReceiverRoleField.getText(),
                companyReceiverCpfField.getText(),
                returnReasonComboBox.getValue(),
                returnReasonOtherField.getText(),
                buildMovementAt(),
                notesArea.getText()
        );
    }

    private DeliveryProtocolResultDto registerDeliveryWithProtocol() {
        File selectedFile = chooseProtocolOutputFile(MovementType.ENTREGA_FUNCIONARIO);
        if (selectedFile == null) {
            formStatusLabel.setText("Geracao do protocolo cancelada pelo usuario.");
            applyStatusStyle("form-status-neutral");
            return null;
        }

        return deliveryProtocolService.registerDelivery(
                buildDeliveryRequest(),
                selectedFile.toPath(),
                UserSession.requireAuthenticatedUser()
        );
    }

    private ReturnProtocolResultDto registerReturnWithProtocol() {
        File selectedFile = chooseProtocolOutputFile(MovementType.DEVOLUCAO_FUNCIONARIO);
        if (selectedFile == null) {
            formStatusLabel.setText("Geracao do protocolo cancelada pelo usuario.");
            applyStatusStyle("form-status-neutral");
            return null;
        }

        return returnProtocolService.registerReturn(
                buildReturnRequest(),
                selectedFile.toPath(),
                UserSession.requireAuthenticatedUser()
        );
    }

    private File chooseProtocolOutputFile(MovementType movementType) {
        FileChooser fileChooser = new FileChooser();
        MovementEquipmentOptionDto selectedEquipment = movementEquipmentComboBox.getValue();

        if (movementType == MovementType.ENTREGA_FUNCIONARIO) {
            fileChooser.setTitle("Salvar protocolo de entrega");
            fileChooser.setInitialFileName(deliveryProtocolService.buildDefaultFileName(
                    selectedEquipment == null ? null : selectedEquipment.internalCode(),
                    responsibleNameField.getText(),
                    movementDatePicker.getValue()
            ));
        } else if (movementType == MovementType.DEVOLUCAO_FUNCIONARIO) {
            fileChooser.setTitle("Salvar protocolo de devolucao");
            fileChooser.setInitialFileName(returnProtocolService.buildDefaultFileName(
                    selectedEquipment == null ? null : selectedEquipment.internalCode(),
                    returnEmployeeNameField.getText(),
                    movementDatePicker.getValue()
            ));
        } else {
            return null;
        }

        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Documento Word (*.docx)", "*.docx")
        );
        return fileChooser.showSaveDialog(registerMovementButton.getScene().getWindow());
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

        boolean deliveryWithProtocol = movementType == MovementType.ENTREGA_FUNCIONARIO;
        boolean returnWithProtocol = movementType == MovementType.DEVOLUCAO_FUNCIONARIO;
        boolean protocolMovement = deliveryWithProtocol || returnWithProtocol;
        boolean sourceRequired = movementType != null && movementType.requiresSourceLocation();
        boolean destinationRequired = movementType != null && movementType.requiresDestinationLocation();
        LookupOptionDto currentLocation = selectedEquipment == null ? null : findLocationById(selectedEquipment.locationId());
        boolean lockDestinationToCurrentLocation = movementType == MovementType.ENTRADA
                && selectedEquipment != null
                && selectedEquipment.quantity() > 0
                && currentLocation != null;
        boolean useTextDestination = deliveryWithProtocol;

        sourceLocationComboBox.setDisable(!sourceRequired);
        destinationLocationComboBox.setManaged(!useTextDestination);
        destinationLocationComboBox.setVisible(!useTextDestination);
        destinationLocationComboBox.setDisable(useTextDestination || !destinationRequired || lockDestinationToCurrentLocation);
        deliveryDestinationField.setManaged(useTextDestination);
        deliveryDestinationField.setVisible(useTextDestination);
        deliveryDestinationField.setDisable(!useTextDestination);

        if (sourceRequired && currentLocation != null) {
            sourceLocationComboBox.getSelectionModel().select(currentLocation);
        } else {
            sourceLocationComboBox.getSelectionModel().clearSelection();
        }

        if (!destinationRequired) {
            destinationLocationComboBox.getSelectionModel().clearSelection();
        } else if (lockDestinationToCurrentLocation) {
            destinationLocationComboBox.getSelectionModel().select(currentLocation);
        } else if (deliveryWithProtocol && currentLocation != null && destinationLocationComboBox.getValue() == null) {
            destinationLocationComboBox.getSelectionModel().select(currentLocation);
        }

        protocolFieldsContainer.setManaged(protocolMovement);
        protocolFieldsContainer.setVisible(protocolMovement);
        deliveryProtocolFieldsContainer.setManaged(deliveryWithProtocol);
        deliveryProtocolFieldsContainer.setVisible(deliveryWithProtocol);
        returnProtocolFieldsContainer.setManaged(returnWithProtocol);
        returnProtocolFieldsContainer.setVisible(returnWithProtocol);

        quantityField.setEditable(!protocolMovement);
        quantityField.setPromptText(protocolMovement ? "Saldo total do registro" : "Ex.: 1");
        responsibleNameLabel.setText(returnWithProtocol ? "Responsavel da empresa" : "Responsavel / colaborador");
        responsibleNameField.setPromptText(deliveryWithProtocol
                ? "Nome completo do colaborador que assumira a responsabilidade"
                : returnWithProtocol
                ? "Nome completo de quem recebeu a devolucao"
                : "Ex.: Equipe Infra ou colaborador responsavel");

        protocolSectionCaptionLabel.setText(deliveryWithProtocol
                ? "Dados obrigatorios para gerar o protocolo de entrega em DOCX, incluindo o destino digitado."
                : returnWithProtocol
                ? "Dados obrigatorios para gerar o protocolo de devolucao em DOCX."
                : "Os campos do protocolo aparecem de acordo com o tipo selecionado.");

        registerMovementButton.setText(protocolMovement ? "Gerar protocolo e registrar" : "Registrar movimentacao");
        protocolFlowHintLabel.setText(deliveryWithProtocol
                ? "Preencha nome, CPF, cargo e o destino em texto livre. Ao clicar em gerar, o sistema vai pedir onde salvar o DOCX do protocolo de entrega."
                : returnWithProtocol
                ? "Preencha os dados de quem devolve, de quem recebeu na empresa e o motivo. Ao clicar em gerar, o sistema vai pedir onde salvar o DOCX do protocolo de devolucao."
                : "Para gerar protocolo, escolha 'Entrega com protocolo' ou 'Devolucao com protocolo'. Os campos do documento aparecem automaticamente.");

        if (protocolMovement) {
            if (selectedEquipment != null) {
                quantityField.setText(String.valueOf(selectedEquipment.quantity()));
            } else {
                quantityField.clear();
            }
        }

        if (returnWithProtocol) {
            if (selectedEquipment != null && selectedEquipment.responsibleName() != null && !selectedEquipment.responsibleName().isBlank()) {
                returnEmployeeNameField.setText(selectedEquipment.responsibleName());
            }
        }

        updateReturnReasonFieldState();
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
            case ENTREGA_FUNCIONARIO -> "Entregas com protocolo transferem o saldo total do registro para o colaborador, usam o destino digitado no protocolo, mantem o controle interno no local atual e geram o DOCX oficial para assinatura.";
            case DEVOLUCAO_FUNCIONARIO -> "Devolucoes com protocolo exigem item em uso, retornam o saldo total para uma localizacao da empresa, voltam o status para Disponivel e geram o DOCX oficial de recebimento.";
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
        deliveryDestinationField.clear();
        responsibleNameField.clear();
        recipientCpfField.clear();
        recipientRoleField.clear();
        returnEmployeeNameField.clear();
        returnEmployeeCpfField.clear();
        companyReceiverRoleField.clear();
        companyReceiverCpfField.clear();
        returnReasonComboBox.getSelectionModel().clearSelection();
        returnReasonOtherField.clear();
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

    private void showProtocolActions(Path protocolPath) {
        lastGeneratedProtocolPath = protocolPath;
        generatedProtocolPathLabel.setText(protocolPath.toAbsolutePath().toString());
        protocolActionsContainer.setManaged(true);
        protocolActionsContainer.setVisible(true);
        openProtocolButton.setDisable(false);
        printProtocolButton.setDisable(false);
    }

    private void hideProtocolActions() {
        lastGeneratedProtocolPath = null;
        generatedProtocolPathLabel.setText("Nenhum protocolo foi gerado nesta sessao.");
        protocolActionsContainer.setManaged(false);
        protocolActionsContainer.setVisible(false);
        openProtocolButton.setDisable(true);
        printProtocolButton.setDisable(true);
    }

    private void updateReturnReasonFieldState() {
        boolean otherReasonSelected = returnReasonComboBox.getValue() == ReturnProtocolReason.OUTROS;
        returnReasonOtherField.setDisable(!otherReasonSelected);
        returnReasonOtherField.setPromptText(otherReasonSelected
                ? "Descreva o motivo da devolucao"
                : "Selecione 'Outros' para detalhar");
        if (!otherReasonSelected) {
            returnReasonOtherField.clear();
        }
    }

    private void applyStatusStyle(String styleClass) {
        formStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        formStatusLabel.getStyleClass().add(styleClass);
    }

    private void updateResultsSummary(int resultCount) {
        if (resultCount == 0) {
            resultsSummaryLabel.setText("Nenhuma movimentacao no recorte atual");
            return;
        }

        resultsSummaryLabel.setText(resultCount == 1
                ? "1 evento operacional exibido"
                : resultCount + " eventos operacionais exibidos");
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

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "Nao informado" : value;
    }
}
