package com.stokivo.stockflow.sale;

import java.math.BigDecimal;

public record SaleItemResponse(
		Long id,
		Long productId,
		String sku,
		String productName,
		int quantity,
		int returnedQuantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal
) {
}
