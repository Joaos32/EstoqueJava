-- Execute este arquivo conectado ao banco "postgres" ou outro banco administrativo.
-- Ele cria o banco principal da aplicacao.

CREATE DATABASE estoqueti
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0;
