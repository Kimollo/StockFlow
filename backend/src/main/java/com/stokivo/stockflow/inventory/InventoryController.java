package com.stokivo.stockflow.inventory;

import com.stokivo.stockflow.common.PageResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/receipts")
	@ResponseStatus(HttpStatus.CREATED)
	public StockMovementResponse receive(@Valid @RequestBody ReceiptRequest request) {
		return inventoryService.receive(request);
	}

	@PostMapping("/adjustments")
	@ResponseStatus(HttpStatus.CREATED)
	public StockMovementResponse adjust(@Valid @RequestBody AdjustmentRequest request) {
		return inventoryService.adjust(request);
	}

	@GetMapping("/movements")
	public PageResponse<StockMovementResponse> movements(
			@RequestParam(required = false) Long productId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return inventoryService.movements(productId, page, size);
	}
}
