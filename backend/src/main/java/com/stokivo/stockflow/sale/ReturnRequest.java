package com.stokivo.stockflow.sale;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ReturnRequest(
		@NotEmpty(message = "Choose at least one item to return.") List<@Valid ReturnLineRequest> items
) {
}
