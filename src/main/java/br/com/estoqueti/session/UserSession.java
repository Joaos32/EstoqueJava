package br.com.estoqueti.session;

import br.com.estoqueti.config.ApplicationProperties;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.exception.AuthorizationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class UserSession {

    private static final long DEFAULT_SESSION_TIMEOUT_MINUTES = 30L;

    private static volatile AuthenticatedUserDto currentUser;
    private static volatile Instant lastActivityAt;

    private UserSession() {
    }

    public static void login(AuthenticatedUserDto authenticatedUser) {
        currentUser = authenticatedUser;
        lastActivityAt = Instant.now();
    }

    public static void logout() {
        currentUser = null;
        lastActivityAt = null;
    }

    public static boolean touch() {
        if (currentUser == null) {
            return false;
        }

        Instant now = Instant.now();
        Instant lastActivity = lastActivityAt;
        if (lastActivity != null && isExpired(lastActivity, now)) {
            logout();
            return false;
        }

        lastActivityAt = now;
        return true;
    }

    public static boolean isAuthenticated() {
        return currentUser != null && !isExpired();
    }

    public static boolean isExpired() {
        AuthenticatedUserDto authenticatedUser = currentUser;
        if (authenticatedUser == null) {
            return false;
        }

        Instant lastActivity = lastActivityAt;
        if (lastActivity == null) {
            return true;
        }

        return isExpired(lastActivity, Instant.now());
    }

    public static Optional<AuthenticatedUserDto> getCurrentUser() {
        if (isExpired()) {
            logout();
            return Optional.empty();
        }
        return Optional.ofNullable(currentUser);
    }

    public static AuthenticatedUserDto requireAuthenticatedUser() {
        AuthenticatedUserDto authenticatedUser = currentUser;
        if (authenticatedUser == null) {
            throw new AuthorizationException("Nenhum usuario autenticado na sessao atual.");
        }
        if (isExpired()) {
            logout();
            throw new AuthorizationException("Sessao expirada por inatividade. Faca login novamente.");
        }
        return authenticatedUser;
    }

    private static boolean isExpired(Instant lastActivity, Instant referenceTime) {
        Duration sessionTimeout = resolveSessionTimeout();
        return !Duration.between(lastActivity, referenceTime).minus(sessionTimeout).isNegative();
    }

    private static Duration resolveSessionTimeout() {
        long timeoutMinutes = ApplicationProperties.getLong("app.session-timeout-minutes", DEFAULT_SESSION_TIMEOUT_MINUTES);
        return timeoutMinutes <= 0L
                ? Duration.ZERO
                : Duration.ofMinutes(timeoutMinutes);
    }
}
