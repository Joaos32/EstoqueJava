# ETAPA 5 - Autenticacao e Usuarios

## O que foi feito

Nesta etapa, o projeto passou a ter autenticacao real baseada em banco, perfis de acesso e uma tela funcional de usuarios dentro da area autenticada.

## Entregas principais

- entidade JPA `User` mapeada para `app_user`
- entidade JPA `AuditLog` mapeada para `audit_log`
- enums `Role` e `AuditAction`
- `AuthenticationService` com validacao de login e senha BCrypt
- `UserService` com listagem e cadastro de usuarios
- auditoria automatica de login bem-sucedido e cadastro de usuario
- sessao de usuario autenticado na aplicacao desktop
- tela real de login
- layout principal autenticado com menu lateral
- tela de usuarios com tabela e formulario de cadastro

## Regras implementadas

- somente usuarios ativos podem autenticar
- senha validada com BCrypt
- somente `ADMIN` pode cadastrar usuarios
- `TECNICO` e `VISUALIZADOR` acessam a tela de usuarios apenas em modo consulta
- todo login bem-sucedido gera auditoria
- todo cadastro de usuario gera auditoria

## Arquivos principais

- `src/main/java/br/com/estoqueti/model/entity/User.java`
- `src/main/java/br/com/estoqueti/model/entity/AuditLog.java`
- `src/main/java/br/com/estoqueti/model/enums/Role.java`
- `src/main/java/br/com/estoqueti/model/enums/AuditAction.java`
- `src/main/java/br/com/estoqueti/service/AuthenticationService.java`
- `src/main/java/br/com/estoqueti/service/UserService.java`
- `src/main/java/br/com/estoqueti/controller/LoginController.java`
- `src/main/java/br/com/estoqueti/controller/MainLayoutController.java`
- `src/main/java/br/com/estoqueti/controller/UserController.java`
- `src/main/resources/br/com/estoqueti/view/fxml/login-view.fxml`
- `src/main/resources/br/com/estoqueti/view/fxml/main-layout-view.fxml`
- `src/main/resources/br/com/estoqueti/view/fxml/user-view.fxml`

## Resultado da etapa

A aplicacao agora consegue:

- autenticar usuarios reais do PostgreSQL
- diferenciar perfis de acesso
- abrir a area autenticada apos login
- listar usuarios cadastrados
- cadastrar novos usuarios com senha hash BCrypt
- registrar auditoria das acoes criticas dessa etapa

## Validacao prevista

- compilacao Maven
- testes de integracao de autenticacao
- testes de integracao de usuarios
