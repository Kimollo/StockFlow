package com.stokivo.stockflow.sale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.common.PageResponse;
import com.stokivo.stockflow.inventory.MovementType;
import com.stokivo.stockflow.inventory.StockMovement;
import com.stokivo.stockflow.inventory.StockMovementRepository;
import com.stokivo.stockflow.product.Product;
import com.stokivo.stockflow.product.ProductRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

	private static final int MAX_QUANTITY = 1_000_000;

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final StockMovementRepository movementRepository;
	private final EntityManager entityManager;

	public SaleService(
			SaleRepository saleRepository,
			ProductRepository productRepository,
			StockMovementRepository movementRepository,
			EntityManager entityManager) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.movementRepository = movementRepository;
		this.entityManager = entityManager;
	}

	@Transactional
	public SaleResponse checkout(SaleRequest request) {
		Map<Long, Integer> quantities = quantitiesByProduct(request.items());
		List<Product> products = lockProducts(quantities);

		Sale sale = new Sale();
		sale.setSaleNumber(nextSaleNumber());
		sale.setPaymentMethod(request.paymentMethod());
		sale.setStatus(SaleStatus.COMPLETED);
		sale.setSoldAt(Instant.now());

		BigDecimal subtotal = money(BigDecimal.ZERO);
		for (Product product : products) {
			int quantity = quantities.get(product.getId());
			BigDecimal unitPrice = money(product.getSellingPrice());
			BigDecimal lineTotal = money(unitPrice.multiply(BigDecimal.valueOf(quantity)));
			SaleItem item = new SaleItem();
			item.setProduct(product);
			item.setSku(product.getSku());
			item.setProductName(product.getName());
			item.setQuantity(quantity);
			item.setUnitPrice(unitPrice);
			item.setLineTotal(lineTotal);
			sale.addItem(item);
			subtotal = subtotal.add(lineTotal);
		}
		sale.setSubtotal(subtotal);
		sale.setTotal(subtotal);
		Sale saved = saleRepository.saveAndFlush(sale);

		for (Product product : products) {
			int quantity = quantities.get(product.getId());
			int updated = product.getStockQuantity() - quantity;
			product.setStockQuantity(updated);
			StockMovement movement = new StockMovement();
			movement.setProduct(product);
			movement.setSale(saved);
			movement.setMovementType(MovementType.SALE);
			movement.setQuantityChange(-quantity);
			movement.setQuantityAfter(updated);
			movement.setReason("Sale " + saved.getSaleNumber());
			movementRepository.save(movement);
		}
		movementRepository.flush();
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public PageResponse<SaleResponse> list(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? 20 : Math.min(size, 100);
		var results = saleRepository.findAll(PageRequest.of(
				safePage,
				safeSize,
				Sort.by(Sort.Order.desc("soldAt"), Sort.Order.desc("id"))));
		return PageResponse.from(results.map(this::toResponse));
	}

	@Transactional(readOnly = true)
	public SaleResponse get(Long id) {
		return toResponse(saleRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sale not found.")));
	}

	private List<Product> lockProducts(Map<Long, Integer> quantities) {
		List<Long> productIds = quantities.keySet().stream().sorted().toList();
		List<Product> products = new ArrayList<>();
		for (Long productId : productIds) {
			Product product = productRepository.findByIdForUpdate(productId)
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
			if (!product.isActive()) {
				throw new ApiException(HttpStatus.CONFLICT, product.getName() + " is not available for sale.");
			}
			if (product.getStockQuantity() < quantities.get(productId)) {
				throw new ApiException(HttpStatus.CONFLICT, "Not enough stock for " + product.getName() + ".");
			}
			products.add(product);
		}
		return products;
	}

	private Map<Long, Integer> quantitiesByProduct(List<SaleLineRequest> lines) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (SaleLineRequest line : lines) {
			quantities.merge(line.productId(), line.quantity(), this::addQuantity);
		}
		return quantities;
	}

	private int addQuantity(int current, int added) {
		int total;
		try {
			total = Math.addExact(current, added);
		} catch (ArithmeticException exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Quantity is too large.");
		}
		if (total > MAX_QUANTITY) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Quantity is too large.");
		}
		return total;
	}

	private String nextSaleNumber() {
		Number value = (Number) entityManager
				.createNativeQuery("select nextval('sales_number_seq')")
				.getSingleResult();
		return "S-%06d".formatted(value.longValue());
	}

	private SaleResponse toResponse(Sale sale) {
		List<SaleItemResponse> items = sale.getItems().stream()
				.map(item -> new SaleItemResponse(
						item.getId(),
						item.getProduct().getId(),
						item.getSku(),
						item.getProductName(),
						item.getQuantity(),
						item.getUnitPrice(),
						item.getLineTotal()))
				.toList();
		return new SaleResponse(
				sale.getId(),
				sale.getSaleNumber(),
				sale.getPaymentMethod().name(),
				sale.getStatus().name(),
				sale.getSubtotal(),
				sale.getTotal(),
				sale.getSoldAt(),
				items);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}
}
