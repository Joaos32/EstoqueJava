package br.com.estoqueti.session;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.model.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSessionTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("app.session-timeout-minutes");
        UserSession.logout();
    }

    @Test
    void shouldKeepSessionAliveWhenThereIsRecentActivity() {
        UserSession.login(new AuthenticatedUserDto(1L, "Administrador", "admin", Role.ADMIN, true));

        assertTrue(UserSession.touch());
        assertTrue(UserSession.isAuthenticated());
    }

    @Test
    void shouldExpireSessionWhenTimeoutIsZero() {
        System.setProperty("app.session-timeout-minutes", "0");
        UserSession.login(new AuthenticatedUserDto(1L, "Administrador", "admin", Role.ADMIN, true));

        assertTrue(UserSession.isExpired());
        assertThrows(AuthorizationException.class, UserSession::requireAuthenticatedUser);
    }

    @Test
    void shouldNotReviveExpiredSessionAfterTimeout() {
        System.setProperty("app.session-timeout-minutes", "0");
        UserSession.login(new AuthenticatedUserDto(1L, "Administrador", "admin", Role.ADMIN, true));

        assertFalse(UserSession.touch());
        assertFalse(UserSession.isAuthenticated());
        assertThrows(AuthorizationException.class, UserSession::requireAuthenticatedUser);
    }
}
