package com.stokivo.stockflow.user;

public record UserResponse(
		Long id,
		String name,
		String username,
		String role,
		boolean active
) {
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getUsername(),
				user.getRole().name(),
				user.isActive());
	}
}
