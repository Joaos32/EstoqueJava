package br.com.estoqueti.controller;

import br.com.estoqueti.dto.RecoverPasswordRequest;
import br.com.estoqueti.dto.ResetPasswordRequest;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.user.UserCreateRequest;
import br.com.estoqueti.dto.user.UserListItemDto;
import br.com.estoqueti.service.ApiAuthenticatedUserService;
import br.com.estoqueti.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios")
public class UserApiController {

    private final UserService userService;
    private final ApiAuthenticatedUserService authenticatedUserService;

    public UserApiController(UserService userService, ApiAuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios cadastrados")
    public List<UserListItemDto> listUsers(
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return userService.listUsers(authenticatedUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuario")
    public UserListItemDto createUser(
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestBody UserCreateRequest request
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return userService.createUser(request, authenticatedUser);
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Redefine a senha de um usuario")
    public UserListItemDto resetPassword(
            @PathVariable Long userId,
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestBody ResetPasswordRequest request
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return userService.resetPassword(userId, request.newPassword(), authenticatedUser);
    }

    @PostMapping("/recover-password")
    @Operation(summary = "Recupera a senha diretamente pelo login")
    public UserListItemDto recoverPassword(@RequestBody RecoverPasswordRequest request) {
        return userService.recoverPassword(request.targetUsername(), request.newPassword(), request.workstation());
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Inativa um usuario")
    public UserListItemDto deactivateUser(
            @PathVariable Long userId,
            @Parameter(description = "ID do usuario autenticado", required = true)
            @RequestHeader("X-User-Id") Long authenticatedUserId
    ) {
        AuthenticatedUserDto authenticatedUser = authenticatedUserService.requireAuthenticatedUser(authenticatedUserId);
        return userService.deactivateUser(userId, authenticatedUser);
    }
}
