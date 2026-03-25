package br.com.estoqueti.dto.delivery;

import java.nio.file.Path;

public record DeliveryProtocolResultDto(
        String protocolNumber,
        String equipmentInternalCode,
        String recipientName,
        Path outputPath
) {
}