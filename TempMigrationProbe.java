import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class TempMigrationProbe {

    public static void main(String[] args) throws Exception {
        List<String> statements = List.of(
                "ALTER TABLE estoque_ti.stock_movement DROP CONSTRAINT IF EXISTS ck_stock_movement_type",
                "ALTER TABLE estoque_ti.stock_movement ADD CONSTRAINT ck_stock_movement_type CHECK (movement_type IN ('ENTRADA', 'SAIDA', 'TRANSFERENCIA', 'ENTREGA_FUNCIONARIO', 'DEVOLUCAO_FUNCIONARIO', 'ENVIO_MANUTENCAO', 'RETORNO_MANUTENCAO', 'BAIXA_DESCARTE'))",
                "ALTER TABLE estoque_ti.stock_movement DROP CONSTRAINT IF EXISTS ck_stock_movement_origin_destination_rule",
                "ALTER TABLE estoque_ti.stock_movement ADD CONSTRAINT ck_stock_movement_origin_destination_rule CHECK ((movement_type = 'ENTRADA' AND source_location_id IS NULL AND destination_location_id IS NOT NULL) OR (movement_type = 'SAIDA' AND source_location_id IS NOT NULL AND destination_location_id IS NULL) OR (movement_type = 'TRANSFERENCIA' AND source_location_id IS NOT NULL AND destination_location_id IS NOT NULL AND source_location_id <> destination_location_id) OR (movement_type = 'ENTREGA_FUNCIONARIO' AND source_location_id IS NOT NULL AND destination_location_id IS NOT NULL) OR (movement_type = 'DEVOLUCAO_FUNCIONARIO' AND source_location_id IS NOT NULL AND destination_location_id IS NOT NULL) OR (movement_type = 'ENVIO_MANUTENCAO' AND source_location_id IS NOT NULL AND destination_location_id IS NOT NULL AND source_location_id <> destination_location_id) OR (movement_type = 'RETORNO_MANUTENCAO' AND source_location_id IS NOT NULL AND destination_location_id IS NOT NULL AND source_location_id <> destination_location_id) OR (movement_type = 'BAIXA_DESCARTE' AND source_location_id IS NOT NULL AND destination_location_id IS NULL))",
                "CREATE TABLE IF NOT EXISTS estoque_ti.delivery_protocol (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, protocol_number VARCHAR(40) NOT NULL, stock_movement_id BIGINT NOT NULL, equipment_id BIGINT NOT NULL, recipient_name VARCHAR(120) NOT NULL, recipient_cpf VARCHAR(14) NOT NULL, recipient_role VARCHAR(120) NOT NULL, item_quantity INTEGER NOT NULL, item_description VARCHAR(250) NOT NULL, item_identifier VARCHAR(180) NULL, item_observations TEXT NULL, delivery_at TIMESTAMPTZ NOT NULL, generated_by_user_id BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT ck_delivery_protocol_protocol_number_not_blank CHECK (btrim(protocol_number) <> ''), CONSTRAINT ck_delivery_protocol_recipient_name_not_blank CHECK (btrim(recipient_name) <> ''), CONSTRAINT ck_delivery_protocol_recipient_cpf_not_blank CHECK (btrim(recipient_cpf) <> ''), CONSTRAINT ck_delivery_protocol_recipient_role_not_blank CHECK (btrim(recipient_role) <> ''), CONSTRAINT ck_delivery_protocol_item_quantity_positive CHECK (item_quantity > 0), CONSTRAINT ck_delivery_protocol_item_description_not_blank CHECK (btrim(item_description) <> ''), CONSTRAINT fk_delivery_protocol_stock_movement FOREIGN KEY (stock_movement_id) REFERENCES estoque_ti.stock_movement (id) ON UPDATE RESTRICT ON DELETE RESTRICT, CONSTRAINT fk_delivery_protocol_equipment FOREIGN KEY (equipment_id) REFERENCES estoque_ti.equipment (id) ON UPDATE RESTRICT ON DELETE RESTRICT, CONSTRAINT fk_delivery_protocol_generated_by_user FOREIGN KEY (generated_by_user_id) REFERENCES estoque_ti.app_user (id) ON UPDATE RESTRICT ON DELETE RESTRICT)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_protocol_protocol_number ON estoque_ti.delivery_protocol (protocol_number)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_protocol_stock_movement ON estoque_ti.delivery_protocol (stock_movement_id)",
                "CREATE INDEX IF NOT EXISTS idx_delivery_protocol_delivery_at ON estoque_ti.delivery_protocol (delivery_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_delivery_protocol_recipient_name_ci ON estoque_ti.delivery_protocol (LOWER(recipient_name))",
                "CREATE TABLE IF NOT EXISTS estoque_ti.return_protocol (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, protocol_number VARCHAR(40) NOT NULL, stock_movement_id BIGINT NOT NULL, equipment_id BIGINT NOT NULL, employee_name VARCHAR(120) NOT NULL, employee_cpf VARCHAR(14) NOT NULL, company_receiver_name VARCHAR(120) NOT NULL, company_receiver_role VARCHAR(120) NOT NULL, company_receiver_cpf VARCHAR(14) NOT NULL, return_reason VARCHAR(40) NOT NULL, other_reason TEXT NULL, item_quantity INTEGER NOT NULL, item_description VARCHAR(250) NOT NULL, item_identifier VARCHAR(180) NULL, item_observations TEXT NULL, returned_at TIMESTAMPTZ NOT NULL, generated_by_user_id BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT ck_return_protocol_protocol_number_not_blank CHECK (btrim(protocol_number) <> ''), CONSTRAINT ck_return_protocol_employee_name_not_blank CHECK (btrim(employee_name) <> ''), CONSTRAINT ck_return_protocol_employee_cpf_not_blank CHECK (btrim(employee_cpf) <> ''), CONSTRAINT ck_return_protocol_company_receiver_name_not_blank CHECK (btrim(company_receiver_name) <> ''), CONSTRAINT ck_return_protocol_company_receiver_role_not_blank CHECK (btrim(company_receiver_role) <> ''), CONSTRAINT ck_return_protocol_company_receiver_cpf_not_blank CHECK (btrim(company_receiver_cpf) <> ''), CONSTRAINT ck_return_protocol_item_quantity_positive CHECK (item_quantity > 0), CONSTRAINT ck_return_protocol_item_description_not_blank CHECK (btrim(item_description) <> ''), CONSTRAINT fk_return_protocol_stock_movement FOREIGN KEY (stock_movement_id) REFERENCES estoque_ti.stock_movement (id) ON UPDATE RESTRICT ON DELETE RESTRICT, CONSTRAINT fk_return_protocol_equipment FOREIGN KEY (equipment_id) REFERENCES estoque_ti.equipment (id) ON UPDATE RESTRICT ON DELETE RESTRICT, CONSTRAINT fk_return_protocol_generated_by_user FOREIGN KEY (generated_by_user_id) REFERENCES estoque_ti.app_user (id) ON UPDATE RESTRICT ON DELETE RESTRICT)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_return_protocol_protocol_number ON estoque_ti.return_protocol (protocol_number)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_return_protocol_stock_movement ON estoque_ti.return_protocol (stock_movement_id)",
                "CREATE INDEX IF NOT EXISTS idx_return_protocol_returned_at ON estoque_ti.return_protocol (returned_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_return_protocol_employee_name_ci ON estoque_ti.return_protocol (LOWER(employee_name))"
        );

        String username = args.length > 0 ? args[0] : "postgres";
        String password = args.length > 1 ? args[1] : "123456";

        try (Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/estoqueti",
                username,
                password)) {
            connection.setAutoCommit(false);
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    System.out.println("OK: " + sql);
                } catch (SQLException exception) {
                    System.out.println("FAIL: " + sql);
                    System.out.println("STATE: " + exception.getSQLState());
                    System.out.println("MESSAGE: " + exception.getMessage());
                    connection.rollback();
                    return;
                }
            }
            connection.rollback();
        }
    }
}
