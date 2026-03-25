package br.com.estoqueti.dto.user;

import br.com.estoqueti.model.enums.Role;

public record UserCreateRequest(
        String fullName,
        String username,
        String rawPassword,
        Role role,
        boolean active
) {
}
