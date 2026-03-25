package br.com.estoqueti.dto.user;

import br.com.estoqueti.model.enums.Role;

import java.time.OffsetDateTime;

public record UserListItemDto(
        Long id,
        String fullName,
        String username,
        Role role,
        boolean active,
        OffsetDateTime lastLoginAt
) {
}
