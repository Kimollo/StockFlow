package com.stokivo.stockflow.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
		@NotBlank(message = "SKU is required.")
		@Size(max = 40, message = "SKU must be 40 characters or fewer.")
		String sku,
		@NotBlank(message = "Name is required.")
		@Size(max = 160, message = "Name must be 160 characters or fewer.")
		String name,
		@NotNull(message = "Category is required.")
		Long categoryId,
		@NotNull(message = "Selling price is required.")
		@DecimalMin(value = "0.01", message = "Selling price must be greater than zero.")
		@Digits(integer = 10, fraction = 2, message = "Selling price can have at most 2 decimal places.")
		BigDecimal sellingPrice,
		@NotNull(message = "Cost price is required.")
		@DecimalMin(value = "0.00", message = "Cost price cannot be negative.")
		@Digits(integer = 10, fraction = 2, message = "Cost price can have at most 2 decimal places.")
		BigDecimal costPrice,
		@NotNull(message = "Minimum stock level is required.")
		@Min(value = 0, message = "Minimum stock level cannot be negative.")
		Integer minimumStockLevel
) {
}
