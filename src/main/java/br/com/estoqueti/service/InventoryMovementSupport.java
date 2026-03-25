package br.com.estoqueti.service;

import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;

import java.util.Objects;

final class InventoryMovementSupport {

    private InventoryMovementSupport() {
    }

    static void validateMovementRules(
            Equipment equipment,
            MovementType movementType,
            int movementQuantity,
            Location sourceLocation,
            Location destinationLocation
    ) {
        if (movementType.requiresSourceLocation() && sourceLocation == null) {
            throw new ValidationException("Selecione a localizacao de origem da movimentacao.");
        }
        if (!movementType.requiresSourceLocation() && sourceLocation != null) {
            throw new ValidationException("Este tipo de movimentacao nao permite origem informada.");
        }
        if (movementType.requiresDestinationLocation() && destinationLocation == null) {
            throw new ValidationException("Selecione a localizacao de destino da movimentacao.");
        }
        if (!movementType.requiresDestinationLocation() && destinationLocation != null) {
            throw new ValidationException("Este tipo de movimentacao nao permite destino informado.");
        }
        if (movementType.requiresDistinctLocations() && sourceLocation != null && destinationLocation != null
                && Objects.equals(sourceLocation.getId(), destinationLocation.getId())) {
            throw new ValidationException("Origem e destino devem ser diferentes para essa movimentacao.");
        }
        if (sourceLocation != null && !Objects.equals(sourceLocation.getId(), equipment.getLocation().getId())) {
            throw new ValidationException("A localizacao de origem deve ser igual ao local atual do equipamento.");
        }
        if (movementType != MovementType.ENTRADA && equipment.getQuantity() <= 0) {
            throw new ValidationException("O equipamento nao possui saldo disponivel para essa movimentacao.");
        }
        if (movementType != MovementType.ENTRADA && movementQuantity > equipment.getQuantity()) {
            throw new ValidationException("A quantidade movimentada nao pode ser maior que o saldo atual do equipamento.");
        }
        if (movementType == MovementType.RETORNO_MANUTENCAO && equipment.getStatus() != EquipmentStatus.EM_MANUTENCAO) {
            throw new ValidationException("Somente equipamentos em manutencao podem registrar retorno de manutencao.");
        }
        if (movementType == MovementType.DEVOLUCAO_FUNCIONARIO && equipment.getStatus() != EquipmentStatus.EM_USO) {
            throw new ValidationException("Somente equipamentos em uso podem registrar devolucao com protocolo.");
        }
        if (movementType != MovementType.ENTRADA && equipment.getStatus() == EquipmentStatus.DESCARTADO) {
            throw new ValidationException("Equipamentos descartados nao podem receber novas movimentacoes.");
        }
        if (movementType == MovementType.ENTRADA && equipment.getQuantity() > 0 && destinationLocation != null
                && !Objects.equals(destinationLocation.getId(), equipment.getLocation().getId())) {
            throw new ValidationException("Entradas adicionais devem usar a mesma localizacao atual do registro, a menos que o saldo esteja zerado.");
        }
        if (movementType.requiresFullBalance() && movementQuantity != equipment.getQuantity()) {
            throw new ValidationException("Esse tipo de movimentacao exige a transferencia do saldo total do registro para manter a consistencia do estoque.");
        }
    }

    static void applyMovementToEquipment(
            Equipment equipment,
            MovementType movementType,
            int movementQuantity,
            Location destinationLocation,
            String responsibleName
    ) {
        if (movementType == MovementType.ENTRADA) {
            equipment.addQuantity(movementQuantity);
        }
        if (movementType.decreasesQuantity()) {
            equipment.removeQuantity(movementQuantity);
        }
        if (destinationLocation != null) {
            equipment.changeLocation(destinationLocation);
        }

        int resultingQuantity = equipment.getQuantity();
        equipment.changeStatus(resolveStatusAfterMovement(equipment.getStatus(), movementType, resultingQuantity));
        equipment.changeResponsibleName(responsibleName);
    }

    private static EquipmentStatus resolveStatusAfterMovement(EquipmentStatus currentStatus, MovementType movementType, int resultingQuantity) {
        return switch (movementType) {
            case ENTRADA -> EquipmentStatus.DISPONIVEL;
            case SAIDA, TRANSFERENCIA -> currentStatus;
            case ENTREGA_FUNCIONARIO -> EquipmentStatus.EM_USO;
            case DEVOLUCAO_FUNCIONARIO -> EquipmentStatus.DISPONIVEL;
            case ENVIO_MANUTENCAO -> EquipmentStatus.EM_MANUTENCAO;
            case RETORNO_MANUTENCAO -> EquipmentStatus.DISPONIVEL;
            case BAIXA_DESCARTE -> resultingQuantity == 0 ? EquipmentStatus.DESCARTADO : currentStatus;
        };
    }

    static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
