package br.com.estoqueti.service;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.exception.AuthenticationException;
import br.com.estoqueti.model.enums.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceIntegrationTest {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldAuthenticateSeedAdminUser() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");

        AuthenticatedUserDto authenticatedUser = authenticationService.authenticate(
                new LoginRequest("admin", "Admin@123", "TESTE-INTEGRACAO")
        );

        assertEquals("admin", authenticatedUser.username());
        assertEquals(Role.ADMIN, authenticatedUser.role());
        assertTrue(authenticatedUser.canManageUsers());
    }

    @Test
    void shouldRejectInvalidPassword() {
        Assumptions.assumeTrue(!ApplicationProperties.get("database.password", "").isBlank(), "Senha do banco nao configurada para o teste.");

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(
                new LoginRequest("admin", "senha-invalida", "TESTE-INTEGRACAO")
        ));
    }
}
