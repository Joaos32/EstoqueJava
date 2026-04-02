package br.com.estoqueti.controller;

import br.com.estoqueti.dto.dashboard.DashboardSummaryDto;
import br.com.estoqueti.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Carrega o resumo gerencial do dashboard")
    public DashboardSummaryDto loadDashboard() {
        return dashboardService.loadDashboard();
    }
}
