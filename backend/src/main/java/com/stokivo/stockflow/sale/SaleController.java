package com.stokivo.stockflow.sale;

import com.stokivo.stockflow.common.PageResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@GetMapping
	public PageResponse<SaleResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return saleService.list(page, size);
	}

	@GetMapping("/{id}")
	public SaleResponse get(@PathVariable Long id) {
		return saleService.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SaleResponse checkout(@Valid @RequestBody SaleRequest request) {
		return saleService.checkout(request);
	}

	@PostMapping("/{id}/returns")
	public SaleResponse returnItems(@PathVariable Long id, @Valid @RequestBody ReturnRequest request) {
		return saleService.returnItems(id, request);
	}
}
