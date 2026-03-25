package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.delivery.DeliveryProtocolCreateRequest;
import br.com.estoqueti.dto.delivery.DeliveryProtocolDocumentData;
import br.com.estoqueti.dto.delivery.DeliveryProtocolResultDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.DeliveryProtocol;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.DeliveryProtocolRepository;
import br.com.estoqueti.repository.EquipmentRepository;
import br.com.estoqueti.repository.LocationRepository;
import br.com.estoqueti.repository.StockMovementRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaDeliveryProtocolRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentRepository;
import br.com.estoqueti.repository.impl.JpaLocationRepository;
import br.com.estoqueti.repository.impl.JpaStockMovementRepository;
import br.com.estoqueti.util.WorkstationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DeliveryProtocolService {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter PROTOCOL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

    private final DeliveryProtocolDocumentService documentService = new DeliveryProtocolDocumentService();

    public DeliveryProtocolResultDto registerDelivery(
            DeliveryProtocolCreateRequest request,
            Path outputPath,
            AuthenticatedUserDto authenticatedUser
    ) {
        if (authenticatedUser == null || !authenticatedUser.canManageInventory()) {
            throw new AuthorizationException("Somente administradores e tecnicos podem registrar entregas com protocolo.");
        }

        validateRequest(request, outputPath);
        Path normalizedOutputPath = outputPath.toAbsolutePath().normalize();

        try {
            return JpaExecutor.transaction(entityManager -> {
                EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
                LocationRepository locationRepository = new JpaLocationRepository(entityManager);
                StockMovementRepository stockMovementRepository = new JpaStockMovementRepository(entityManager);
                DeliveryProtocolRepository deliveryProtocolRepository = new JpaDeliveryProtocolRepository(entityManager);
                AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

                Equipment equipment = equipmentRepository.findActiveByIdForUpdate(request.equipmentId())
                        .orElseThrow(() -> new ValidationException("Selecione um equipamento valido para entrega."));

                Location sourceLocation = equipment.getLocation();
                Location destinationLocation = resolveLocation(locationRepository, request.destinationLocationId(), "destino");
                String recipientName = InventoryMovementSupport.normalizeRequired(request.recipientName());
                String recipientCpf = normalizeCpf(request.recipientCpf());
                String recipientRole = InventoryMovementSupport.normalizeRequired(request.recipientRole());
                String notes = InventoryMovementSupport.normalizeOptional(request.notes());
                int deliveryQuantity = request.quantity();

                validateEquipmentForDelivery(equipment);
                InventoryMovementSupport.validateMovementRules(equipment, MovementType.ENTREGA_FUNCIONARIO, deliveryQuantity, sourceLocation, destinationLocation);

                String protocolNumber = generateProtocolNumber(request.deliveryAt());
                String movementNotes = buildMovementNotes(protocolNumber, notes);
                String itemDescription = buildItemDescription(equipment);
                String itemIdentifier = buildItemIdentifier(equipment);
                String itemObservations = buildItemObservations(sourceLocation, destinationLocation, notes);

                DeliveryProtocolDocumentData documentData = new DeliveryProtocolDocumentData(
                        protocolNumber,
                        recipientName,
                        recipientCpf,
                        recipientRole,
                        request.deliveryAt(),
                        deliveryQuantity,
                        itemDescription,
                        itemIdentifier,
                        itemObservations
                );

                InventoryMovementSupport.applyMovementToEquipment(
                        equipment,
                        MovementType.ENTREGA_FUNCIONARIO,
                        deliveryQuantity,
                        destinationLocation,
                        recipientName
                );
                equipmentRepository.save(equipment);

                StockMovement stockMovement = StockMovement.of(
                        equipment,
                        MovementType.ENTREGA_FUNCIONARIO,
                        deliveryQuantity,
                        sourceLocation,
                        destinationLocation,
                        recipientName,
                        request.deliveryAt(),
                        movementNotes,
                        entityManager.getReference(User.class, authenticatedUser.id())
                );
                stockMovementRepository.save(stockMovement);

                DeliveryProtocol deliveryProtocol = DeliveryProtocol.of(
                        protocolNumber,
                        stockMovement,
                        equipment,
                        recipientName,
                        recipientCpf,
                        recipientRole,
                        deliveryQuantity,
                        itemDescription,
                        itemIdentifier,
                        itemObservations,
                        request.deliveryAt(),
                        entityManager.getReference(User.class, authenticatedUser.id())
                );
                deliveryProtocolRepository.save(deliveryProtocol);

                documentService.generateDocument(documentData, normalizedOutputPath);

                auditLogRepository.save(AuditLog.of(
                        entityManager.getReference(User.class, authenticatedUser.id()),
                        AuditAction.MOVIMENTACAO,
                        "delivery_protocol",
                        deliveryProtocol.getId(),
                        "Entrega com protocolo registrada: " + equipment.getInternalCode() + " - " + protocolNumber,
                        WorkstationUtils.resolveStationIdentifier()
                ));

                return new DeliveryProtocolResultDto(protocolNumber, equipment.getInternalCode(), recipientName, normalizedOutputPath);
            });
        } catch (RuntimeException exception) {
            deleteGeneratedFile(normalizedOutputPath);
            throw exception;
        }
    }

    public String buildDefaultFileName(String equipmentInternalCode, String recipientName, LocalDate deliveryDate) {
        String normalizedEquipmentCode = sanitizeFileFragment(equipmentInternalCode == null ? "equipamento" : equipmentInternalCode);
        String normalizedRecipient = sanitizeFileFragment(recipientName == null ? "colaborador" : recipientName);
        LocalDate effectiveDate = deliveryDate == null ? LocalDate.now() : deliveryDate;
        return "protocolo-entrega-" + normalizedEquipmentCode + "-" + normalizedRecipient + "-" + FILE_DATE_FORMATTER.format(effectiveDate) + ".docx";
    }

    private void validateRequest(DeliveryProtocolCreateRequest request, Path outputPath) {
        if (request == null) {
            throw new ValidationException("Os dados da entrega com protocolo sao obrigatorios.");
        }
        if (request.equipmentId() == null) {
            throw new ValidationException("Selecione o equipamento que sera entregue.");
        }
        if (request.quantity() == null) {
            throw new ValidationException("Informe a quantidade da entrega.");
        }
        if (request.quantity() <= 0) {
            throw new ValidationException("A quantidade da entrega deve ser maior que zero.");
        }
        if (request.destinationLocationId() == null) {
            throw new ValidationException("Selecione a localizacao de destino da entrega.");
        }
        if (request.recipientName() == null || request.recipientName().isBlank()) {
            throw new ValidationException("Informe o nome completo do colaborador responsavel.");
        }
        if (request.recipientCpf() == null || request.recipientCpf().isBlank()) {
            throw new ValidationException("Informe o CPF do colaborador responsavel.");
        }
        if (request.recipientRole() == null || request.recipientRole().isBlank()) {
            throw new ValidationException("Informe o cargo do colaborador responsavel.");
        }
        if (request.deliveryAt() == null) {
            throw new ValidationException("Informe a data e hora da entrega.");
        }
        if (outputPath == null) {
            throw new ValidationException("Escolha onde o arquivo do protocolo sera salvo.");
        }
        if (!outputPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new ValidationException("O protocolo precisa ser salvo no formato DOCX.");
        }
    }

    private void validateEquipmentForDelivery(Equipment equipment) {
        if (equipment.getStatus() == EquipmentStatus.EM_MANUTENCAO) {
            throw new ValidationException("Equipamentos em manutencao nao podem ser entregues com protocolo.");
        }
        if (equipment.getStatus() == EquipmentStatus.DEFEITUOSO) {
            throw new ValidationException("Equipamentos com status Defeituoso nao podem ser entregues com protocolo.");
        }
        if (equipment.getStatus() == EquipmentStatus.DESCARTADO) {
            throw new ValidationException("Equipamentos descartados nao podem ser entregues com protocolo.");
        }
    }

    private Location resolveLocation(LocationRepository locationRepository, Long locationId, String fieldName) {
        return locationRepository.findActiveById(locationId)
                .orElseThrow(() -> new ValidationException("Selecione uma localizacao de " + fieldName + " valida."));
    }

    private String normalizeCpf(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new ValidationException("Informe um CPF valido com 11 digitos.");
        }
        return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String buildMovementNotes(String protocolNumber, String notes) {
        if (notes == null || notes.isBlank()) {
            return "Protocolo " + protocolNumber;
        }
        return "Protocolo " + protocolNumber + " | " + notes;
    }

    private String buildItemDescription(Equipment equipment) {
        List<String> parts = new ArrayList<>();
        parts.add(equipment.getName());
        if (equipment.getBrand() != null && !equipment.getBrand().isBlank()) {
            parts.add(equipment.getBrand());
        }
        if (equipment.getModel() != null && !equipment.getModel().isBlank()) {
            parts.add(equipment.getModel());
        }
        return String.join(" | ", parts);
    }

    private String buildItemIdentifier(Equipment equipment) {
        List<String> identifiers = new ArrayList<>();
        identifiers.add("Codigo: " + equipment.getInternalCode());
        if (equipment.getSerialNumber() != null && !equipment.getSerialNumber().isBlank()) {
            identifiers.add("Serie: " + equipment.getSerialNumber());
        }
        if (equipment.getPatrimonyNumber() != null && !equipment.getPatrimonyNumber().isBlank()) {
            identifiers.add("Patrimonio: " + equipment.getPatrimonyNumber());
        }
        return String.join(" | ", identifiers);
    }

    private String buildItemObservations(Location sourceLocation, Location destinationLocation, String notes) {
        List<String> observations = new ArrayList<>();
        observations.add("Origem: " + sourceLocation.getName());
        observations.add("Destino: " + destinationLocation.getName());
        if (notes != null && !notes.isBlank()) {
            observations.add(notes);
        }
        return String.join(" | ", observations);
    }

    private String generateProtocolNumber(OffsetDateTime deliveryAt) {
        String datePart = PROTOCOL_DATE_FORMATTER.format(deliveryAt.toLocalDate());
        return "PTE-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String sanitizeFileFragment(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "arquivo" : normalized;
    }

    private void deleteGeneratedFile(Path outputPath) {
        try {
            Files.deleteIfExists(outputPath);
        } catch (IOException ignored) {
        }
    }
}