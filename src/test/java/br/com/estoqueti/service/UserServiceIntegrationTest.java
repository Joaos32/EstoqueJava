package br.com.estoqueti.service;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceIntegrationTest {

    private static final String TEST_PREFIX = "sec-user-";

    private final UserService userService = new UserService();

    @AfterEach
    void cleanUpTestUsers() {
        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE description LIKE :prefix")
                    .setParameter("prefix", "%" + TEST_PREFIX + "%")
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
