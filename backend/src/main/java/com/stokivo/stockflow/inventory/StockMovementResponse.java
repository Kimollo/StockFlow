package com.stokivo.stockflow.inventory;

import java.time.Instant;

public record StockMovementResponse(
		Long id,
		Long productId,
		String sku,
		String productName,
		String type,
		int quantityChange,
		int stockAfter,
		String reason,
		Long saleId,
		Instant createdAt
) {
}
