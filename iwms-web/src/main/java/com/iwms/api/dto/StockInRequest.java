package com.iwms.api.dto;

public record StockInRequest(
        String sku,
        String batchNumber,
        String binId,
        int quantity,
        String expiryDate,
        String reason
) {
}
