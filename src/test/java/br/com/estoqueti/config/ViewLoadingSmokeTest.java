package br.com.estoqueti.config;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.session.UserSession;
import br.com.estoqueti.util.ViewManager;
import javafx.application.Platform;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ViewLoadingSmokeTest {

    @BeforeAll
    static void setupToolkit() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        try {
            Platform.startup(startupLatch::countDown);
        } catch (IllegalStateException exception) {
            startupLatch.countDown();
        }

        if (!startupLatch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Nao foi possivel inicializar o JavaFX Toolkit.");
        }

        Platform.setImplicitExit(false);
        UserSession.login(new AuthenticatedUserDto(1L, "Administrador do Sistema", "admin", Role.ADMIN, true));
    }

    @AfterAll
    static void cleanupSession() {
        UserSession.logout();
    }

    @Test
    void shouldLoadMainAuthenticatedViews() throws Exception {
        assertLoads(ViewManager.LOGIN_VIEW);
        assertLoads(ViewManager.MAIN_LAYOUT_VIEW);
        assertLoads(ViewManager.DASHBOARD_VIEW);
        assertLoads(ViewManager.EQUIPMENT_VIEW);
        assertLoads(ViewManager.MOVEMENT_VIEW);
        assertLoads(ViewManager.USER_VIEW);
        assertLoads(ViewManager.REPORT_VIEW);
    }

    private void assertLoads(String viewPath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Parent> parentRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                parentRef.set(ViewManager.loadView(viewPath));
            } catch (Throwable throwable) {
                errorRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Tempo esgotado ao carregar a view: " + viewPath);
        }

        if (errorRef.get() != null) {
            throw new RuntimeException("Falha ao carregar a view: " + viewPath, errorRef.get());
        }

        assertNotNull(parentRef.get(), "A view deveria carregar: " + viewPath);
    }
}