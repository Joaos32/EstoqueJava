package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.mapper.UserMapper;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.UserRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaUserRepository;
import br.com.estoqueti.util.PasswordUtils;
import br.com.estoqueti.util.WorkstationUtils;

import java.util.List;

public class UserService {

    public List<UserListItemDto> listUsers() {
        return JpaExecutor.query(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            return userRepository.findAllOrderedByName()
                    .stream()
                    .map(UserMapper::toListItemDto)
                    .toList();
        });
    }

    public UserListItemDto createUser(UserCreateRequest request, AuthenticatedUserDto authenticatedUser) {
        if (authenticatedUser == null || !authenticatedUser.canManageUsers()) {
            throw new AuthorizationException("Somente administradores podem cadastrar usuarios.");
        }

        validateRequest(request);

        return JpaExecutor.transaction(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

            if (userRepository.existsByUsernameIgnoreCase(request.username().trim())) {
                throw new ValidationException("Ja existe um usuario cadastrado com esse login.");
            }

            User user = new User(
                    request.fullName().trim(),
                    request.username().trim(),
                    PasswordUtils.hash(request.rawPassword()),
                    request.role(),
                    request.active()
            );

            userRepository.save(user);

            auditLogRepository.save(AuditLog.of(
                    entityManager.getReference(User.class, authenticatedUser.id()),
                    AuditAction.CADASTRO,
                    "app_user",
                    user.getId(),
                    "Cadastro de usuario realizado: " + user.getUsername(),
                    WorkstationUtils.resolveStationIdentifier()
            ));

            return UserMapper.toListItemDto(user);
        });
    }

    private void validateRequest(UserCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Os dados do usuario sao obrigatorios.");
        }
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ValidationException("Informe o nome completo do usuario.");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new ValidationException("Informe o login do usuario.");
        }
        if (request.role() == null) {
            throw new ValidationException("Selecione o perfil de acesso do usuario.");
        }
        if (request.rawPassword() == null || request.rawPassword().isBlank()) {
            throw new ValidationException("Informe a senha do usuario.");
        }
        if (request.rawPassword().trim().length() < 8) {
            throw new ValidationException("A senha deve ter pelo menos 8 caracteres.");
        }
    }
}
