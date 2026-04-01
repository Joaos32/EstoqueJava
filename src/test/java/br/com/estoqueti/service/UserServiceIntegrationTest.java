package br.com.estoqueti.service;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.exception.AuthenticationException;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.util.PasswordUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceIntegrationTest {

    private static final String TEST_PREFIX = "sec-user-";

    private final UserService userService = new UserService();
    private final AuthenticationService authenticationService = new AuthenticationService();

    @AfterEach
    void cleanUpTestUsers() {
        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE user_id IN (SELECT id FROM estoque_ti.app_user WHERE username LIKE :userPrefix) OR description LIKE :descriptionPrefix")
                    .setParameter("userPrefix", TEST_PREFIX + "%")
                    .setParameter("descriptionPrefix", "%" + TEST_PREFIX + "%")
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
    void shouldListUsersOnlyForAdministrators() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User admin = createUser("Administrador de Teste", Role.ADMIN, true);
        createUser("Visualizador de Teste", Role.VISUALIZADOR, true);

        List<UserListItemDto> users = userService.listUsers(toAuthenticatedUser(admin));

        assertTrue(users.stream().anyMatch(user -> user.username().equals(admin.getUsername())));
    }

    @Test
    void shouldRejectUserListingForNonAdminProfile() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User tecnico = createUser("Tecnico de Teste", Role.TECNICO, true);

        assertThrows(AuthorizationException.class, () -> userService.listUsers(toAuthenticatedUser(tecnico)));
    }

    @Test
    void shouldRejectUserCreationForNonAdminProfile() {
        AuthenticatedUserDto tecnico = new AuthenticatedUserDto(999L, "Tecnico de Teste", TEST_PREFIX + "tecnico", Role.TECNICO, true);

        assertThrows(AuthorizationException.class, () -> userService.createUser(
                new UserCreateRequest("Usuario Bloqueado", TEST_PREFIX + "bloqueado", "Senha@123", Role.VISUALIZADOR, true),
                tecnico
        ));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User admin = createUser("Administrador de Teste", Role.ADMIN, true);
        User existingUser = createUser("Existente", Role.TECNICO, true);

        assertThrows(ValidationException.class, () -> userService.createUser(
                new UserCreateRequest("Administrador Duplicado", existingUser.getUsername(), "Senha@123", Role.ADMIN, true),
                toAuthenticatedUser(admin)
        ));
    }

    @Test
    void shouldRejectWeakPassword() {
        AuthenticatedUserDto admin = new AuthenticatedUserDto(999L, "Administrador", TEST_PREFIX + "admin", Role.ADMIN, true);

        assertThrows(ValidationException.class, () -> userService.createUser(
                new UserCreateRequest("Usuario Fraco", TEST_PREFIX + "fraco", "12345678", Role.VISUALIZADOR, true),
                admin
        ));
    }

    @Test
    void shouldRejectUsernameWithInvalidCharacters() {
        AuthenticatedUserDto admin = new AuthenticatedUserDto(999L, "Administrador", TEST_PREFIX + "admin", Role.ADMIN, true);

        assertThrows(ValidationException.class, () -> userService.createUser(
                new UserCreateRequest("Usuario Invalido", "usuario invalido", "Senha@123", Role.VISUALIZADOR, true),
                admin
        ));
    }

    @Test
    void shouldResetPasswordForAdministrator() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User admin = createUser("Administrador Reset", Role.ADMIN, true);
        User targetUser = createUser("Usuario Resetado", Role.VISUALIZADOR, true);

        UserListItemDto updatedUser = userService.resetPassword(targetUser.getId(), "NovaSenha@123", toAuthenticatedUser(admin));
        String storedHash = JpaExecutor.query(entityManager -> (String) entityManager.createNativeQuery(
                        "SELECT password_hash FROM estoque_ti.app_user WHERE id = :id")
                .setParameter("id", targetUser.getId())
                .getSingleResult());
        boolean auditRegistered = JpaExecutor.query(entityManager -> !entityManager.createNativeQuery(
                        "SELECT 1 FROM estoque_ti.audit_log WHERE entity_id = :id AND action = 'EDICAO' AND description LIKE :description")
                .setParameter("id", targetUser.getId())
                .setParameter("description", "%Redefinicao de senha realizada para o usuario: " + targetUser.getUsername() + "%")
                .getResultList().isEmpty());

        assertEquals(targetUser.getUsername(), updatedUser.username());
        assertTrue(PasswordUtils.matches("NovaSenha@123", storedHash));
        assertFalse(PasswordUtils.matches("Senha@123", storedHash));
        assertTrue(auditRegistered);
    }

    @Test
    void shouldRejectPasswordResetForNonAdminProfile() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User targetUser = createUser("Usuario Alvo Reset", Role.VISUALIZADOR, true);
        User tecnico = createUser("Tecnico Reset", Role.TECNICO, true);

        assertThrows(AuthorizationException.class, () -> userService.resetPassword(targetUser.getId(), "NovaSenha@123", toAuthenticatedUser(tecnico)));
    }

    @Test
    void shouldRecoverPasswordFromLoginFlowForActiveUser() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User targetUser = createUser("Usuario Login", Role.VISUALIZADOR, true);

        UserListItemDto recoveredUser = userService.recoverPassword(
                targetUser.getUsername(),
                "NovaSenha@123",
                "TESTE-RECUPERACAO-LOGIN"
        );
        AuthenticatedUserDto authenticatedUser = authenticationService.authenticate(
                new LoginRequest(targetUser.getUsername(), "NovaSenha@123", "TESTE-RECUPERACAO-LOGIN")
        );
        boolean auditRegistered = JpaExecutor.query(entityManager -> !entityManager.createNativeQuery(
                        "SELECT 1 FROM estoque_ti.audit_log WHERE entity_id = :id AND action = 'EDICAO' AND description LIKE :description")
                .setParameter("id", targetUser.getId())
                .setParameter("description", "%Recuperacao de senha realizada na tela de login para o usuario: " + targetUser.getUsername() + "%")
                .getResultList().isEmpty());

        assertEquals(targetUser.getUsername(), recoveredUser.username());
        assertEquals(targetUser.getUsername(), authenticatedUser.username());
        assertTrue(auditRegistered);
    }

    @Test
    void shouldRejectPasswordRecoveryForInactiveUser() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User inactiveUser = createUser("Usuario Inativo", Role.VISUALIZADOR, false);

        ValidationException exception = assertThrows(ValidationException.class, () -> userService.recoverPassword(
                inactiveUser.getUsername(),
                "NovaSenha@123",
                "TESTE-RECUPERACAO-LOGIN"
        ));

        assertEquals("Usuario inativo. Verifique com o administrador.", exception.getMessage());
    }

    @Test
    void shouldClearTemporaryBlockAfterPasswordRecovery() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User targetUser = createUser("Usuario Bloqueado", Role.VISUALIZADOR, true);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                    new LoginRequest(targetUser.getUsername(), "senha-invalida", "TESTE-RESET")
            ));
        }

        AuthenticationException blockedException = assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                new LoginRequest(targetUser.getUsername(), "Senha@123", "TESTE-RESET")
        ));
        assertTrue(blockedException.getMessage().contains("Muitas tentativas invalidas"));

        userService.recoverPassword(targetUser.getUsername(), "NovaSenha@123", "TESTE-RESET");

        AuthenticatedUserDto authenticatedUser = authenticationService.authenticate(
                new LoginRequest(targetUser.getUsername(), "NovaSenha@123", "TESTE-RESET")
        );

        assertEquals(targetUser.getUsername(), authenticatedUser.username());
    }

    @Test
    void shouldDeactivateUserForAdministrator() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User admin = createUser("Administrador Remocao", Role.ADMIN, true);
        User targetUser = createUser("Usuario Removido", Role.VISUALIZADOR, true);

        UserListItemDto removedUser = userService.deactivateUser(targetUser.getId(), toAuthenticatedUser(admin));
        boolean active = JpaExecutor.query(entityManager -> (Boolean) entityManager.createNativeQuery(
                        "SELECT active FROM estoque_ti.app_user WHERE id = :id")
                .setParameter("id", targetUser.getId())
                .getSingleResult());

        assertFalse(removedUser.active());
        assertFalse(active);
    }

    @Test
    void shouldRejectUserRemovalForNonAdminProfile() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User tecnico = createUser("Tecnico de Teste", Role.TECNICO, true);
        User targetUser = createUser("Usuario Alvo", Role.VISUALIZADOR, true);

        assertThrows(AuthorizationException.class, () -> userService.deactivateUser(targetUser.getId(), toAuthenticatedUser(tecnico)));
    }

    @Test
    void shouldRejectSelfRemoval() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");
        User admin = createUser("Administrador de Teste", Role.ADMIN, true);

        assertThrows(ValidationException.class, () -> userService.deactivateUser(admin.getId(), toAuthenticatedUser(admin)));
    }

    private User createUser(String fullName, Role role, boolean active) {
        String username = TEST_PREFIX + System.nanoTime();
        return JpaExecutor.transaction(entityManager -> {
            User user = new User(fullName, username, PasswordUtils.hash("Senha@123"), role, active);
            entityManager.persist(user);
            return user;
        });
    }

    private AuthenticatedUserDto toAuthenticatedUser(User user) {
        return new AuthenticatedUserDto(user.getId(), user.getFullName(), user.getUsername(), user.getRole(), user.isActive());
    }
}