package com.stokivo.stockflow.product;

import java.math.BigDecimal;

public record ProductResponse(
		Long id,
		String sku,
		String name,
		Long categoryId,
		String categoryName,
		BigDecimal sellingPrice,
		BigDecimal costPrice,
		int stockQuantity,
		int minimumStockLevel,
		boolean active,
		boolean lowStock,
		boolean outOfStock
) {
}
