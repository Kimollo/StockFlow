package com.stokivo.stockflow.dashboard;

import java.math.BigDecimal;

public record TopProduct(
		Long productId,
		String sku,
		String name,
		long quantitySold,
		BigDecimal revenue
) {
}
