package com.stokivo.stockflow.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank(message = "Name is required.")
		@Size(max = 80, message = "Name must be 80 characters or fewer.")
		String name
) {
}
