# ETAPA 8 - Dashboard

## O que foi implementado

Nesta etapa, a aplicacao passou a abrir diretamente no dashboard com indicadores operacionais do estoque e historico recente de movimentacoes.

A entrega inclui:
- modulo visual de dashboard em JavaFX
- servico de agregacao com indicadores de estoque
- consultas para itens abaixo do estoque minimo
- consultas para ultimas movimentacoes
- integracao do dashboard como tela inicial do layout principal
- teste de integracao para validar indicadores e ordenacao do historico

## Indicadores entregues

O dashboard apresenta:
- total de itens cadastrados no estoque ativo
- total disponivel
- total em uso
- total em manutencao
- itens abaixo do estoque minimo
- tabela com itens em estoque baixo
- tabela com ultimas movimentacoes

## Escolha tecnica

As agregacoes foram concentradas em um repositorio especifico de dashboard para evitar espalhar consultas de leitura analitica pelos modulos de equipamento e movimentacao.

Com isso, a camada de servico ficou responsavel apenas por compor o painel e a interface permaneceu desacoplada da logica de acesso a dados.

## Arquivos principais

### Backend
- `src/main/java/br/com/estoqueti/service/DashboardService.java`
- `src/main/java/br/com/estoqueti/repository/DashboardRepository.java`
- `src/main/java/br/com/estoqueti/repository/impl/JpaDashboardRepository.java`
- `src/main/java/br/com/estoqueti/mapper/DashboardMapper.java`
- `src/main/java/br/com/estoqueti/dto/dashboard/DashboardSummaryDto.java`
- `src/main/java/br/com/estoqueti/dto/dashboard/DashboardLowStockItemDto.java`

### Frontend
- `src/main/java/br/com/estoqueti/controller/DashboardController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/dashboard-view.fxml`
- `src/main/java/br/com/estoqueti/controller/MainLayoutController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/main-layout-view.fxml`
- `src/main/java/br/com/estoqueti/util/ViewManager.java`
- `src/main/resources/br/com/estoqueti/view/css/application.css`

### Testes
- `src/test/java/br/com/estoqueti/service/DashboardServiceIntegrationTest.java`

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
- `22 testes`, `0 falhas`, `0 erros`

## Proximo passo

A base agora esta pronta para a ETAPA 9, que vai implementar exportacao de relatorios em PDF e CSV.