package br.com.estoqueti.support;

import br.com.estoqueti.config.JpaExecutor;
import br.com.estoqueti.dto.auth.AuthenticatedUserDto;
import br.com.estoqueti.model.entity.User;
import br.com.estoqueti.model.enums.Role;
import br.com.estoqueti.util.PasswordUtils;
import jakarta.persistence.EntityManager;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public final class IntegrationTestDatabaseSupport {

    private static final String ADMIN_USERNAME = "itest.admin";
    private static final String TECH_USERNAME = "itest.tech";
    private static final String VIEWER_USERNAME = "itest.viewer";

    private static volatile boolean baselineEnsured;

    private IntegrationTestDatabaseSupport() {
    }

    public static synchronized void ensureBaselineData() {
        if (baselineEnsured && hasBaselineData()) {
            return;
        }

        JpaExecutor.transaction(entityManager -> {
            ensureSupportUsers(entityManager);
            ensureCategories(entityManager);
            ensureLocations(entityManager);
            ensureSuppliers(entityManager);
            ensureBaselineEquipment(entityManager);
            ensureBaselineMovements(entityManager);
            return null;
        });

        baselineEnsured = true;
    }

    public static AuthenticatedUserDto adminUser() {
        ensureBaselineData();
        return loadUserByUsername(ADMIN_USERNAME);
    }

    public static AuthenticatedUserDto technicianUser() {
        ensureBaselineData();
        return loadUserByUsername(TECH_USERNAME);
    }

    public static AuthenticatedUserDto viewerUser() {
        ensureBaselineData();
        return loadUserByUsername(VIEWER_USERNAME);
    }

    private static boolean hasBaselineData() {
        return JpaExecutor.query(entityManager -> count(entityManager,
                "SELECT COUNT(*) FROM estoque_ti.equipment WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')") >= 5L);
    }

    private static void ensureSupportUsers(EntityManager entityManager) {
        ensureUser(entityManager, "Administrador de Integracao", ADMIN_USERNAME, Role.ADMIN);
        ensureUser(entityManager, "Tecnico de Integracao", TECH_USERNAME, Role.TECNICO);
        ensureUser(entityManager, "Visualizador de Integracao", VIEWER_USERNAME, Role.VISUALIZADOR);
    }

    private static void ensureUser(EntityManager entityManager, String fullName, String username, Role role) {
        if (findUser(entityManager, username).isPresent()) {
            return;
        }

        entityManager.persist(new User(fullName, username, PasswordUtils.hash("Senha@123"), role, true));
        entityManager.flush();
    }

    private static void ensureCategories(EntityManager entityManager) {
        insertCategoryIfAbsent(entityManager, "Notebook", "Computadores portateis corporativos");
        insertCategoryIfAbsent(entityManager, "Monitor", "Monitores LCD e LED");
        insertCategoryIfAbsent(entityManager, "Mouse", "Mouses USB e sem fio");
        insertCategoryIfAbsent(entityManager, "Switch", "Switches gerenciaveis e nao gerenciaveis");
        insertCategoryIfAbsent(entityManager, "SSD", "Unidades de armazenamento SSD");
        insertCategoryIfAbsent(entityManager, "Escritorio", "Materiais e itens de escritorio");
        insertCategoryIfAbsent(entityManager, "Equipamento", "Equipamentos gerais e itens diversos de operacao");
    }

    private static void ensureLocations(EntityManager entityManager) {
        insertLocationIfAbsent(entityManager, "Almoxarifado TI", "Estoque central de itens de tecnologia");
        insertLocationIfAbsent(entityManager, "Sala Tecnica", "Area tecnica interna");
        insertLocationIfAbsent(entityManager, "Escritorio Administrativo", "Postos de trabalho do administrativo");
        insertLocationIfAbsent(entityManager, "Filial Sao Paulo", "Unidade operacional de Sao Paulo");
        insertLocationIfAbsent(entityManager, "Manutencao Externa", "Itens enviados para parceiro de manutencao");
        insertLocationIfAbsent(entityManager, "Descarte", "Area destinada a itens baixados ou descartados");
        insertLocationIfAbsent(entityManager, "Estoque Escritorio", "Estoque de materiais e itens de escritorio");
    }

    private static void ensureSuppliers(EntityManager entityManager) {
        insertSupplierIfAbsent(entityManager,
                "Tech Distribuidora Ltda",
                "Tech Distribuidora",
                "12.345.678/0001-90",
                "Renata Lima",
                "(11) 3322-1100",
                "contato@techdistribuidora.com.br");
        insertSupplierIfAbsent(entityManager,
                "Infra Network Solutions Ltda",
                "Infra Network",
                "23.456.789/0001-01",
                "Marcos Souza",
                "(11) 3456-2200",
                "vendas@infranetwork.com.br");
        insertSupplierIfAbsent(entityManager,
                "Office Equipamentos S.A.",
                "Office Equipamentos",
                "34.567.890/0001-12",
                "Paula Mendes",
                "(11) 3678-3300",
                "comercial@officeequip.com.br");
    }

    private static void ensureBaselineEquipment(EntityManager entityManager) {
        ensureEquipment(entityManager,
                "NTB-0001",
                "Notebook Corporativo Dell Latitude 5440",
                "Notebook",
                "Dell",
                "Latitude 5440",
                "DL5440SN001",
                "PAT-1001",
                1,
                1,
                "EM_USO",
                "Filial Sao Paulo",
                "Joao Almeida",
                "Tech Distribuidora Ltda",
                Date.valueOf("2026-01-10"),
                "Equipamento entregue ao setor comercial.");
        ensureEquipment(entityManager,
                "MON-0010",
                "Monitor Dell 24 Polegadas",
                "Monitor",
                "Dell",
                "P2422H",
                null,
                null,
                8,
                3,
                "DISPONIVEL",
                "Almoxarifado TI",
                null,
                "Office Equipamentos S.A.",
                Date.valueOf("2026-02-15"),
                "Lote para reposicao de estacoes de trabalho.");
        ensureEquipment(entityManager,
                "MOU-0025",
                "Mouse Logitech M90",
                "Mouse",
                "Logitech",
                "M90",
                null,
                null,
                25,
                10,
                "DISPONIVEL",
                "Almoxarifado TI",
                null,
                "Office Equipamentos S.A.",
                Date.valueOf("2026-02-20"),
                "Item de consumo recorrente.");
        ensureEquipment(entityManager,
                "SWT-0003",
                "Switch TP-Link Gerenciavel 28 Portas",
                "Switch",
                "TP-Link",
                "TL-SG3428",
                "TLSG3428SN003",
                "PAT-2003",
                1,
                0,
                "EM_MANUTENCAO",
                "Manutencao Externa",
                "Equipe Infra",
                "Infra Network Solutions Ltda",
                Date.valueOf("2025-11-30"),
                "Equipamento enviado para avaliacao tecnica.");
        ensureEquipment(entityManager,
                "SSD-0012",
                "SSD Kingston A400 480GB",
                "SSD",
                "Kingston",
                "A400 480GB",
                null,
                null,
                4,
                5,
                "DISPONIVEL",
                "Sala Tecnica",
                null,
                "Tech Distribuidora Ltda",
                Date.valueOf("2026-02-28"),
                "Estoque abaixo do minimo para manutencoes preventivas.");
    }

    private static void ensureBaselineMovements(EntityManager entityManager) {
        Long adminUserId = requireUserId(entityManager, ADMIN_USERNAME);
        Long techUserId = requireUserId(entityManager, TECH_USERNAME);

        ensureMovement(entityManager, "NTB-0001", "ENTRADA", 1, null, "Almoxarifado TI", "Administrador do Sistema", OffsetDateTime.parse("2026-01-10T09:00:00-03:00"), "Entrada inicial do notebook no estoque.", adminUserId);
        ensureMovement(entityManager, "NTB-0001", "TRANSFERENCIA", 1, "Almoxarifado TI", "Filial Sao Paulo", "Carlos Tecnico", OffsetDateTime.parse("2026-01-12T14:30:00-03:00"), "Entrega do notebook para usuario final.", techUserId);
        ensureMovement(entityManager, "MON-0010", "ENTRADA", 10, null, "Almoxarifado TI", "Administrador do Sistema", OffsetDateTime.parse("2026-02-15T10:00:00-03:00"), "Recebimento do lote de monitores.", adminUserId);
        ensureMovement(entityManager, "MON-0010", "SAIDA", 2, "Almoxarifado TI", null, "Carlos Tecnico", OffsetDateTime.parse("2026-03-01T11:00:00-03:00"), "Saida para montagem de novas estacoes de trabalho.", techUserId);
        ensureMovement(entityManager, "MOU-0025", "ENTRADA", 30, null, "Almoxarifado TI", "Administrador do Sistema", OffsetDateTime.parse("2026-02-20T15:00:00-03:00"), "Entrada inicial do lote de mouses.", adminUserId);
        ensureMovement(entityManager, "MOU-0025", "SAIDA", 5, "Almoxarifado TI", null, "Carlos Tecnico", OffsetDateTime.parse("2026-03-05T08:45:00-03:00"), "Distribuicao para postos de trabalho.", techUserId);
        ensureMovement(entityManager, "SWT-0003", "ENTRADA", 1, null, "Sala Tecnica", "Administrador do Sistema", OffsetDateTime.parse("2025-11-30T16:20:00-03:00"), "Recebimento do switch para uso na infraestrutura.", adminUserId);
        ensureMovement(entityManager, "SWT-0003", "ENVIO_MANUTENCAO", 1, "Sala Tecnica", "Manutencao Externa", "Carlos Tecnico", OffsetDateTime.parse("2026-03-10T13:15:00-03:00"), "Equipamento com falha intermitente nas portas 17 a 20.", techUserId);
        ensureMovement(entityManager, "SSD-0012", "ENTRADA", 4, null, "Sala Tecnica", "Administrador do Sistema", OffsetDateTime.parse("2026-02-28T17:00:00-03:00"), "Entrada de SSDs para manutencao e upgrade de maquinas.", adminUserId);
    }

    private static void insertCategoryIfAbsent(EntityManager entityManager, String name, String description) {
        if (existsByLower(entityManager, "estoque_ti.equipment_category", "name", name)) {
            return;
        }
        entityManager.createNativeQuery("INSERT INTO estoque_ti.equipment_category (name, description, active) VALUES (:name, :description, TRUE)")
                .setParameter("name", name)
                .setParameter("description", description)
                .executeUpdate();
    }

    private static void insertLocationIfAbsent(EntityManager entityManager, String name, String description) {
        if (existsByLower(entityManager, "estoque_ti.location", "name", name)) {
            return;
        }
        entityManager.createNativeQuery("INSERT INTO estoque_ti.location (name, description, active) VALUES (:name, :description, TRUE)")
                .setParameter("name", name)
                .setParameter("description", description)
                .executeUpdate();
    }

    private static void insertSupplierIfAbsent(EntityManager entityManager, String corporateName, String tradeName, String documentNumber, String contactName, String phone, String email) {
        if (existsByLower(entityManager, "estoque_ti.supplier", "corporate_name", corporateName)) {
            return;
        }
        entityManager.createNativeQuery("INSERT INTO estoque_ti.supplier (corporate_name, trade_name, document_number, contact_name, phone, email, active) VALUES (:corporateName, :tradeName, :documentNumber, :contactName, :phone, :email, TRUE)")
                .setParameter("corporateName", corporateName)
                .setParameter("tradeName", tradeName)
                .setParameter("documentNumber", documentNumber)
                .setParameter("contactName", contactName)
                .setParameter("phone", phone)
                .setParameter("email", email)
                .executeUpdate();
    }

    private static void ensureEquipment(EntityManager entityManager, String internalCode, String name, String categoryName, String brand, String model, String serialNumber, String patrimonyNumber, int quantity, int minimumStock, String status, String locationName, String responsibleName, String supplierCorporateName, Date entryDate, String notes) {
        if (existsByExact(entityManager, "estoque_ti.equipment", "internal_code", internalCode)) {
            return;
        }

        entityManager.createNativeQuery("""
                INSERT INTO estoque_ti.equipment (
                    internal_code,
                    name,
                    category_id,
                    brand,
                    model,
                    serial_number,
                    patrimony_number,
                    quantity,
                    minimum_stock,
                    status,
                    location_id,
                    responsible_name,
                    supplier_id,
                    entry_date,
                    notes,
                    active,
                    version
                )
                VALUES (
                    :internalCode,
                    :name,
                    :categoryId,
                    :brand,
                    :model,
                    :serialNumber,
                    :patrimonyNumber,
                    :quantity,
                    :minimumStock,
                    :status,
                    :locationId,
                    :responsibleName,
                    :supplierId,
                    :entryDate,
                    :notes,
                    TRUE,
                    0
                )
                """)
                .setParameter("internalCode", internalCode)
                .setParameter("name", name)
                .setParameter("categoryId", requireIdByLower(entityManager, "estoque_ti.equipment_category", "name", categoryName))
                .setParameter("brand", brand)
                .setParameter("model", model)
                .setParameter("serialNumber", serialNumber)
                .setParameter("patrimonyNumber", patrimonyNumber)
                .setParameter("quantity", quantity)
                .setParameter("minimumStock", minimumStock)
                .setParameter("status", status)
                .setParameter("locationId", requireIdByLower(entityManager, "estoque_ti.location", "name", locationName))
                .setParameter("responsibleName", responsibleName)
                .setParameter("supplierId", supplierCorporateName == null ? null : requireIdByLower(entityManager, "estoque_ti.supplier", "corporate_name", supplierCorporateName))
                .setParameter("entryDate", entryDate)
                .setParameter("notes", notes)
                .executeUpdate();
    }

    private static void ensureMovement(EntityManager entityManager, String equipmentCode, String movementType, int quantity, String sourceLocationName, String destinationLocationName, String responsibleName, OffsetDateTime movementAt, String notes, Long performedByUserId) {
        Long equipmentId = requireIdByExact(entityManager, "estoque_ti.equipment", "internal_code", equipmentCode);
        if (movementExists(entityManager, equipmentId, movementType, movementAt)) {
            return;
        }

        entityManager.createNativeQuery("""
                INSERT INTO estoque_ti.stock_movement (
                    equipment_id,
                    movement_type,
                    quantity,
                    source_location_id,
                    destination_location_id,
                    responsible_name,
                    movement_at,
                    notes,
                    performed_by_user_id
                )
                VALUES (
                    :equipmentId,
                    :movementType,
                    :quantity,
                    :sourceLocationId,
                    :destinationLocationId,
                    :responsibleName,
                    :movementAt,
                    :notes,
                    :performedByUserId
                )
                """)
                .setParameter("equipmentId", equipmentId)
                .setParameter("movementType", movementType)
                .setParameter("quantity", quantity)
                .setParameter("sourceLocationId", sourceLocationName == null ? null : requireIdByLower(entityManager, "estoque_ti.location", "name", sourceLocationName))
                .setParameter("destinationLocationId", destinationLocationName == null ? null : requireIdByLower(entityManager, "estoque_ti.location", "name", destinationLocationName))
                .setParameter("responsibleName", responsibleName)
                .setParameter("movementAt", movementAt)
                .setParameter("notes", notes)
                .setParameter("performedByUserId", performedByUserId)
                .executeUpdate();
    }

    private static boolean movementExists(EntityManager entityManager, Long equipmentId, String movementType, OffsetDateTime movementAt) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM estoque_ti.stock_movement WHERE equipment_id = :equipmentId AND movement_type = :movementType AND movement_at = :movementAt")
                .setParameter("equipmentId", equipmentId)
                .setParameter("movementType", movementType)
                .setParameter("movementAt", movementAt)
                .getSingleResult();
        return count != null && count.longValue() > 0L;
    }

    private static boolean existsByLower(EntityManager entityManager, String tableName, String columnName, String value) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE LOWER(" + columnName + ") = LOWER(:value)")
                .setParameter("value", value)
                .getSingleResult();
        return count != null && count.longValue() > 0L;
    }

    private static boolean existsByExact(EntityManager entityManager, String tableName, String columnName, String value) {
        Number count = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = :value")
                .setParameter("value", value)
                .getSingleResult();
        return count != null && count.longValue() > 0L;
    }

    private static Long requireIdByLower(EntityManager entityManager, String tableName, String columnName, String value) {
        Object result = entityManager.createNativeQuery("SELECT id FROM " + tableName + " WHERE LOWER(" + columnName + ") = LOWER(:value) LIMIT 1")
                .setParameter("value", value)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private static Long requireIdByExact(EntityManager entityManager, String tableName, String columnName, String value) {
        Object result = entityManager.createNativeQuery("SELECT id FROM " + tableName + " WHERE " + columnName + " = :value LIMIT 1")
                .setParameter("value", value)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private static Long requireUserId(EntityManager entityManager, String username) {
        return findUser(entityManager, username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Usuario de suporte nao encontrado: " + username));
    }

    private static Optional<User> findUser(EntityManager entityManager, String username) {
        List<User> users = entityManager.createQuery(
                        "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)",
                        User.class
                )
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        return users.stream().findFirst();
    }

    private static AuthenticatedUserDto loadUserByUsername(String username) {
        return JpaExecutor.query(entityManager -> findUser(entityManager, username)
                .map(user -> new AuthenticatedUserDto(user.getId(), user.getFullName(), user.getUsername(), user.getRole(), user.isActive()))
                .orElseThrow(() -> new IllegalStateException("Usuario de integracao nao encontrado: " + username)));
    }

    private static long count(EntityManager entityManager, String sql) {
        Number count = (Number) entityManager.createNativeQuery(sql).getSingleResult();
        return count == null ? 0L : count.longValue();
    }
}
