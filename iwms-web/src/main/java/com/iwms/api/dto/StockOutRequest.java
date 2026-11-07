package com.iwms.api.dto;

public record StockOutRequest(
        String sku,
        int quantity,
        String reason
) {
}
