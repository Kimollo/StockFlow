package com.stokivo.stockflow.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
		Long id,
		String saleNumber,
		String paymentMethod,
		String status,
		BigDecimal subtotal,
		BigDecimal total,
		Instant soldAt,
		List<SaleItemResponse> items
) {
}
