package com.stokivo.stockflow.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.stokivo.stockflow.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey key;
	private final long ttlHours;

	public JwtService(
			@Value("${stockflow.jwt.secret}") String secret,
			@Value("${stockflow.jwt.ttl-hours}") long ttlHours) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.ttlHours = ttlHours;
	}

	public String createToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("role", user.getRole().name())
				.claim("ver", user.getCredentialsVersion())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(ttlHours * 3600)))
				.signWith(key)
				.compact();
	}

	public String username(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return claims.getSubject();
		} catch (JwtException | IllegalArgumentException exception) {
			return null;
		}
	}

	public int credentialsVersion(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			Object version = claims.get("ver");
			if (version == null) {
				return 0;
			}
			if (version instanceof Number number) {
				return number.intValue();
			}
			return -1;
		} catch (JwtException | IllegalArgumentException exception) {
			return -1;
		}
	}
}
