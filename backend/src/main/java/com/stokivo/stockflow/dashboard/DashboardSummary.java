package com.stokivo.stockflow.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummary(
		LocalDate from,
		LocalDate to,
		BigDecimal todayTotal,
		long todayCount,
		BigDecimal periodTotal,
		long periodCount,
		long activeProducts,
		long lowStockProducts
) {
}
