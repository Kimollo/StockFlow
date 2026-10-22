package com.stokivo.stockflow.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		String database = result != null && result == 1 ? "UP" : "DOWN";
		return new HealthResponse("UP", "StockFlow", database);
	}
}
