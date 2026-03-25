package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.exception.BusinessException;
import br.com.estoqueti.service.AuthenticationService;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ViewManager;
import br.com.estoqueti.util.WorkstationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        statusLabel.setText("Informe suas credenciais para acessar o sistema.");
        applyStatusStyle("form-status-neutral");
    }

    @FXML
    private void handleLogin() {
        try {
            loginButton.setDisable(true);
            AuthenticatedUserDto authenticatedUser = authenticationService.authenticate(
                    new LoginRequest(
                            usernameField.getText(),
                            passwordField.getText(),
                            WorkstationUtils.resolveStationIdentifier()
                    )
            );

            UserSession.login(authenticatedUser);
            ViewManager.showMainLayout(loginButton);
        } catch (BusinessException exception) {
            statusLabel.setText(exception.getMessage());
            applyStatusStyle("form-status-error");
        } finally {
            loginButton.setDisable(false);
        }
    }

    private void applyStatusStyle(String styleClass) {
        statusLabel.getStyleClass().removeAll("form-status-neutral", "form-status-error", "form-status-success");
        statusLabel.getStyleClass().add(styleClass);
    }
}
