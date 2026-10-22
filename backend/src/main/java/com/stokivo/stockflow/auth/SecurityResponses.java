package com.stokivo.stockflow.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

final class SecurityResponses {

	private SecurityResponses() {
	}

	static void write(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\":\"" + message + "\"}");
	}
}
