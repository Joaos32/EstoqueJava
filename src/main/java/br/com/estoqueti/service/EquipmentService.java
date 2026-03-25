package br.com.estoqueti.service;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.equipment.EquipmentCreateRequest;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentReferenceDataDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.mapper.EquipmentMapper;
import br.com.estoqueti.model.entity.AuditLog;
import br.com.estoqueti.model.entity.Equipment;
import br.com.estoqueti.model.entity.EquipmentCategory;
import br.com.estoqueti.model.entity.Location;
import br.com.estoqueti.model.entity.Supplier;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.AuditAction;
import br.com.estoqueti.repository.AuditLogRepository;
import br.com.estoqueti.repository.EquipmentCategoryRepository;
import br.com.estoqueti.repository.EquipmentRepository;
import br.com.estoqueti.repository.LocationRepository;
import br.com.estoqueti.repository.SupplierRepository;
import br.com.estoqueti.repository.impl.JpaAuditLogRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentCategoryRepository;
import br.com.estoqueti.repository.impl.JpaEquipmentRepository;
import br.com.estoqueti.repository.impl.JpaLocationRepository;
import br.com.estoqueti.repository.impl.JpaSupplierRepository;
import br.com.estoqueti.util.WorkstationUtils;

import java.util.List;

public class EquipmentService {

    public List<EquipmentListItemDto> searchEquipment(EquipmentSearchFilter filter) {
        EquipmentSearchFilter effectiveFilter = filter == null
                ? new EquipmentSearchFilter(null, null, null, null, null, null, null, null)
                : filter;

        return JpaExecutor.query(entityManager -> {
            EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
            return equipmentRepository.searchActive(effectiveFilter)
                    .stream()
                    .map(EquipmentMapper::toListItemDto)
                    .toList();
        });
    }

    public EquipmentReferenceDataDto listReferenceData() {
        return JpaExecutor.query(entityManager -> {
            EquipmentCategoryRepository categoryRepository = new JpaEquipmentCategoryRepository(entityManager);
            LocationRepository locationRepository = new JpaLocationRepository(entityManager);
            SupplierRepository supplierRepository = new JpaSupplierRepository(entityManager);

            List<LookupOptionDto> categories = categoryRepository.findAllActiveOrderByName()
                    .stream()
                    .map(EquipmentMapper::toLookupOptionDto)
                    .toList();

            List<LookupOptionDto> locations = locationRepository.findAllActiveOrderByName()
                    .stream()
                    .map(EquipmentMapper::toLookupOptionDto)
                    .toList();

            List<LookupOptionDto> suppliers = supplierRepository.findAllActiveOrderByCorporateName()
                    .stream()
                    .map(EquipmentMapper::toLookupOptionDto)
                    .toList();

            return new EquipmentReferenceDataDto(categories, locations, suppliers);
        });
    }

    public EquipmentListItemDto createEquipment(EquipmentCreateRequest request, AuthenticatedUserDto authenticatedUser) {
        if (authenticatedUser == null || !authenticatedUser.canManageInventory()) {
            throw new AuthorizationException("Somente administradores e tecnicos podem cadastrar equipamentos.");
        }

        validateRequest(request);

        return JpaExecutor.transaction(entityManager -> {
            EquipmentRepository equipmentRepository = new JpaEquipmentRepository(entityManager);
            EquipmentCategoryRepository categoryRepository = new JpaEquipmentCategoryRepository(entityManager);
            LocationRepository locationRepository = new JpaLocationRepository(entityManager);
            SupplierRepository supplierRepository = new JpaSupplierRepository(entityManager);
            AuditLogRepository auditLogRepository = new JpaAuditLogRepository(entityManager);

            String normalizedInternalCode = normalizeRequired(request.internalCode());
            String normalizedName = normalizeRequired(request.name());
            String normalizedBrand = normalizeOptional(request.brand());
            String normalizedModel = normalizeOptional(request.model());
            String normalizedSerialNumber = normalizeOptional(request.serialNumber());
            String normalizedPatrimonyNumber = normalizeOptional(request.patrimonyNumber());
            String normalizedResponsibleName = normalizeOptional(request.responsibleName());
            String normalizedNotes = normalizeOptional(request.notes());

            if (equipmentRepository.existsByInternalCodeIgnoreCase(normalizedInternalCode)) {
                throw new ValidationException("Ja existe um equipamento cadastrado com esse codigo interno.");
            }
            if (normalizedSerialNumber != null && equipmentRepository.existsBySerialNumberIgnoreCase(normalizedSerialNumber)) {
                throw new ValidationException("Ja existe um equipamento cadastrado com esse numero de serie.");
            }
            if (normalizedPatrimonyNumber != null && equipmentRepository.existsByPatrimonyNumberIgnoreCase(normalizedPatrimonyNumber)) {
                throw new ValidationException("Ja existe um equipamento cadastrado com esse numero de patrimonio.");
            }

            EquipmentCategory category = categoryRepository.findActiveById(request.categoryId())
                    .orElseThrow(() -> new ValidationException("Selecione uma categoria valida."));

            Location location = locationRepository.findActiveById(request.locationId())
                    .orElseThrow(() -> new ValidationException("Selecione uma localizacao valida."));

            Supplier supplier = request.supplierId() == null ? null : supplierRepository.findActiveById(request.supplierId())
                    .orElseThrow(() -> new ValidationException("Selecione um fornecedor valido."));

            Equipment equipment = new Equipment(
                    normalizedInternalCode,
                    normalizedName,
                    category,
                    normalizedBrand,
                    normalizedModel,
                    normalizedSerialNumber,
                    normalizedPatrimonyNumber,
                    request.quantity(),
                    request.minimumStock() == null ? 0 : request.minimumStock(),
                    request.status(),
                    location,
                    normalizedResponsibleName,
                    supplier,
                    request.entryDate(),
                    normalizedNotes,
                    true
            );

            equipmentRepository.save(equipment);

            auditLogRepository.save(AuditLog.of(
                    entityManager.getReference(User.class, authenticatedUser.id()),
                    AuditAction.CADASTRO,
                    "equipment",
                    equipment.getId(),
                    "Cadastro de equipamento realizado: " + equipment.getInternalCode(),
                    WorkstationUtils.resolveStationIdentifier()
            ));

            return EquipmentMapper.toListItemDto(equipment);
        });
    }

    private void validateRequest(EquipmentCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Os dados do equipamento sao obrigatorios.");
        }
        if (request.internalCode() == null || request.internalCode().isBlank()) {
            throw new ValidationException("Informe o codigo interno do equipamento.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Informe o nome do equipamento.");
        }
        if (request.categoryId() == null) {
            throw new ValidationException("Selecione a categoria do equipamento.");
        }
        if (request.quantity() == null) {
            throw new ValidationException("Informe a quantidade em estoque.");
        }
        if (request.quantity() < 0) {
            throw new ValidationException("A quantidade em estoque nao pode ser negativa.");
        }
        if (request.minimumStock() != null && request.minimumStock() < 0) {
            throw new ValidationException("O estoque minimo nao pode ser negativo.");
        }
        if (request.status() == null) {
            throw new ValidationException("Selecione o status do equipamento.");
        }
        if (request.locationId() == null) {
            throw new ValidationException("Selecione a localizacao atual do equipamento.");
        }
        if (request.entryDate() == null) {
            throw new ValidationException("Informe a data de entrada do equipamento.");
        }
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