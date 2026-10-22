package com.marketplace.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBatchDto(Long id, Long productId, LocalDate expiryDate, BigDecimal quantity, boolean active) {
}
