# ETAPA 7 - Movimentacoes de Estoque

## O que foi implementado

Nesta etapa, o sistema passou a registrar movimentacoes reais de estoque com impacto imediato no cadastro de equipamentos.

A entrega inclui:
- entidade JPA para `stock_movement`
- enum `MovementType`
- repositorio e servico transacional para movimentacoes
- regras de negocio para entrada, saida, transferencia, envio para manutencao, retorno de manutencao e baixa/descarte
- atualizacao automatica de quantidade, status, localizacao e responsavel do equipamento
- auditoria com `AuditAction.MOVIMENTACAO`
- tela JavaFX para consulta e registro de movimentacoes
- integracao do modulo no menu principal
- testes de integracao validados no PostgreSQL local

## Regras aplicadas

O servico de movimentacoes garante:
- somente `ADMIN` e `TECNICO` podem registrar movimentacoes
- `VISUALIZADOR` possui acesso somente de consulta
- quantidade deve ser maior que zero
- saida e baixa nao podem exceder o saldo atual
- origem deve ser igual ao local atual do equipamento quando exigida
- origem e destino devem ser diferentes em transferencia e manutencao
- envio e retorno de manutencao exigem movimentacao do saldo total do registro
- entradas adicionais em registros com saldo exigem a mesma localizacao atual
- retorno de manutencao so e aceito para itens em `EM_MANUTENCAO`
- baixa total altera o status para `DESCARTADO`

## Observacao de modelagem

Para preservar consistencia com o modelo atual da tabela `equipment`, transferencias e manutencoes nao aceitam quantidade parcial. Isso evita que um mesmo registro fique representando, ao mesmo tempo, saldo em locais ou status diferentes. Em uma evolucao futura, esse comportamento pode ser expandido com desmembramento automatico de lotes.

## Arquivos principais

### Backend
- `src/main/java/br/com/estoqueti/model/entity/StockMovement.java`
- `src/main/java/br/com/estoqueti/model/enums/MovementType.java`
- `src/main/java/br/com/estoqueti/service/StockMovementService.java`
- `src/main/java/br/com/estoqueti/repository/impl/JpaStockMovementRepository.java`
- `src/main/java/br/com/estoqueti/mapper/StockMovementMapper.java`

### Frontend
- `src/main/java/br/com/estoqueti/controller/MovementController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/movement-view.fxml`
- `src/main/java/br/com/estoqueti/controller/MainLayoutController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/main-layout-view.fxml`
- `src/main/java/br/com/estoqueti/util/ViewManager.java`

### Testes
- `src/test/java/br/com/estoqueti/service/StockMovementServiceIntegrationTest.java`

## Validacao executada

Compilacao:
```powershell
& 'd:\Estoque\.tools\apache-maven-3.9.6\bin\mvn.cmd' -DskipTests compile
```

Testes:
```powershell
& 'd:\Estoque\.tools\apache-maven-3.9.6\bin\mvn.cmd' "-Ddatabase.password=123456" test
```

Resultado:
- `BUILD SUCCESS`
- `19 testes`, `0 falhas`, `0 erros`

## Proximo passo

A base agora esta pronta para a ETAPA 8, que vai construir o dashboard com indicadores de estoque, saldos criticos e ultimas movimentacoes.