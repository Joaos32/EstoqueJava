package br.com.estoqueti.dto.delivery;

import java.time.OffsetDateTime;

public record DeliveryProtocolCreateRequest(
        Long equipmentId,
        Integer quantity,
        String destinationDescription,
        String recipientName,
        String recipientCpf,
        String recipientRole,
        OffsetDateTime deliveryAt,
        String notes
) {
}