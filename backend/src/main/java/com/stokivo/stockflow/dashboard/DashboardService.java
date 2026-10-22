package com.stokivo.stockflow.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.product.ProductRepository;
import com.stokivo.stockflow.sale.Sale;
import com.stokivo.stockflow.sale.SaleItem;
import com.stokivo.stockflow.sale.SaleRepository;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final ZoneId zone = ZoneId.systemDefault();

	public DashboardService(SaleRepository saleRepository, ProductRepository productRepository) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public DashboardSummary summary(LocalDate from, LocalDate to) {
		DateRange period = range(from, to);
		List<Sale> sales = salesBetween(period.start(), period.end());
		BigDecimal periodTotal = money(sales.stream().map(this::net).reduce(BigDecimal.ZERO, BigDecimal::add));
		LocalDate today = LocalDate.now(zone);
		List<Sale> todaysSales = salesBetween(today, today);
		BigDecimal todayTotal = money(todaysSales.stream().map(this::net).reduce(BigDecimal.ZERO, BigDecimal::add));
		return new DashboardSummary(
				period.start(),
				period.end(),
				todayTotal,
				todaysSales.size(),
				periodTotal,
				sales.size(),
				productRepository.countByActiveTrue(),
				productRepository.countActiveLowStock());
	}

	@Transactional(readOnly = true)
	public SalesTrendResponse trend(LocalDate from, LocalDate to) {
		DateRange period = range(from, to);
		Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
		Map<LocalDate, Long> counts = new LinkedHashMap<>();
		for (Sale sale : salesBetween(period.start(), period.end())) {
			LocalDate day = sale.getSoldAt().atZone(zone).toLocalDate();
			totals.merge(day, net(sale), BigDecimal::add);
			counts.merge(day, 1L, Long::sum);
		}
		List<SalesTrendPoint> points = new ArrayList<>();
		for (LocalDate day = period.start(); !day.isAfter(period.end()); day = day.plusDays(1)) {
			points.add(new SalesTrendPoint(
					day,
					money(totals.getOrDefault(day, BigDecimal.ZERO)),
					counts.getOrDefault(day, 0L)));
		}
		return new SalesTrendResponse(period.start(), period.end(), points);
	}

	@Transactional(readOnly = true)
	public TopProductsResponse topProducts(LocalDate from, LocalDate to, int limit) {
		DateRange period = range(from, to);
		int safeLimit = limit <= 0 ? 5 : Math.min(limit, 20);
		Map<Long, ProductTotal> totals = new LinkedHashMap<>();
		for (Sale sale : salesBetween(period.start(), period.end())) {
			for (SaleItem item : sale.getItems()) {
				int remaining = item.getQuantity() - item.getReturnedQuantity();
				if (remaining == 0) {
					continue;
				}
				ProductTotal total = totals.computeIfAbsent(item.getProduct().getId(), id -> new ProductTotal(
						id,
						item.getProduct().getSku(),
						item.getProduct().getName()));
				total.quantity += remaining;
				total.revenue = total.revenue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(remaining)));
			}
		}
		List<TopProduct> products = totals.values().stream()
				.sorted(Comparator.comparing((ProductTotal total) -> total.revenue).reversed()
						.thenComparing(Comparator.comparingLong((ProductTotal total) -> total.quantity).reversed()))
				.limit(safeLimit)
				.map(total -> new TopProduct(
						total.productId,
						total.sku,
						total.name,
						total.quantity,
						money(total.revenue)))
				.toList();
		return new TopProductsResponse(period.start(), period.end(), products);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<String> export(LocalDate from, LocalDate to) {
		DateRange period = range(from, to);
		StringBuilder csv = new StringBuilder();
		csv.append("sale_number,sold_at,payment_method,sku,product_name,quantity,unit_price,line_total,sale_total,returned_quantity\n");
		List<Sale> sales = salesBetween(period.start(), period.end());
		sales.sort(Comparator.comparing(Sale::getSoldAt).thenComparing(Sale::getId).reversed());
		for (Sale sale : sales) {
			for (SaleItem item : sale.getItems()) {
				csv.append(cell(sale.getSaleNumber())).append(',')
						.append(cell(sale.getSoldAt().toString())).append(',')
						.append(cell(sale.getPaymentMethod().name())).append(',')
						.append(cell(item.getSku())).append(',')
						.append(cell(item.getProductName())).append(',')
						.append(item.getQuantity()).append(',')
						.append(money(item.getUnitPrice())).append(',')
						.append(money(item.getLineTotal())).append(',')
						.append(money(sale.getTotal())).append(',')
						.append(item.getReturnedQuantity())
						.append('\n');
			}
		}
		String filename = "stockflow-sales-" + period.start() + "-to-" + period.end() + ".csv";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(csv.toString());
	}

	private List<Sale> salesBetween(LocalDate from, LocalDate to) {
		return saleRepository.findWithItemsBetween(startOf(from), startOf(to.plusDays(1)));
	}

	private DateRange range(LocalDate from, LocalDate to) {
		LocalDate today = LocalDate.now(zone);
		LocalDate start = from == null ? today : from;
		LocalDate end = to == null ? today : to;
		if (end.isBefore(start)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The end date is before the start date.");
		}
		if (ChronoUnit.DAYS.between(start, end) > 366) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a period of one year or less.");
		}
		return new DateRange(start, end);
	}

	private Instant startOf(LocalDate date) {
		return date.atStartOfDay(zone).toInstant();
	}

	private BigDecimal net(Sale sale) {
		BigDecimal total = BigDecimal.ZERO;
		for (SaleItem item : sale.getItems()) {
			int remaining = item.getQuantity() - item.getReturnedQuantity();
			total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(remaining)));
		}
		return money(total);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private String cell(String value) {
		String safe = value == null ? "" : value.replace("\"", "\"\"");
		return "\"" + safe + "\"";
	}

	private record DateRange(LocalDate start, LocalDate end) {
	}

	private static final class ProductTotal {
		private final Long productId;
		private final String sku;
		private final String name;
		private long quantity;
		private BigDecimal revenue = BigDecimal.ZERO;

		private ProductTotal(Long productId, String sku, String name) {
			this.productId = productId;
			this.sku = sku;
			this.name = name;
		}
	}
}
