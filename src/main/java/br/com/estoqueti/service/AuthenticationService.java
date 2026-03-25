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

import java.time.OffsetDateTime;

public class AuthenticationService {

    public AuthenticatedUserDto authenticate(LoginRequest request) {
        validateRequest(request);

        return JpaExecutor.transaction(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

            User user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                    .orElseThrow(() -> new AuthenticationException("Usuario ou senha invalidos."));

            if (!user.isActive()) {
                throw new AuthenticationException("Usuario inativo. Procure um administrador.");
            }

            if (!PasswordUtils.matches(request.password(), user.getPasswordHash())) {
                throw new AuthenticationException("Usuario ou senha invalidos.");
            }

            user.registerSuccessfulLogin(OffsetDateTime.now());
            userRepository.save(user);

            auditLogRepository.save(AuditLog.of(
                    user,
                    AuditAction.LOGIN,
                    "app_user",
                    user.getId(),
                    "Login realizado com sucesso.",
                    request.workstation()
            ));

            return UserMapper.toAuthenticatedUserDto(user);
        });
    }

    private void validateRequest(LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank() || request.password() == null || request.password().isBlank()) {
            throw new AuthenticationException("Informe usuario e senha para continuar.");
        }
    }
}
