package br.com.estoqueti;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.util.ViewManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppLauncher extends Application {

    private static final String APP_NAME = "EstoqueTI Desktop";
    private static final String LOG_DIRECTORY_PROPERTY = "estoqueti.log.dir";
    private static final String LOG_FILE_PROPERTY = "estoqueti.log.file";
    private static final AtomicBoolean FATAL_ERROR_REPORTED = new AtomicBoolean(false);

    static {
        configureRuntimeDefaults();
        configureLoggingProperties();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AppLauncher.class);

    @Override
    public void start(Stage stage) {
        try {
            stage.setOnShown(event -> LOGGER.info("Janela principal exibida com sucesso."));
            ApplicationProperties.load();
            ViewManager.showLogin(stage);
            LOGGER.info("Aplicacao iniciada com sucesso.");
        } catch (Throwable throwable) {
            reportFatalError("Nao foi possivel iniciar a interface do sistema.", throwable);
            Platform.exit();
            throw wrap(throwable);
        }
    }

    @Override
    public void stop() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
        LOGGER.info("Aplicacao encerrada com sucesso.");
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                reportFatalError("Erro nao tratado durante a execucao na thread '" + thread.getName() + "'.", throwable)
        );

        try {
            launch(args);
        } catch (Throwable throwable) {
            reportFatalError("Falha fatal ao inicializar o aplicativo.", throwable);
        }
    }

    private static void configureRuntimeDefaults() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win") && System.getProperty("prism.order") == null) {
            System.setProperty("prism.order", "d3d,sw");
        }
    }

    private static void configureLoggingProperties() {
        Path logDirectory = resolveLogDirectory();
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            System.err.println("Nao foi possivel preparar o diretorio de logs: " + logDirectory);
            exception.printStackTrace(System.err);
        }

        System.setProperty(LOG_DIRECTORY_PROPERTY, logDirectory.toString());
        System.setProperty(LOG_FILE_PROPERTY, logDirectory.resolve("estoqueti.log").toString());
    }

    private static Path resolveLogDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (hasText(localAppData)) {
            return Path.of(localAppData, APP_NAME, "logs");
        }

        String userHome = System.getProperty("user.home");
        if (hasText(userHome)) {
            return Path.of(userHome, ".estoqueti-desktop", "logs");
        }

        return Path.of("logs").toAbsolutePath().normalize();
    }

    private static void reportFatalError(String message, Throwable throwable) {
        if (!FATAL_ERROR_REPORTED.compareAndSet(false, true)) {
            return;
        }

        try {
            LOGGER.error(message, throwable);
        } catch (Throwable ignored) {
            System.err.println(message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }

        String logFile = System.getProperty(LOG_FILE_PROPERTY, "logs/estoqueti.log");
        String fullMessage = message
                + System.lineSeparator()
                + System.lineSeparator()
                + "Veja os detalhes em:"
                + System.lineSeparator()
                + logFile;

        try {
            JOptionPane.showMessageDialog(null, fullMessage, APP_NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            System.err.println(fullMessage);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    private static RuntimeException wrap(Throwable throwable) {
        return throwable instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException(throwable);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}