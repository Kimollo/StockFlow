package com.stokivo.stockflow.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use letters, numbers, dots, or hyphens.") String username,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotNull Role role
) {
}
