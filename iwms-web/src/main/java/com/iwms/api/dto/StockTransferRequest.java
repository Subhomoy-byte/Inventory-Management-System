package com.iwms.api.dto;

public record StockTransferRequest(
        String sku,
        String batchNumber,
        String fromBinId,
        String toBinId,
        int quantity,
        String reason
) {
}
