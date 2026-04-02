package br.com.estoqueti.config;

import br.com.estoqueti.service.ApiAuthenticatedUserService;
import br.com.estoqueti.service.ApiFileExportService;
import br.com.estoqueti.service.AuthenticationService;
import br.com.estoqueti.service.DashboardService;
import br.com.estoqueti.service.DeliveryProtocolService;
import br.com.estoqueti.service.EquipmentService;
import br.com.estoqueti.service.ReturnProtocolService;
import br.com.estoqueti.service.StockMovementService;
import br.com.estoqueti.service.UserService;
import br.com.estoqueti.service.report.ReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiServicesConfiguration {

    @Bean
    AuthenticationService authenticationService() {
        return new AuthenticationService();
    }

    @Bean
    DashboardService dashboardService() {
        return new DashboardService();
    }

    @Bean
    EquipmentService equipmentService() {
        return new EquipmentService();
    }

    @Bean
    StockMovementService stockMovementService() {
        return new StockMovementService();
    }

    @Bean
    UserService userService() {
        return new UserService();
    }

    @Bean
    DeliveryProtocolService deliveryProtocolService() {
        return new DeliveryProtocolService();
    }

    @Bean
    ReturnProtocolService returnProtocolService() {
        return new ReturnProtocolService();
    }

    @Bean
    ReportService reportService() {
        return new ReportService();
    }

    @Bean
    ApiAuthenticatedUserService apiAuthenticatedUserService() {
        return new ApiAuthenticatedUserService();
    }

    @Bean
    ApiFileExportService apiFileExportService(
            DeliveryProtocolService deliveryProtocolService,
            ReturnProtocolService returnProtocolService,
            ReportService reportService
    ) {
        return new ApiFileExportService(deliveryProtocolService, returnProtocolService, reportService);
    }
}
