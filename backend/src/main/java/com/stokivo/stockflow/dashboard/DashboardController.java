package com.stokivo.stockflow.dashboard;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/summary")
	public DashboardSummary summary(
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return dashboardService.summary(from, to);
	}

	@GetMapping("/sales-trend")
	public SalesTrendResponse trend(
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return dashboardService.trend(from, to);
	}

	@GetMapping("/top-products")
	public TopProductsResponse topProducts(
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "5") int limit) {
		return dashboardService.topProducts(from, to, limit);
	}

	@GetMapping(value = "/sales.csv", produces = "text/csv")
	public ResponseEntity<String> export(
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return dashboardService.export(from, to);
	}
}
