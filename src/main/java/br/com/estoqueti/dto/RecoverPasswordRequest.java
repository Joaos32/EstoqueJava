package br.com.estoqueti.dto;

public record RecoverPasswordRequest(
        String targetUsername,
        String newPassword,
        String workstation
) {
}
