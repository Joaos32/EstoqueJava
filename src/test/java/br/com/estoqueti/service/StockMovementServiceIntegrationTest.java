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
import br.com.estoqueti.dto.movement.StockMovementCreateRequest;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.MovementReferenceDataDto;
import br.com.estoqueti.exception.AuthorizationException;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.support.IntegrationTestDatabaseSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockMovementServiceIntegrationTest {

    private static final String TEST_PREFIX = "TST-MOV-";

    private final EquipmentService equipmentService = new EquipmentService();
    private final StockMovementService stockMovementService = new StockMovementService();

    @BeforeAll
    static void prepareBaseline() {
        IntegrationTestDatabaseSupport.ensureBaselineData();
    }

    @AfterEach
    void cleanUpTestData() {
        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE description LIKE :prefix")
                    .setParameter("prefix", "%" + TEST_PREFIX + "%")
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM estoque_ti.stock_movement WHERE equipment_id IN (SELECT id FROM estoque_ti.equipment WHERE internal_code LIKE :prefix)")
                    .setParameter("prefix", TEST_PREFIX + "%")
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
    void shouldListSeedMovements() {
        List<StockMovementListItemDto> movements = stockMovementService.searchMovements(null);

        assertTrue(movements.size() >= 9);
    }

    @Test
    void shouldRejectMovementRegistrationForViewerProfile() {
        AuthenticatedUserDto viewer = IntegrationTestDatabaseSupport.viewerUser();

        assertThrows(AuthorizationException.class, () -> stockMovementService.registerMovement(
                new StockMovementCreateRequest(1L, MovementType.ENTRADA, 1, null, 1L, "Equipe TI", nowOffset(), "Teste"),
                viewer
        ));
    }

    @Test
    void shouldIncreaseStockOnEntry() {
        String internalCode = TEST_PREFIX + "ENTRADA-" + System.nanoTime();
        createTestEquipment(internalCode, 0, EquipmentStatus.DISPONIVEL, "Almoxarifado TI");
        Long equipmentId = findEquipmentId(internalCode);
        Long locationId = requireLocation("Almoxarifado TI").id();

        stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.ENTRADA,
                        5,
                        null,
                        locationId,
                        "Equipe Estoque TI",
                        nowOffset(),
                        "Entrada para recompor saldo"
                ),
                adminUser()
        );

        EquipmentListItemDto equipment = findEquipmentByCode(internalCode);
        assertEquals(5, equipment.quantity());
        assertEquals(EquipmentStatus.DISPONIVEL, equipment.status());
        assertEquals("Almoxarifado TI", equipment.locationName());
    }

    @Test
    void shouldTransferWholeBalanceAndChangeLocation() {
        String internalCode = TEST_PREFIX + "TRANSF-" + System.nanoTime();
        createTestEquipment(internalCode, 3, EquipmentStatus.DISPONIVEL, "Almoxarifado TI");
        Long equipmentId = findEquipmentId(internalCode);

        stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.TRANSFERENCIA,
                        3,
                        requireLocation("Almoxarifado TI").id(),
                        requireLocation("Sala Tecnica").id(),
                        "Equipe Infra",
                        nowOffset(),
                        "Transferencia para preparo tecnico"
                ),
                adminUser()
        );

        EquipmentListItemDto equipment = findEquipmentByCode(internalCode);
        assertEquals(3, equipment.quantity());
        assertEquals("Sala Tecnica", equipment.locationName());
        assertEquals(EquipmentStatus.DISPONIVEL, equipment.status());
        assertEquals("Equipe Infra", equipment.responsibleName());
    }

    @Test
    void shouldRejectPartialTransferToPreserveConsistency() {
        String internalCode = TEST_PREFIX + "PARCIAL-" + System.nanoTime();
        createTestEquipment(internalCode, 4, EquipmentStatus.DISPONIVEL, "Almoxarifado TI");
        Long equipmentId = findEquipmentId(internalCode);

        assertThrows(ValidationException.class, () -> stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.TRANSFERENCIA,
                        2,
                        requireLocation("Almoxarifado TI").id(),
                        requireLocation("Sala Tecnica").id(),
                        "Equipe Infra",
                        nowOffset(),
                        "Tentativa parcial"
                ),
                adminUser()
        ));
    }

    @Test
    void shouldRejectExitAboveAvailableStock() {
        String internalCode = TEST_PREFIX + "SAIDA-" + System.nanoTime();
        createTestEquipment(internalCode, 2, EquipmentStatus.DISPONIVEL, "Sala Tecnica");
        Long equipmentId = findEquipmentId(internalCode);

        assertThrows(ValidationException.class, () -> stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.SAIDA,
                        5,
                        requireLocation("Sala Tecnica").id(),
                        null,
                        "Equipe TI",
                        nowOffset(),
                        "Saida acima do saldo"
                ),
                adminUser()
        ));
    }

    @Test
    void shouldSendEquipmentToMaintenanceAndReturnIt() {
        String internalCode = TEST_PREFIX + "MANUT-" + System.nanoTime();
        createTestEquipment(internalCode, 1, EquipmentStatus.DISPONIVEL, "Sala Tecnica");
        Long equipmentId = findEquipmentId(internalCode);

        stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.ENVIO_MANUTENCAO,
                        1,
                        requireLocation("Sala Tecnica").id(),
                        requireLocation("Manutencao Externa").id(),
                        "Fornecedor de Manutencao",
                        nowOffset(),
                        "Envio para diagnostico"
                ),
                adminUser()
        );

        EquipmentListItemDto afterSend = findEquipmentByCode(internalCode);
        assertEquals(EquipmentStatus.EM_MANUTENCAO, afterSend.status());
        assertEquals("Manutencao Externa", afterSend.locationName());

        stockMovementService.registerMovement(
                new StockMovementCreateRequest(
                        equipmentId,
                        MovementType.RETORNO_MANUTENCAO,
                        1,
                        requireLocation("Manutencao Externa").id(),
                        requireLocation("Sala Tecnica").id(),
                        "Equipe TI Interna",
                        nowOffset(),
                        "Retorno apos reparo"
                ),
                adminUser()
        );

        EquipmentListItemDto afterReturn = findEquipmentByCode(internalCode);
        assertEquals(EquipmentStatus.DISPONIVEL, afterReturn.status());
        assertEquals("Sala Tecnica", afterReturn.locationName());
        assertEquals("Equipe TI Interna", afterReturn.responsibleName());
    }

    private void createTestEquipment(String internalCode, int quantity, EquipmentStatus status, String locationName) {
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();

        equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        internalCode,
                        "Equipamento de teste de movimentacao",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Dell",
                        "Latitude Teste",
                        internalCode + "-SN",
                        internalCode + "-PAT",
                        quantity,
                        0,
                        status,
                        requireOption(referenceData.locations(), locationName).id(),
                        "Equipe QA",
                        referenceData.suppliers().isEmpty() ? null : referenceData.suppliers().get(0).id(),
                        LocalDate.of(2026, 3, 23),
                        "Cadastro automatizado para testes de movimentacao"
                ),
                adminUser()
        );
    }

    private EquipmentListItemDto findEquipmentByCode(String internalCode) {
        List<EquipmentListItemDto> result = equipmentService.searchEquipment(
                new EquipmentSearchFilter(null, internalCode, null, null, null, null, null, null)
        );

        assertFalse(result.isEmpty());
        return result.get(0);
    }

    private Long findEquipmentId(String internalCode) {
        return findEquipmentByCode(internalCode).id();
    }

    private LookupOptionDto requireLocation(String locationName) {
        MovementReferenceDataDto referenceData = stockMovementService.listReferenceData();
        return requireOption(referenceData.locations(), locationName);
    }

    private LookupOptionDto requireOption(List<LookupOptionDto> options, String label) {
        return options.stream()
                .filter(option -> option.label().equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Opcao obrigatoria nao encontrada: " + label));
    }

    private AuthenticatedUserDto adminUser() {
        return IntegrationTestDatabaseSupport.adminUser();
    }

    private java.time.OffsetDateTime nowOffset() {
        return ZonedDateTime.of(LocalDate.of(2026, 3, 23), LocalTime.of(14, 30), ZoneId.systemDefault()).toOffsetDateTime();
    }
}