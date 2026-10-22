package com.stokivo.stockflow.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReturnLineRequest(
		@NotNull Long saleItemId,
		@NotNull @Min(1) Integer quantity
) {
}
