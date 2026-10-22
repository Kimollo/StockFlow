package com.stokivo.stockflow.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustmentRequest(
		@NotNull(message = "Product is required.")
		Long productId,
		@NotNull(message = "Quantity is required.")
		@Min(value = 1, message = "Quantity must be at least 1.")
		@Max(value = 1_000_000, message = "Quantity is too large.")
		Integer quantity,
		@NotNull(message = "Direction is required.")
		StockDirection direction,
		@NotBlank(message = "A reason is required.")
		@Size(max = 255, message = "Reason must be 255 characters or fewer.")
		String reason
) {
}
