package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.auth.LoginRequest;
import br.com.estoqueti.exception.AuthenticationException;
import br.com.estoqueti.mapper.UserMapper;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.UserRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaUserRepository;
import br.com.estoqueti.util.PasswordUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_USERNAME_LENGTH = 80;
    private static final int MAX_WORKSTATION_LENGTH = 120;
    private static final ConcurrentMap<LoginAttemptKey, FailedLoginState> FAILED_LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    public AuthenticatedUserDto authenticate(LoginRequest request) {
        validateRequest(request);

        Instant now = Instant.now();
        cleanupExpiredFailures(now);

        String normalizedUsername = request.username().trim();
        String normalizedWorkstation = normalizeWorkstation(request.workstation());
        LoginAttemptKey attemptKey = new LoginAttemptKey(
                normalizedUsername.toLowerCase(Locale.ROOT),
                normalizedWorkstation.toLowerCase(Locale.ROOT)
        );

        ensureLoginAllowed(attemptKey, now);

        UserAuthenticationSnapshot userSnapshot = JpaExecutor.query(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            return userRepository.findByUsernameIgnoreCase(normalizedUsername)
                    .map(user -> new UserAuthenticationSnapshot(user.getId(), user.isActive(), user.getPasswordHash()))
                    .orElse(null);
        });

        if (userSnapshot == null) {
            registerFailedAttempt(attemptKey, null, normalizedUsername, normalizedWorkstation, "usuario inexistente");
            throw invalidCredentials();
        }

        if (!userSnapshot.active()) {
            registerFailedAttempt(attemptKey, userSnapshot.id(), normalizedUsername, normalizedWorkstation, "usuario inativo");
            throw new AuthenticationException("Usuario inativo. Procure um administrador.");
        }

        if (!PasswordUtils.matches(request.password(), userSnapshot.passwordHash())) {
            registerFailedAttempt(attemptKey, userSnapshot.id(), normalizedUsername, normalizedWorkstation, "senha invalida");
            throw invalidCredentials();
        }

        AuthenticatedUserDto authenticatedUser = JpaExecutor.transaction(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);
            User user = entityManager.find(User.class, userSnapshot.id());
            if (user == null || !user.isActive()) {
                throw invalidCredentials();
            }

            user.registerSuccessfulLogin(OffsetDateTime.now());
            userRepository.save(user);

            auditLogRepository.save(AuditLog.of(
                    user,
                    AuditAction.LOGIN,
                    "app_user",
                    user.getId(),
                    "Login realizado com sucesso.",
                    normalizedWorkstation
            ));

            return UserMapper.toAuthenticatedUserDto(user);
        });

        FAILED_LOGIN_ATTEMPTS.remove(attemptKey);
        return authenticatedUser;
    }

    private void validateRequest(LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank() || request.password() == null || request.password().isBlank()) {
            throw new AuthenticationException("Informe usuario e senha para continuar.");
        }
        if (request.username().trim().length() > MAX_USERNAME_LENGTH) {
            throw invalidCredentials();
        }
    }

    private void ensureLoginAllowed(LoginAttemptKey attemptKey, Instant now) {
        FailedLoginState state = FAILED_LOGIN_ATTEMPTS.get(attemptKey);
        if (state == null) {
            return;
        }

        if (shouldResetFailures(state, now)) {
            FAILED_LOGIN_ATTEMPTS.remove(attemptKey, state);
            return;
        }

        if (state.blockedUntil() != null && now.isBefore(state.blockedUntil())) {
            long remainingMinutes = Math.max(1, ChronoUnit.MINUTES.between(now, state.blockedUntil()));
            throw new AuthenticationException("Muitas tentativas invalidas. Aguarde " + remainingMinutes + " minuto(s) para tentar novamente.");
        }
    }

    private void registerFailedAttempt(LoginAttemptKey attemptKey, Long userId, String username, String workstation, String reason) {
        recordFailedAuthentication(userId, username, workstation, reason);
        updateFailureState(attemptKey);
    }

    private void recordFailedAuthentication(Long userId, String username, String workstation, String reason) {
        JpaExecutor.transaction(entityManager -> {
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);
            User user = userId == null ? null : entityManager.find(User.class, userId);
            auditLogRepository.save(AuditLog.of(
                    user,
                    AuditAction.LOGIN,
                    "app_user",
                    userId,
                    "Falha de autenticacao para o login '" + username + "': " + reason + ".",
                    workstation
            ));
            return null;
        });
    }

    private void updateFailureState(LoginAttemptKey attemptKey) {
        Instant now = Instant.now();
        cleanupExpiredFailures(now);
        FAILED_LOGIN_ATTEMPTS.compute(attemptKey, (ignored, currentState) -> {
            FailedLoginState effectiveState = shouldResetFailures(currentState, now)
                    ? new FailedLoginState(0, now, null)
                    : currentState;

            int failedAttempts = effectiveState.failureCount() + 1;
            Instant blockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS
                    ? now.plus(LOGIN_BLOCK_DURATION)
                    : null;
            return new FailedLoginState(failedAttempts, now, blockedUntil);
        });
    }

    private void cleanupExpiredFailures(Instant now) {
        FAILED_LOGIN_ATTEMPTS.entrySet().removeIf(entry -> shouldResetFailures(entry.getValue(), now));
    }

    private boolean shouldResetFailures(FailedLoginState state, Instant now) {
        if (state == null) {
            return true;
        }
        if (state.blockedUntil() != null) {
            return !now.isBefore(state.blockedUntil());
        }
        return Duration.between(state.lastFailureAt(), now).compareTo(LOGIN_BLOCK_DURATION) >= 0;
    }

    private String normalizeWorkstation(String workstation) {
        if (workstation == null || workstation.isBlank()) {
            return "DESCONHECIDA";
        }

        String sanitizedWorkstation = workstation.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        if (sanitizedWorkstation.isBlank()) {
            return "DESCONHECIDA";
        }
        if (sanitizedWorkstation.length() > MAX_WORKSTATION_LENGTH) {
            return sanitizedWorkstation.substring(0, MAX_WORKSTATION_LENGTH);
        }
        return sanitizedWorkstation;
    }

    private AuthenticationException invalidCredentials() {
        return new AuthenticationException("Usuario ou senha invalidos.");
    }

    private record UserAuthenticationSnapshot(Long id, boolean active, String passwordHash) {
    }

    private record LoginAttemptKey(String username, String workstation) {
    }

    private record FailedLoginState(int failureCount, Instant lastFailureAt, Instant blockedUntil) {
    }
}
