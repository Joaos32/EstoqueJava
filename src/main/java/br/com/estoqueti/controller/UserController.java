package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.service.UserService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ResponsiveLayoutSupport;
import br.com.estoqueti.util.UiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserController {

    private static final DateTimeFormatter LAST_LOGIN_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserService userService = new UserService();
    private AuthenticatedUserDto authenticatedUser;

    @FXML
    private GridPane userLayoutPane;

    @FXML
    private VBox usersTableCard;

    @FXML
    private VBox userFormCard;

    @FXML
    private TableView<UserListItemDto> userTable;

    @FXML
    private TableColumn<UserListItemDto, String> fullNameColumn;

    @FXML
    private TableColumn<UserListItemDto, String> usernameColumn;

    @FXML
    private TableColumn<UserListItemDto, String> roleColumn;

    @FXML
    private TableColumn<UserListItemDto, String> statusColumn;

    @FXML
    private TableColumn<UserListItemDto, String> lastLoginColumn;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Button saveUserButton;

    @FXML
    private Label formStatusLabel;

    @FXML
    private Label accessHintLabel;

    @FXML
    private Label resultsSummaryLabel;

    @FXML
    public void initialize() {
        authenticatedUser = UserSession.requireAuthenticatedUser();
        configureTable();
        roleComboBox.setItems(FXCollections.observableArrayList(Role.values()));
        roleComboBox.getSelectionModel().select(Role.TECNICO);
        activeCheckBox.setSelected(true);
        formStatusLabel.setText("Cadastre usuarios com perfis compativeis com a rotina da equipe.");
        applyStatusStyle("form-status-neutral");
        ResponsiveLayoutSupport.configureResponsiveSplit(userLayoutPane, usersTableCard, 58, 520, userFormCard, 42, 380, 1120);
        configurePermissions();
        if (authenticatedUser.canManageUsers()) {
            loadUsers();
        } else {
            userTable.setDisable(true);
            userTable.setPlaceholder(UiSupport.createTablePlaceholder(
                    "Acesso restrito",
                    "Somente administradores podem visualizar os usuarios cadastrados."
            ));
            resultsSummaryLabel.setText("Consulta de usuarios restrita a administradores");
        }
    }

    @FXML
    private void handleCreateUser() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            formStatusLabel.setText("A confirmacao da senha nao confere.");
            applyStatusStyle("form-status-error");
            return;
        }

        try {
            UserListItemDto createdUser = userService.createUser(
                    new UserCreateRequest(
                            fullNameField.getText(),
                            usernameField.getText(),
                            passwordField.getText(),
                            roleComboBox.getValue(),
                            activeCheckBox.isSelected()
                    ),
                    authenticatedUser
            );

            formStatusLabel.setText("Usuario cadastrado com sucesso: " + createdUser.username());
            applyStatusStyle("form-status-success");
            clearForm();
            loadUsers();
        } catch (BusinessException exception) {
            formStatusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
        formStatusLabel.setText("Formulario limpo. Informe os dados do novo usuario.");
        applyStatusStyle("form-status-neutral");
    }

    private void configureTable() {
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role().getDisplayName()));
        roleColumn.setCellFactory(UiSupport.badgeCellFactory(this::resolveRoleBadgeStyle));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().active() ? "Ativo" : "Inativo"));
        statusColumn.setCellFactory(UiSupport.badgeCellFactory(this::resolveStatusBadgeStyle));
        lastLoginColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().lastLoginAt() == null ? "Nunca acessou" : LAST_LOGIN_FORMATTER.format(data.getValue().lastLoginAt())
        ));
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        userTable.setPlaceholder(UiSupport.createTablePlaceholder(
                "Nenhum usuario disponivel",
                "Assim que novos acessos forem cadastrados, eles aparecerao nesta grade."
        ));
        userTable.setRowFactory(ignored -> new TableRow<>() {
            @Override
            protected void updateItem(UserListItemDto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("table-row-muted", "table-row-info");

                if (empty || item == null) {
                    return;
                }

                if (!item.active()) {
                    getStyleClass().add("table-row-muted");
                } else if (item.role() == Role.ADMIN) {
                    getStyleClass().add("table-row-info");
                }
            }
        });
    }

    private void configurePermissions() {
        boolean canManageUsers = authenticatedUser.canManageUsers();

        accessHintLabel.setText(canManageUsers
                ? "Voce esta autenticado como administrador e pode cadastrar novos usuarios."
                : "Seu perfil nao possui permissao para consultar nem cadastrar usuarios.");

        fullNameField.setDisable(!canManageUsers);
        usernameField.setDisable(!canManageUsers);
        roleComboBox.setDisable(!canManageUsers);
        passwordField.setDisable(!canManageUsers);
        confirmPasswordField.setDisable(!canManageUsers);
        activeCheckBox.setDisable(!canManageUsers);
        saveUserButton.setDisable(!canManageUsers);
    }

    private void loadUsers() {
        List<UserListItemDto> users = userService.listUsers(authenticatedUser);
        userTable.setItems(FXCollections.observableArrayList(users));
        updateResultsSummary(users.size());
    }

    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleComboBox.getSelectionModel().select(Role.TECNICO);
        activeCheckBox.setSelected(true);
    }

    private void applyStatusStyle(String styleClass) {
        formStatusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        formStatusLabel.getStyleClass().add(styleClass);
    }

    private void updateResultsSummary(int resultCount) {
        if (resultCount == 0) {
            resultsSummaryLabel.setText("Nenhum acesso cadastrado ate o momento");
            return;
        }

        resultsSummaryLabel.setText(resultCount == 1
                ? "1 perfil ativo na operacao"
                : resultCount + " perfis ativos na operacao");
    }

    private String resolveRoleBadgeStyle(String role) {
        return switch (role) {
            case "Administrador" -> "badge-danger";
            case "Tecnico" -> "badge-info";
            case "Visualizador" -> "badge-neutral";
            default -> "badge-neutral";
        };
    }

    private String resolveStatusBadgeStyle(String status) {
        return "Ativo".equals(status) ? "badge-success" : "badge-muted";
    }
}