package br.com.estoqueti.dto.delivery;

import java.time.OffsetDateTime;

public record DeliveryProtocolDocumentData(
        String protocolNumber,
        String recipientName,
        String recipientCpf,
        String recipientRole,
        OffsetDateTime deliveryAt,
        int itemQuantity,
        String itemDescription,
        String itemIdentifier,
        String itemObservations
) {
}