package br.com.estoqueti.config;

import java.util.regex.Pattern;

public final class SqlIdentifierValidator {

    private static final Pattern SIMPLE_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private SqlIdentifierValidator() {
    }

    public static String requireSimpleIdentifier(String value, String propertyName) {
        if (value == null) {
            throw new IllegalStateException("A propriedade obrigatoria nao foi configurada: " + propertyName);
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalStateException("A propriedade obrigatoria nao foi configurada: " + propertyName);
        }
        if (!SIMPLE_IDENTIFIER_PATTERN.matcher(normalizedValue).matches()) {
            throw new IllegalStateException("Valor invalido para identificador SQL em " + propertyName + ": " + value);
        }
        return normalizedValue;
    }
}