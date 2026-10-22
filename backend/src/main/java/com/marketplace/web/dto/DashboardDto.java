package com.marketplace.web.dto;

import java.util.List;
import java.util.Map;

public record DashboardDto(
        String title,
        String message,
        List<Map<String, Object>> items
) {
}
