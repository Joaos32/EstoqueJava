# ETAPA 6 - Cadastro e Listagem de Equipamentos

## O que foi implementado

Nesta etapa, o projeto passou a ter o modulo funcional de equipamentos com consulta, filtros e cadastro integrado ao PostgreSQL.

A solucao entregue inclui:
- entidades JPA para `equipment`, `equipment_category`, `location` e `supplier`
- enum `EquipmentStatus`
- DTOs para cadastro, filtros, listagem e combos de apoio
- repositorios JPA dedicados ao modulo
- servico `EquipmentService` com validacoes, permissoes e auditoria
- tela JavaFX para consulta e cadastro de equipamentos
- integracao do modulo no layout principal da aplicacao
- testes de integracao executados contra o banco local

## Regras aplicadas

O cadastro de equipamentos segue as regras abaixo:
- somente `ADMIN` e `TECNICO` podem cadastrar equipamentos
- `VISUALIZADOR` possui acesso somente de consulta
- codigo interno e obrigatorio e deve ser unico
- numero de serie e patrimonio, quando informados, tambem devem ser unicos
- quantidade e estoque minimo nao podem ser negativos
- categoria, localizacao, status e data de entrada sao obrigatorios
- toda criacao gera registro em `audit_log`

## Arquivos principais

### Backend
- `src/main/java/br/com/estoqueti/model/entity/Equipment.java`
- `src/main/java/br/com/estoqueti/model/entity/EquipmentCategory.java`
- `src/main/java/br/com/estoqueti/model/entity/Location.java`
- `src/main/java/br/com/estoqueti/model/entity/Supplier.java`
- `src/main/java/br/com/estoqueti/model/enums/EquipmentStatus.java`
- `src/main/java/br/com/estoqueti/service/EquipmentService.java`
- `src/main/java/br/com/estoqueti/repository/impl/JpaEquipmentRepository.java`

### Frontend
- `src/main/java/br/com/estoqueti/controller/EquipmentController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/equipment-view.fxml`
- `src/main/resources/br/com/estoqueti/view/fxml/main-layout-view.fxml`
- `src/main/resources/br/com/estoqueti/view/css/application.css`

### Testes
- `src/test/java/br/com/estoqueti/service/EquipmentServiceIntegrationTest.java`

## Validacao executada

Comando de compilacao:
```powershell
& 'd:\Estoque\.tools\apache-maven-3.9.6\bin\mvn.cmd' -DskipTests compile
```

Comando de testes:
```powershell
& 'd:\Estoque\.tools\apache-maven-3.9.6\bin\mvn.cmd' "-Ddatabase.password=123456" test
```

Resultado:
- compilacao com `BUILD SUCCESS`
- `12 testes`, `0 falhas`, `0 erros`

## Observacoes para a proxima etapa

A base agora esta pronta para a ETAPA 7, que vai implementar movimentacoes de estoque usando os equipamentos ja cadastrados e auditados.