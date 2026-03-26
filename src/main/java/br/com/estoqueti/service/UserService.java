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
import java.util.regex.Pattern;

public class UserService {

    private static final int MAX_FULL_NAME_LENGTH = 150;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,40}");
    private static final Pattern PASSWORD_UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_SPECIAL_CHARACTER_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");

    public List<UserListItemDto> listUsers(AuthenticatedUserDto authenticatedUser) {
        if (authenticatedUser == null || !authenticatedUser.canManageUsers()) {
            throw new AuthorizationException("Somente administradores podem consultar usuarios.");
        }

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
        String normalizedFullName = request.fullName().trim();
        String normalizedUsername = request.username().trim();

        return JpaExecutor.transaction(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

            if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
                throw new ValidationException("Ja existe um usuario cadastrado com esse login.");
            }

            User user = new User(
                    normalizedFullName,
                    normalizedUsername,
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
        if (request.fullName().trim().length() > MAX_FULL_NAME_LENGTH) {
            throw new ValidationException("O nome completo do usuario excede o limite permitido.");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new ValidationException("Informe o login do usuario.");
        }

        String normalizedUsername = request.username().trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new ValidationException("O login deve ter entre 3 e 40 caracteres e conter apenas letras, numeros, ponto, underscore ou hifen.");
        }
        if (request.role() == null) {
            throw new ValidationException("Selecione o perfil de acesso do usuario.");
        }
        if (request.rawPassword() == null || request.rawPassword().isBlank()) {
            throw new ValidationException("Informe a senha do usuario.");
        }
        if (request.rawPassword().length() < 8) {
            throw new ValidationException("A senha deve ter pelo menos 8 caracteres.");
        }
        if (!PASSWORD_UPPERCASE_PATTERN.matcher(request.rawPassword()).matches()
                || !PASSWORD_LOWERCASE_PATTERN.matcher(request.rawPassword()).matches()
                || !PASSWORD_NUMBER_PATTERN.matcher(request.rawPassword()).matches()
                || !PASSWORD_SPECIAL_CHARACTER_PATTERN.matcher(request.rawPassword()).matches()) {
            throw new ValidationException("A senha deve conter letra maiuscula, letra minuscula, numero e caractere especial.");
        }
    }
}
