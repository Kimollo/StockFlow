package com.stokivo.stockflow.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesTrendPoint(
		LocalDate date,
		BigDecimal total,
		long count
) {
}
