package com.stokivo.stockflow.auth;

import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.user.User;
import com.stokivo.stockflow.user.UserRepository;
import com.stokivo.stockflow.user.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		User user = users.findByUsernameIgnoreCase(request.username().trim())
				.orElseThrow(this::invalid);
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalid();
		}
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "This account is deactivated.");
		}
		return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
	}

	private ApiException invalid() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "The username or password is incorrect.");
	}
}
