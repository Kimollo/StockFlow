package com.stokivo.stockflow.category;

import java.util.List;

import com.stokivo.stockflow.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> list() {
		return categoryRepository.findAll().stream()
				.sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().trim();
		if (categoryRepository.existsByNameIgnoreCase(name)) {
			throw new ApiException(HttpStatus.CONFLICT, "A category with this name already exists.");
		}
		Category category = new Category();
		category.setName(name);
		return toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
		String name = request.name().trim();
		if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ApiException(HttpStatus.CONFLICT, "A category with this name already exists.");
		}
		category.setName(name);
		return toResponse(category);
	}

	private CategoryResponse toResponse(Category category) {
		return new CategoryResponse(category.getId(), category.getName());
	}
}
