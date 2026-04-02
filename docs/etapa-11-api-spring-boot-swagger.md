# ETAPA 11 - API Spring Boot com Swagger

## Objetivo

Esta etapa adiciona um backend HTTP em Spring Boot ao projeto, reaproveitando o dominio, os DTOs, os servicos e a persistencia JPA ja existentes.
A ideia foi migrar a camada de backend para uma API REST sem quebrar a aplicacao desktop JavaFX no mesmo repositorio.

## O que foi adicionado

- `EstoqueApiApplication` como entrypoint Spring Boot.
- `spring-boot-starter-web` para expor endpoints REST.
- `springdoc-openapi-starter-webmvc-ui` para documentacao OpenAPI e Swagger UI.
- `RestControllerAdvice` para padronizar respostas de erro em JSON.
- Controllers REST para autenticacao, sistema, dashboard, equipamentos, movimentacoes, usuarios, protocolos e relatorios.
- Suporte a download de arquivos para relatorios e protocolos gerados em CSV, PDF e DOCX.

## Endpoints principais

- `POST /api/auth/login`
- `GET /api/system/database-connectivity`
- `GET /api/dashboard`
- `GET /api/equipments`
- `POST /api/equipments`
- `DELETE /api/equipments/{equipmentId}`
- `GET /api/stock-movements`
- `POST /api/stock-movements`
- `GET /api/users`
- `POST /api/users`
- `POST /api/users/{userId}/reset-password`
- `POST /api/users/recover-password`
- `DELETE /api/users/{userId}`
- `POST /api/reports/generate`
- `POST /api/reports/export/{reportFormat}`
- `POST /api/delivery-protocols/export`
- `POST /api/return-protocols/export`

## Swagger

Com a API em execucao, a documentacao fica disponivel em:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

## Autenticacao nesta fase

Para acelerar a migracao e reaproveitar as regras existentes, as operacoes de escrita usam o cabecalho `X-User-Id`.
Esse header identifica o usuario autenticado que esta executando a operacao.
A autenticacao via token pode entrar como proxima etapa sobre a mesma base Spring Boot.

## Execucao

```powershell
mvn spring-boot:run
```

## Observacao de arquitetura

Esta migracao foca no backend HTTP e na documentacao da API.
A regra de negocio continua centralizada nos servicos atuais, e o acesso a dados continua reaproveitando a infraestrutura JPA/Hibernate ja existente no projeto.
