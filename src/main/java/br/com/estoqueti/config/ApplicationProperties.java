package br.com.estoqueti.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public final class ApplicationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProperties.class);
    private static final String BASE_RESOURCE_NAME = "application.properties";
    private static final String LOCAL_RESOURCE_NAME = "application-local.properties";
    private static final Properties PROPERTIES = new Properties();
    private static volatile boolean loaded;

    private ApplicationProperties() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        PROPERTIES.clear();
        loadRequiredResource(BASE_RESOURCE_NAME);
        loadOptionalResource(LOCAL_RESOURCE_NAME);
        loaded = true;
        LOGGER.info("Propriedades da aplicacao carregadas com sucesso.");
    }

    public static String get(String key) {
        return resolveValue(Objects.requireNonNull(key, "A chave da propriedade e obrigatoria."));
    }

    public static String get(String key, String defaultValue) {
        String value = resolveValue(Objects.requireNonNull(key, "A chave da propriedade e obrigatoria."));
        return hasText(value) ? value : defaultValue;
    }

    public static String getRequired(String key) {
        String value = resolveValue(Objects.requireNonNull(key, "A chave da propriedade e obrigatoria."));
        if (!hasText(value)) {
            throw new IllegalStateException("A propriedade obrigatoria nao foi configurada: " + key);
        }
        return value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Valor inteiro invalido para a propriedade: " + key, exception);
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Valor long invalido para a propriedade: " + key, exception);
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value.trim());
    }

    private static void loadRequiredResource(String resourceName) {
        try (InputStream inputStream = ApplicationProperties.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Arquivo de propriedades nao encontrado: " + resourceName);
            }

            PROPERTIES.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar o arquivo de propriedades: " + resourceName, exception);
        }
    }

    private static void loadOptionalResource(String resourceName) {
        try (InputStream inputStream = ApplicationProperties.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return;
            }

            Properties overrideProperties = new Properties();
            overrideProperties.load(inputStream);
            PROPERTIES.putAll(overrideProperties);
            LOGGER.info("Arquivo opcional de propriedades carregado: {}", resourceName);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar o arquivo opcional de propriedades: " + resourceName, exception);
        }
    }

    private static String resolveValue(String key) {
        load();

        String systemValue = System.getProperty(key);
        if (hasText(systemValue)) {
            return systemValue;
        }

        String environmentValue = System.getenv(toEnvironmentKey(key));
        if (hasText(environmentValue)) {
            return environmentValue;
        }

        return PROPERTIES.getProperty(key);
    }

    private static String toEnvironmentKey(String key) {
        return "ESTOQUETI_" + key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
