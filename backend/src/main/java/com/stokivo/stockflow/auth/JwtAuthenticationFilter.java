package com.stokivo.stockflow.auth;

import java.io.IOException;
import java.util.List;

import com.stokivo.stockflow.user.User;
import com.stokivo.stockflow.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository users;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = header.substring(7).trim();
		String username = jwtService.username(token);
		if (username == null) {
			SecurityResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Sign in again.");
			return;
		}
		User user = users.findByUsernameIgnoreCase(username).orElse(null);
		if (user == null || !user.isActive() || jwtService.credentialsVersion(token) != user.getCredentialsVersion()) {
			String message = user != null && !user.isActive() ? "This account is deactivated." : "Sign in again.";
			SecurityResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, message);
			return;
		}
		var authentication = new UsernamePasswordAuthenticationToken(
				user.getUsername(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
		authentication.setDetails(user);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}
}
