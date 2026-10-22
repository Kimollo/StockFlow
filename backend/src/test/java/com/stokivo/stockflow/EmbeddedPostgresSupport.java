package com.stokivo.stockflow;

import java.io.IOException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class EmbeddedPostgresSupport {

	private static final EmbeddedPostgres POSTGRES = start();

	private static EmbeddedPostgres start() {
		try {
			EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					postgres.close();
				}
				catch (IOException ignored) {
					// The test JVM is already exiting.
				}
			}));
			return postgres;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Could not start embedded PostgreSQL for tests.", exception);
		}
	}

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}
}
