package br.com.estoqueti.config;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class PersistenceShutdownHook {

    @PreDestroy
    void closePersistenceResources() {
        EntityManagerFactoryProvider.close();
        DataSourceFactory.close();
    }
}
