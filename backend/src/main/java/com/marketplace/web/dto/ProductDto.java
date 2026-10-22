package com.marketplace.web.dto;

public record ProductDto(Long id, Long addressId, String name, boolean active) {
}
