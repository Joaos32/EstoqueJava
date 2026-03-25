# ETAPA 3 - Estrutura Inicial Maven

## O que foi feito

Nesta etapa, foi criada a base profissional do projeto Maven para a aplicacao desktop `EstoqueTI Desktop`, mantendo compatibilidade com a arquitetura e com o banco definidos nas etapas anteriores.

## Decisoes principais

### 1. Projeto Maven single-module

Foi mantido um projeto unico nesta primeira versao para reduzir complexidade operacional e facilitar:

- compilacao;
- execucao local;
- organizacao inicial;
- evolucao incremental nas proximas etapas.

### 2. JavaFX com FXML e CSS

Foram adicionadas as dependencias e um launcher funcional para validar a stack desktop desde o inicio.

### 3. Hibernate em vez de JDBC puro

As dependencias de persistencia ja foram preparadas para as proximas etapas, porque o sistema foi desenhado com entidades relacionadas, filtros, auditoria, dashboard e relatorios.

### 4. Sem `module-info.java` nesta V1

O projeto foi mantido no classpath tradicional para evitar atrito inicial com reflexao do Hibernate e simplificar o bootstrap da aplicacao desktop.

## Arquivos criados

- `pom.xml`
- `.gitignore`
- `src/main/java/br/com/estoqueti/AppLauncher.java`
- `src/main/java/br/com/estoqueti/config/ApplicationProperties.java`
- `src/main/java/br/com/estoqueti/controller/StartupController.java`
- `src/main/resources/application.properties`
- `src/main/resources/logback.xml`
- `src/main/resources/br/com/estoqueti/view/fxml/startup-view.fxml`
- `src/main/resources/br/com/estoqueti/view/css/application.css`

## Estrutura criada

```text
src
+-- main
¦   +-- java
¦   ¦   +-- br/com/estoqueti
¦   ¦       +-- config
¦   ¦       +-- controller
¦   ¦       +-- dto
¦   ¦       +-- exception
¦   ¦       +-- mapper
¦   ¦       +-- model
¦   ¦       +-- repository
¦   ¦       +-- service
¦   ¦       +-- session
¦   ¦       +-- util
¦   +-- resources
¦       +-- br/com/estoqueti/view
¦           +-- css
¦           +-- component
¦           +-- fxml
+-- test
    +-- java
        +-- br/com/estoqueti
```

## O que esta pronto ao final desta etapa

- base Maven configurada;
- dependencias principais registradas;
- launcher JavaFX funcional;
- estrutura de pacotes criada;
- recursos FXML e CSS organizados;
- base pronta para a ETAPA 4 de conexao com PostgreSQL.
## Evolucao posterior da V1

Na ETAPA 10, os arquivos `StartupController.java` e `startup-view.fxml` foram removidos porque a aplicacao passou a iniciar diretamente pela tela de login, que representa o fluxo definitivo da V1.