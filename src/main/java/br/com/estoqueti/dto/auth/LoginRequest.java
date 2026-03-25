package br.com.estoqueti.dto.auth;

public record LoginRequest(
        String username,
        String password,
        String workstation
) {
}
