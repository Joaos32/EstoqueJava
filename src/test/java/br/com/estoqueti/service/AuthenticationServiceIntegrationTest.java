package br.com.estoqueti.service;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.exception.AuthenticationException;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.util.PasswordUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceIntegrationTest {

    private static final String TEST_PREFIX = "sec-auth-";

    private final AuthenticationService authenticationService = new AuthenticationService();

    @AfterEach
    void cleanUpTestUsers() {
        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE user_id IN (SELECT id FROM estoque_ti.app_user WHERE username LIKE :userPrefix) OR description LIKE :descriptionPrefix OR ip_or_station IN ('TESTE-INTEGRACAO', 'TESTE-BLOQUEIO')")
                    .setParameter("userPrefix", TEST_PREFIX + "%")
                    .setParameter("descriptionPrefix", "%sec-auth-%")
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM estoque_ti.app_user WHERE username LIKE :prefix")
                    .setParameter("prefix", TEST_PREFIX + "%")
                    .executeUpdate();
            return null;
        });
    }

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldAuthenticateActiveUserCreatedForTheTest() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User user = createUser(Role.ADMIN, true);

        AuthenticatedUserDto authenticatedUser = authenticationService.authenticate(
                new LoginRequest(user.getUsername(), "Senha@123", "TESTE-INTEGRACAO")
        );

        assertEquals(user.getUsername(), authenticatedUser.username());
        assertEquals(Role.ADMIN, authenticatedUser.role());
        assertTrue(authenticatedUser.canManageUsers());
    }

    @Test
    void shouldRejectInvalidPassword() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User user = createUser(Role.ADMIN, true);

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                new LoginRequest(user.getUsername(), "senha-invalida", "TESTE-INTEGRACAO")
        ));
    }

    @Test
    void shouldTemporarilyBlockAfterRepeatedFailures() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User user = createUser(Role.ADMIN, true);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                    new LoginRequest(user.getUsername(), "senha-invalida", "TESTE-BLOQUEIO")
            ));
        }

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                new LoginRequest(user.getUsername(), "Senha@123", "TESTE-BLOQUEIO")
        ));
        assertTrue(exception.getMessage().contains("Muitas tentativas invalidas"));
    }

    @Test
    void shouldTruncateOversizedWorkstationIdentifierInAuditLog() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User user = createUser(Role.ADMIN, true);
        String longWorkstation = "ESTACAO-" + "X".repeat(180);

        authenticationService.authenticate(new LoginRequest(user.getUsername(), "Senha@123", longWorkstation));

        String storedWorkstation = JpaExecutor.query(entityManager -> (String) entityManager
                .createNativeQuery("SELECT ip_or_station FROM estoque_ti.audit_log WHERE user_id = :userId ORDER BY id DESC LIMIT 1")
                .setParameter("userId", user.getId())
                .getSingleResult());

        assertTrue(storedWorkstation.length() <= 120);
        assertTrue(storedWorkstation.startsWith("ESTACAO-"));
    }

    private User createUser(Role role, boolean active) {
        String username = TEST_PREFIX + System.nanoTime();
        return JpaExecutor.transaction(entityManager -> {
            User user = new User(
                    "Usuario de Seguranca",
                    username,
                    PasswordUtils.hash("Senha@123"),
                    role,
                    active
            );
            entityManager.persist(user);
            return user;
        });
    }
}
