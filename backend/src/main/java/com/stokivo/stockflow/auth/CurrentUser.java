package com.stokivo.stockflow.auth;

import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.user.Role;
import com.stokivo.stockflow.user.User;
import com.stokivo.stockflow.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUser {

	private final UserRepository users;
	private User cached;

	public CurrentUser(UserRepository users) {
		this.users = users;
	}

	public User require() {
		if (cached != null) {
			return cached;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in again.");
		}
		if (authentication.getDetails() instanceof User user) {
			cached = user;
			return user;
		}
		cached = users.findByUsernameIgnoreCase(authentication.getName())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sign in again."));
		return cached;
	}

	public boolean administrator() {
		return require().getRole() == Role.ADMINISTRATOR;
	}
}
