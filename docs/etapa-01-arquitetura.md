# ETAPA 1 - Arquitetura do Sistema

## Nome proposto

**EstoqueTI Desktop**

O nome foi escolhido por ser direto, profissional e facil de entender no contexto corporativo. Ele comunica com clareza que se trata de uma aplicacao desktop para gestao de estoque de itens de TI.

## Objetivo da solucao

Desenvolver uma aplicacao desktop em Java para controle de estoque de equipamentos e perifericos de TI, com autenticacao, controle de perfis, cadastro de itens, movimentacoes, dashboard, auditoria e geracao de relatorios.

## Arquitetura escolhida

Foi escolhida uma **arquitetura em camadas**, com a camada de apresentacao seguindo o padrao **MVC com JavaFX**.

Essa abordagem combina:

- separacao clara entre interface, regras de negocio e persistencia;
- menor acoplamento entre telas e banco de dados;
- manutencao mais simples;
- facilidade para testes e evolucao futura;
- estrutura adequada para um sistema desktop corporativo sem adicionar complexidade desnecessaria.

### Camadas da aplicacao

#### 1. Presentation Layer

Responsavel pela interface grafica em JavaFX.

- FXML para estrutura das telas
- Controllers JavaFX para eventos da interface
- CSS para identidade visual e padronizacao

Responsabilidades:

- capturar interacoes do usuario;
- exibir dados;
- aplicar validacoes basicas de formulario;
- chamar a camada de servico por meio de DTOs.

#### 2. Application / Service Layer

Responsavel pelas regras de negocio e casos de uso do sistema.

Responsabilidades:

- autenticar usuarios;
- validar operacoes de estoque;
- impedir inconsistencias de quantidade;
- coordenar transacoes;
- acionar auditoria;
- preparar dados para dashboard e relatorios.

#### 3. Domain Layer

Responsavel pelo nucleo do negocio.

Responsabilidades:

- representar entidades;
- definir enums e regras centrais;
- manter o modelo alinhado ao dominio de estoque de TI.

#### 4. Persistence / Infrastructure Layer

Responsavel pela comunicacao com PostgreSQL e recursos tecnicos da aplicacao.

Responsabilidades:

- configuracao do Hibernate;
- gerenciamento de conexao com pool;
- implementacao de repositories;
- exportacao CSV e PDF;
- utilitarios tecnicos;
- logging e configuracoes.

## Fluxo principal

O fluxo padrao sera:

`Controller JavaFX -> DTO -> Service -> Repository -> PostgreSQL`

E o retorno:

`PostgreSQL -> Repository -> Service -> DTO/ViewModel -> Controller -> Tela`

Com isso, a interface nao acessa o banco diretamente e as regras ficam concentradas na camada correta.

## Tecnologia escolhida

### Linguagem e runtime

- Java 17

Justificativa:

- versao LTS;
- estabilidade para aplicacoes corporativas;
- compatibilidade ampla com bibliotecas e ferramentas modernas.

### Interface desktop

- JavaFX com FXML e CSS

Justificativa:

- separa layout da logica;
- facilita manutencao das telas;
- suporta interface moderna e corporativa;
- e a melhor opcao nativa em Java para desktop atualmente.

### Persistencia

- JPA/Hibernate como ORM
- HikariCP para pool de conexoes
- PostgreSQL como banco relacional

Justificativa da escolha de JPA/Hibernate em vez de JDBC puro:

- o sistema possui varias entidades relacionadas;
- ha necessidade de filtros, auditoria, dashboard e relatorios;
- reduz bastante o volume de codigo repetitivo de CRUD;
- facilita mapeamento entre objetos e banco;
- melhora a manutencao conforme o sistema crescer;
- continua permitindo consultas nativas quando forem melhores para dashboard e relatorios.

Para um desktop corporativo com varias operacoes de negocio, Hibernate oferece melhor equilibrio entre produtividade, organizacao e escalabilidade.

### Seguranca

- BCrypt para hash de senhas

Justificativa:

- padrao seguro e consolidado;
- evita armazenamento de senha em texto puro;
- simples de integrar ao fluxo de autenticacao.

### Exportacao de relatorios

- OpenPDF para geracao de PDF
- Apache Commons CSV para exportacao CSV

Justificativa:

- bibliotecas maduras e leves;
- boas para relatorios tabulares;
- integracao simples com Java puro.

### Logging e suporte operacional

- SLF4J + Logback

Justificativa:

- separa logging da regra de negocio;
- facilita diagnostico de erros sem poluir a interface.

### Validacao

- validacoes de formulario na camada de apresentacao;
- validacoes de negocio na camada de servico;
- possibilidade de Bean Validation onde fizer sentido.

## Estrutura do projeto

Sera adotado um projeto Maven **single-module** inicialmente, com organizacao profissional por pacotes.

Justificativa:

- reduz complexidade de setup;
- facilita compilacao e distribuicao;
- e suficiente para o porte da primeira versao;
- pode evoluir para multi-modulo depois, sem quebrar a arquitetura.

### Pacote raiz sugerido

`br.com.estoqueti`

### Estrutura de pacotes

```text
br.com.estoqueti
├── config
├── controller
├── dto
│   ├── auth
│   ├── dashboard
│   ├── equipment
│   ├── movement
│   ├── report
│   └── user
├── exception
├── mapper
├── model
│   ├── entity
│   └── enums
├── repository
│   ├── impl
│   └── projection
├── service
├── session
├── util
├── view
│   ├── css
│   ├── fxml
│   └── component
└── AppLauncher
```

### Papel de cada pacote

#### `config`

Configuracoes centrais da aplicacao.

Exemplos:

- leitura de propriedades;
- configuracao do Hibernate;
- configuracao de datasource;
- bootstrap do sistema.

#### `controller`

Controllers JavaFX das telas.

Exemplos:

- `LoginController`
- `MainLayoutController`
- `DashboardController`
- `EquipmentController`
- `MovementController`
- `UserController`
- `ReportController`

#### `dto`

Objetos de transferencia entre interface e servicos.

Objetivo:

- evitar expor entidades diretamente para a UI;
- desacoplar telas do modelo persistente;
- facilitar validacao e evolucao de campos.

#### `exception`

Excecoes de negocio e tecnicas tratadas de forma amigavel.

Exemplos:

- `BusinessException`
- `ValidationException`
- `AuthenticationException`
- `InsufficientStockException`
- `EntityNotFoundException`

#### `mapper`

Conversao entre entidades, DTOs e objetos de exibicao.

#### `model.entity`

Entidades persistidas no banco.

#### `model.enums`

Enums de dominio, como status, tipos de movimentacao e perfis.

#### `repository`

Abstracao de acesso a dados.

Objetivo:

- encapsular consultas;
- centralizar regras de persistencia;
- permitir manutencao futura com menor impacto.

#### `repository.impl`

Implementacoes concretas com JPA/Hibernate.

#### `repository.projection`

Consultas otimizadas para dashboard e relatorios.

#### `service`

Camada principal de regras de negocio.

#### `session`

Controle do usuario autenticado e contexto da sessao.

#### `util`

Funcoes utilitarias compartilhadas.

Exemplos:

- mascaras;
- validadores;
- conversores de data;
- exportadores;
- formatadores.

#### `view.fxml`

Arquivos FXML das telas.

#### `view.css`

Arquivos CSS da interface.

#### `view.component`

Componentes visuais reutilizaveis.

## Entidades do dominio

As entidades abaixo formam o nucleo do sistema.

### 1. User

Representa os usuarios que acessam o sistema.

Campos principais:

- id
- name
- username
- passwordHash
- role
- active
- lastLoginAt
- createdAt
- updatedAt

Observacoes:

- `passwordHash` sera salvo com BCrypt;
- `role` sera enum com os perfis `ADMIN`, `TECNICO` e `VISUALIZADOR`;
- `active` permite desativacao sem excluir historico.

### 2. EquipmentCategory

Representa a categoria do item.

Campos principais:

- id
- name
- description
- active

Exemplos:

- Notebook
- Desktop
- Monitor
- Teclado
- Mouse
- Impressora
- Roteador
- Switch
- Nobreak
- HD/SSD
- Memoria
- Cabo
- Peca de reposicao

### 3. Location

Representa onde o item esta fisicamente.

Campos principais:

- id
- name
- description
- active

Exemplos:

- Almoxarifado TI
- Sala tecnica
- Filial A
- Manutencao externa

### 4. Supplier

Representa o fornecedor do item.

Campos principais:

- id
- corporateName
- tradeName
- documentNumber
- contactName
- phone
- email
- active

### 5. Equipment

Entidade principal do estoque.

Campos principais:

- id
- internalCode
- name
- category
- brand
- model
- serialNumber
- patrimonyNumber
- quantity
- minimumStock
- status
- location
- responsibleName
- supplier
- entryDate
- notes
- active
- version
- createdAt
- updatedAt

Observacoes importantes:

- essa entidade cobre tanto ativos unitarios quanto itens de estoque;
- para itens rastreaveis individualmente, o normal sera `quantity = 1`;
- `serialNumber` e `patrimonyNumber` serao unicos quando informados;
- `version` sera usado para controle de concorrencia otimista, importante em ambiente com varios usuarios.

### 6. StockMovement

Representa toda alteracao de estoque ou localizacao.

Campos principais:

- id
- equipment
- movementType
- quantity
- sourceLocation
- destinationLocation
- responsibleName
- movementAt
- notes
- performedBy
- createdAt

Tipos previstos:

- ENTRADA
- SAIDA
- TRANSFERENCIA
- ENVIO_MANUTENCAO
- RETORNO_MANUTENCAO
- BAIXA_DESCARTE

### 7. AuditLog

Registra eventos importantes executados pelos usuarios.

Campos principais:

- id
- user
- action
- entityName
- entityId
- description
- ipOrStation
- createdAt

Acoes previstas:

- LOGIN
- CADASTRO
- EDICAO
- EXCLUSAO
- MOVIMENTACAO

## Enums principais

### Role

- ADMIN
- TECNICO
- VISUALIZADOR

### EquipmentStatus

- DISPONIVEL
- EM_USO
- EM_MANUTENCAO
- DEFEITUOSO
- DESCARTADO

### MovementType

- ENTRADA
- SAIDA
- TRANSFERENCIA
- ENVIO_MANUTENCAO
- RETORNO_MANUTENCAO
- BAIXA_DESCARTE

### AuditAction

- LOGIN
- CADASTRO
- EDICAO
- EXCLUSAO
- MOVIMENTACAO

## Regras de negocio definidas desde a arquitetura

Estas regras ja orientam as proximas etapas:

1. O sistema nao permitira quantidade negativa em estoque.
2. Movimentacoes de saida, descarte e manutencao deverao validar saldo disponivel.
3. `internalCode` sera obrigatorio e unico.
4. `serialNumber` sera unico quando informado.
5. `patrimonyNumber` sera unico quando informado.
6. Usuarios `VISUALIZADOR` terao acesso somente leitura.
7. Usuarios `TECNICO` poderao operar estoque e consultar relatorios, mas nao gerenciar estrutura critica de usuarios como um `ADMIN`.
8. Exclusoes de entidades com historico associado deverao ser preferencialmente logicas, nao fisicas.
9. Toda operacao critica devera gerar registro de auditoria.
10. Controllers nao terao regra de negocio relevante embutida.

## Estrategia de telas

As telas minimas da V1 serao:

- `login-view.fxml`
- `main-layout.fxml`
- `dashboard-view.fxml`
- `equipment-view.fxml`
- `movement-view.fxml`
- `user-view.fxml`
- `report-view.fxml`

### Navegacao proposta

- login
- layout principal com menu lateral
- dashboard como tela inicial apos autenticacao
- modulos por area funcional

Essa estrutura melhora usabilidade, reduz poluicao visual e facilita crescimento futuro.

## Decisoes de design importantes

### 1. Nao usar Spring nesta V1

Motivo:

- para uma aplicacao desktop, o ganho nao compensa o aumento de complexidade;
- a arquitetura em camadas com Hibernate atende muito bem o problema;
- inicializacao mais simples e distribuicao mais leve.

### 2. Usar FXML em vez de telas montadas totalmente por codigo

Motivo:

- melhor separacao entre interface e comportamento;
- manutencao visual mais simples;
- facilita padronizacao e evolucao da UI.

### 3. Manter DTOs separados das entidades

Motivo:

- evita acoplamento entre banco e interface;
- protege o modelo;
- facilita filtros, formularios e exportacoes.

### 4. Adotar concorrencia otimista no cadastro principal

Motivo:

- evita sobrescrita silenciosa entre usuarios em diferentes maquinas;
- importante para um sistema desktop conectado a banco centralizado.

## Resultado esperado apos a ETAPA 1

Ao final desta etapa, a solucao esta conceitualmente fechada com:

- nome do sistema;
- arquitetura definida;
- tecnologias escolhidas;
- entidades mapeadas;
- estrutura de pacotes organizada;
- regras estruturais de negocio definidas;
- base pronta para modelagem do banco na ETAPA 2.

## Preparacao para a ETAPA 2

Na proxima etapa, o banco PostgreSQL sera modelado com:

- tabelas;
- chaves primarias;
- chaves estrangeiras;
- constraints;
- indices;
- dados iniciais para teste.

Essa modelagem seguira exatamente as decisoes registradas neste documento para evitar retrabalho.
