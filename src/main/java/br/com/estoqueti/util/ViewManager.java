package br.com.estoqueti.util;

import br.com.estoqueti.AppLauncher;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public final class ViewManager {

    public static final String LOGIN_VIEW = "/br/com/estoqueti/view/fxml/login-view.fxml";
    public static final String MAIN_LAYOUT_VIEW = "/br/com/estoqueti/view/fxml/main-layout-view.fxml";
    public static final String DASHBOARD_VIEW = "/br/com/estoqueti/view/fxml/dashboard-view.fxml";
    public static final String USER_VIEW = "/br/com/estoqueti/view/fxml/user-view.fxml";
    public static final String EQUIPMENT_VIEW = "/br/com/estoqueti/view/fxml/equipment-view.fxml";
    public static final String MOVEMENT_VIEW = "/br/com/estoqueti/view/fxml/movement-view.fxml";
    public static final String REPORT_VIEW = "/br/com/estoqueti/view/fxml/report-view.fxml";
    private static final String STYLESHEET = "/br/com/estoqueti/view/css/application.css";

    private ViewManager() {
    }

    public static void showLogin(Stage stage) {
        showView(stage, LOGIN_VIEW, "EstoqueTI Desktop - Login", 1040, 720);
    }

    public static void showMainLayout(Stage stage) {
        showView(stage, MAIN_LAYOUT_VIEW, "EstoqueTI Desktop", 1360, 820);
    }

    public static void showLogin(Node sourceNode) {
        showLogin(extractStage(sourceNode));
    }

    public static void showMainLayout(Node sourceNode) {
        showMainLayout(extractStage(sourceNode));
    }

    public static Parent loadView(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(resolveResource(resourcePath));
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar a view: " + resourcePath, exception);
        }
    }

    private static void showView(Stage stage, String resourcePath, String title, double width, double height) {
        Parent root = loadView(resourcePath);
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(resolveResource(STYLESHEET).toExternalForm());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.setIconified(false);
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    private static URL resolveResource(String resourcePath) {
        URL resource = AppLauncher.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Recurso nao encontrado: " + resourcePath);
        }
        return resource;
    }

    private static Stage extractStage(Node sourceNode) {
        return (Stage) sourceNode.getScene().getWindow();
    }
}