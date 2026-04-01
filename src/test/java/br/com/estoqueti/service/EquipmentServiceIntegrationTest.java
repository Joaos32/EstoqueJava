package br.com.estoqueti.service;

import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.equipment.EquipmentCreateRequest;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentReferenceDataDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.support.IntegrationTestDatabaseSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentServiceIntegrationTest {

    private static final String TEST_PREFIX = "TST-EQP-";

    private final EquipmentService equipmentService = new EquipmentService();

    @BeforeAll
    static void prepareBaseline() {
        IntegrationTestDatabaseSupport.ensureBaselineData();
    }

    @AfterEach
    void cleanUpTestData() {
        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE entity_name = 'equipment' AND description LIKE :description")
                    .setParameter("description", "%" + TEST_PREFIX + "%")
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM estoque_ti.equipment WHERE internal_code LIKE :prefix")
                    .setParameter("prefix", TEST_PREFIX + "%")
                    .executeUpdate();
            return null;
        });
    }

    @AfterAll
    static void tearDown() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }

    @Test
    void shouldListSeedEquipment() {
        List<EquipmentListItemDto> equipments = equipmentService.searchEquipment(new EquipmentSearchFilter(null, null, null, null, null, null, null, null));

        assertTrue(equipments.size() >= 5);
    }

    @Test
    void shouldFilterByInternalCode() {
        List<EquipmentListItemDto> equipments = equipmentService.searchEquipment(new EquipmentSearchFilter(null, "NTB-0001", null, null, null, null, null, null));

        assertFalse(equipments.isEmpty());
        assertEquals("NTB-0001", equipments.get(0).internalCode());
    }

    @Test
    void shouldRejectEquipmentCreationForViewerProfile() {
        AuthenticatedUserDto visualizador = IntegrationTestDatabaseSupport.viewerUser();
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();

        assertThrows(AuthorizationException.class, () -> equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        TEST_PREFIX + "VIEW",
                        "Equipamento Bloqueado",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Dell",
                        "Latitude",
                        null,
                        null,
                        1,
                        0,
                        EquipmentStatus.DISPONIVEL,
                        requireOption(referenceData.locations(), "Almoxarifado TI").id(),
                        null,
                        null,
                        LocalDate.of(2026, 3, 23),
                        "Teste de permissao"
                ),
                visualizador
        ));
    }

    @Test
    void shouldRejectDuplicateInternalCode() {
        AuthenticatedUserDto admin = IntegrationTestDatabaseSupport.adminUser();
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();

        assertThrows(ValidationException.class, () -> equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        "NTB-0001",
                        "Notebook Duplicado",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Dell",
                        "Latitude",
                        null,
                        null,
                        1,
                        0,
                        EquipmentStatus.DISPONIVEL,
                        requireOption(referenceData.locations(), "Almoxarifado TI").id(),
                        null,
                        null,
                        LocalDate.of(2026, 3, 23),
                        "Teste de duplicidade"
                ),
                admin
        ));
    }

    @Test
    void shouldCreateEquipmentForAdmin() {
        AuthenticatedUserDto admin = IntegrationTestDatabaseSupport.adminUser();
        EquipmentListItemDto created = createTestEquipment(admin, 2);

        List<EquipmentListItemDto> searchResult = equipmentService.searchEquipment(
                new EquipmentSearchFilter(null, created.internalCode(), null, null, null, null, null, null)
        );

        assertEquals(created.internalCode(), searchResult.get(0).internalCode());
        assertEquals(1, searchResult.size());
    }

    @Test
    void shouldRejectEquipmentRemovalForViewerProfile() {
        AuthenticatedUserDto admin = IntegrationTestDatabaseSupport.adminUser();
        AuthenticatedUserDto visualizador = IntegrationTestDatabaseSupport.viewerUser();
        EquipmentListItemDto created = createTestEquipment(admin, 0);

        assertThrows(AuthorizationException.class, () -> equipmentService.deactivateEquipment(created.id(), visualizador));
    }

    @Test
    void shouldRejectRemovalWhenEquipmentHasAvailableQuantity() {
        AuthenticatedUserDto admin = IntegrationTestDatabaseSupport.adminUser();
        EquipmentListItemDto created = createTestEquipment(admin, 1);

        assertThrows(ValidationException.class, () -> equipmentService.deactivateEquipment(created.id(), admin));
    }

    @Test
    void shouldDeactivateEquipmentWithZeroQuantity() {
        AuthenticatedUserDto admin = IntegrationTestDatabaseSupport.adminUser();
        EquipmentListItemDto created = createTestEquipment(admin, 0);

        EquipmentListItemDto removed = equipmentService.deactivateEquipment(created.id(), admin);
        List<EquipmentListItemDto> searchResult = equipmentService.searchEquipment(
                new EquipmentSearchFilter(null, created.internalCode(), null, null, null, null, null, null)
        );
        boolean active = JpaExecutor.query(entityManager -> (Boolean) entityManager.createNativeQuery(
                        "SELECT active FROM estoque_ti.equipment WHERE id = :id")
                .setParameter("id", created.id())
                .getSingleResult());

        assertEquals(created.id(), removed.id());
        assertTrue(searchResult.isEmpty());
        assertFalse(active);
    }

    private EquipmentListItemDto createTestEquipment(AuthenticatedUserDto authenticatedUser, int quantity) {
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();
        String internalCode = TEST_PREFIX + System.nanoTime();
        String serialNumber = TEST_PREFIX + "SN-" + System.currentTimeMillis();
        String patrimonyNumber = TEST_PREFIX + "PAT-" + System.currentTimeMillis();
        Long supplierId = referenceData.suppliers().isEmpty() ? null : referenceData.suppliers().get(0).id();

        return equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        internalCode,
                        "Equipamento de Integracao",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Lenovo",
                        "ThinkPad L14",
                        serialNumber,
                        patrimonyNumber,
                        quantity,
                        0,
                        EquipmentStatus.DISPONIVEL,
                        requireOption(referenceData.locations(), "Almoxarifado TI").id(),
                        "Equipe QA",
                        supplierId,
                        LocalDate.of(2026, 3, 23),
                        "Cadastro de teste automatizado"
                ),
                authenticatedUser
        );
    }

    private LookupOptionDto requireOption(List<LookupOptionDto> options, String label) {
        return options.stream()
                .filter(option -> option.label().equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Opcao obrigatoria nao encontrada: " + label));
    }
}
