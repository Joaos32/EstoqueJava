package br.com.estoqueti.controller;

import br.com.estoqueti.config.DatabaseConnectionStatus;
import br.com.estoqueti.service.DatabaseConnectivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Tag(name = "Sistema")
public class SystemApiController {

    @GetMapping("/database-connectivity")
    @Operation(summary = "Valida a conectividade com PostgreSQL e bootstrap JPA")
    public DatabaseConnectionStatus checkDatabaseConnectivity() {
        return DatabaseConnectivityService.checkConnection();
    }
}
