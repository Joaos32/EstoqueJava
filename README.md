# EstoqueTI Desktop

Sistema desktop corporativo para controle de estoque de equipamentos de TI, desenvolvido em Java 17 com JavaFX, Maven e PostgreSQL.

## Visao geral

O projeto foi construido para controlar o ciclo de vida de ativos e perifericos de TI em um ambiente corporativo, com autenticacao, cadastro de equipamentos, movimentacoes de estoque, dashboard gerencial, auditoria e exportacao de relatorios.

## Funcionalidades implementadas

- Login com usuario e senha usando BCrypt.
- Perfis de acesso `ADMIN`, `TECNICO` e `VISUALIZADOR`.
- Cadastro e consulta de equipamentos com filtros por nome, codigo, patrimonio, serie, categoria, status, localizacao e responsavel.
- Registro de movimentacoes de estoque: `ENTRADA`, `SAIDA`, `TRANSFERENCIA`, `ENVIO_MANUTENCAO`, `RETORNO_MANUTENCAO` e `BAIXA_DESCARTE`.
- Dashboard com indicadores principais, itens abaixo do estoque minimo e ultimas movimentacoes.
- Relatorios em CSV e PDF.
- Auditoria das acoes principais ja implementadas no fluxo: login, cadastro de usuarios, cadastro de equipamentos e movimentacoes.
- Testes de integracao executados contra PostgreSQL real.

## Stack tecnica

- Java 17
- JavaFX + FXML + CSS
- Maven
- PostgreSQL
- JPA/Hibernate
- HikariCP
- BCrypt
- OpenPDF
- Apache Commons CSV
- JUnit 5
- SLF4J + Logback

## Arquitetura

A aplicacao segue arquitetura em camadas com apresentacao MVC:

- `controller`: camada JavaFX/FXML.
- `service`: regras de negocio e orquestracao.
- `repository`: acesso a dados com JPA.
- `model`: entidades e enums do dominio.
- `dto`: contratos de entrada e saida entre UI e servicos.
- `config`: bootstrap da aplicacao, JPA, pool e propriedades.
- `util` e `session`: apoio tecnico e sessao autenticada.

Essa divisao reduz acoplamento entre interface e regra de negocio, facilita testes e deixa o projeto pronto para novas funcionalidades.

## Estrutura do repositorio

```text
.
|-- database/
|   `-- postgresql/
|       |-- 01-create-database.sql
|       `-- 02-schema-and-seed.sql
|-- docs/
|   |-- etapa-01-arquitetura.md
|   |-- etapa-02-banco-postgresql.md
|   |-- etapa-03-estrutura-maven.md
|   |-- etapa-04-conexao-postgresql.md
|   |-- etapa-05-autenticacao-usuarios.md
|   |-- etapa-06-cadastro-listagem-equipamentos.md
|   |-- etapa-07-movimentacoes-estoque.md
|   |-- etapa-08-dashboard.md
|   |-- etapa-09-relatorios-pdf-csv.md
|   `-- etapa-10-revisao-preparacao-github.md
|-- .github/
|   `-- workflows/
|       `-- ci.yml
|-- src/
|   |-- main/
|   |   |-- java/br/com/estoqueti/
|   |   `-- resources/
|   `-- test/java/br/com/estoqueti/
|-- .editorconfig
|-- .gitattributes
|-- .gitignore
`-- pom.xml
```

## Banco de dados

O sistema usa exclusivamente PostgreSQL, conforme o requisito do projeto.

### 1. Criar o banco

Execute primeiro:

```powershell
psql -h localhost -U postgres -d postgres -f database/postgresql/01-create-database.sql
```

### 2. Criar schema, tabelas, indices e seeds

Depois execute:

```powershell
psql -h localhost -U postgres -d estoqueti -f database/postgresql/02-schema-and-seed.sql
```

Os scripts criam o schema `estoque_ti`, constraints, indices e dados iniciais para testes.

## Configuracao local

A configuracao padrao fica em `src/main/resources/application.properties`.

Para manter senha e ajustes locais fora do versionamento:

1. Copie `src/main/resources/application-local.properties.example` para `src/main/resources/application-local.properties`.
2. Ajuste a senha do PostgreSQL e, se necessario, a URL de conexao.

Exemplo:

```properties
app.name=EstoqueTI Desktop
app.version=1.0.0-SNAPSHOT

database.driver-class-name=org.postgresql.Driver
database.url=jdbc:postgresql://localhost:5432/estoqueti
database.username=postgres
database.password=SUA_SENHA
database.schema=estoque_ti
```

Tambem e possivel sobrescrever propriedades via variaveis de ambiente usando o prefixo `ESTOQUETI_` ou parametros `-D` no Maven.

## Como executar

### Compilar

```powershell
mvn clean compile
```

### Rodar testes

```powershell
mvn "-Ddatabase.password=SUA_SENHA" test
```

### Abrir a aplicacao desktop

```powershell
mvn "-Ddatabase.password=SUA_SENHA" javafx:run
```

## Usuarios iniciais para teste

Os scripts de seed criam os seguintes usuarios:

- `admin` / `Admin@123`
- `tecnico` / `Tecnico@123`
- `visual` / `Visual@123`

## Modulos da interface

- `Login`: autenticacao e abertura da sessao.
- `Dashboard`: visao executiva do estoque e das ultimas movimentacoes.
- `Equipamentos`: filtros e cadastro de ativos.
- `Movimentacoes`: entradas, saidas, transferencias, manutencao e descarte.
- `Usuarios`: consulta e cadastro de usuarios por perfil autorizado.
- `Relatorios`: exportacao em CSV e PDF.

## Qualidade e manutencao

- Persistencia com `RESOURCE_LOCAL` adequada para desktop.
- Validacoes de entrada e mensagens amigaveis na interface.
- Regras para evitar saldo negativo e inconsistencias de localizacao/status.
- Seeds para acelerar homologacao e demonstracao.
- Workflow de CI em `.github/workflows/ci.yml` para validar o projeto em GitHub Actions.

## Documentacao por etapa

- [ETAPA 1](docs/etapa-01-arquitetura.md)
- [ETAPA 2](docs/etapa-02-banco-postgresql.md)
- [ETAPA 3](docs/etapa-03-estrutura-maven.md)
- [ETAPA 4](docs/etapa-04-conexao-postgresql.md)
- [ETAPA 5](docs/etapa-05-autenticacao-usuarios.md)
- [ETAPA 6](docs/etapa-06-cadastro-listagem-equipamentos.md)
- [ETAPA 7](docs/etapa-07-movimentacoes-estoque.md)
- [ETAPA 8](docs/etapa-08-dashboard.md)
- [ETAPA 9](docs/etapa-09-relatorios-pdf-csv.md)
- [ETAPA 10](docs/etapa-10-revisao-preparacao-github.md)

## Evolucao recomendada

Os proximos incrementos naturais para a V2 sao:

- edicao completa de equipamentos e usuarios;
- inativacao controlada com historico;
- paginacao e filtros avancados em tabelas maiores;
- empacotamento instalavel com `jpackage`;
- CI com publicacao automatizada de artefatos de release.