package br.com.estoqueti.service;

import br.com.estoqueti.config.DataSourceFactory;
import br.com.estoqueti.config.EntityManagerFactoryProvider;
import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.dto.common.LookupOptionDto;
import br.com.estoqueti.dto.delivery.DeliveryProtocolCreateRequest;
import br.com.estoqueti.dto.delivery.DeliveryProtocolResultDto;
import br.com.estoqueti.dto.equipment.EquipmentCreateRequest;
import br.com.estoqueti.dto.equipment.EquipmentListItemDto;
import br.com.estoqueti.dto.equipment.EquipmentReferenceDataDto;
import br.com.estoqueti.dto.equipment.EquipmentSearchFilter;
import br.com.estoqueti.dto.movement.StockMovementListItemDto;
import br.com.estoqueti.dto.movement.StockMovementSearchFilter;
import br.com.estoqueti.exception.ValidationException;
import br.com.estoqueti.model.enums.EquipmentStatus;
import br.com.estoqueti.model.enums.MovementType;
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

class DeliveryProtocolServiceIntegrationTest {

    private static final String TEST_PREFIX = "TST-DELIV-";

    private final EquipmentService equipmentService = new EquipmentService();
    private final StockMovementService stockMovementService = new StockMovementService();
    private final DeliveryProtocolService deliveryProtocolService = new DeliveryProtocolService();
    private final List<Path> generatedFiles = new ArrayList<>();

    @AfterEach
    void cleanUpTestData() {
        generatedFiles.forEach(this::deleteFileSilently);
        generatedFiles.clear();

        JpaExecutor.transaction(entityManager -> {
            entityManager.createNativeQuery("DELETE FROM estoque_ti.audit_log WHERE description LIKE :prefix")
                    .setParameter("prefix", "%" + TEST_PREFIX + "%")
                    .executeUpdate();
            entityManager.createNativeQuery("DELETE FROM estoque_ti.delivery_protocol WHERE equipment_id IN (SELECT id FROM estoque_ti.equipment WHERE internal_code LIKE :prefix)")
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
    void shouldRegisterDeliveryWithProtocolAndGenerateDocx() throws IOException {
        String internalCode = TEST_PREFIX + "DOCX-" + System.nanoTime();
        createTestEquipment(internalCode, 1, EquipmentStatus.DISPONIVEL, "Sala Tecnica");
        EquipmentListItemDto createdEquipment = findEquipmentByCode(internalCode);
        Path outputPath = Path.of("target", "test-output", internalCode + "-protocolo.docx");
        generatedFiles.add(outputPath);

        DeliveryProtocolResultDto result = deliveryProtocolService.registerDelivery(
                new DeliveryProtocolCreateRequest(
                        createdEquipment.id(),
                        1,
                        "Home office - Recife",
                        "Joao Almeida",
                        "12345678901",
                        "Analista de Suporte",
                        nowOffset(),
                        TEST_PREFIX + " Entrega inicial para trabalho presencial"
                ),
                outputPath,
                adminUser()
        );

        assertTrue(result.protocolNumber().startsWith("PTE-"));
        assertEquals(internalCode, result.equipmentInternalCode());
        assertTrue(Files.exists(outputPath));

        EquipmentListItemDto updatedEquipment = findEquipmentByCode(internalCode);
        assertEquals(EquipmentStatus.EM_USO, updatedEquipment.status());
        assertEquals("Joao Almeida", updatedEquipment.responsibleName());
        assertEquals("Sala Tecnica", updatedEquipment.locationName());

        List<StockMovementListItemDto> movements = stockMovementService.searchMovements(
                new StockMovementSearchFilter(createdEquipment.id(), MovementType.ENTREGA_FUNCIONARIO, null, null)
        );
        assertFalse(movements.isEmpty());
        assertEquals(MovementType.ENTREGA_FUNCIONARIO, movements.get(0).movementType());
        assertEquals("Home office - Recife", movements.get(0).destinationLocationName());

        ProtocolSnapshot snapshot = loadProtocolSnapshot(internalCode);
        assertEquals(result.protocolNumber(), snapshot.protocolNumber());
        assertEquals("123.456.789-01", snapshot.recipientCpf());
        assertEquals("Analista de Suporte", snapshot.recipientRole());

        String documentXml = readDocumentXml(outputPath);
        assertTrue(documentXml.contains(result.protocolNumber()));
        assertTrue(documentXml.contains("Joao Almeida"));
        assertTrue(documentXml.contains("123.456.789-01"));
        assertTrue(documentXml.contains("Analista de Suporte"));
        assertTrue(documentXml.contains(internalCode));
        assertTrue(documentXml.contains("Home office - Recife"));
        assertTrue(documentXml.contains("<w:br"));
        assertTrue(documentXml.contains("w:w=\"4040\""));

        String signatureSection = documentXml.substring(documentXml.indexOf("Assinatura do(a) Colaborador(a)"));
        assertTrue(signatureSection.indexOf("Nome:") < signatureSection.indexOf("Joao Almeida"));
        assertTrue(signatureSection.indexOf("Cargo:") < signatureSection.indexOf("Analista de Suporte"));
        assertTrue(signatureSection.indexOf("CPF:") < signatureSection.indexOf("123.456.789-01"));
    }

    @Test
    void shouldRejectDeliveryWhenCpfIsInvalid() {
        String internalCode = TEST_PREFIX + "CPF-" + System.nanoTime();
        createTestEquipment(internalCode, 1, EquipmentStatus.DISPONIVEL, "Sala Tecnica");
        EquipmentListItemDto createdEquipment = findEquipmentByCode(internalCode);
        Path outputPath = Path.of("target", "test-output", internalCode + "-protocolo.docx");
        generatedFiles.add(outputPath);

        assertThrows(ValidationException.class, () -> deliveryProtocolService.registerDelivery(
                new DeliveryProtocolCreateRequest(
                        createdEquipment.id(),
                        1,
                        "Home office - Recife",
                        "Maria Souza",
                        "123",
                        "Analista",
                        nowOffset(),
                        TEST_PREFIX + " CPF invalido"
                ),
                outputPath,
                adminUser()
        ));
    }

    private void createTestEquipment(String internalCode, int quantity, EquipmentStatus status, String locationName) {
        EquipmentReferenceDataDto referenceData = equipmentService.listReferenceData();

        equipmentService.createEquipment(
                new EquipmentCreateRequest(
                        internalCode,
                        "Equipamento de teste para entrega com protocolo",
                        requireOption(referenceData.categories(), "Notebook").id(),
                        "Dell",
                        "Latitude Teste",
                        internalCode + "-SN",
                        internalCode + "-PAT",
                        quantity,
                        0,
                        status,
                        requireOption(referenceData.locations(), locationName).id(),
                        TEST_PREFIX + " Responsavel anterior",
                        referenceData.suppliers().isEmpty() ? null : referenceData.suppliers().get(0).id(),
                        LocalDate.of(2026, 3, 25),
                        TEST_PREFIX + " Cadastro automatizado para testes de entrega"
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
                    SELECT protocol_number, recipient_cpf, recipient_role
                    FROM estoque_ti.delivery_protocol
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
        return ZonedDateTime.of(LocalDate.of(2026, 3, 25), LocalTime.of(16, 15), ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void deleteFileSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record ProtocolSnapshot(String protocolNumber, String recipientCpf, String recipientRole) {
    }
}