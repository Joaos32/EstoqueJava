-- Remove dados de demonstracao e integracao antes da entrada em producao.
-- Execute este arquivo conectado ao banco "estoqueti".

SET search_path TO estoque_ti, public;

DELETE FROM audit_log
WHERE user_id IN (
        SELECT id FROM app_user
        WHERE username IN ('itest.admin', 'itest.tech', 'itest.viewer')
           OR full_name ILIKE '%bootstrap pendente%'
    )
   OR entity_id IN (
        SELECT id FROM stock_movement
        WHERE equipment_id IN (
            SELECT id FROM equipment
            WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')
        )
    )
   OR entity_id IN (
        SELECT id FROM equipment
        WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')
    )
   OR description ILIKE '%Cadastro do lote inicial de monitores%'
   OR description ILIKE '%Saida de monitores para novas estacoes de trabalho%'
   OR description ILIKE '%Envio de switch para manutencao externa%';

DELETE FROM delivery_protocol
WHERE equipment_id IN (
    SELECT id FROM equipment
    WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')
);

DELETE FROM return_protocol
WHERE equipment_id IN (
    SELECT id FROM equipment
    WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')
);

DELETE FROM stock_movement
WHERE equipment_id IN (
    SELECT id FROM equipment
    WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012')
);

DELETE FROM equipment
WHERE internal_code IN ('NTB-0001', 'MON-0010', 'MOU-0025', 'SWT-0003', 'SSD-0012');

DELETE FROM supplier
WHERE corporate_name IN (
    'Tech Distribuidora Ltda',
    'Infra Network Solutions Ltda',
    'Office Equipamentos S.A.'
);

DELETE FROM app_user
WHERE username IN ('itest.admin', 'itest.tech', 'itest.viewer')
   OR full_name ILIKE '%bootstrap pendente%';
