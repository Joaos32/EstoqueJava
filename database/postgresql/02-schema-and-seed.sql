-- Execute este arquivo conectado ao banco "estoqueti".
-- Ele cria o schema, tabelas, constraints, indices e dados iniciais de referencia para o ambiente.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS estoque_ti;

SET search_path TO estoque_ti, public;

CREATE OR REPLACE FUNCTION estoque_ti.fn_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE app_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_app_user_full_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_app_user_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'TECNICO', 'VISUALIZADOR'))
);

CREATE UNIQUE INDEX uq_app_user_username_ci
    ON app_user (LOWER(username));

CREATE INDEX idx_app_user_role_active
    ON app_user (role, active);

CREATE TABLE equipment_category (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_equipment_category_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX uq_equipment_category_name_ci
    ON equipment_category (LOWER(name));

CREATE TABLE location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_location_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX uq_location_name_ci
    ON location (LOWER(name));

CREATE TABLE supplier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    corporate_name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150) NULL,
    document_number VARCHAR(20) NULL,
    contact_name VARCHAR(120) NULL,
    phone VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_supplier_corporate_name_not_blank CHECK (btrim(corporate_name) <> '')
);

CREATE UNIQUE INDEX uq_supplier_document_number
    ON supplier (document_number)
    WHERE document_number IS NOT NULL;

CREATE UNIQUE INDEX uq_supplier_email_ci
    ON supplier (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TABLE equipment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    internal_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    brand VARCHAR(80) NULL,
    model VARCHAR(80) NULL,
    serial_number VARCHAR(120) NULL,
    patrimony_number VARCHAR(120) NULL,
    quantity INTEGER NOT NULL,
    minimum_stock INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    location_id BIGINT NOT NULL,
    responsible_name VARCHAR(120) NULL,
    supplier_id BIGINT NULL,
    entry_date DATE NOT NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_equipment_internal_code_not_blank CHECK (btrim(internal_code) <> ''),
    CONSTRAINT ck_equipment_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_equipment_serial_number_not_blank CHECK (serial_number IS NULL OR btrim(serial_number) <> ''),
    CONSTRAINT ck_equipment_patrimony_number_not_blank CHECK (patrimony_number IS NULL OR btrim(patrimony_number) <> ''),
    CONSTRAINT ck_equipment_responsible_name_not_blank CHECK (responsible_name IS NULL OR btrim(responsible_name) <> ''),
    CONSTRAINT ck_equipment_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT ck_equipment_minimum_stock_non_negative CHECK (minimum_stock >= 0),
    CONSTRAINT ck_equipment_version_non_negative CHECK (version >= 0),
    CONSTRAINT ck_equipment_status CHECK (status IN ('DISPONIVEL', 'EM_USO', 'EM_MANUTENCAO', 'DEFEITUOSO', 'DESCARTADO')),
    CONSTRAINT fk_equipment_category FOREIGN KEY (category_id)
        REFERENCES equipment_category (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_equipment_location FOREIGN KEY (location_id)
        REFERENCES location (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_equipment_supplier FOREIGN KEY (supplier_id)
        REFERENCES supplier (id)
        ON UPDATE RESTRICT
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_equipment_internal_code_ci
    ON equipment (LOWER(internal_code));

CREATE UNIQUE INDEX uq_equipment_serial_number_ci
    ON equipment (LOWER(serial_number))
    WHERE serial_number IS NOT NULL;

CREATE UNIQUE INDEX uq_equipment_patrimony_number_ci
    ON equipment (LOWER(patrimony_number))
    WHERE patrimony_number IS NOT NULL;

CREATE INDEX idx_equipment_name_ci
    ON equipment (LOWER(name));

CREATE INDEX idx_equipment_category_id
    ON equipment (category_id);

CREATE INDEX idx_equipment_status
    ON equipment (status);

CREATE INDEX idx_equipment_location_id
    ON equipment (location_id);

CREATE INDEX idx_equipment_responsible_name_ci
    ON equipment (LOWER(responsible_name))
    WHERE responsible_name IS NOT NULL;

CREATE INDEX idx_equipment_active_status
    ON equipment (active, status);

CREATE INDEX idx_equipment_stock_levels
    ON equipment (quantity, minimum_stock);

CREATE TABLE stock_movement (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    source_location_id BIGINT NULL,
    destination_location_id BIGINT NULL,
    responsible_name VARCHAR(120) NOT NULL,
    movement_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    performed_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_stock_movement_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_movement_responsible_name_not_blank CHECK (btrim(responsible_name) <> ''),
    CONSTRAINT ck_stock_movement_type CHECK (
        movement_type IN (
            'ENTRADA',
            'SAIDA',
            'TRANSFERENCIA',
            'ENTREGA_FUNCIONARIO',
            'DEVOLUCAO_FUNCIONARIO',
            'ENVIO_MANUTENCAO',
            'RETORNO_MANUTENCAO',
            'BAIXA_DESCARTE'
        )
    ),
    CONSTRAINT ck_stock_movement_origin_destination_rule CHECK (
        (
            movement_type = 'ENTRADA'
            AND source_location_id IS NULL
            AND destination_location_id IS NOT NULL
        )
        OR
        (
            movement_type = 'SAIDA'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NULL
        )
        OR
        (
            movement_type = 'TRANSFERENCIA'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
            AND source_location_id <> destination_location_id
        )
        OR
        (
            movement_type = 'ENTREGA_FUNCIONARIO'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
        )
        OR
        (
            movement_type = 'DEVOLUCAO_FUNCIONARIO'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
        )
        OR
        (
            movement_type = 'ENVIO_MANUTENCAO'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
            AND source_location_id <> destination_location_id
        )
        OR
        (
            movement_type = 'RETORNO_MANUTENCAO'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NOT NULL
            AND source_location_id <> destination_location_id
        )
        OR
        (
            movement_type = 'BAIXA_DESCARTE'
            AND source_location_id IS NOT NULL
            AND destination_location_id IS NULL
        )
    ),
    CONSTRAINT fk_stock_movement_equipment FOREIGN KEY (equipment_id)
        REFERENCES equipment (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_stock_movement_source_location FOREIGN KEY (source_location_id)
        REFERENCES location (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_stock_movement_destination_location FOREIGN KEY (destination_location_id)
        REFERENCES location (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_stock_movement_performed_by_user FOREIGN KEY (performed_by_user_id)
        REFERENCES app_user (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE INDEX idx_stock_movement_equipment_movement_at
    ON stock_movement (equipment_id, movement_at DESC);

CREATE INDEX idx_stock_movement_type_movement_at
    ON stock_movement (movement_type, movement_at DESC);

CREATE INDEX idx_stock_movement_performed_by_user
    ON stock_movement (performed_by_user_id);

CREATE INDEX idx_stock_movement_source_location
    ON stock_movement (source_location_id)
    WHERE source_location_id IS NOT NULL;

CREATE INDEX idx_stock_movement_destination_location
    ON stock_movement (destination_location_id)
    WHERE destination_location_id IS NOT NULL;

CREATE TABLE delivery_protocol (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    protocol_number VARCHAR(40) NOT NULL,
    stock_movement_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    recipient_cpf VARCHAR(14) NOT NULL,
    recipient_role VARCHAR(120) NOT NULL,
    item_quantity INTEGER NOT NULL,
    item_description VARCHAR(250) NOT NULL,
    item_identifier VARCHAR(180) NULL,
    item_observations TEXT NULL,
    delivery_at TIMESTAMPTZ NOT NULL,
    generated_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_delivery_protocol_protocol_number_not_blank CHECK (btrim(protocol_number) <> ''),
    CONSTRAINT ck_delivery_protocol_recipient_name_not_blank CHECK (btrim(recipient_name) <> ''),
    CONSTRAINT ck_delivery_protocol_recipient_cpf_not_blank CHECK (btrim(recipient_cpf) <> ''),
    CONSTRAINT ck_delivery_protocol_recipient_role_not_blank CHECK (btrim(recipient_role) <> ''),
    CONSTRAINT ck_delivery_protocol_item_quantity_positive CHECK (item_quantity > 0),
    CONSTRAINT ck_delivery_protocol_item_description_not_blank CHECK (btrim(item_description) <> ''),
    CONSTRAINT fk_delivery_protocol_stock_movement FOREIGN KEY (stock_movement_id)
        REFERENCES stock_movement (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_protocol_equipment FOREIGN KEY (equipment_id)
        REFERENCES equipment (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_protocol_generated_by_user FOREIGN KEY (generated_by_user_id)
        REFERENCES app_user (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_delivery_protocol_protocol_number
    ON delivery_protocol (protocol_number);

CREATE UNIQUE INDEX uq_delivery_protocol_stock_movement
    ON delivery_protocol (stock_movement_id);

CREATE INDEX idx_delivery_protocol_delivery_at
    ON delivery_protocol (delivery_at DESC);

CREATE INDEX idx_delivery_protocol_recipient_name_ci
    ON delivery_protocol (LOWER(recipient_name));

CREATE TABLE return_protocol (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    protocol_number VARCHAR(40) NOT NULL,
    stock_movement_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    employee_name VARCHAR(120) NOT NULL,
    employee_cpf VARCHAR(14) NOT NULL,
    company_receiver_name VARCHAR(120) NOT NULL,
    company_receiver_role VARCHAR(120) NOT NULL,
    company_receiver_cpf VARCHAR(14) NOT NULL,
    return_reason VARCHAR(40) NOT NULL,
    other_reason TEXT NULL,
    item_quantity INTEGER NOT NULL,
    item_description VARCHAR(250) NOT NULL,
    item_identifier VARCHAR(180) NULL,
    item_observations TEXT NULL,
    returned_at TIMESTAMPTZ NOT NULL,
    generated_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_return_protocol_protocol_number_not_blank CHECK (btrim(protocol_number) <> ''),
    CONSTRAINT ck_return_protocol_employee_name_not_blank CHECK (btrim(employee_name) <> ''),
    CONSTRAINT ck_return_protocol_employee_cpf_not_blank CHECK (btrim(employee_cpf) <> ''),
    CONSTRAINT ck_return_protocol_company_receiver_name_not_blank CHECK (btrim(company_receiver_name) <> ''),
    CONSTRAINT ck_return_protocol_company_receiver_role_not_blank CHECK (btrim(company_receiver_role) <> ''),
    CONSTRAINT ck_return_protocol_company_receiver_cpf_not_blank CHECK (btrim(company_receiver_cpf) <> ''),
    CONSTRAINT ck_return_protocol_item_quantity_positive CHECK (item_quantity > 0),
    CONSTRAINT ck_return_protocol_item_description_not_blank CHECK (btrim(item_description) <> ''),
    CONSTRAINT fk_return_protocol_stock_movement FOREIGN KEY (stock_movement_id)
        REFERENCES stock_movement (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_return_protocol_equipment FOREIGN KEY (equipment_id)
        REFERENCES equipment (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_return_protocol_generated_by_user FOREIGN KEY (generated_by_user_id)
        REFERENCES app_user (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_return_protocol_protocol_number
    ON return_protocol (protocol_number);

CREATE UNIQUE INDEX uq_return_protocol_stock_movement
    ON return_protocol (stock_movement_id);

CREATE INDEX idx_return_protocol_returned_at
    ON return_protocol (returned_at DESC);

CREATE INDEX idx_return_protocol_employee_name_ci
    ON return_protocol (LOWER(employee_name));

CREATE TABLE audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(20) NOT NULL,
    entity_name VARCHAR(80) NULL,
    entity_id BIGINT NULL,
    description TEXT NOT NULL,
    ip_or_station VARCHAR(120) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_log_action CHECK (
        action IN ('LOGIN', 'CADASTRO', 'EDICAO', 'EXCLUSAO', 'MOVIMENTACAO')
    ),
    CONSTRAINT ck_audit_log_description_not_blank CHECK (btrim(description) <> ''),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id)
        REFERENCES app_user (id)
        ON UPDATE RESTRICT
        ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_user_created_at
    ON audit_log (user_id, created_at DESC);

CREATE INDEX idx_audit_log_action_created_at
    ON audit_log (action, created_at DESC);

CREATE INDEX idx_audit_log_entity
    ON audit_log (entity_name, entity_id);

CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION estoque_ti.fn_set_updated_at();

CREATE TRIGGER trg_equipment_category_updated_at
    BEFORE UPDATE ON equipment_category
    FOR EACH ROW
    EXECUTE FUNCTION estoque_ti.fn_set_updated_at();

CREATE TRIGGER trg_location_updated_at
    BEFORE UPDATE ON location
    FOR EACH ROW
    EXECUTE FUNCTION estoque_ti.fn_set_updated_at();

CREATE TRIGGER trg_supplier_updated_at
    BEFORE UPDATE ON supplier
    FOR EACH ROW
    EXECUTE FUNCTION estoque_ti.fn_set_updated_at();

CREATE TRIGGER trg_equipment_updated_at
    BEFORE UPDATE ON equipment
    FOR EACH ROW
    EXECUTE FUNCTION estoque_ti.fn_set_updated_at();

INSERT INTO equipment_category (name, description)
VALUES
    ('Notebook', 'Computadores portateis corporativos'),
    ('Desktop', 'Computadores de mesa corporativos'),
    ('Monitor', 'Monitores LCD e LED'),
    ('Teclado', 'Teclados USB e sem fio'),
    ('Mouse', 'Mouses USB e sem fio'),
    ('Impressora', 'Impressoras e multifuncionais'),
    ('Roteador', 'Roteadores de rede'),
    ('Switch', 'Switches gerenciaveis e nao gerenciaveis'),
    ('Nobreak', 'Nobreaks e estabilizadores'),
    ('SSD', 'Unidades de armazenamento SSD'),
    ('Memoria', 'Modulos de memoria RAM'),
    ('Cabo', 'Cabos de energia, rede e video'),
    ('Peca de reposicao', 'Itens para manutencao e substituicao'),
    ('Escritorio', 'Materiais e itens de escritorio'),
    ('Equipamento', 'Equipamentos gerais e itens diversos de operacao');

INSERT INTO location (name, description)
VALUES
    ('Almoxarifado TI', 'Estoque central de itens de tecnologia'),
    ('Sala Tecnica', 'Area tecnica interna'),
    ('Escritorio Administrativo', 'Postos de trabalho do administrativo'),
    ('Filial Sao Paulo', 'Unidade operacional de Sao Paulo'),
    ('Manutencao Externa', 'Itens enviados para parceiro de manutencao'),
    ('Descarte', 'Area destinada a itens baixados ou descartados'),
    ('Estoque Escritorio', 'Estoque de materiais e itens de escritorio');

-- Dados mestres de referencia para o ambiente.
-- Fornecedores, usuarios, equipamentos, movimentacoes e auditorias
-- devem ser cadastrados de acordo com a operacao real do ambiente.
