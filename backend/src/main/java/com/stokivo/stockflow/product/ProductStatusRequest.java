package com.stokivo.stockflow.product;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
		@NotNull(message = "Active status is required.")
		Boolean active
) {
}
