package br.com.estoqueti.controller;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.service.AuthenticationService;
import br.com.estoqueti.service.UserService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ViewManager;
import br.com.estoqueti.util.WorkstationUtils;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class LoginController {

    private static final String DEFAULT_STATUS_MESSAGE = "Informe suas credenciais para acessar o sistema.";
    private static final String CONFIG_HINT_MESSAGE = "Banco sem senha configurada no app. Se o PostgreSQL exigir senha, preencha config/application-local.properties ou app/config/application-local.properties antes do primeiro login.";
    private static final String LOGIN_IN_PROGRESS_MESSAGE = "Validando acesso e conectando ao banco...";
    private static final String PASSWORD_RECOVERY_IN_PROGRESS_MESSAGE = "Validando o usuario e redefinindo a senha...";
    private static final String DEFAULT_LOGIN_BUTTON_TEXT = "Entrar";
    private static final String DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT = "Esqueci minha senha";

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final UserService userService = new UserService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button forgotPasswordButton;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        statusLabel.setText(resolveInitialStatusMessage());
        applyStatusStyle("form-status-neutral");
        loginButton.setText(DEFAULT_LOGIN_BUTTON_TEXT);
        forgotPasswordButton.setText(DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String workstation = WorkstationUtils.resolveStationIdentifier();

        setLoadingState(true, "Entrando...", DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT);
        statusLabel.setText(LOGIN_IN_PROGRESS_MESSAGE);
        applyStatusStyle("form-status-neutral");

        Task<AuthenticatedUserDto> loginTask = new Task<>() {
            @Override
            protected AuthenticatedUserDto call() {
                return authenticationService.authenticate(new LoginRequest(username, password, workstation));
            }
        };

        loginTask.setOnSucceeded(event -> {
            UserSession.login(loginTask.getValue());
            ViewManager.showMainLayout(loginButton);
        });

        loginTask.setOnFailed(event -> {
            statusLabel.setText(resolveErrorMessage(loginTask.getException()));
            applyStatusStyle("form-status-error");
            setLoadingState(false, DEFAULT_LOGIN_BUTTON_TEXT, DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT);
        });

        Thread worker = new Thread(loginTask, "login-auth-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleForgotPassword() {
        Optional<PasswordRecoveryRequest> recoveryRequest = showPasswordRecoveryDialog();
        if (recoveryRequest.isEmpty()) {
            return;
        }

        setLoadingState(true, DEFAULT_LOGIN_BUTTON_TEXT, "Recuperando...");
        statusLabel.setText(PASSWORD_RECOVERY_IN_PROGRESS_MESSAGE);
        applyStatusStyle("form-status-neutral");

        Task<UserListItemDto> recoveryTask = new Task<>() {
            @Override
            protected UserListItemDto call() {
                PasswordRecoveryRequest request = recoveryRequest.get();
                return userService.recoverPassword(
                        request.targetUsername(),
                        request.newPassword(),
                        WorkstationUtils.resolveStationIdentifier()
                );
            }
        };

        recoveryTask.setOnSucceeded(event -> {
            UserListItemDto recoveredUser = recoveryTask.getValue();
            usernameField.setText(recoveredUser.username());
            passwordField.clear();
            statusLabel.setText("Senha redefinida com sucesso para " + recoveredUser.username() + ". Agora faca login com a nova senha.");
            applyStatusStyle("form-status-success");
            setLoadingState(false, DEFAULT_LOGIN_BUTTON_TEXT, DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT);
        });

        recoveryTask.setOnFailed(event -> {
            statusLabel.setText(resolveErrorMessage(recoveryTask.getException()));
            applyStatusStyle("form-status-error");
            setLoadingState(false, DEFAULT_LOGIN_BUTTON_TEXT, DEFAULT_FORGOT_PASSWORD_BUTTON_TEXT);
        });

        Thread worker = new Thread(recoveryTask, "login-password-recovery-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private Optional<PasswordRecoveryRequest> showPasswordRecoveryDialog() {
        Dialog<PasswordRecoveryRequest> dialog = new Dialog<>();
        dialog.setTitle("Recuperar senha");
        dialog.setHeaderText("Redefina a senha informando apenas o login do usuario");
        dialog.initOwner(loginButton.getScene().getWindow());

        ButtonType confirmButtonType = new ButtonType("Redefinir senha", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(440);

        TextField targetUsernameField = new TextField(usernameField.getText());
        targetUsernameField.setPromptText("Login do usuario");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nova senha forte");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Repita a nova senha");

        Label guidanceLabel = new Label(
                "Se o usuario estiver ativo no banco, a senha sera redefinida imediatamente. Se estiver inativo, o sistema vai orientar a procurar o administrador."
        );
        guidanceLabel.setWrapText(true);

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().add("form-status-error");

        VBox content = new VBox(10,
                guidanceLabel,
                new Label("Usuario para recuperar"),
                targetUsernameField,
                new Label("Nova senha"),
                newPasswordField,
                new Label("Confirmacao da nova senha"),
                confirmPasswordField,
                errorLabel
        );
        dialog.getDialogPane().setContent(content);

        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (targetUsernameField.getText() == null || targetUsernameField.getText().isBlank()) {
                errorLabel.setText("Informe o login do usuario que tera a senha recuperada.");
                event.consume();
                return;
            }
            if (newPasswordField.getText() == null || newPasswordField.getText().isBlank()) {
                errorLabel.setText("Informe a nova senha para continuar.");
                event.consume();
                return;
            }
            if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                errorLabel.setText("A confirmacao da nova senha nao confere.");
                event.consume();
                return;
            }
            errorLabel.setText("");
        });

        dialog.setResultConverter(buttonType -> buttonType == confirmButtonType
                ? new PasswordRecoveryRequest(
                        targetUsernameField.getText().trim(),
                        newPasswordField.getText()
                )
                : null);
        return dialog.showAndWait();
    }

    private String resolveInitialStatusMessage() {
        if (ApplicationProperties.get("database.password", "").isBlank()) {
            return CONFIG_HINT_MESSAGE;
        }
        return DEFAULT_STATUS_MESSAGE;
    }

    private String resolveErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException && businessException.getMessage() != null) {
                return businessException.getMessage();
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return "Nao foi possivel concluir a operacao agora. Verifique a conexao com o banco e tente novamente.";
    }

    private void setLoadingState(boolean loading, String loginButtonText, String forgotPasswordButtonText) {
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loginButton.setDisable(loading);
        forgotPasswordButton.setDisable(loading);
        loginButton.setText(loginButtonText);
        forgotPasswordButton.setText(forgotPasswordButtonText);
    }

    private void applyStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        statusLabel.getStyleClass().add(styleClass);
    }

    private record PasswordRecoveryRequest(
            String targetUsername,
            String newPassword
    ) {
    }
}