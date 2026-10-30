package com.marketplace.web.dto;

public record ActivityDto(
        Long id,
        Long addressId,
        Long processId,
        String text,
        String imagePath,
        String status,
        boolean shared,
        boolean active
) {}
