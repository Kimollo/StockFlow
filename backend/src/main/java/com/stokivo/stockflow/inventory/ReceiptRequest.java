package com.stokivo.stockflow.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReceiptRequest(
		@NotNull(message = "Product is required.")
		Long productId,
		@NotNull(message = "Quantity is required.")
		@Min(value = 1, message = "Quantity must be at least 1.")
		@Max(value = 1_000_000, message = "Quantity is too large.")
		Integer quantity,
		@Size(max = 255, message = "Note must be 255 characters or fewer.")
		String note
) {
}
