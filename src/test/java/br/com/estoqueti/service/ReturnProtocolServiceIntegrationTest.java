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
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolCreateRequest;
import br.com.estoqueti.dto.returnprotocol.ReturnProtocolResultDto;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
import br.com.estoqueti.model.enums.ReturnProtocolReason;
import br.com.estoqueti.model.enums.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnProtocolServiceIntegrationTest {

    private static final String TEST_PREFIX = "TST-RET-";

    private final EquipmentService equipmentService = new EquipmentService();
    private final StockMovementService stockMovementService = new StockMovementService();
    private final ReturnProtocolService returnProtocolService = new ReturnProtocolService();
    private final List<Path> generatedFiles = new ArrayList<>();

    @AfterEach
    void cleanUpTestData() {
        generatedFiles.forEach(this::deleteFileSilently);
        generatedFiles.clear();

        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE description LIKE :prefix")
                    .setParameter("prefix", "%" + TEST_PREFIX + "%")
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM estoque_ti.return_protocol WHERE equipment_id IN (SELECT id FROM estoque_ti.equipment WHERE internal_code LIKE :prefix)")
                    .setParameter("prefix", TEST_PREFIX + "%")
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
    void shouldRegisterReturnWithProtocolAndGenerateDocx() throws IOException {
        String internalCode = TEST_PREFIX + "DOCX-" + System.nanoTime();
        createTestEquipment(internalCode, 1, EquipmentStatus.EM_USO, "Escritorio Administrativo", "Joao Almeida");
        EquipmentListItemDto createdEquipment = findEquipmentByCode(internalCode);
        Path outputPath = Path.of("target", "test-output", internalCode + "-devolucao.docx");
        generatedFiles.add(outputPath);

        ReturnProtocolResultDto result = returnProtocolService.registerReturn(
                new ReturnProtocolCreateRequest(
                        createdEquipment.id(),
                        1,
                        requireLocation("Almoxarifado TI").id(),
                        "Joao Almeida",
                        "12345678901",
                        "Carlos Tecnico",
                        "Tecnico de Infra",
                        "98765432100",
                        ReturnProtocolReason.OUTROS,
                        TEST_PREFIX + " Encerramento de projeto",
                        nowOffset(),
                        TEST_PREFIX + " Equipamento devolvido em bom estado"
                ),
                outputPath,
                adminUser()
        );

        assertTrue(result.protocolNumber().startsWith("PTD-"));
        assertEquals(internalCode, result.equipmentInternalCode());
        assertTrue(Files.exists(outputPath));

        EquipmentListItemDto updatedEquipment = findEquipmentByCode(internalCode);
        assertEquals(EquipmentStatus.DISPONIVEL, updatedEquipment.status());
        assertEquals("Carlos Tecnico", updatedEquipment.responsibleName());
        assertEquals("Almoxarifado TI", updatedEquipment.locationName());

        List<StockMovementListItemDto> movements = stockMovementService.searchMovements(
                new StockMovementSearchFilter(createdEquipment.id(), MovementType.DEVOLUCAO_FUNCIONARIO, null, null)
        );
        assertFalse(movements.isEmpty());
        assertEquals(MovementType.DEVOLUCAO_FUNCIONARIO, movements.get(0).movementType());

        ProtocolSnapshot snapshot = loadProtocolSnapshot(internalCode);
        assertEquals(result.protocolNumber(), snapshot.protocolNumber());
        assertEquals("123.456.789-01", snapshot.employeeCpf());
        assertEquals(ReturnProtocolReason.OUTROS.name(), snapshot.returnReason());

        String documentXml = readDocumentXml(outputPath);
        assertTrue(documentXml.contains(result.protocolNumber()));
        assertTrue(documentXml.contains("Joao Almeida"));
        assertTrue(documentXml.contains("123.456.789-01"));
        assertTrue(documentXml.contains("Carlos Tecnico"));
        assertTrue(documentXml.contains("987.654.321-00"));
        assertTrue(documentXml.contains(TEST_PREFIX + " Encerramento de projeto"));
        assertTrue(documentXml.contains(internalCode));
    }

    @Test
    void shouldRejectReturnWhenEquipmentIsNotInUse() {
        String internalCode = TEST_PREFIX + "STATUS-" + System.nanoTime();
        createTestEquipment(internalCode, 1, EquipmentStatus.DISPONIVEL, "Sala Tecnica", "Equipe Infra");
        EquipmentListItemDto createdEquipment = findEquipmentByCode(internalCode);
        Path outputPath = Path.of("target", "test-output", internalCode + "-devolucao.docx");
        generatedFiles.add(outputPath);

        assertThrows(ValidationException.class, () -> returnProtocolService.registerReturn(
                new ReturnProtocolCreateRequest(
                        createdEquipment.id(),
                        1,
                        requireLocation("Almoxarifado TI").id(),
                        "Maria Souza",
                        "12345678901",
                        "Carlos Tecnico",
                        "Tecnico de Infra",
                        "98765432100",
                        ReturnProtocolReason.DESLIGAMENTO_EMPRESA,
                        null,
                        nowOffset(),
                        TEST_PREFIX + " Status invalido"
                ),
                outputPath,
                adminUser()
        ));
    }

    private void createTestEquipment(String internalCode, int quantity, EquipmentStatus status, String locationName, String responsibleName) {
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();

        equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        internalCode,
                        "Equipamento de teste para devolucao com protocolo",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Dell",
                        "Latitude Teste",
                        internalCode + "-SN",
                        internalCode + "-PAT",
                        quantity,
                        0,
                        status,
                        requireOption(referenceData.locations(), locationName).id(),
                        responsibleName,
                        referenceData.suppliers().isEmpty() ? null : referenceData.suppliers().get(0).id(),
                        LocalDate.of(2026, 3, 25),
                        TEST_PREFIX + " Cadastro automatizado para testes de devolucao"
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

    private LookupOptionDto requireLocation(String locationName) {
        return requireOption(stockMovementService.listReferenceData().locations(), locationName);
    }

    private LookupOptionDto requireOption(List<LookupOptionDto> options, String label) {
        return options.stream()
                .filter(option -> option.label().equalsIgnoreCase(label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Opcao obrigatoria nao encontrada: " + label));
    }

    private ProtocolSnapshot loadProtocolSnapshot(String internalCode) {
        return JpaExecutor.query(entityManager -> {
            Object[] row = (Object[]) entityManager.createNativeQuery("""
                    SELECT protocol_number, employee_cpf, return_reason
                    FROM estoque_ti.return_protocol
                    WHERE equipment_id IN (
                        SELECT id FROM estoque_ti.equipment WHERE internal_code = :internalCode
                    )
                    ORDER BY id DESC
                    LIMIT 1
                    """)
                    .setParameter("internalCode", internalCode)
                    .getSingleResult();
            return new ProtocolSnapshot((String) row[0], (String) row[1], (String) row[2]);
        });
    }

    private String readDocumentXml(Path outputPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(outputPath.toFile())) {
            assertNotNull(zipFile.getEntry("word/document.xml"));
            return new String(zipFile.getInputStream(zipFile.getEntry("word/document.xml")).readAllBytes());
        }
    }

    private AuthenticatedUserDto adminUser() {
        return new AuthenticatedUserDto(1L, "Administrador do Sistema", "admin", Role.ADMIN, true);
    }

    private java.time.OffsetDateTime nowOffset() {
        return ZonedDateTime.of(LocalDate.of(2026, 3, 25), LocalTime.of(17, 0), ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void deleteFileSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record ProtocolSnapshot(String protocolNumber, String employeeCpf, String returnReason) {
    }
}
