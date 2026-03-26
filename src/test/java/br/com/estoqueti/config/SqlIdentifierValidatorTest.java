package br.com.estoqueti.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlIdentifierValidatorTest {

    @Test
    void shouldAcceptSimpleSchemaIdentifier() {
        assertEquals("estoque_ti", SqlIdentifierValidator.requireSimpleIdentifier("estoque_ti", "database.schema"));
    }

    @Test
    void shouldRejectSchemaIdentifierWithDangerousCharacters() {
        assertThrows(IllegalStateException.class, () -> SqlIdentifierValidator.requireSimpleIdentifier("estoque_ti;DROP TABLE app_user", "database.schema"));
    }
}