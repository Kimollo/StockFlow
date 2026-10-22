package com.stokivo.stockflow.sale;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleLineRequest(
		@NotNull(message = "Product is required.")
		Long productId,
		@NotNull(message = "Quantity is required.")
		@Min(value = 1, message = "Quantity must be at least 1.")
		@Max(value = 1_000_000, message = "Quantity is too large.")
		Integer quantity
) {
}
