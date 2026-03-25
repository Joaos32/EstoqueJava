package br.com.estoqueti;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.util.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLauncher extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppLauncher.class);

    @Override
    public void start(Stage stage) {
        ApplicationProperties.load();
        ViewManager.showLogin(stage);
        LOGGER.info("Aplicacao iniciada com sucesso.");
    }

    @Override
    public void stop() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
        LOGGER.info("Aplicacao encerrada com sucesso.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
