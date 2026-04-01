package br.com.estoqueti.service;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.lang.reflect.Method;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceMessageTest {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @Test
    void shouldExplainMissingDatabasePassword() throws Exception {
        String message = resolveInfrastructureMessage(
                new PSQLException(
                        "The server requested SCRAM-based authentication, but the password is an empty string.",
                        PSQLState.CONNECTION_UNABLE_TO_CONNECT
                )
        );

        assertTrue(message.contains("config/application-local.properties"));
        assertTrue(message.contains("reinicie o app"));
    }

    @Test
    void shouldExplainConnectionTimeout() throws Exception {
        String message = resolveInfrastructureMessage(
                new SQLTransientConnectionException("Connection is not available, request timed out")
        );

        assertTrue(message.contains("nao respondeu a tempo"));
        assertTrue(message.contains("PostgreSQL"));
    }

    private String resolveInfrastructureMessage(Throwable throwable) throws Exception {
        Method method = AuthenticationService.class.getDeclaredMethod("resolveInfrastructureMessage", Throwable.class);
        method.setAccessible(true);
        return (String) method.invoke(authenticationService, throwable);
    }
}
