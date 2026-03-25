package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.movement.MovementEquipmentOptionDto;
import br.com.estoqueti.dto.movement.MovementReferenceDataDto;
import br.com.estoqueti.dto.movement.StockMovementCreateRequest;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.mapper.EquipmentMapper;
import br.com.estoqueti.mapper.StockMovementMapper;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.EquipmentRepository;
import br.com.estoqueti.repository.LocationRepository;
import br.com.estoqueti.repository.StockMovementRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentRepository;
import br.com.estoqueti.repository.impl.JpaLocationRepository;
import br.com.estoqueti.repository.impl.JpaStockMovementRepository;
import br.com.estoqueti.util.WorkstationUtils;

import java.util.List;
import java.util.Objects;

public class StockMovementService {

    public MovementReferenceDataDto listReferenceData() {
        return JpaExecutor.query(entityManager -> {
            EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
            LocationRepository locationRepository = new JpaLocationRepository(entityManager);

            List<MovementEquipmentOptionDto> equipments = equipmentRepository.findAllActiveOrderedByName()
                    .stream()
                    .map(StockMovementMapper::toEquipmentOptionDto)
                    .toList();

            List<LookupOptionDto> locations = locationRepository.findAllActiveOrderByName()
                    .stream()
                    .map(EquipmentMapper::toLookupOptionDto)
                    .toList();

            return new MovementReferenceDataDto(equipments, locations);
        });
    }

    public List<StockMovementListItemDto> searchMovements(StockMovementSearchFilter filter) {
        return JpaExecutor.query(entityManager -> {
            StockMovementRepository movementRepository = new JpaStockMovementRepository(entityManager);
            return movementRepository.search(filter)
                    .stream()
                    .map(StockMovementMapper::toListItemDto)
                    .toList();
        });
    }

    public StockMovementListItemDto registerMovement(StockMovementCreateRequest request, AuthenticatedUserDto authenticatedUser) {
        if (authenticatedUser == null || !authenticatedUser.canManageInventory()) {
            throw new AuthorizationException("Somente administradores e tecnicos podem registrar movimentacoes de estoque.");
        }

        validateRequest(request);

        return JpaExecutor.transaction(entityManager -> {
            EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
            LocationRepository locationRepository = new JpaLocationRepository(entityManager);
            StockMovementRepository stockMovementRepository = new JpaStockMovementRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

            Equipment equipment = equipmentRepository.findActiveByIdForUpdate(request.equipmentId())
                    .orElseThrow(() -> new ValidationException("Selecione um equipamento valido para movimentacao."));

            Location sourceLocation = resolveLocation(locationRepository, request.sourceLocationId(), "origem");
            Location destinationLocation = resolveLocation(locationRepository, request.destinationLocationId(), "destino");
            String responsibleName = normalizeRequired(request.responsibleName());
            String notes = normalizeOptional(request.notes());
            int movementQuantity = request.quantity();

            validateMovementRules(equipment, request.movementType(), movementQuantity, sourceLocation, destinationLocation);

            applyMovementToEquipment(equipment, request.movementType(), movementQuantity, destinationLocation, responsibleName);
            equipmentRepository.save(equipment);

            StockMovement stockMovement = StockMovement.of(
                    equipment,
                    request.movementType(),
                    movementQuantity,
                    sourceLocation,
                    destinationLocation,
                    responsibleName,
                    request.movementAt(),
                    notes,
                    entityManager.getReference(User.class, authenticatedUser.id())
            );

            stockMovementRepository.save(stockMovement);

            auditLogRepository.save(AuditLog.of(
                    entityManager.getReference(User.class, authenticatedUser.id()),
                    AuditAction.MOVIMENTACAO,
                    "stock_movement",
                    stockMovement.getId(),
                    "Movimentacao de estoque registrada: " + equipment.getInternalCode() + " - " + request.movementType().name(),
                    WorkstationUtils.resolveStationIdentifier()
            ));

            return StockMovementMapper.toListItemDto(stockMovement);
        });
    }

    private void validateRequest(StockMovementCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Os dados da movimentacao sao obrigatorios.");
        }
        if (request.equipmentId() == null) {
            throw new ValidationException("Selecione o equipamento que sera movimentado.");
        }
        if (request.movementType() == null) {
            throw new ValidationException("Selecione o tipo de movimentacao.");
        }
        if (request.quantity() == null) {
            throw new ValidationException("Informe a quantidade movimentada.");
        }
        if (request.quantity() <= 0) {
            throw new ValidationException("A quantidade movimentada deve ser maior que zero.");
        }
        if (request.responsibleName() == null || request.responsibleName().isBlank()) {
            throw new ValidationException("Informe o responsavel pela movimentacao.");
        }
        if (request.movementAt() == null) {
            throw new ValidationException("Informe a data e hora da movimentacao.");
        }
    }

    private Location resolveLocation(LocationRepository locationRepository, Long locationId, String fieldName) {
        if (locationId == null) {
            return null;
        }

        return locationRepository.findActiveById(locationId)
                .orElseThrow(() -> new ValidationException("Selecione uma localizacao de " + fieldName + " valida."));
    }

    private void validateMovementRules(
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
        if (movementType != MovementType.ENTRADA && equipment.getStatus() == EquipmentStatus.DESCARTADO) {
            throw new ValidationException("Equipamentos descartados nao podem receber novas movimentacoes.");
        }
        if (movementType == MovementType.ENTRADA && equipment.getQuantity() > 0 && destinationLocation != null
                && !Objects.equals(destinationLocation.getId(), equipment.getLocation().getId())) {
            throw new ValidationException("Entradas adicionais devem usar a mesma localizacao atual do registro, a menos que o saldo esteja zerado.");
        }
        if ((movementType == MovementType.TRANSFERENCIA
                || movementType == MovementType.ENVIO_MANUTENCAO
                || movementType == MovementType.RETORNO_MANUTENCAO)
                && movementQuantity != equipment.getQuantity()) {
            throw new ValidationException("Esse tipo de movimentacao exige a transferencia do saldo total do registro para manter a consistencia do estoque.");
        }
    }

    private void applyMovementToEquipment(
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

    private EquipmentStatus resolveStatusAfterMovement(EquipmentStatus currentStatus, MovementType movementType, int resultingQuantity) {
        return switch (movementType) {
            case ENTRADA -> EquipmentStatus.DISPONIVEL;
            case SAIDA, TRANSFERENCIA -> currentStatus;
            case ENVIO_MANUTENCAO -> EquipmentStatus.EM_MANUTENCAO;
            case RETORNO_MANUTENCAO -> EquipmentStatus.DISPONIVEL;
            case BAIXA_DESCARTE -> resultingQuantity == 0 ? EquipmentStatus.DESCARTADO : currentStatus;
        };
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}