package com.stokivo.stockflow.dashboard;

import java.time.LocalDate;
import java.util.List;

public record TopProductsResponse(
		LocalDate from,
		LocalDate to,
		List<TopProduct> products
) {
}
