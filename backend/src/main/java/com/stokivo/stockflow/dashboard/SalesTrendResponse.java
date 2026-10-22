package com.stokivo.stockflow.dashboard;

import java.time.LocalDate;
import java.util.List;

public record SalesTrendResponse(
		LocalDate from,
		LocalDate to,
		List<SalesTrendPoint> points
) {
}
