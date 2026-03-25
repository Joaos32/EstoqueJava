package br.com.estoqueti.dto.auth;

import br.com.estoqueti.model.enums.Role;

public record AuthenticatedUserDto(
        Long id,
        String fullName,
        String username,
        Role role,
        boolean active
) {

    public boolean canManageUsers() {
        return role != null && role.canManageUsers();
    }

    public boolean canManageInventory() {
        return role != null && role.canManageInventory();
    }
}