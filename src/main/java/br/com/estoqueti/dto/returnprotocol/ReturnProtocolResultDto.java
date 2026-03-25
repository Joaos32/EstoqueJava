package br.com.estoqueti.dto.returnprotocol;

import java.nio.file.Path;

public record ReturnProtocolResultDto(
        String protocolNumber,
        String equipmentInternalCode,
        String employeeName,
        Path outputPath
) {
}
