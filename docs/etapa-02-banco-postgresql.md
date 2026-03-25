# ETAPA 2 - Modelagem do Banco PostgreSQL

## O que foi feito

Nesta etapa, o dominio definido na ETAPA 1 foi convertido para um modelo relacional em PostgreSQL com foco em:

- integridade referencial;
- constraints de validacao;
- indices para consultas comuns;
- dados iniciais para testes;
- compatibilidade com a futura camada JPA/Hibernate.

## Estrutura de entrega

Os arquivos foram separados em dois scripts por um motivo importante:

1. `CREATE DATABASE` deve ser executado antes e fora da conexao com o proprio banco alvo.
2. O schema, tabelas e inserts devem ser executados depois, ja conectados ao banco criado.

## Arquivos desta etapa

### `database/postgresql/01-create-database.sql`

Responsavel por criar o banco `estoqueti`.

### `database/postgresql/02-schema-and-seed.sql`

Responsavel por:

- criar a extensao `pgcrypto`;
- criar o schema `estoque_ti`;
- criar tabelas;
- criar constraints e foreign keys;
- criar indices;
- criar trigger para `updated_at`;
- inserir dados iniciais de teste.

## Decisoes de modelagem

### 1. Schema dedicado

Foi criado o schema `estoque_ti` para manter a organizacao do banco e evitar mistura com objetos genericos do `public`.

### 2. Enums em `VARCHAR` com `CHECK`

Em vez de enums nativos do PostgreSQL, os campos de dominio foram modelados com `VARCHAR` e constraints `CHECK`.

Motivo:

- melhor compatibilidade com JPA/Hibernate;
- menos atrito em futuras migracoes;
- leitura simples no banco.

### 3. Exclusao logica

As tabelas principais possuem `active`, evitando remocoes fisicas em casos onde existe historico relacionado.

### 4. Concorrencia otimista

A tabela `equipment` possui a coluna `version`, preparada para uso com `@Version` no Hibernate.

### 5. Atualizacao automatica de `updated_at`

Foi criada uma funcao com trigger para manter o campo `updated_at` consistente em atualizacoes.

### 6. Senhas iniciais com BCrypt

Os inserts de usuarios usam `crypt(..., gen_salt('bf', 10))`, que gera hash BCrypt via extensao `pgcrypto`.

Isso atende o requisito de seguranca sem depender de hash manual no script.

## Tabelas criadas

- `app_user`
- `equipment_category`
- `location`
- `supplier`
- `equipment`
- `stock_movement`
- `audit_log`

## Regras importantes reforcadas no banco

- username unico por indice case-insensitive;
- nome de categoria unico;
- nome de localizacao unico;
- codigo interno unico;
- numero de serie unico quando informado;
- numero de patrimonio unico quando informado;
- quantidade de equipamento nao pode ser negativa;
- estoque minimo nao pode ser negativo;
- quantidade de movimentacao deve ser maior que zero;
- regras de origem e destino variam conforme o tipo de movimentacao;
- foreign keys restringem exclusoes que comprometeriam o historico.

## Usuarios iniciais para teste

Os usuarios abaixo sao criados pelo script:

- `admin` / `Admin@123`
- `tecnico` / `Tecnico@123`
- `visual` / `Visual@123`

## Ordem de execucao

1. Executar `01-create-database.sql` conectado ao banco administrativo.
2. Conectar no banco `estoqueti`.
3. Executar `02-schema-and-seed.sql`.

## Resultado esperado apos a ETAPA 2

Ao final desta etapa, o banco estara pronto para suportar:

- autenticacao;
- cadastro de usuarios;
- cadastro e consulta de equipamentos;
- movimentacoes;
- auditoria;
- dashboard;
- relatorios.

Essa base foi pensada para se encaixar diretamente na ETAPA 3, quando a estrutura Maven e as dependencias do projeto serao criadas.
