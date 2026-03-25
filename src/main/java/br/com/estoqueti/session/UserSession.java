package br.com.estoqueti.session;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.exception.AuthorizationException;

import java.util.Optional;

public final class UserSession {

    private static volatile AuthenticatedUserDto currentUser;

    private UserSession() {
    }

    public static void login(AuthenticatedUserDto authenticatedUser) {
        currentUser = authenticatedUser;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static Optional<AuthenticatedUserDto> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static AuthenticatedUserDto requireAuthenticatedUser() {
        AuthenticatedUserDto authenticatedUser = currentUser;
        if (authenticatedUser == null) {
            throw new AuthorizationException("Nenhum usuario autenticado na sessao atual.");
        }
        return authenticatedUser;
    }
}
