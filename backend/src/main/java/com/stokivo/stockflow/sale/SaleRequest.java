package com.stokivo.stockflow.sale;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SaleRequest(
		@NotNull(message = "Payment method is required.")
		PaymentMethod paymentMethod,
		@NotEmpty(message = "Add at least one product.")
		@Valid
		List<SaleLineRequest> items
) {
}
