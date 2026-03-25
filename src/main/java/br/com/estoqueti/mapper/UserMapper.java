package br.com.estoqueti.mapper;

import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.model.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static AuthenticatedUserDto toAuthenticatedUserDto(User user) {
        return new AuthenticatedUserDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole(),
                user.isActive()
        );
    }

    public static UserListItemDto toListItemDto(User user) {
        return new UserListItemDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt()
        );
    }
}
