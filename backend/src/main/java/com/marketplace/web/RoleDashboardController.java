package com.marketplace.web;

import com.marketplace.service.DashboardService;
import com.marketplace.web.dto.DashboardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Role dashboards")
public class RoleDashboardController {

    private final DashboardService dashboardService;

    public RoleDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/student/dashboard")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Student dashboard data")
    public DashboardDto studentDashboard() {
        return dashboardService.studentDashboard();
    }

    @GetMapping("/api/employee/dashboard")
    @PreAuthorize("hasAuthority('EMPLOYEE')")
    @Operation(summary = "Employee dashboard data")
    public DashboardDto employeeDashboard(Authentication authentication) {
        return dashboardService.employeeDashboard(Long.parseLong(authentication.getName()));
    }

    @GetMapping("/api/store/dashboard")
    @PreAuthorize("hasAuthority('STORE_ADMIN')")
    @Operation(summary = "Store admin dashboard data")
    public DashboardDto storeDashboard(Authentication authentication) {
        return dashboardService.storeAdminDashboard(Long.parseLong(authentication.getName()));
    }
}
