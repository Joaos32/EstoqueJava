package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;

public class MainLayoutController {

    @FXML
    private Label currentUserNameLabel;

    @FXML
    private Label currentUsernameLabel;

    @FXML
    private Label currentUserRoleLabel;

    @FXML
    private Label contentTitleLabel;

    @FXML
    private Button dashboardMenuButton;

    @FXML
    private Button equipmentMenuButton;

    @FXML
    private Button movementMenuButton;

    @FXML
    private Button usersMenuButton;

    @FXML
    private Button reportsMenuButton;

    @FXML
    private Button logoutButton;

    @FXML
    private StackPane contentContainer;

    @FXML
    public void initialize() {
        AuthenticatedUserDto authenticatedUser = UserSession.requireAuthenticatedUser();
        currentUserNameLabel.setText(authenticatedUser.fullName());
        currentUsernameLabel.setText("@" + authenticatedUser.username());
        currentUserRoleLabel.setText(authenticatedUser.role().getDisplayName());
        showDashboardModule();
    }

    @FXML
    private void handleDashboardMenu() {
        contentTitleLabel.setText("Dashboard");
        setActiveMenu(dashboardMenuButton);
        Parent content = ViewManager.loadView(ViewManager.DASHBOARD_VIEW);
        contentContainer.getChildren().setAll(content);
    }

    @FXML
    private void handleEquipmentMenu() {
        contentTitleLabel.setText("Equipamentos");
        setActiveMenu(equipmentMenuButton);
        Parent content = ViewManager.loadView(ViewManager.EQUIPMENT_VIEW);
        contentContainer.getChildren().setAll(content);
    }

    @FXML
    private void handleMovementMenu() {
        contentTitleLabel.setText("Movimentacoes");
        setActiveMenu(movementMenuButton);
        Parent content = ViewManager.loadView(ViewManager.MOVEMENT_VIEW);
        contentContainer.getChildren().setAll(content);
    }

    @FXML
    private void handleUsersMenu() {
        contentTitleLabel.setText("Usuarios");
        setActiveMenu(usersMenuButton);
        Parent content = ViewManager.loadView(ViewManager.USER_VIEW);
        contentContainer.getChildren().setAll(content);
    }

    @FXML
    private void handleReportsMenu() {
        contentTitleLabel.setText("Relatorios");
        setActiveMenu(reportsMenuButton);
        Parent content = ViewManager.loadView(ViewManager.REPORT_VIEW);
        contentContainer.getChildren().setAll(content);
    }

    @FXML
    private void handleLogout() {
        UserSession.logout();
        ViewManager.showLogin(logoutButton);
    }

    private void showDashboardModule() {
        handleDashboardMenu();
    }

    private void setActiveMenu(Button activeButton) {
        List<Button> menuButtons = List.of(
                dashboardMenuButton,
                equipmentMenuButton,
                movementMenuButton,
                usersMenuButton,
                reportsMenuButton
        );

        menuButtons.forEach(button -> button.getStyleClass().remove("active"));
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }
}