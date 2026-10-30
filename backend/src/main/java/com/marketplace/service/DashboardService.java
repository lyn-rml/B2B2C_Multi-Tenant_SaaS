package com.marketplace.service;

import com.marketplace.auth.TenantContextHolder;
import com.marketplace.domain.RoleType;
import com.marketplace.domain.TenantEntity;
import com.marketplace.repository.TenantRepository;
import com.marketplace.web.dto.AddressDto;
import com.marketplace.web.dto.DashboardDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final TenantSchemaAddressService tenantSchemaAddressService;
    private final TenantRepository tenantRepository;

    public DashboardService(
            TenantSchemaAddressService tenantSchemaAddressService,
            TenantRepository tenantRepository
    ) {
        this.tenantSchemaAddressService = tenantSchemaAddressService;
        this.tenantRepository = tenantRepository;
    }

    public DashboardDto studentDashboard() {
        TenantContextHolder.requireActiveRole(RoleType.STUDENT);

        List<Map<String, Object>> offers = new ArrayList<>();
        for (TenantEntity tenant : tenantRepository.findAll()) {
            offers.addAll(tenantSchemaAddressService.listSharedBasketsInSchema(tenant));
        }

        return new DashboardDto(
                "Student marketplace",
                "Discover discounted baskets near your campus.",
                List.of(
                        Map.of("label", "Shared offers", "value", offers.size()),
                        Map.of("label", "Offers", "value", offers)
                )
        );
    }

    public DashboardDto employeeDashboard(Long userId) {
        return new DashboardDto(
                "Employee workspace",
                "Your employee account.",
                List.of(
                        Map.of("userId", userId)
                )
        );
    }

    public DashboardDto storeAdminDashboard(Long userId) {
        List<AddressDto> addresses = tenantSchemaAddressService.listAssignedAddressesForStoreAdmin(userId);
        List<Map<String, Object>> items = addresses.stream()
                .map(this::addressToMap)
                .toList();

        return new DashboardDto(
                "Store administration",
                "Locations you manage within your chain.",
                items
        );
    }

    private Map<String, Object> addressToMap(AddressDto address) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", address.id());
        map.put("label", address.label());
        map.put("city", address.city());
        map.put("street_address", address.streetAddress());
        map.put("tenantId", address.tenantId());
        map.put("tenantName", address.tenantName());
        return map;
    }
}
