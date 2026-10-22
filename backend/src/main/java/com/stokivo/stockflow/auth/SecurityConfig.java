package com.stokivo.stockflow.auth;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${stockflow.cors.allowed-origins}") String[] allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(allowedOrigins));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) ->
								SecurityResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Sign in to continue."))
						.accessDeniedHandler((request, response, exception) ->
								SecurityResponses.write(response, HttpServletResponse.SC_FORBIDDEN, "You do not have access to that.")))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasRole("ADMINISTRATOR")
						.requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMINISTRATOR")
						.requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMINISTRATOR")
						.requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**").hasRole("ADMINISTRATOR")
						.requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMINISTRATOR")
						.requestMatchers("/api/inventory/**").hasRole("ADMINISTRATOR")
						.requestMatchers("/api/dashboard/**").hasRole("ADMINISTRATOR")
						.requestMatchers("/api/users/**").hasRole("ADMINISTRATOR")
						.requestMatchers("/api/**").authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
