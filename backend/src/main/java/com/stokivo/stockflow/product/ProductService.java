package com.stokivo.stockflow.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.stokivo.stockflow.category.Category;
import com.stokivo.stockflow.category.CategoryRepository;
import com.stokivo.stockflow.common.ApiException;
import com.stokivo.stockflow.common.PageResponse;
import com.stokivo.stockflow.inventory.InventoryService;
import com.stokivo.stockflow.inventory.ReceiptRequest;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final InventoryService inventoryService;

	public ProductService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			InventoryService inventoryService) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.inventoryService = inventoryService;
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> list(
			String search,
			Long categoryId,
			Boolean active,
			Boolean lowStock,
			int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? 20 : Math.min(size, 100);
		var results = productRepository.findAll(
				filtered(search, categoryId, active, lowStock),
				PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
		return PageResponse.from(results.map(this::toResponse));
	}

	@Transactional(readOnly = true)
	public ProductResponse get(Long id) {
		return toResponse(findProduct(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		String sku = request.sku().trim();
		if (productRepository.existsBySkuIgnoreCase(sku)) {
			throw new ApiException(HttpStatus.CONFLICT, "A product with this SKU already exists.");
		}
		Product product = new Product();
		apply(product, sku, request.name(), request.categoryId(), request.sellingPrice(), request.costPrice(),
				request.minimumStockLevel());
		product.setStockQuantity(0);
		product.setActive(true);
		Product saved = productRepository.saveAndFlush(product);
		if (request.initialQuantity() > 0) {
			inventoryService.receive(new ReceiptRequest(saved.getId(), request.initialQuantity(), "Opening stock"));
		}
		return toResponse(findProduct(saved.getId()));
	}

	@Transactional
	public ProductResponse update(Long id, ProductUpdateRequest request) {
		Product product = findProduct(id);
		String sku = request.sku().trim();
		if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
			throw new ApiException(HttpStatus.CONFLICT, "A product with this SKU already exists.");
		}
		apply(product, sku, request.name(), request.categoryId(), request.sellingPrice(), request.costPrice(),
				request.minimumStockLevel());
		return toResponse(product);
	}

	@Transactional
	public ProductResponse changeStatus(Long id, boolean active) {
		Product product = findProduct(id);
		product.setActive(active);
		return toResponse(product);
	}

	private void apply(
			Product product,
			String sku,
			String name,
			Long categoryId,
			BigDecimal sellingPrice,
			BigDecimal costPrice,
			int minimumStockLevel) {
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
		product.setSku(sku);
		product.setName(name.trim());
		product.setCategory(category);
		product.setSellingPrice(money(sellingPrice));
		product.setCostPrice(money(costPrice));
		product.setMinimumStockLevel(minimumStockLevel);
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
	}

	private ProductResponse toResponse(Product product) {
		int stock = product.getStockQuantity();
		int minimum = product.getMinimumStockLevel();
		return new ProductResponse(
				product.getId(),
				product.getSku(),
				product.getName(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getSellingPrice(),
				product.getCostPrice(),
				stock,
				minimum,
				product.isActive(),
				stock <= minimum,
				stock == 0);
	}

	private BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private Specification<Product> filtered(String search, Long categoryId, Boolean active, Boolean lowStock) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (search != null && !search.isBlank()) {
				String like = "%" + search.trim().toLowerCase() + "%";
				predicates.add(builder.or(
						builder.like(builder.lower(root.get("name")), like),
						builder.like(builder.lower(root.get("sku")), like)));
			}
			if (categoryId != null) {
				predicates.add(builder.equal(root.get("category").get("id"), categoryId));
			}
			if (active != null) {
				predicates.add(builder.equal(root.get("active"), active));
			}
			if (Boolean.TRUE.equals(lowStock)) {
				predicates.add(builder.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minimumStockLevel")));
			}
			if (predicates.isEmpty()) {
				return builder.conjunction();
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
