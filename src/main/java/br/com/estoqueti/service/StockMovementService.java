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
            String responsibleName = InventoryMovementSupport.normalizeRequired(request.responsibleName());
            String notes = InventoryMovementSupport.normalizeOptional(request.notes());
            int movementQuantity = request.quantity();

            InventoryMovementSupport.validateMovementRules(equipment, request.movementType(), movementQuantity, sourceLocation, destinationLocation);

            InventoryMovementSupport.applyMovementToEquipment(equipment, request.movementType(), movementQuantity, destinationLocation, responsibleName);
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
        if (request.movementType() == MovementType.ENTREGA_FUNCIONARIO || request.movementType() == MovementType.DEVOLUCAO_FUNCIONARIO) {
            throw new ValidationException("Use o fluxo de protocolo para registrar esse tipo de movimentacao.");
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
}
