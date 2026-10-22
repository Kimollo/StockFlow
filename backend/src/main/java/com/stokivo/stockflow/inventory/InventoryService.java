package com.stokivo.stockflow.inventory;

import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.common.PageResponse;
import com.stokivo.stockflow.product.Product;
import com.stokivo.stockflow.product.ProductRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

	private final ProductRepository productRepository;
	private final StockMovementRepository movementRepository;

	public InventoryService(ProductRepository productRepository, StockMovementRepository movementRepository) {
		this.productRepository = productRepository;
		this.movementRepository = movementRepository;
	}

	@Transactional
	public StockMovementResponse receive(ReceiptRequest request) {
		Product product = lockedProduct(request.productId());
		int updated = product.getStockQuantity() + request.quantity();
		product.setStockQuantity(updated);
		StockMovement movement = record(product, MovementType.RECEIPT, request.quantity(), updated, clean(request.note()));
		return toResponse(movementRepository.saveAndFlush(movement));
	}

	@Transactional
	public StockMovementResponse adjust(AdjustmentRequest request) {
		Product product = lockedProduct(request.productId());
		int change = request.direction() == StockDirection.INCREASE ? request.quantity() : -request.quantity();
		int updated = product.getStockQuantity() + change;
		if (updated < 0) {
			throw new ApiException(HttpStatus.CONFLICT, "Stock cannot become negative.");
		}
		product.setStockQuantity(updated);
		StockMovement movement = record(product, MovementType.ADJUSTMENT, change, updated, request.reason().trim());
		return toResponse(movementRepository.saveAndFlush(movement));
	}

	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> movements(Long productId, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), boundedSize(size));
		var results = productId == null
				? movementRepository.findAllByOrderByCreatedAtDesc(pageRequest)
				: movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageRequest);
		return PageResponse.from(results.map(this::toResponse));
	}

	private Product lockedProduct(Long productId) {
		return productRepository.findByIdForUpdate(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
	}

	private StockMovement record(Product product, MovementType type, int change, int stockAfter, String reason) {
		StockMovement movement = new StockMovement();
		movement.setProduct(product);
		movement.setMovementType(type);
		movement.setQuantityChange(change);
		movement.setQuantityAfter(stockAfter);
		movement.setReason(reason);
		return movement;
	}

	private StockMovementResponse toResponse(StockMovement movement) {
		Product product = movement.getProduct();
		return new StockMovementResponse(
				movement.getId(),
				product.getId(),
				product.getSku(),
				product.getName(),
				movement.getMovementType().name(),
				movement.getQuantityChange(),
				movement.getQuantityAfter(),
				movement.getReason(),
				movement.getSale() == null ? null : movement.getSale().getId(),
				movement.getCreatedAt());
	}

	private String clean(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private int boundedSize(int size) {
		if (size <= 0) {
			return 20;
		}
		return Math.min(size, 100);
	}
}
