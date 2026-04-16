package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.equipment.EquipmentCreateRequest;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentReferenceDataDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.service.EquipmentService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ResponsiveLayoutSupport;
import br.com.estoqueti.util.UiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class EquipmentController {

    private static final DateTimeFormatter ENTRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EquipmentService equipmentService = new EquipmentService();
    private AuthenticatedUserDto authenticatedUser;

    @FXML
    private GridPane equipmentLayoutPane;

    @FXML
    private VBox catalogCard;

    @FXML
    private VBox formCard;

    @FXML
    private TableView<EquipmentListItemDto> equipmentTable;

    @FXML
    private TableColumn<EquipmentListItemDto, String> internalCodeColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> nameColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> categoryColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> statusColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> quantityColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> minimumStockColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> locationColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> responsibleColumn;

    @FXML
    private TableColumn<EquipmentListItemDto, String> entryDateColumn;

    @FXML
    private TextField nameFilterField;

    @FXML
    private TextField internalCodeFilterField;

    @FXML
    private TextField patrimonyFilterField;

    @FXML
    private TextField serialNumberFilterField;

    @FXML
    private ComboBox<LookupOptionDto> categoryFilterComboBox;

    @FXML
    private ComboBox<EquipmentStatus> statusFilterComboBox;

    @FXML
    private ComboBox<LookupOptionDto> locationFilterComboBox;

    @FXML
    private TextField responsibleFilterField;

    @FXML
    private Label accessHintLabel;

    @FXML
    private Label resultsSummaryLabel;

    @FXML
    private VBox formFieldsContainer;

    @FXML
    private TextField internalCodeField;

    @FXML
    private TextField equipmentNameField;

    @FXML
    private ComboBox<LookupOptionDto> categoryComboBox;

    @FXML
    private TextField brandField;

    @FXML
    private TextField modelField;

    @FXML
    private TextField serialNumberField;

    @FXML
    private TextField patrimonyNumberField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField minimumStockField;

    @FXML
    private ComboBox<EquipmentStatus> statusComboBox;

    @FXML
    private ComboBox<LookupOptionDto> locationComboBox;

    @FXML
    private TextField responsibleNameField;

    @FXML
    private ComboBox<LookupOptionDto> supplierComboBox;

    @FXML
    private DatePicker entryDatePicker;

    @FXML
    private TextArea notesArea;

    @FXML
    private Button saveEquipmentButton;

    @FXML
    private Button removeEquipmentButton;

    @FXML
    private Label formStatusLabel;

    @FXML
    public void initialize() {
        authenticatedUser = UserSession.requireAuthenticatedUser();
        configureTable();
        configureSelectionActions();
        configureNumericField(quantityField);
        configureNumericField(minimumStockField);
        statusComboBox.setItems(FXCollections.observableArrayList(EquipmentStatus.values()));
        statusFilterComboBox.setItems(FXCollections.observableArrayList(EquipmentStatus.values()));
        statusComboBox.getSelectionModel().select(EquipmentStatus.DISPONIVEL);
        entryDatePicker.setValue(LocalDate.now());
        formStatusLabel.setText("Cadastre ativos e perifericos com dados completos para manter o estoque consistente.");
        applyStatusStyle("form-status-neutral");
        ResponsiveLayoutSupport.configureResponsiveSplit(equipmentLayoutPane, catalogCard, 61, 540, formCard, 39, 370, 1200);
        loadReferenceData();
        configurePermissions();
        loadEquipment();
    }

    @FXML
    private void handleSearch() {
        loadEquipment();
    }

    @FXML
    private void handleClearFilters() {
        nameFilterField.clear();
        internalCodeFilterField.clear();
        patrimonyFilterField.clear();
        serialNumberFilterField.clear();
        responsibleFilterField.clear();
        categoryFilterComboBox.getSelectionModel().clearSelection();
        statusFilterComboBox.getSelectionModel().clearSelection();
        locationFilterComboBox.getSelectionModel().clearSelection();
        loadEquipment();
    }

    @FXML
    private void handleCreateEquipment() {
        try {
            EquipmentListItemDto createdEquipment = equipmentService.createEquipment(buildCreateRequest(), authenticatedUser);
            formStatusLabel.setText("Equipamento cadastrado com sucesso: " + createdEquipment.internalCode());
            applyStatusStyle("form-status-success");
            clearForm();
            loadEquipment();
        } catch (BusinessException exception) {
            formStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        }
    }

    @FXML
    private void handleRemoveEquipment() {
        EquipmentListItemDto selectedEquipment = equipmentTable.getSelectionModel().getSelectedItem();
        if (selectedEquipment == null) {
            formStatusLabel.setText("Selecione um equipamento para excluir.");
            applyStatusStyle("form-status-error");
            return;
        }
        if (selectedEquipment.quantity() > 0) {
            formStatusLabel.setText("Somente equipamentos com quantidade zero podem ser excluidos do catalogo.");
            applyStatusStyle("form-status-error");
            return;
        }
        if (!showConfirmationDialog(
                "Excluir equipamento",
                "Remover " + selectedEquipment.internalCode() + " do catalogo?",
                "O equipamento sera marcado como inativo e deixara de aparecer na consulta principal."
        )) {
            return;
        }

        try {
            EquipmentListItemDto removedEquipment = equipmentService.deactivateEquipment(selectedEquipment.id(), authenticatedUser);
            formStatusLabel.setText("Equipamento excluido do catalogo: " + removedEquipment.internalCode());
            applyStatusStyle("form-status-success");
            loadEquipment();
        } catch (BusinessException exception) {
            formStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
        formStatusLabel.setText("Formulario limpo. Informe os dados do novo equipamento.");
        applyStatusStyle("form-status-neutral");
    }

    private void configureTable() {
        internalCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().internalCode()));
        internalCodeColumn.setStyle("-fx-alignment: CENTER;");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryName()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().getDisplayName()));
        statusColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setCellFactory(UiSupport.centeredBadgeCellFactory(this::resolveEquipmentStatusStyle));
        quantityColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().quantity())));
        quantityColumn.setStyle("-fx-alignment: CENTER;");
        quantityColumn.setCellFactory(UiSupport.centeredTextCellFactory());
        minimumStockColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().minimumStock())));
        minimumStockColumn.setStyle("-fx-alignment: CENTER;");
        minimumStockColumn.setCellFactory(UiSupport.centeredTextCellFactory());
        locationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().locationName()));
        responsibleColumn.setCellValueFactory(data -> new SimpleStringProperty(defaultValue(data.getValue().responsibleName())));
        entryDateColumn.setCellValueFactory(data -> new SimpleStringProperty(ENTRY_DATE_FORMATTER.format(data.getValue().entryDate())));
        entryDateColumn.setStyle("-fx-alignment: CENTER;");
        entryDateColumn.setCellFactory(UiSupport.centeredTextCellFactory());
        equipmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        equipmentTable.setPlaceholder(UiSupport.createTablePlaceholder(
                "Nenhum equipamento encontrado",
                "Ajuste os filtros ou cadastre um novo ativo para preencher esta lista."
        ));
        equipmentTable.setRowFactory(ignored -> new TableRow<>() {
            @Override
            protected void updateItem(EquipmentListItemDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("table-row-warning", "table-row-danger", "table-row-muted");

                if (empty || item == null) {
                    return;
                }

                if (item.status() == EquipmentStatus.DESCARTADO || item.status() == EquipmentStatus.DEFEITUOSO) {
                    getStyleClass().add("table-row-danger");
                } else if (item.minimumStock() > 0 && item.quantity() < item.minimumStock()) {
                    getStyleClass().add("table-row-warning");
                } else if (item.status() == EquipmentStatus.EM_MANUTENCAO) {
                    getStyleClass().add("table-row-muted");
                }
            }
        });
    }

    private void configureSelectionActions() {
        equipmentTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateRemoveButtonState());
    }

    private void configurePermissions() {
        boolean canManageInventory = authenticatedUser.canManageInventory();

        accessHintLabel.setText(canManageInventory
                ? "Seu perfil pode consultar, cadastrar e excluir equipamentos do catalogo quando eles nao tiverem saldo em estoque."
                : "Seu perfil possui acesso somente de consulta para equipamentos.");

        formFieldsContainer.setDisable(!canManageInventory);
        saveEquipmentButton.setDisable(!canManageInventory);
        updateRemoveButtonState();
    }

    private void configureNumericField(TextField textField) {
        UnaryOperator<TextFormatter.Change> numericFilter = change -> change.getControlNewText().matches("\\d*") ? change : null;
        textField.setTextFormatter(new TextFormatter<>(numericFilter));
    }

    private void loadReferenceData() {
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();
        List<LookupOptionDto> categories = referenceData.categories();
        List<LookupOptionDto> locations = referenceData.locations();
        List<LookupOptionDto> suppliers = referenceData.suppliers();

        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(categories));
        locationComboBox.setItems(FXCollections.observableArrayList(locations));
        locationFilterComboBox.setItems(FXCollections.observableArrayList(locations));
        supplierComboBox.setItems(FXCollections.observableArrayList(suppliers));
    }

    private void loadEquipment() {
        List<EquipmentListItemDto> equipments = equipmentService.searchEquipment(buildSearchFilter());
        equipmentTable.setItems(FXCollections.observableArrayList(equipments));
        updateResultsSummary(equipments.size());
        updateRemoveButtonState();
    }

    private EquipmentSearchFilter buildSearchFilter() {
        return new EquipmentSearchFilter(
                nameFilterField.getText(),
                internalCodeFilterField.getText(),
                patrimonyFilterField.getText(),
                serialNumberFilterField.getText(),
                selectedId(categoryFilterComboBox),
                statusFilterComboBox.getValue(),
                selectedId(locationFilterComboBox),
                responsibleFilterField.getText()
        );
    }

    private EquipmentCreateRequest buildCreateRequest() {
        return new EquipmentCreateRequest(
                internalCodeField.getText(),
                equipmentNameField.getText(),
                selectedId(categoryComboBox),
                brandField.getText(),
                modelField.getText(),
                serialNumberField.getText(),
                patrimonyNumberField.getText(),
                parseInteger(quantityField.getText(), "quantidade"),
                parseInteger(minimumStockField.getText(), "estoque minimo"),
                statusComboBox.getValue(),
                selectedId(locationComboBox),
                responsibleNameField.getText(),
                selectedId(supplierComboBox),
                entryDatePicker.getValue(),
                notesArea.getText()
        );
    }

    private Integer parseInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException("Informe um valor numerico valido para " + fieldName + ".");
        }
    }

    private Long selectedId(ComboBox<LookupOptionDto> comboBox) {
        return comboBox.getValue() == null ? null : comboBox.getValue().id();
    }

    private void clearForm() {
        internalCodeField.clear();
        equipmentNameField.clear();
        brandField.clear();
        modelField.clear();
        serialNumberField.clear();
        patrimonyNumberField.clear();
        quantityField.clear();
        minimumStockField.clear();
        responsibleNameField.clear();
        notesArea.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().select(EquipmentStatus.DISPONIVEL);
        locationComboBox.getSelectionModel().clearSelection();
        supplierComboBox.getSelectionModel().clearSelection();
        entryDatePicker.setValue(LocalDate.now());
    }

    private void applyStatusStyle(String styleClass) {
        formStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        formStatusLabel.getStyleClass().add(styleClass);
    }

    private void updateResultsSummary(int resultCount) {
        if (resultCount == 0) {
            resultsSummaryLabel.setText("Sem resultados com os filtros atuais");
            return;
        }

        resultsSummaryLabel.setText(resultCount == 1
                ? "1 equipamento em destaque"
                : resultCount + " equipamentos em destaque");
    }

    private void updateRemoveButtonState() {
        boolean canManageInventory = authenticatedUser != null && authenticatedUser.canManageInventory();
        removeEquipmentButton.setDisable(!canManageInventory || equipmentTable.getSelectionModel().getSelectedItem() == null);
    }

    private boolean showConfirmationDialog(String title, String header, String content) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle(title);
        confirmationAlert.setHeaderText(header);
        confirmationAlert.setContentText(content);

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String resolveEquipmentStatusStyle(String status) {
        return switch (status) {
            case "Disponivel" -> "badge-success";
            case "Em uso" -> "badge-info";
            case "Em manutencao" -> "badge-warning";
            case "Defeituoso" -> "badge-danger";
            case "Descartado" -> "badge-muted";
            default -> "badge-neutral";
        };
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "Nao informado" : value;
    }
}
