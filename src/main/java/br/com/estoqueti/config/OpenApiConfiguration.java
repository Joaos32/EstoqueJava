package br.com.estoqueti.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI estoqueJavaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EstoqueJava API")
                        .description("Backend Spring Boot para controle de estoque de equipamentos de TI.")
                        .version("v1")
                        .contact(new Contact()
                                .name("EstoqueJava")
                                .url("https://github.com/Joaos32/EstoqueJava.git"))
                        .license(new License()
                                .name("Projeto interno")
                                .url("https://github.com/Joaos32/EstoqueJava.git")));
    }
}
