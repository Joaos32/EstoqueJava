package br.com.estoqueti.dto.returnprotocol;

import br.com.estoqueti.model.enums.ReturnProtocolReason;

import java.time.OffsetDateTime;

public record ReturnProtocolDocumentData(
        String protocolNumber,
        String employeeName,
        String employeeCpf,
        String companyReceiverName,
        String companyReceiverRole,
        String companyReceiverCpf,
        ReturnProtocolReason returnReason,
        String otherReason,
        OffsetDateTime returnedAt,
        int itemQuantity,
        String itemDescription,
        String itemIdentifier,
        String itemObservations
) {
}
