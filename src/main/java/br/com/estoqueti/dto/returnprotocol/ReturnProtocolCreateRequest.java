package br.com.estoqueti.dto.returnprotocol;

import br.com.estoqueti.model.enums.ReturnProtocolReason;

import java.time.OffsetDateTime;

public record ReturnProtocolCreateRequest(
        Long equipmentId,
        Integer quantity,
        Long destinationLocationId,
        String employeeName,
        String employeeCpf,
        String companyReceiverName,
        String companyReceiverRole,
        String companyReceiverCpf,
        ReturnProtocolReason returnReason,
        String otherReason,
        OffsetDateTime returnedAt,
        String notes
) {
}
