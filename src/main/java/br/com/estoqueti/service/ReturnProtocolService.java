package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolCreateRequest;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolDocumentData;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolResultDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.entity.ReturnProtocol;
import br.com.estoqueti.model.entity.StockMovement;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.model.enums.ReturnProtocolReason;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.EquipmentRepository;
import br.com.estoqueti.repository.LocationRepository;
import br.com.estoqueti.repository.ReturnProtocolRepository;
import br.com.estoqueti.repository.StockMovementRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentRepository;
import br.com.estoqueti.repository.impl.JpaLocationRepository;
import br.com.estoqueti.repository.impl.JpaReturnProtocolRepository;
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

public class ReturnProtocolService {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter PROTOCOL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

    private final ReturnProtocolDocumentService documentService = new ReturnProtocolDocumentService();

    public ReturnProtocolResultDto registerReturn(
            ReturnProtocolCreateRequest request,
            Path outputPath,
            AuthenticatedUserDto authenticatedUser
    ) {
        if (authenticatedUser == null || !authenticatedUser.canManageInventory()) {
            throw new AuthorizationException("Somente administradores e tecnicos podem registrar devolucoes com protocolo.");
        }

        validateRequest(request, outputPath);
        Path normalizedOutputPath = outputPath.toAbsolutePath().normalize();

        try {
            return JpaExecutor.transaction(entityManager -> {
                EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
                LocationRepository locationRepository = new JpaLocationRepository(entityManager);
                StockMovementRepository stockMovementRepository = new JpaStockMovementRepository(entityManager);
                ReturnProtocolRepository returnProtocolRepository = new JpaReturnProtocolRepository(entityManager);
                AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

                Equipment equipment = equipmentRepository.findActiveByIdForUpdate(request.equipmentId())
                        .orElseThrow(() -> new ValidationException("Selecione um equipamento valido para devolucao."));

                Location sourceLocation = equipment.getLocation();
                Location destinationLocation = resolveLocation(locationRepository, request.destinationLocationId(), "destino");
                String employeeName = InventoryMovementSupport.normalizeRequired(request.employeeName());
                String employeeCpf = normalizeCpf(request.employeeCpf());
                String companyReceiverName = InventoryMovementSupport.normalizeRequired(request.companyReceiverName());
                String companyReceiverRole = InventoryMovementSupport.normalizeRequired(request.companyReceiverRole());
                String companyReceiverCpf = normalizeCpf(request.companyReceiverCpf());
                ReturnProtocolReason returnReason = request.returnReason();
                String otherReason = InventoryMovementSupport.normalizeOptional(request.otherReason());
                String notes = InventoryMovementSupport.normalizeOptional(request.notes());
                int returnQuantity = request.quantity();

                validateEquipmentForReturn(equipment);
                InventoryMovementSupport.validateMovementRules(equipment, MovementType.DEVOLUCAO_FUNCIONARIO, returnQuantity, sourceLocation, destinationLocation);

                String protocolNumber = generateProtocolNumber(request.returnedAt());
                String movementNotes = buildMovementNotes(protocolNumber, returnReason, otherReason, notes);
                String itemDescription = buildItemDescription(equipment);
                String itemIdentifier = buildItemIdentifier(equipment);
                String itemObservations = buildItemObservations(sourceLocation, destinationLocation, returnReason, otherReason, notes);

                ReturnProtocolDocumentData documentData = new ReturnProtocolDocumentData(
                        protocolNumber,
                        employeeName,
                        employeeCpf,
                        companyReceiverName,
                        companyReceiverRole,
                        companyReceiverCpf,
                        returnReason,
                        otherReason,
                        request.returnedAt(),
                        returnQuantity,
                        itemDescription,
                        itemIdentifier,
                        itemObservations
                );

                InventoryMovementSupport.applyMovementToEquipment(
                        equipment,
                        MovementType.DEVOLUCAO_FUNCIONARIO,
                        returnQuantity,
                        destinationLocation,
                        companyReceiverName
                );
                equipmentRepository.save(equipment);

                StockMovement stockMovement = StockMovement.of(
                        equipment,
                        MovementType.DEVOLUCAO_FUNCIONARIO,
                        returnQuantity,
                        sourceLocation,
                        destinationLocation,
                        companyReceiverName,
                        request.returnedAt(),
                        movementNotes,
                        entityManager.getReference(User.class, authenticatedUser.id())
                );
                stockMovementRepository.save(stockMovement);

                ReturnProtocol returnProtocol = ReturnProtocol.of(
                        protocolNumber,
                        stockMovement,
                        equipment,
                        employeeName,
                        employeeCpf,
                        companyReceiverName,
                        companyReceiverRole,
                        companyReceiverCpf,
                        returnReason,
                        otherReason,
                        returnQuantity,
                        itemDescription,
                        itemIdentifier,
                        itemObservations,
                        request.returnedAt(),
                        entityManager.getReference(User.class, authenticatedUser.id())
                );
                returnProtocolRepository.save(returnProtocol);

                documentService.generateDocument(documentData, normalizedOutputPath);

                auditLogRepository.save(AuditLog.of(
                        entityManager.getReference(User.class, authenticatedUser.id()),
                        AuditAction.MOVIMENTACAO,
                        "return_protocol",
                        returnProtocol.getId(),
                        "Devolucao com protocolo registrada: " + equipment.getInternalCode() + " - " + protocolNumber,
                        WorkstationUtils.resolveStationIdentifier()
                ));

                return new ReturnProtocolResultDto(protocolNumber, equipment.getInternalCode(), employeeName, normalizedOutputPath);
            });
        } catch (RuntimeException exception) {
            deleteGeneratedFile(normalizedOutputPath);
            throw exception;
        }
    }

    public String buildDefaultFileName(String equipmentInternalCode, String employeeName, LocalDate returnDate) {
        String normalizedEquipmentCode = sanitizeFileFragment(equipmentInternalCode == null ? "equipamento" : equipmentInternalCode);
        String normalizedEmployee = sanitizeFileFragment(employeeName == null ? "colaborador" : employeeName);
        LocalDate effectiveDate = returnDate == null ? LocalDate.now() : returnDate;
        return "protocolo-devolucao-" + normalizedEquipmentCode + "-" + normalizedEmployee + "-" + FILE_DATE_FORMATTER.format(effectiveDate) + ".docx";
    }

    private void validateRequest(ReturnProtocolCreateRequest request, Path outputPath) {
        if (request == null) {
            throw new ValidationException("Os dados da devolucao com protocolo sao obrigatorios.");
        }
        if (request.equipmentId() == null) {
            throw new ValidationException("Selecione o equipamento que sera devolvido.");
        }
        if (request.quantity() == null) {
            throw new ValidationException("Informe a quantidade da devolucao.");
        }
        if (request.quantity() <= 0) {
            throw new ValidationException("A quantidade da devolucao deve ser maior que zero.");
        }
        if (request.destinationLocationId() == null) {
            throw new ValidationException("Selecione a localizacao de destino da devolucao.");
        }
        if (request.employeeName() == null || request.employeeName().isBlank()) {
            throw new ValidationException("Informe o nome do colaborador que esta devolvendo o equipamento.");
        }
        if (request.employeeCpf() == null || request.employeeCpf().isBlank()) {
            throw new ValidationException("Informe o CPF do colaborador que esta devolvendo o equipamento.");
        }
        if (request.companyReceiverName() == null || request.companyReceiverName().isBlank()) {
            throw new ValidationException("Informe o responsavel da empresa que recebeu a devolucao.");
        }
        if (request.companyReceiverRole() == null || request.companyReceiverRole().isBlank()) {
            throw new ValidationException("Informe o cargo do responsavel da empresa.");
        }
        if (request.companyReceiverCpf() == null || request.companyReceiverCpf().isBlank()) {
            throw new ValidationException("Informe o CPF do responsavel da empresa.");
        }
        if (request.returnReason() == null) {
            throw new ValidationException("Selecione o motivo da devolucao.");
        }
        if (request.returnReason() == ReturnProtocolReason.OUTROS && (request.otherReason() == null || request.otherReason().isBlank())) {
            throw new ValidationException("Descreva o motivo da devolucao quando a opcao 'Outros' for selecionada.");
        }
        if (request.returnedAt() == null) {
            throw new ValidationException("Informe a data e hora da devolucao.");
        }
        if (outputPath == null) {
            throw new ValidationException("Escolha onde o arquivo do protocolo sera salvo.");
        }
        if (!outputPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new ValidationException("O protocolo precisa ser salvo no formato DOCX.");
        }
    }

    private void validateEquipmentForReturn(Equipment equipment) {
        if (equipment.getStatus() != EquipmentStatus.EM_USO) {
            throw new ValidationException("A devolucao com protocolo exige que o equipamento esteja com status Em uso.");
        }
        if (equipment.getStatus() == EquipmentStatus.DESCARTADO) {
            throw new ValidationException("Equipamentos descartados nao podem registrar devolucao com protocolo.");
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

    private String buildMovementNotes(String protocolNumber, ReturnProtocolReason returnReason, String otherReason, String notes) {
        List<String> parts = new ArrayList<>();
        parts.add("Protocolo " + protocolNumber);
        parts.add("Motivo: " + describeReason(returnReason, otherReason));
        if (notes != null && !notes.isBlank()) {
            parts.add(notes);
        }
        return String.join(" | ", parts);
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

    private String buildItemObservations(Location sourceLocation, Location destinationLocation, ReturnProtocolReason returnReason, String otherReason, String notes) {
        List<String> observations = new ArrayList<>();
        observations.add("Origem: " + sourceLocation.getName());
        observations.add("Destino: " + destinationLocation.getName());
        observations.add("Motivo: " + describeReason(returnReason, otherReason));
        if (notes != null && !notes.isBlank()) {
            observations.add(notes);
        }
        return String.join(" | ", observations);
    }

    private String describeReason(ReturnProtocolReason returnReason, String otherReason) {
        if (returnReason == ReturnProtocolReason.OUTROS) {
            return "Outros - " + InventoryMovementSupport.normalizeRequired(otherReason);
        }
        return returnReason.getDisplayName();
    }

    private String generateProtocolNumber(OffsetDateTime returnedAt) {
        String datePart = PROTOCOL_DATE_FORMATTER.format(returnedAt.toLocalDate());
        return "PTD-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
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
