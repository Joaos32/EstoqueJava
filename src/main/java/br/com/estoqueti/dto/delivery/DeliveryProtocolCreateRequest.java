package br.com.estoqueti.dto.delivery;

import java.time.OffsetDateTime;

public record DeliveryProtocolCreateRequest(
        Long equipmentId,
        Integer quantity,
        Long destinationLocationId,
        String recipientName,
        String recipientCpf,
        String recipientRole,
        OffsetDateTime deliveryAt,
        String notes
) {
}