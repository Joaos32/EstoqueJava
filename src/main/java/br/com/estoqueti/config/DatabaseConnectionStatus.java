package br.com.estoqueti.config;

import java.time.LocalDateTime;

public record DatabaseConnectionStatus(
        boolean successful,
        String jdbcUrl,
        String title,
        String details,
        LocalDateTime checkedAt
) {
}
