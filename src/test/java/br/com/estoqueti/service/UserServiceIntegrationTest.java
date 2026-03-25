package br.com.estoqueti.service;

import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceIntegrationTest {

    private final UserService userService = new UserService();

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldListSeedUsers() {
        List<UserListItemDto> users = userService.listUsers();

        assertTrue(users.size() >= 3);
    }

    @Test
    void shouldRejectUserCreationForNonAdminProfile() {
        AuthenticatedUserDto tecnico = new AuthenticatedUserDto(2L, "Carlos Tecnico", "tecnico", Role.TECNICO, true);

        assertThrows(AuthorizationException.class, () -> userService.createUser(
                new UserCreateRequest("Usuario Bloqueado", "bloqueado.teste", "Senha@123", Role.VISUALIZADOR, true),
                tecnico
        ));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        AuthenticatedUserDto admin = new AuthenticatedUserDto(1L, "Administrador do Sistema", "admin", Role.ADMIN, true);

        assertThrows(ValidationException.class, () -> userService.createUser(
                new UserCreateRequest("Administrador Duplicado", "admin", "Senha@123", Role.ADMIN, true),
                admin
        ));
    }
}
