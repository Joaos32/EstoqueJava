package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.exception.AuthenticationException;
import br.com.estoqueti.mapper.UserMapper;
import br.com.estoqueti.repository.UserRepository;
import br.com.estoqueti.repository.impl.JpaUserRepository;

public class ApiAuthenticatedUserService {

    public AuthenticatedUserDto requireAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("Informe o cabecalho X-User-Id para operacoes autenticadas.");
        }

        AuthenticatedUserDto authenticatedUser = JpaExecutor.query(entityManager -> {
            UserRepository userRepository = new JpaUserRepository(entityManager);
            return userRepository.findById(userId)
                    .map(UserMapper::toAuthenticatedUserDto)
                    .orElse(null);
        });

        if (authenticatedUser == null) {
            throw new AuthenticationException("Usuario autenticado nao encontrado para o cabecalho X-User-Id informado.");
        }
        if (!authenticatedUser.active()) {
            throw new AuthenticationException("O usuario informado no cabecalho X-User-Id esta inativo.");
        }

        return authenticatedUser;
    }
}
