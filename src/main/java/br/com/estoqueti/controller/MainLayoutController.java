package br.com.estoqueti.controller;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ViewManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.InputEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

public class MainLayoutController {

    private static final Duration CONTENT_TRANSITION_DURATION = Duration.millis(220);
    private static final Duration SCROLL_ANIMATION_DURATION = Duration.millis(180);
    private static final Duration SESSION_CHECK_INTERVAL = Duration.seconds(20);
    private static final double SCROLL_DELTA_MULTIPLIER = 1.15;

    private Timeline scrollAnimation;
    private Timeline sessionMonitor;
    private double targetScrollValue;
    private AuthenticatedUserDto authenticatedUser;
    private boolean redirectingToLogin;

    @FXML
    private BorderPane mainRoot;

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
    private ScrollPane contentScrollPane;

    @FXML
    private StackPane contentContainer;

    @FXML
    public void initialize() {
        authenticatedUser = UserSession.requireAuthenticatedUser();
        currentUserNameLabel.setText(authenticatedUser.fullName());
        currentUsernameLabel.setText("@" + authenticatedUser.username());
        currentUserRoleLabel.setText(authenticatedUser.role().getDisplayName());
        usersMenuButton.setVisible(authenticatedUser.canManageUsers());
        usersMenuButton.setManaged(authenticatedUser.canManageUsers());
        contentContainer.setAlignment(Pos.TOP_LEFT);
        configureContentScroll();
        configureSessionMonitoring();
        showDashboardModule();
    }

    @FXML
    private void handleDashboardMenu() {
        showModule("Dashboard", dashboardMenuButton, ViewManager.DASHBOARD_VIEW);
    }

    @FXML
    private void handleEquipmentMenu() {
        showModule("Equipamentos", equipmentMenuButton, ViewManager.EQUIPMENT_VIEW);
    }

    @FXML
    private void handleMovementMenu() {
        showModule("Movimentacoes", movementMenuButton, ViewManager.MOVEMENT_VIEW);
    }

    @FXML
    private void handleUsersMenu() {
        if (!authenticatedUser.canManageUsers()) {
            showDashboardModule();
            return;
        }
        showModule("Usuarios", usersMenuButton, ViewManager.USER_VIEW);
    }

    @FXML
    private void handleReportsMenu() {
        showModule("Relatorios", reportsMenuButton, ViewManager.REPORT_VIEW);
    }

    @FXML
    private void handleLogout() {
        stopSessionMonitor();
        UserSession.logout();
        ViewManager.showLogin(logoutButton);
    }

    private void showDashboardModule() {
        handleDashboardMenu();
    }

    private void configureContentScroll() {
        contentScrollPane.setPannable(true);
        contentContainer.setMaxWidth(Double.MAX_VALUE);
        targetScrollValue = contentScrollPane.getVvalue();
        contentScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, bounds) ->
                contentContainer.setPrefWidth(bounds.getWidth()));
        contentScrollPane.addEventFilter(ScrollEvent.SCROLL, this::handleSmoothScroll);
    }

    private void configureSessionMonitoring() {
        UserSession.touch();
        mainRoot.addEventFilter(InputEvent.ANY, this::handleSessionActivity);
        sessionMonitor = new Timeline(new KeyFrame(SESSION_CHECK_INTERVAL, event -> enforceActiveSession()));
        sessionMonitor.setCycleCount(Timeline.INDEFINITE);
        sessionMonitor.play();
    }

    private void handleSessionActivity(InputEvent event) {
        if (UserSession.touch()) {
            return;
        }
        redirectToLogin(event);
    }

    private void enforceActiveSession() {
        if (!UserSession.isAuthenticated()) {
            redirectToLogin(null);
        }
    }

    private void redirectToLogin(InputEvent event) {
        if (redirectingToLogin) {
            if (event != null) {
                event.consume();
            }
            return;
        }

        redirectingToLogin = true;
        stopSessionMonitor();
        UserSession.logout();
        if (event != null) {
            event.consume();
        }
        ViewManager.showLogin(mainRoot);
    }

    private void stopSessionMonitor() {
        if (sessionMonitor != null) {
            sessionMonitor.stop();
            sessionMonitor = null;
        }
    }

    private void handleSmoothScroll(ScrollEvent event) {
        if (event.getDeltaY() == 0 || isInsideNestedScrollable(event.getTarget())) {
            return;
        }

        Node scrollContent = contentScrollPane.getContent();
        if (scrollContent == null) {
            return;
        }

        Bounds viewportBounds = contentScrollPane.getViewportBounds();
        double contentHeight = scrollContent.getBoundsInLocal().getHeight();
        double scrollableHeight = contentHeight - viewportBounds.getHeight();
        if (scrollableHeight <= 0) {
            return;
        }

        double currentBaseValue = scrollAnimation != null && scrollAnimation.getStatus() == Timeline.Status.RUNNING
                ? targetScrollValue
                : contentScrollPane.getVvalue();
        double delta = (event.getDeltaY() * SCROLL_DELTA_MULTIPLIER) / scrollableHeight;
        targetScrollValue = clamp(currentBaseValue - delta, 0, 1);

        animateScrollTo(targetScrollValue);
        event.consume();
    }

    private boolean isInsideNestedScrollable(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }

        Node current = node;
        while (current != null && current != contentScrollPane) {
            if (current instanceof TableView<?> || current instanceof TextArea || current instanceof ScrollPane) {
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    private void animateScrollTo(double destination) {
        if (scrollAnimation != null) {
            scrollAnimation.stop();
        }

        scrollAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(contentScrollPane.vvalueProperty(), contentScrollPane.getVvalue())),
                new KeyFrame(SCROLL_ANIMATION_DURATION,
                        new KeyValue(contentScrollPane.vvalueProperty(), destination, Interpolator.EASE_BOTH))
        );
        scrollAnimation.play();
    }

    private void showModule(String title, Button activeButton, String viewPath) {
        contentTitleLabel.setText(title);
        setActiveMenu(activeButton);
        Parent content = ViewManager.loadView(viewPath);
        animateContentSwap(content);
    }

    private void animateContentSwap(Parent content) {
        releasePreviousContentBindings();
        prepareContentNode(content);
        content.setOpacity(0);
        content.setTranslateY(12);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        contentContainer.getChildren().setAll(content);

        if (contentScrollPane != null) {
            if (scrollAnimation != null) {
                scrollAnimation.stop();
            }
            targetScrollValue = 0;
            contentScrollPane.setHvalue(0);
            contentScrollPane.setVvalue(0);
        }

        FadeTransition fadeTransition = new FadeTransition(CONTENT_TRANSITION_DURATION, content);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        TranslateTransition translateTransition = new TranslateTransition(CONTENT_TRANSITION_DURATION, content);
        translateTransition.setFromY(12);
        translateTransition.setToY(0);

        new ParallelTransition(fadeTransition, translateTransition).play();
    }

    private void prepareContentNode(Parent content) {
        if (content instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            region.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(0, contentContainer.getWidth() - contentContainer.snappedLeftInset() - contentContainer.snappedRightInset()),
                    contentContainer.widthProperty(),
                    contentContainer.paddingProperty()
            ));
        }
    }

    private void releasePreviousContentBindings() {
        for (Node child : contentContainer.getChildren()) {
            if (child instanceof Region region) {
                region.prefWidthProperty().unbind();
            }
        }
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
