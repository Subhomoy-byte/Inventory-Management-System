package com.iwms.api.dto;

import java.util.List;

public record ProductDto(
        String sku,
        String name,
        String category,
        Double unitPrice,
        Double costPrice,
        Integer minStockLevel,
        Integer currentStock,
        Double storageCost,
        List<String> handlingRules
) {
}
